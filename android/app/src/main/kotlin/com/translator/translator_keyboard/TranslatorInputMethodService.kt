package com.translator.translator_keyboard

import android.view.View
import android.view.inputmethod.EditorInfo
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.android.FlutterView
import io.flutter.plugin.common.MethodChannel
import android.inputmethodservice.InputMethodService

class TranslatorInputMethodService : InputMethodService() {

    private lateinit var flutterEngine: FlutterEngine
    private lateinit var methodChannel: MethodChannel

    override fun onCreate() {
        super.onCreate()
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
        // Embed Flutter view inside the IME
        return FlutterView(this).apply {
            attachToFlutterEngine(flutterEngine)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Notify Flutter that the keyboard became active
        methodChannel.invokeMethod("onKeyboardShown", null)
    }

    override fun onDestroy() {
        flutterEngine.destroy()
        super.onDestroy()
    }
}
