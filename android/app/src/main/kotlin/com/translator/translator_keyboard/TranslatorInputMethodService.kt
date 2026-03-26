package com.translator.translator_keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import android.inputmethodservice.InputMethodService
import java.util.Timer
import java.util.TimerTask

private const val TAG = "TranslatorIME"

// ── Language Data ──────────────────────────────────────────
data class LangInfo(val code: String, val name: String, val flag: String)

private val LANGUAGES = listOf(
    LangInfo("en", "English", "\uD83C\uDDEC\uD83C\uDDE7"),
    LangInfo("ar", "Arabic", "\uD83C\uDDF8\uD83C\uDDE6"),
    LangInfo("fr", "French", "\uD83C\uDDEB\uD83C\uDDF7"),
)

// ── Key Types ──────────────────────────────────────────────
enum class KeyType { CHAR, BACKSPACE, SHIFT, SPACE, ENTER, TO_NUM, TO_ALPHA, TO_SYM }

data class KeyData(
    val label: String,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f,
)

// ── Layouts ────────────────────────────────────────────────
private val QWERTY = listOf(
    listOf(KeyData("q"), KeyData("w"), KeyData("e"), KeyData("r"), KeyData("t"), KeyData("y"), KeyData("u"), KeyData("i"), KeyData("o"), KeyData("p")),
    listOf(KeyData("a"), KeyData("s"), KeyData("d"), KeyData("f"), KeyData("g"), KeyData("h"), KeyData("j"), KeyData("k"), KeyData("l")),
    listOf(KeyData("⇧", KeyType.SHIFT, 1.5f), KeyData("z"), KeyData("x"), KeyData("c"), KeyData("v"), KeyData("b"), KeyData("n"), KeyData("m"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.5f), KeyData(","), KeyData(" ", KeyType.SPACE, 4f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.5f)),
)

