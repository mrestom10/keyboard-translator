package com.translator.translator_keyboard

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.plugin.common.MethodChannel
import android.inputmethodservice.InputMethodService

class TranslatorInputMethodService : InputMethodService() {

    private lateinit var flutterEngine: FlutterEngine
    private lateinit var methodChannel: MethodChannel
    private var flutterView: FlutterView? = null

    override fun onCreate() {
        super.onCreate()

        // Ensure Flutter is initialized
        io.flutter.FlutterInjector.instance().flutterLoader().startInitialization(this)
        io.flutter.FlutterInjector.instance().flutterLoader().ensureInitializationComplete(this, null)

        initFlutterEngine()
    }

    private fun initFlutterEngine() {
        flutterEngine = FlutterEngine(this).apply {
            dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint(
                    io.flutter.FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                    "keyboardMain"
                )
            )
            FlutterEngineCache.getInstance().put("keyboard_engine", this)
        }

        // MethodChannel to receive inject/dismiss commands from Flutter
        methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "translator_keyboard/actions"
        ).apply {
            setMethodCallHandler { call, result ->
                when (call.method) {
                    "injectText" -> {
                        val text = call.argument<String>("text") ?: ""
                        currentInputConnection?.commitText(text, 1)
                        result.success(null)
                    }
                    "dismissKeyboard" -> {
                        requestHideSelf(0)
                        result.success(null)
                    }
                    "deleteLastChar" -> {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        // Detach previous FlutterView if it exists
        flutterView?.detachFromFlutterEngine()

        // Create a container with explicit keyboard height
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (300 * resources.displayMetrics.density).toInt() // 300dp height
            )
        }

        // Create FlutterView with TextureView rendering mode
        // SurfaceView (default) creates a separate window surface that causes
        // black screen in IME services on Android 15+
        val textureView = FlutterTextureView(this)
        flutterView = FlutterView(this, textureView).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            attachToFlutterEngine(flutterEngine)
        }

        container.addView(flutterView)

        // Tell Flutter engine the app is in foreground so it starts rendering frames
        flutterEngine.lifecycleChannel.appIsResumed()

        return container
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Ensure Flutter keeps rendering when keyboard reappears
        flutterEngine.lifecycleChannel.appIsResumed()
        // Notify Flutter that the keyboard became active
        methodChannel.invokeMethod("onKeyboardShown", null)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Pause rendering when keyboard is hidden to save resources
        flutterEngine.lifecycleChannel.appIsInactive()
    }

    override fun onDestroy() {
        flutterView?.detachFromFlutterEngine()
        flutterEngine.destroy()
        super.onDestroy()
    }
}
