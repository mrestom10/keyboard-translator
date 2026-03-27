package com.translator.translator_keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
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
    LangInfo("de", "German", "\uD83C\uDDE9\uD83C\uDDEA"),
)

// ── Key Types ──────────────────────────────────────────────
enum class KeyType {
    CHAR, BACKSPACE, SHIFT, SPACE, ENTER,
    TO_NUM, TO_ALPHA, TO_SYM,
    GLOBE,   // switch keyboard language
    EMOJI,   // open system emoji/keyboard picker
}

data class KeyData(
    val label: String,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f,
    val shiftLabel: String? = null,
)

// ── Layouts ────────────────────────────────────────────────
private val QWERTY = listOf(
    listOf(KeyData("q"), KeyData("w"), KeyData("e"), KeyData("r"), KeyData("t"), KeyData("y"), KeyData("u"), KeyData("i"), KeyData("o"), KeyData("p")),
    listOf(KeyData("a"), KeyData("s"), KeyData("d"), KeyData("f"), KeyData("g"), KeyData("h"), KeyData("j"), KeyData("k"), KeyData("l")),
    listOf(KeyData("⇧", KeyType.SHIFT, 1.5f), KeyData("z"), KeyData("x"), KeyData("c"), KeyData("v"), KeyData("b"), KeyData("n"), KeyData("m"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData("\uD83D\uDE00", KeyType.EMOJI, 1f), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.3f)),
)

private val ARABIC_LAYOUT = listOf(
    listOf(KeyData("ض"), KeyData("ص"), KeyData("ث"), KeyData("ق"), KeyData("ف"), KeyData("غ"), KeyData("ع"), KeyData("ه"), KeyData("خ"), KeyData("ح")),
    listOf(KeyData("ش"), KeyData("س"), KeyData("ي"), KeyData("ب"), KeyData("ل"), KeyData("ا"), KeyData("ت"), KeyData("ن"), KeyData("م"), KeyData("ك")),
    listOf(KeyData("⇧", KeyType.SHIFT, 1.5f), KeyData("ئ"), KeyData("ء"), KeyData("ؤ"), KeyData("ر"), KeyData("ى"), KeyData("ة"), KeyData("و"), KeyData("ز"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData("\uD83D\uDE00", KeyType.EMOJI, 1f), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.3f)),
)

private val ARABIC_SHIFTED = listOf(
    listOf(KeyData("َ"), KeyData("ً"), KeyData("ُ"), KeyData("ٌ"), KeyData("ِ"), KeyData("ٍ"), KeyData("ّ"), KeyData("ْ"), KeyData("آ"), KeyData("أ")),
    listOf(KeyData("إ"), KeyData("ذ"), KeyData("ج"), KeyData("ظ"), KeyData("ط"), KeyData("لا"), KeyData("د"), KeyData("ـ"), KeyData("؛"), KeyData(":")),
    listOf(KeyData("⇧", KeyType.SHIFT, 1.5f), KeyData("«"), KeyData("»"), KeyData("{"), KeyData("}"), KeyData("["), KeyData("]"), KeyData("،"), KeyData("؟"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData("\uD83D\uDE00", KeyType.EMOJI, 1f), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.3f)),
)

