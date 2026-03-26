package com.translator.translator_keyboard

import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.android.FlutterSurfaceView
import io.flutter.plugin.common.MethodChannel
import android.inputmethodservice.InputMethodService

private const val TAG = "TranslatorIME"

class TranslatorInputMethodService : InputMethodService() {

    private var flutterEngine: FlutterEngine? = null
    private var methodChannel: MethodChannel? = null
    private var flutterView: FlutterView? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")

        try {
            // Ensure Flutter is initialized
            val loader = io.flutter.FlutterInjector.instance().flutterLoader()
            if (!loader.initialized()) {
                loader.startInitialization(applicationContext)
                loader.ensureInitializationComplete(applicationContext, null)
            }
            Log.d(TAG, "Flutter loader initialized")

            initFlutterEngine()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    private fun initFlutterEngine() {
        val engine = FlutterEngine(applicationContext)
        engine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint(
                io.flutter.FlutterInjector.instance().flutterLoader().findAppBundlePath(),
                "keyboardMain"
            )
        )
        FlutterEngineCache.getInstance().put("keyboard_engine", engine)
        flutterEngine = engine
        Log.d(TAG, "Flutter engine initialized")

        // MethodChannel to receive inject/dismiss commands from Flutter
        methodChannel = MethodChannel(
            engine.dartExecutor.binaryMessenger,
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
        Log.d(TAG, "onCreateInputView called")

        val engine = flutterEngine
        if (engine == null) {
            Log.e(TAG, "FlutterEngine is null!")
            return FrameLayout(this)
        }

        try {
            // Detach previous FlutterView if it exists
            flutterView?.detachFromFlutterEngine()
            flutterView = null

            // Full custom keyboard height: translation bar (~120dp) + 4 key rows (~180dp)
            val heightPx = (380 * resources.displayMetrics.density).toInt()

            // Create a container with explicit keyboard height
            val container = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heightPx
                )
            }

            // Use FlutterSurfaceView with renderTransparently=true
            // This calls setZOrderOnTop(true) internally, which prevents
            // the black screen issue in IME services on Android 14/15
            val surfaceView = FlutterSurfaceView(this, true)
            val fView = FlutterView(this, surfaceView)
            fView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            fView.attachToFlutterEngine(engine)
            flutterView = fView

            container.addView(fView)

            // Tell Flutter engine the app is resumed so it renders frames
            engine.lifecycleChannel.appIsResumed()
            Log.d(TAG, "FlutterView created and attached, height=${heightPx}px")

            return container
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateInputView", e)
            return FrameLayout(this)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "onStartInputView called, restarting=$restarting")
        flutterEngine?.lifecycleChannel?.appIsResumed()
        methodChannel?.invokeMethod("onKeyboardShown", null)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        flutterEngine?.lifecycleChannel?.appIsInactive()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        flutterView?.detachFromFlutterEngine()
        flutterView = null
        flutterEngine?.destroy()
        flutterEngine = null
        super.onDestroy()
    }
}