private val NUMBERS = listOf(
    listOf(KeyData("1"), KeyData("2"), KeyData("3"), KeyData("4"), KeyData("5"), KeyData("6"), KeyData("7"), KeyData("8"), KeyData("9"), KeyData("0")),
    listOf(KeyData("@"), KeyData("#"), KeyData("$"), KeyData("%"), KeyData("&"), KeyData("-"), KeyData("+"), KeyData("("), KeyData(")")),
    listOf(KeyData("#+=", KeyType.TO_SYM, 1.5f), KeyData("*"), KeyData("\""), KeyData("'"), KeyData(":"), KeyData(";"), KeyData("!"), KeyData("?"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("ABC", KeyType.TO_ALPHA, 1.5f), KeyData(","), KeyData(" ", KeyType.SPACE, 4f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.5f)),
)

private val SYMBOLS = listOf(
    listOf(KeyData("~"), KeyData("`"), KeyData("|"), KeyData("•"), KeyData("√"), KeyData("π"), KeyData("÷"), KeyData("×"), KeyData("{"), KeyData("}")),
    listOf(KeyData("£"), KeyData("¢"), KeyData("€"), KeyData("¥"), KeyData("^"), KeyData("°"), KeyData("="), KeyData("["), KeyData("]")),
    listOf(KeyData("123", KeyType.TO_NUM, 1.5f), KeyData("\\"), KeyData("/"), KeyData("_"), KeyData("<"), KeyData(">"), KeyData("…"), KeyData("¿"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("ABC", KeyType.TO_ALPHA, 1.5f), KeyData(","), KeyData(" ", KeyType.SPACE, 4f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.5f)),
)

// ── Colors ─────────────────────────────────────────────────
private object KB {
    val BG = Color.parseColor("#D2D3D9")
    val KEY_BG = Color.parseColor("#FFFFFF")
    val KEY_PRESSED = Color.parseColor("#C8C9CE")
    val SPECIAL_BG = Color.parseColor("#AEB2BF")
    val SPECIAL_PRESSED = Color.parseColor("#9B9FAB")
    val ENTER_BG = Color.parseColor("#4A90D9")
    val ENTER_PRESSED = Color.parseColor("#3A7BC8")
    val TEXT = Color.parseColor("#1C1B1F")
    val TEXT_LIGHT = Color.parseColor("#636366")
    val SURFACE = Color.parseColor("#F2F2F7")
    val PRIMARY = Color.parseColor("#4A90D9")
    val SEND_BG = Color.parseColor("#E8F0FE")
    val DIVIDER = Color.parseColor("#C6C6C8")
    val ERROR = Color.parseColor("#FF3B30")
}

// ── IME Service ────────────────────────────────────────────
class TranslatorInputMethodService : InputMethodService() {

    private var flutterEngine: FlutterEngine? = null
    private var translationChannel: MethodChannel? = null
    private var cachedView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    // State
    private var typedBuffer = StringBuilder()
    private var isShifted = false
    private var currentLayout = QWERTY
    private var targetLangIndex = 1  // Arabic by default
    private var detectedLangCode: String? = null
    private var translatedText: String? = null
    private var isTranslating = false
    private var translationError: String? = null

    // UI refs
    private var keyboardContainer: LinearLayout? = null
    private var inputPreview: TextView? = null
    private var translationView: TextView? = null
    private var sendButton: TextView? = null
    private var sourceLangView: TextView? = null
    private var targetLangView: TextView? = null
    private var translationStrip: LinearLayout? = null

    // Debounce
    private var debounceTimer: Timer? = null

    // Backspace repeat
    private var backspaceTimer: Timer? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        try {
            val loader = io.flutter.FlutterInjector.instance().flutterLoader()
            if (!loader.initialized()) {
                loader.startInitialization(applicationContext)
                loader.ensureInitializationComplete(applicationContext, null)
            }
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

        translationChannel = MethodChannel(
            engine.dartExecutor.binaryMessenger,
            "translator_keyboard/translation"
        )
        Log.d(TAG, "Flutter engine + translation channel ready")
    }

    // ── View Creation ──────────────────────────────────────

    override fun onCreateInputView(): View {
        Log.d(TAG, "onCreateInputView")
        cachedView?.let { return it }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KB.BG)
        }

        // Translation strip
        root.addView(buildTranslationStrip())

        // Keyboard rows
        val kbContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }
        buildKeyboardRows(kbContainer)
        keyboardContainer = kbContainer
        root.addView(kbContainer)

        cachedView = root
        return root
    }

    private fun buildTranslationStrip(): View {
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KB.SURFACE)
        }
        translationStrip = strip

        // ── Language Bar ──
        val langBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }

        // Source lang chip
        sourceLangView = TextView(this).apply {
            text = "Auto"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.TEXT_LIGHT)
            background = makeRoundRect(KB.KEY_PRESSED, dp(6).toFloat())
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        langBar.addView(sourceLangView)

        // Swap arrow
        val swapBtn = TextView(this).apply {
            text = " ⇄ "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(KB.PRIMARY)
            gravity = Gravity.CENTER
            setOnClickListener { /* TODO: swap */ }
        }
        langBar.addView(swapBtn)

        // Target lang chip
        targetLangView = TextView(this).apply {
            updateTargetLangDisplay()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.PRIMARY)
            background = makeRoundRect(KB.SEND_BG, dp(6).toFloat())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { cycleTargetLanguage() }
        }
        langBar.addView(targetLangView)

        // Spacer
        langBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })

        // Clear button
        val clearBtn = TextView(this).apply {
            text = "✕"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(KB.ERROR)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener { clearAll() }
        }
        langBar.addView(clearBtn)

        strip.addView(langBar)

        // ── Input Preview ──
        inputPreview = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(KB.TEXT)
            maxLines = 1
            setPadding(dp(12), dp(2), dp(12), dp(2))
            visibility = View.GONE
        }
        strip.addView(inputPreview)

        // ── Divider ──
        strip.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
            setBackgroundColor(KB.DIVIDER)
        })

        // ── Translation Row ──
        val transRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(8), dp(4))
            visibility = View.GONE
        }

        translationView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(KB.PRIMARY)
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        transRow.addView(translationView)

        sendButton = TextView(this).apply {
            text = "  Send ▶  "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.PRIMARY)
            setTypeface(null, Typeface.BOLD)
            background = makeRoundRect(KB.SEND_BG, dp(14).toFloat())
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { sendTranslation() }
        }
        transRow.addView(sendButton)

        strip.addView(transRow)
        this.translationStrip = strip

        // Divider below strip
        strip.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
            setBackgroundColor(KB.DIVIDER)
        })

        return strip
    }

    private fun buildKeyboardRows(container: LinearLayout) {
        container.removeAllViews()
        for ((rowIndex, row) in currentLayout.withIndex()) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply { bottomMargin = dp(2) }
            }

            // Add side padding for row 2 (a-l) to center it
            if (currentLayout === QWERTY && rowIndex == 1) {
                rowView.setPadding(dp(16), 0, dp(16), 0)
            }

            for (key in row) {
                rowView.addView(buildKey(key))
            }
            container.addView(rowView)
        }
    }

    private fun buildKey(key: KeyData): View {
        val isSpecial = key.type != KeyType.CHAR && key.type != KeyType.SPACE
        val isEnter = key.type == KeyType.ENTER
        val isShiftKey = key.type == KeyType.SHIFT

        val bgNormal = when {
            isEnter -> KB.ENTER_BG
            isSpecial -> KB.SPECIAL_BG
            else -> KB.KEY_BG
        }
        val bgPressed = when {
            isEnter -> KB.ENTER_PRESSED
            isSpecial -> KB.SPECIAL_PRESSED
            else -> KB.KEY_PRESSED
        }

        val textColor = when {
            isEnter -> Color.WHITE
            isShiftKey && isShifted -> KB.PRIMARY
            else -> KB.TEXT
        }

        val displayLabel = when (key.type) {
            KeyType.SPACE -> "space"
            KeyType.BACKSPACE -> "⌫"
            KeyType.SHIFT -> if (isShifted) "⇧" else "⇧"
            KeyType.ENTER -> "↵"
            KeyType.CHAR -> if (isShifted && currentLayout === QWERTY) key.label.uppercase() else key.label
            else -> key.label
        }

        val tv = TextView(this).apply {
            text = displayLabel
            setTextSize(TypedValue.COMPLEX_UNIT_SP, when {
                key.type == KeyType.SPACE -> 13f
                isSpecial -> 14f
                else -> 18f
            })
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = makeKeyBackground(bgNormal, bgPressed)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, key.weight).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            isClickable = true
            isFocusable = true
        }

        // Handle touch for backspace repeat
        if (key.type == KeyType.BACKSPACE) {
            tv.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        handleKeyPress(key)
                        startBackspaceRepeat()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        stopBackspaceRepeat()
                        true
                    }
                    else -> false
                }
            }
        } else {
            tv.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                handleKeyPress(key)
            }
        }

        return tv
    }

    // ── Key Press Handling ──────────────────────────────────

    private fun handleKeyPress(key: KeyData) {
        when (key.type) {
            KeyType.CHAR -> {
                val ch = if (isShifted && currentLayout === QWERTY)
                    key.label.uppercase() else key.label
                typeChar(ch)
                if (isShifted) {
                    isShifted = false
                    refreshKeyboard()
                }
            }
            KeyType.SPACE -> typeChar(" ")
            KeyType.BACKSPACE -> doBackspace()
            KeyType.ENTER -> {
                currentInputConnection?.commitText("\n", 1)
            }
            KeyType.SHIFT -> {
                isShifted = !isShifted
                refreshKeyboard()
            }
            KeyType.TO_NUM -> {
                currentLayout = NUMBERS
                isShifted = false
                refreshKeyboard()
            }
            KeyType.TO_ALPHA -> {
                currentLayout = QWERTY
                isShifted = false
                refreshKeyboard()
            }
            KeyType.TO_SYM -> {
                currentLayout = SYMBOLS
                isShifted = false
                refreshKeyboard()
            }
        }
    }

    private fun typeChar(ch: String) {
        currentInputConnection?.commitText(ch, 1)
        typedBuffer.append(ch)
        updateInputPreview()
        requestTranslationDebounced()
    }

    private fun doBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (typedBuffer.isNotEmpty()) {
            typedBuffer.deleteCharAt(typedBuffer.length - 1)
        }
        updateInputPreview()
        if (typedBuffer.isEmpty()) {
            clearTranslation()
        } else {
            requestTranslationDebounced()
        }
    }

    private fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        backspaceTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post { doBackspace() }
                }
            }, 400, 60)
        }
    }

    private fun stopBackspaceRepeat() {
        backspaceTimer?.cancel()
        backspaceTimer = null
    }

    // ── Translation ────────────────────────────────────────

    private fun requestTranslationDebounced() {
        debounceTimer?.cancel()
        val text = typedBuffer.toString().trim()
        if (text.length < 2) return

        debounceTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post { requestTranslation(text) }
                }
            }, 400)
        }
    }

    private fun requestTranslation(text: String) {
        val channel = translationChannel ?: return
        val targetLang = LANGUAGES[targetLangIndex].code

        isTranslating = true
        translationError = null
        updateTranslationUI()

        channel.invokeMethod("translate", mapOf(
            "text" to text,
            "targetLang" to targetLang,
        ), object : MethodChannel.Result {
            override fun success(result: Any?) {
                handler.post {
                    isTranslating = false
                    val map = result as? Map<*, *>
                    if (map != null) {
                        val error = map["error"] as? String
                        if (error != null) {
                            translationError = error
                            translatedText = null
                        } else {
                            translatedText = map["translatedText"] as? String
                            detectedLangCode = map["detectedLang"] as? String
                            translationError = null
                        }
                    }
                    updateTranslationUI()
                    updateSourceLangDisplay()
                }
            }

            override fun error(code: String, msg: String?, details: Any?) {
                handler.post {
                    isTranslating = false
                    translationError = msg ?: "Translation failed"
                    updateTranslationUI()
                }
            }

            override fun notImplemented() {
                handler.post {
                    isTranslating = false
                    translationError = "Translation service not ready"
                    updateTranslationUI()
                }
            }
        })
    }

    private fun sendTranslation() {
        val translation = translatedText ?: return
        val conn = currentInputConnection ?: return

        // Delete what was typed
        val len = typedBuffer.length
        conn.deleteSurroundingText(len, 0)

        // Insert translation
        conn.commitText(translation, 1)
        clearAll()
    }

    private fun clearAll() {
        typedBuffer.clear()
        translatedText = null
        translationError = null
        detectedLangCode = null
        isTranslating = false
        updateInputPreview()
        clearTranslation()
        updateSourceLangDisplay()
    }

    private fun clearTranslation() {
        translatedText = null
        translationError = null
        isTranslating = false
        updateTranslationUI()
    }

    // ── UI Updates ─────────────────────────────────────────

    private fun updateInputPreview() {
        val text = typedBuffer.toString()
        inputPreview?.apply {
            if (text.isEmpty()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                this.text = text
            }
        }
    }

    private fun updateTranslationUI() {
        val transRow = translationStrip?.getChildAt(3) as? LinearLayout ?: return

        when {
            typedBuffer.isEmpty() -> {
                transRow.visibility = View.GONE
            }
            isTranslating -> {
                transRow.visibility = View.VISIBLE
                translationView?.text = "Translating…"
                translationView?.setTextColor(KB.TEXT_LIGHT)
                translationView?.setTypeface(null, Typeface.ITALIC)
                sendButton?.visibility = View.GONE
            }
            translationError != null -> {
                transRow.visibility = View.VISIBLE
                translationView?.text = translationError
                translationView?.setTextColor(KB.ERROR)
                translationView?.setTypeface(null, Typeface.NORMAL)
                sendButton?.visibility = View.GONE
            }
            translatedText != null -> {
                transRow.visibility = View.VISIBLE
                translationView?.text = translatedText
                translationView?.setTextColor(KB.PRIMARY)
                translationView?.setTypeface(null, Typeface.BOLD)
                sendButton?.visibility = View.VISIBLE
            }
            else -> {
                transRow.visibility = View.GONE
            }
        }
    }

    private fun updateSourceLangDisplay() {
        val lang = LANGUAGES.find { it.code == detectedLangCode }
        sourceLangView?.text = if (lang != null) "${lang.flag} ${lang.name}" else "Auto"
    }

    private fun TextView.updateTargetLangDisplay() {
        val lang = LANGUAGES[targetLangIndex]
        text = "${lang.flag} ${lang.name} ▾"
    }

    private fun cycleTargetLanguage() {
        targetLangIndex = (targetLangIndex + 1) % LANGUAGES.size
        targetLangView?.updateTargetLangDisplay()
        // Re-translate with new target
        if (typedBuffer.isNotEmpty()) {
            requestTranslation(typedBuffer.toString())
        }
    }

    private fun refreshKeyboard() {
        keyboardContainer?.let { buildKeyboardRows(it) }
    }

    // ── Lifecycle ──────────────────────────────────────────

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d(TAG, "onStartInputView restarting=$restarting")
        if (!restarting) {
            clearAll()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        debounceTimer?.cancel()
        stopBackspaceRepeat()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        debounceTimer?.cancel()
        stopBackspaceRepeat()
        handler.removeCallbacksAndMessages(null)
        flutterEngine?.destroy()
        flutterEngine = null
        cachedView = null
        super.onDestroy()
    }

    // ── Utility ────────────────────────────────────────────

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun makeRoundRect(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun makeKeyBackground(normal: Int, pressed: Int): StateListDrawable {
        val radius = dp(5).toFloat()
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed),
                makeRoundRect(pressed, radius))
            addState(intArrayOf(),
                makeRoundRect(normal, radius))
        }
    }
}