private val NUMBERS = listOf(
    listOf(KeyData("1"), KeyData("2"), KeyData("3"), KeyData("4"), KeyData("5"), KeyData("6"), KeyData("7"), KeyData("8"), KeyData("9"), KeyData("0")),
    listOf(KeyData("@"), KeyData("#"), KeyData("$"), KeyData("%"), KeyData("&"), KeyData("-"), KeyData("+"), KeyData("("), KeyData(")")),
    listOf(KeyData("#+=", KeyType.TO_SYM, 1.5f), KeyData("*"), KeyData("\""), KeyData("'"), KeyData(":"), KeyData(";"), KeyData("!"), KeyData("?"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("ABC", KeyType.TO_ALPHA, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData(","), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.3f)),
)

private val SYMBOLS = listOf(
    listOf(KeyData("~"), KeyData("`"), KeyData("|"), KeyData("•"), KeyData("√"), KeyData("π"), KeyData("÷"), KeyData("×"), KeyData("{"), KeyData("}")),
    listOf(KeyData("£"), KeyData("¢"), KeyData("€"), KeyData("¥"), KeyData("^"), KeyData("°"), KeyData("="), KeyData("["), KeyData("]")),
    listOf(KeyData("123", KeyType.TO_NUM, 1.5f), KeyData("\\"), KeyData("/"), KeyData("_"), KeyData("<"), KeyData(">"), KeyData("…"), KeyData("¿"), KeyData("⌫", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("ABC", KeyType.TO_ALPHA, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData(","), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("↵", KeyType.ENTER, 1.3f)),
)

// ── Colors ─────────────────────────────────────────────────
private object KB {
    val BG = Color.parseColor("#D2D3D9")
    val KEY_BG = Color.parseColor("#FFFFFF")
    val KEY_PRESSED = Color.parseColor("#BABCC2")
    val SPECIAL_BG = Color.parseColor("#ADB0BB")
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
    val GLOBE_BG = Color.parseColor("#C5CAD4")
}

// ── IME Service ────────────────────────────────────────────
class TranslatorInputMethodService : InputMethodService() {

    private var flutterEngine: FlutterEngine? = null
    private var translationChannel: MethodChannel? = null
    private var cachedView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    // Keyboard state
    private var typedBuffer = StringBuilder()
    private var isShifted = false
    private var currentLayoutId = "qwerty"  // "qwerty", "arabic", "numbers", "symbols"
    private var baseLayoutId = "qwerty"     // remembers letters layout when switching to numbers/symbols
    private var targetLangIndex = 1         // Arabic by default
    private var detectedLangCode: String? = null
    private var translatedText: String? = null
    private var isTranslating = false
    private var translationError: String? = null

    // UI references
    private var keyboardContainer: LinearLayout? = null
    private var inputPreview: TextView? = null
    private var translationView: TextView? = null
    private var sendButton: TextView? = null
    private var sourceLangView: TextView? = null
    private var targetLangView: TextView? = null
    private var translationRow: LinearLayout? = null

    // Timers
    private var debounceTimer: Timer? = null
    private var backspaceTimer: Timer? = null

    private val currentLayout: List<List<KeyData>>
        get() = when (currentLayoutId) {
            "arabic" -> if (isShifted) ARABIC_SHIFTED else ARABIC_LAYOUT
            "numbers" -> NUMBERS
            "symbols" -> SYMBOLS
            else -> QWERTY
        }

    // ── Lifecycle ──────────────────────────────────────────

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

    // ── View Creation ──────────────────────────────────────

    override fun onCreateInputView(): View {
        Log.d(TAG, "onCreateInputView")
        cachedView?.let { return it }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KB.BG)
        }

        // Translation strip (top)
        root.addView(buildTranslationStrip())

        // Keyboard rows
        val kbContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), 0)
        }
        buildKeyboardRows(kbContainer)
        keyboardContainer = kbContainer
        root.addView(kbContainer)

        // Bottom padding for navigation bar (#1)
        val navPad = getNavBarPadding()
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, navPad
            )
            setBackgroundColor(KB.BG)
        })

        cachedView = root
        return root
    }

    private fun getNavBarPadding(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Gesture navigation = small bar, button navigation = larger
                val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    val navHeight = resources.getDimensionPixelSize(resourceId)
                    // Only add partial padding — the IME already sits above nav on most devices
                    (navHeight * 0.15).toInt().coerceAtLeast(dp(4))
                } else dp(4)
            } else dp(4)
        } catch (_: Exception) { dp(4) }
    }

    // ── Translation Strip ──────────────────────────────────

    private fun buildTranslationStrip(): View {
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KB.SURFACE)
        }

        // Language bar
        val langBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(4))
        }

        // Source language
        sourceLangView = TextView(this).apply {
            text = "Auto"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.TEXT_LIGHT)
            background = roundRect(Color.parseColor("#E8E8EC"), dp(6).toFloat())
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        langBar.addView(sourceLangView)

        // Swap arrow
        langBar.addView(TextView(this).apply {
            text = " ⇄ "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(KB.PRIMARY)
            gravity = Gravity.CENTER
        })

        // Target language — TAP opens dropdown (#7)
        targetLangView = TextView(this).apply {
            updateTargetDisplay()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.PRIMARY)
            background = roundRect(KB.SEND_BG, dp(6).toFloat())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { showLanguageDropdown(it) }
        }
        langBar.addView(targetLangView)

        // Spacer
        langBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })

        // Clear button
        langBar.addView(TextView(this).apply {
            text = "✕"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(KB.ERROR)
            gravity = Gravity.CENTER
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener { clearAll() }
        })

        strip.addView(langBar)

        // Input preview
        inputPreview = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(KB.TEXT)
            maxLines = 1
            setPadding(dp(12), dp(2), dp(12), dp(2))
            visibility = View.GONE
        }
        strip.addView(inputPreview)

        // Divider
        strip.addView(divider())

        // Translation result row
        val transRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(8), dp(4))
            visibility = View.GONE
        }
        translationRow = transRow

        translationView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(KB.PRIMARY)
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        transRow.addView(translationView)

        sendButton = TextView(this).apply {
            text = " Send ▶ "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = roundRect(KB.ENTER_BG, dp(14).toFloat())
            setPadding(dp(12), dp(6), dp(12), dp(6))
            gravity = Gravity.CENTER
            setOnClickListener { sendTranslation() }
        }
        transRow.addView(sendButton)

        strip.addView(transRow)
        strip.addView(divider())

        return strip
    }

    // ── Target Language Dropdown (#7) ──────────────────────

    private fun showLanguageDropdown(anchor: View) {
        val popup = PopupMenu(this, anchor)
        for ((index, lang) in LANGUAGES.withIndex()) {
            popup.menu.add(0, index, index, "${lang.flag} ${lang.name}")
        }
        popup.setOnMenuItemClickListener { item ->
            targetLangIndex = item.itemId
            targetLangView?.updateTargetDisplay()
            if (typedBuffer.isNotEmpty()) {
                requestTranslation(typedBuffer.toString())
            }
            true
        }
        popup.show()
    }

    // ── Keyboard Rows ──────────────────────────────────────

    private fun buildKeyboardRows(container: LinearLayout) {
        container.removeAllViews()
        val layout = currentLayout
        for ((rowIndex, row) in layout.withIndex()) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
                ).apply { bottomMargin = dp(2) }
            }

            // Center row 2 for QWERTY/Arabic letter layouts
            if (rowIndex == 1 && (currentLayoutId == "qwerty" || currentLayoutId == "arabic")) {
                rowView.setPadding(dp(14), 0, dp(14), 0)
            }

            for (key in row) {
                rowView.addView(buildKey(key))
            }
            container.addView(rowView)
        }
    }

    private fun buildKey(key: KeyData): View {
        val isSpecial = key.type !in listOf(KeyType.CHAR, KeyType.SPACE)
        val isEnter = key.type == KeyType.ENTER
        val isShiftKey = key.type == KeyType.SHIFT
        val isGlobe = key.type == KeyType.GLOBE
        val isEmoji = key.type == KeyType.EMOJI

        val bgNormal = when {
            isEnter -> KB.ENTER_BG
            isGlobe || isEmoji -> KB.GLOBE_BG
            isSpecial -> KB.SPECIAL_BG
            else -> KB.KEY_BG
        }
        val bgPressed = when {
            isEnter -> KB.ENTER_PRESSED
            isSpecial || isGlobe || isEmoji -> KB.SPECIAL_PRESSED
            else -> KB.KEY_PRESSED
        }
        val textColor = when {
            isEnter -> Color.WHITE
            isShiftKey && isShifted -> KB.PRIMARY
            else -> KB.TEXT
        }

        val displayLabel = when (key.type) {
            KeyType.SPACE -> if (currentLayoutId == "arabic") "مسافة" else "space"
            KeyType.BACKSPACE -> "⌫"
            KeyType.SHIFT -> "⇧"
            KeyType.ENTER -> "↵"
            KeyType.GLOBE -> "\uD83C\uDF10"
            KeyType.EMOJI -> "\uD83D\uDE00"
            KeyType.CHAR -> if (isShifted && currentLayoutId == "qwerty")
                key.label.uppercase() else key.label
            else -> key.label
        }

        val fontSize = when {
            key.type == KeyType.SPACE -> 12f
            key.type == KeyType.GLOBE || key.type == KeyType.EMOJI -> 18f
            isSpecial -> 14f
            currentLayoutId == "arabic" -> 20f
            else -> 18f
        }

        val tv = TextView(this).apply {
            text = displayLabel
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = keyBg(bgNormal, bgPressed)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, key.weight
            ).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            isClickable = true
            isFocusable = true
        }

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
                val ch = if (isShifted && currentLayoutId == "qwerty")
                    key.label.uppercase() else key.label
                typeChar(ch)
                if (isShifted && currentLayoutId != "arabic") {
                    isShifted = false
                    refreshKeyboard()
                }
            }
            KeyType.SPACE -> typeChar(" ")
            KeyType.BACKSPACE -> doBackspace()
            KeyType.ENTER -> currentInputConnection?.commitText("\n", 1)
            KeyType.SHIFT -> {
                isShifted = !isShifted
                refreshKeyboard()
            }
            KeyType.TO_NUM -> {
                currentLayoutId = "numbers"
                isShifted = false
                refreshKeyboard()
            }
            KeyType.TO_ALPHA -> {
                currentLayoutId = baseLayoutId
                isShifted = false
                refreshKeyboard()
            }
            KeyType.TO_SYM -> {
                currentLayoutId = "symbols"
                isShifted = false
                refreshKeyboard()
            }
            KeyType.GLOBE -> {
                // Cycle keyboard language (#5)
                if (baseLayoutId == "qwerty") {
                    baseLayoutId = "arabic"
                    currentLayoutId = "arabic"
                } else {
                    baseLayoutId = "qwerty"
                    currentLayoutId = "qwerty"
                }
                isShifted = false
                refreshKeyboard()
            }
            KeyType.EMOJI -> {
                // Open system keyboard picker for emoji access (#4)
                try {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show input method picker", e)
                }
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
                override fun run() { handler.post { doBackspace() } }
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
                override fun run() { handler.post { requestTranslation(text) } }
            }, 400)
        }
    }

    private fun requestTranslation(text: String) {
        val channel = translationChannel ?: run {
            translationError = "Translation service not available"
            updateTranslationUI()
            return
        }
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
                    translationError = "Translation engine starting…"
                    updateTranslationUI()
                    // Retry once after a delay
                    handler.postDelayed({
                        if (typedBuffer.isNotEmpty()) {
                            requestTranslation(typedBuffer.toString())
                        }
                    }, 2000)
                }
            }
        })
    }

    // (#8) Send translated text into the chat, replacing what was typed
    private fun sendTranslation() {
        val translation = translatedText ?: return
        val conn = currentInputConnection ?: return

        // Delete typed text
        conn.deleteSurroundingText(typedBuffer.length, 0)
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
        inputPreview?.apply {
            if (typedBuffer.isEmpty()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = typedBuffer.toString()
            }
        }
    }

    private fun updateTranslationUI() {
        val transRow = translationRow ?: return

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

    // (#3) Show detected source language
    private fun updateSourceLangDisplay() {
        val lang = LANGUAGES.find { it.code == detectedLangCode }
        sourceLangView?.text = if (lang != null) "${lang.flag} ${lang.name}" else "Auto"
    }

    private fun TextView.updateTargetDisplay() {
        val lang = LANGUAGES[targetLangIndex]
        text = "${lang.flag} ${lang.name} ▼"
    }

    private fun refreshKeyboard() {
        keyboardContainer?.let { buildKeyboardRows(it) }
    }

    // ── Utility ────────────────────────────────────────────

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundRect(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun keyBg(normal: Int, pressed: Int): StateListDrawable {
        val r = dp(5).toFloat()
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), roundRect(pressed, r))
            addState(intArrayOf(), roundRect(normal, r))
        }
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        setBackgroundColor(KB.DIVIDER)
    }
}
