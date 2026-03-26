package com.translator.translator_keyboard

import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.plugin.common.MethodChannel
import android.inputmethodservice.InputMethodService

private const val TAG = "TranslatorIME"

class TranslatorInputMethodService : InputMethodService() {

    private var flutterEngine: FlutterEngine? = null
    private var methodChannel: MethodChannel? = null
    private var flutterView: FlutterView? = null
    private var cachedView: FrameLayout? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")

        try {
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
        flutterEngine = engine
        Log.d(TAG, "Flutter engine initialized and Dart entrypoint executed")

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
            Log.e(TAG, "FlutterEngine is null in onCreateInputView!")
            return FrameLayout(this)
        }

        // Reuse cached view if available — avoids recreating the render surface
        cachedView?.let { container ->
            // Detach from old parent if Android recycled it
            (container.parent as? FrameLayout)?.removeView(container)
            engine.lifecycleChannel.appIsResumed()
            Log.d(TAG, "Reusing cached FlutterView")
            return container
        }

        try {
            val heightPx = (380 * resources.displayMetrics.density).toInt()

            val container = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heightPx
                )
                // Solid background so we never see through to white IME window
                setBackgroundColor(0xFFF0F0F3.toInt())
            }

            // FlutterTextureView composites into the normal view hierarchy.
            // Unlike SurfaceView, it doesn't create a separate window surface,
            // so there are no z-ordering issues in IME services.
            val textureView = FlutterTextureView(this)
            val fView = FlutterView(this, textureView)
            fView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            fView.attachToFlutterEngine(engine)
            flutterView = fView

            container.addView(fView)
            cachedView = container

            engine.lifecycleChannel.appIsResumed()
            Log.d(TAG, "FlutterView created with TextureView, height=${heightPx}px")

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
        Log.d(TAG, "onFinishInputView called")
        flutterEngine?.lifecycleChannel?.appIsInactive()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        flutterView?.detachFromFlutterEngine()
        flutterView = null
        cachedView = null
        flutterEngine?.destroy()
        flutterEngine = null
        super.onDestroy()
    }
}
