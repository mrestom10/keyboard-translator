package com.translator.translator_keyboard

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import io.flutter.embedding.engine.FlutterEngine
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
    private var cachedContainer: FrameLayout? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")

        try {
            val loader = io.flutter.FlutterInjector.instance().flutterLoader()
            if (!loader.initialized()) {
                loader.startInitialization(applicationContext)
                loader.ensureInitializationComplete(applicationContext, null)
            }
            Log.d(TAG, "Flutter loader ready")

            initFlutterEngine()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
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
        Log.d(TAG, "Engine started, Dart entrypoint executing")

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
            Log.e(TAG, "Engine is null!")
            return FrameLayout(this)
        }

        // Return cached view if we already built one
        cachedContainer?.let { container ->
            Log.d(TAG, "Returning cached container")
            (container.parent as? FrameLayout)?.removeView(container)
            engine.lifecycleChannel.appIsResumed()
            return container
        }

        try {
            // Detach old view
            flutterView?.detachFromFlutterEngine()
            flutterView = null

            val heightPx = (380 * resources.displayMetrics.density).toInt()

            val container = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    heightPx
                )
            }

            // FlutterSurfaceView with renderTransparently=true:
            // - Calls setZOrderOnTop(true) which places the surface above the IME window
            // - This is the ONLY mode that renders in Android 14/15 IME services
            val surfaceView = FlutterSurfaceView(this, true)
            val fView = FlutterView(this, surfaceView)
            fView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            fView.attachToFlutterEngine(engine)
            flutterView = fView

            container.addView(fView)
            cachedContainer = container

            // Signal resumed lifecycle — schedule slightly after layout to ensure surface is ready
            engine.lifecycleChannel.appIsResumed()
            handler.postDelayed({
                engine.lifecycleChannel.appIsResumed()
                Log.d(TAG, "Sent delayed appIsResumed")
            }, 100)

            Log.d(TAG, "FlutterView created, height=${heightPx}px")
            return container
        } catch (e: Exception) {
            Log.e(TAG, "onCreateInputView error", e)
            return FrameLayout(this)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "onStartInputView, restarting=$restarting")
        flutterEngine?.lifecycleChannel?.appIsResumed()
        methodChannel?.invokeMethod("onKeyboardShown", null)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView")
        flutterEngine?.lifecycleChannel?.appIsInactive()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        handler.removeCallbacksAndMessages(null)
        flutterView?.detachFromFlutterEngine()
        flutterView = null
        cachedContainer = null
        flutterEngine?.destroy()
        flutterEngine = null
        super.onDestroy()
    }
}
