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
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
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
    GLOBE, EMOJI,
}

data class KeyData(
    val label: String,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f,
)

// ── Layouts ────────────────────────────────────────────────
private val QWERTY = listOf(
    listOf(KeyData("q"), KeyData("w"), KeyData("e"), KeyData("r"), KeyData("t"), KeyData("y"), KeyData("u"), KeyData("i"), KeyData("o"), KeyData("p")),
    listOf(KeyData("a"), KeyData("s"), KeyData("d"), KeyData("f"), KeyData("g"), KeyData("h"), KeyData("j"), KeyData("k"), KeyData("l")),
    listOf(KeyData("\u21E7", KeyType.SHIFT, 1.5f), KeyData("z"), KeyData("x"), KeyData("c"), KeyData("v"), KeyData("b"), KeyData("n"), KeyData("m"), KeyData("\u232B", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData("\uD83D\uDE00", KeyType.EMOJI, 1f), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("\u23CE", KeyType.ENTER, 1.3f)),
)

private val ARABIC_LAYOUT = listOf(
    listOf(KeyData("\u0636"), KeyData("\u0635"), KeyData("\u062B"), KeyData("\u0642"), KeyData("\u0641"), KeyData("\u063A"), KeyData("\u0639"), KeyData("\u0647"), KeyData("\u062E"), KeyData("\u062D")),
    listOf(KeyData("\u0634"), KeyData("\u0633"), KeyData("\u064A"), KeyData("\u0628"), KeyData("\u0644"), KeyData("\u0627"), KeyData("\u062A"), KeyData("\u0646"), KeyData("\u0645"), KeyData("\u0643")),
    listOf(KeyData("\u21E7", KeyType.SHIFT, 1.5f), KeyData("\u0626"), KeyData("\u0621"), KeyData("\u0624"), KeyData("\u0631"), KeyData("\u0649"), KeyData("\u0629"), KeyData("\u0648"), KeyData("\u0632"), KeyData("\u232B", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData("\uD83D\uDE00", KeyType.EMOJI, 1f), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("\u23CE", KeyType.ENTER, 1.3f)),
)

private val ARABIC_SHIFTED = listOf(
    listOf(KeyData("\u064E"), KeyData("\u064B"), KeyData("\u064F"), KeyData("\u064C"), KeyData("\u0650"), KeyData("\u064D"), KeyData("\u0651"), KeyData("\u0652"), KeyData("\u0622"), KeyData("\u0623")),
    listOf(KeyData("\u0625"), KeyData("\u0630"), KeyData("\u062C"), KeyData("\u0638"), KeyData("\u0637"), KeyData("\u0644\u0627"), KeyData("\u062F"), KeyData("\u0640"), KeyData("\u061B"), KeyData(":")),
    listOf(KeyData("\u21E7", KeyType.SHIFT, 1.5f), KeyData("\u00AB"), KeyData("\u00BB"), KeyData("{"), KeyData("}"), KeyData("["), KeyData("]"), KeyData("\u060C"), KeyData("\u061F"), KeyData("\u232B", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("123", KeyType.TO_NUM, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData("\uD83D\uDE00", KeyType.EMOJI, 1f), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("\u23CE", KeyType.ENTER, 1.3f)),
)

private val NUMBERS = listOf(
    listOf(KeyData("1"), KeyData("2"), KeyData("3"), KeyData("4"), KeyData("5"), KeyData("6"), KeyData("7"), KeyData("8"), KeyData("9"), KeyData("0")),
    listOf(KeyData("@"), KeyData("#"), KeyData("$"), KeyData("%"), KeyData("&"), KeyData("-"), KeyData("+"), KeyData("("), KeyData(")")),
    listOf(KeyData("#+=", KeyType.TO_SYM, 1.5f), KeyData("*"), KeyData("\""), KeyData("'"), KeyData(":"), KeyData(";"), KeyData("!"), KeyData("?"), KeyData("\u232B", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("ABC", KeyType.TO_ALPHA, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData(","), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("\u23CE", KeyType.ENTER, 1.3f)),
)

private val SYMBOLS = listOf(
    listOf(KeyData("~"), KeyData("`"), KeyData("|"), KeyData("\u2022"), KeyData("\u221A"), KeyData("\u03C0"), KeyData("\u00F7"), KeyData("\u00D7"), KeyData("{"), KeyData("}")),
    listOf(KeyData("\u00A3"), KeyData("\u00A2"), KeyData("\u20AC"), KeyData("\u00A5"), KeyData("^"), KeyData("\u00B0"), KeyData("="), KeyData("["), KeyData("]")),
    listOf(KeyData("123", KeyType.TO_NUM, 1.5f), KeyData("\\"), KeyData("/"), KeyData("_"), KeyData("<"), KeyData(">"), KeyData("\u2026"), KeyData("\u00BF"), KeyData("\u232B", KeyType.BACKSPACE, 1.5f)),
    listOf(KeyData("ABC", KeyType.TO_ALPHA, 1.2f), KeyData("\uD83C\uDF10", KeyType.GLOBE, 1f), KeyData(","), KeyData(" ", KeyType.SPACE, 3.5f), KeyData("."), KeyData("\u23CE", KeyType.ENTER, 1.3f)),
)

// ── Emoji Data ─────────────────────────────────────────────
private data class EmojiCategory(val icon: String, val name: String, val emojis: List<String>)

private val EMOJI_CATEGORIES = listOf(
    EmojiCategory("\uD83D\uDE00", "Smileys", listOf(
        "\uD83D\uDE00","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE01","\uD83D\uDE06","\uD83D\uDE05","\uD83E\uDD23","\uD83D\uDE02","\uD83D\uDE42","\uD83D\uDE43",
        "\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE07","\uD83E\uDD70","\uD83D\uDE0D","\uD83E\uDD29","\uD83D\uDE18","\uD83D\uDE17","\uD83D\uDE1A","\uD83D\uDE19",
        "\uD83E\uDD72","\uD83D\uDE0B","\uD83D\uDE1B","\uD83D\uDE1C","\uD83E\uDD2A","\uD83D\uDE1D","\uD83E\uDD11","\uD83E\uDD17","\uD83E\uDD2D","\uD83E\uDD2B",
        "\uD83E\uDD14","\uD83E\uDD10","\uD83E\uDD28","\uD83D\uDE10","\uD83D\uDE11","\uD83D\uDE36","\uD83D\uDE0F","\uD83D\uDE12","\uD83D\uDE44","\uD83D\uDE2C",
        "\uD83D\uDE24","\uD83D\uDE20","\uD83D\uDE21","\uD83E\uDD2C","\uD83D\uDE22","\uD83D\uDE2D","\uD83D\uDE25","\uD83D\uDE28","\uD83D\uDE30","\uD83D\uDE31",
        "\uD83E\uDD75","\uD83E\uDD76","\uD83D\uDE33","\uD83E\uDD2F","\uD83D\uDE35","\uD83E\uDD74","\uD83D\uDE34","\uD83D\uDE2A","\uD83D\uDE32","\uD83E\uDD25",
    )),
    EmojiCategory("\u2764\uFE0F", "Hearts", listOf(
        "\u2764\uFE0F","\uD83E\uDDE1","\uD83D\uDC9B","\uD83D\uDC9A","\uD83D\uDC99","\uD83D\uDC9C","\uD83E\uDD0E","\uD83D\uDDA4","\uD83E\uDD0D","\uD83D\uDC94",
        "\u2763\uFE0F","\uD83D\uDC95","\uD83D\uDC9E","\uD83D\uDC93","\uD83D\uDC97","\uD83D\uDC96","\uD83D\uDC98","\uD83D\uDC9D","\uD83D\uDC8B","\uD83D\uDC4D",
        "\uD83D\uDC4E","\u270C\uFE0F","\uD83E\uDD1E","\uD83E\uDD1F","\uD83E\uDD18","\uD83D\uDC4C","\uD83D\uDC4B","\uD83E\uDD1A","\uD83D\uDC4F","\uD83D\uDE4F",
    )),
    EmojiCategory("\uD83D\uDC31", "Animals", listOf(
        "\uD83D\uDC36","\uD83D\uDC31","\uD83D\uDC2D","\uD83D\uDC39","\uD83D\uDC30","\uD83E\uDD8A","\uD83D\uDC3B","\uD83D\uDC3C","\uD83D\uDC28","\uD83D\uDC2F",
        "\uD83E\uDD81","\uD83D\uDC2E","\uD83D\uDC37","\uD83D\uDC38","\uD83D\uDC35","\uD83D\uDC14","\uD83D\uDC27","\uD83D\uDC26","\uD83E\uDD85","\uD83E\uDD89",
        "\uD83D\uDC1D","\uD83D\uDC1B","\uD83E\uDD8B","\uD83D\uDC0C","\uD83D\uDC1A","\uD83D\uDC20","\uD83D\uDC1F","\uD83D\uDC21","\uD83D\uDC19","\uD83E\uDD80",
    )),
    EmojiCategory("\uD83C\uDF4E", "Food", listOf(
        "\uD83C\uDF4E","\uD83C\uDF4A","\uD83C\uDF4B","\uD83C\uDF4C","\uD83C\uDF49","\uD83C\uDF47","\uD83C\uDF53","\uD83E\uDED0","\uD83C\uDF51","\uD83C\uDF52",
        "\uD83C\uDF45","\uD83E\uDD51","\uD83C\uDF46","\uD83E\uDD55","\uD83C\uDF3D","\uD83C\uDF36\uFE0F","\uD83E\uDD52","\uD83E\uDD66","\uD83C\uDF54","\uD83C\uDF55",
        "\uD83C\uDF2E","\uD83C\uDF2F","\uD83E\uDD59","\uD83C\uDF73","\uD83C\uDF5E","\uD83E\uDDC0","\uD83C\uDF56","\uD83C\uDF57","\uD83C\uDF70","\uD83C\uDF69",
    )),
    EmojiCategory("\u26BD", "Activities", listOf(
        "\u26BD","\uD83C\uDFC0","\uD83C\uDFC8","\u26BE","\uD83E\uDD4E","\uD83C\uDFBE","\uD83C\uDFD0","\uD83C\uDFC9","\uD83C\uDFB1","\uD83C\uDFD3",
        "\uD83C\uDFF8","\uD83E\uDD4A","\uD83E\uDD4B","\u26F3","\uD83C\uDFC7","\uD83C\uDFBF","\uD83C\uDFC2","\uD83C\uDFA3","\uD83E\uDD3F","\uD83C\uDFAF",
        "\uD83C\uDFAE","\uD83C\uDFB2","\uD83C\uDFB5","\uD83C\uDFB6","\uD83C\uDFA4","\uD83C\uDFAC","\uD83C\uDFA8","\uD83C\uDFAD","\uD83C\uDFAA","\uD83C\uDFA0",
    )),
    EmojiCategory("\uD83C\uDFE0", "Objects", listOf(
        "\uD83D\uDCF1","\uD83D\uDCBB","\u2328\uFE0F","\uD83D\uDCF7","\uD83D\uDCF9","\uD83D\uDCFA","\uD83D\uDD0A","\uD83D\uDCE6","\uD83D\uDCA1","\uD83D\uDD0B",
        "\uD83D\uDD0C","\uD83D\uDCB0","\uD83D\uDCB3","\uD83D\uDC8E","\uD83D\uDD27","\uD83D\uDD28","\u2699\uFE0F","\uD83D\uDD12","\uD83D\uDD11","\uD83D\uDEE1\uFE0F",
        "\u2702\uFE0F","\uD83D\uDCDD","\u270F\uFE0F","\uD83D\uDCDA","\uD83D\uDCC5","\uD83D\uDCC6","\uD83D\uDCCB","\uD83D\uDCCC","\uD83D\uDCCE","\uD83D\uDDD1\uFE0F",
    )),
    EmojiCategory("\u2600\uFE0F", "Nature", listOf(
        "\u2600\uFE0F","\uD83C\uDF24\uFE0F","\u26C5","\uD83C\uDF25\uFE0F","\uD83C\uDF26\uFE0F","\uD83C\uDF27\uFE0F","\u26C8\uFE0F","\uD83C\uDF28\uFE0F","\u2744\uFE0F","\uD83C\uDF2C\uFE0F",
        "\uD83C\uDF08","\u2B50","\uD83C\uDF1F","\uD83D\uDCAB","\u2728","\uD83C\uDF1E","\uD83C\uDF1D","\uD83C\uDF1B","\uD83C\uDF1C","\uD83C\uDF19",
        "\uD83C\uDF3B","\uD83C\uDF39","\uD83C\uDF3A","\uD83C\uDF37","\uD83C\uDF38","\uD83C\uDF3C","\uD83C\uDF3E","\uD83C\uDF32","\uD83C\uDF33","\uD83C\uDF34",
    )),
    EmojiCategory("\uD83D\uDE97", "Travel", listOf(
        "\uD83D\uDE97","\uD83D\uDE95","\uD83D\uDE99","\uD83D\uDE8C","\uD83D\uDE8E","\uD83C\uDFCE\uFE0F","\uD83D\uDE93","\uD83D\uDE91","\uD83D\uDE92","\uD83D\uDEF5",
        "\uD83D\uDEB2","\u2708\uFE0F","\uD83D\uDE80","\uD83D\uDEF8","\uD83D\uDE82","\uD83D\uDE86","\u26F5","\uD83D\uDEA2","\uD83C\uDFE0","\uD83C\uDFE2",
        "\uD83C\uDFEB","\uD83C\uDFE5","\uD83C\uDFEA","\u26EA","\uD83D\uDD4C","\uD83D\uDD4D","\uD83C\uDFEF","\uD83C\uDFF0","\uD83D\uDDFC","\uD83D\uDDFD",
    )),
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
    val TRANSLATE_BG = Color.parseColor("#34A853")
    val REPLACE_BG = Color.parseColor("#EA4335")
    val EMOJI_TAB_ACTIVE = Color.parseColor("#4A90D9")
    val EMOJI_TAB_INACTIVE = Color.parseColor("#8E8E93")
    val EMOJI_BG = Color.parseColor("#F2F2F7")
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
    private var currentLayoutId = "qwerty"
    private var baseLayoutId = "qwerty"
    private var targetLangIndex = 1
    private var detectedLangCode: String? = null
    private var translatedText: String? = null
    private var isTranslating = false
    private var translationError: String? = null
    private var showingEmoji = false
    private var currentEmojiCategory = 0

    // UI references
    private var rootLayout: LinearLayout? = null
    private var keyboardContainer: LinearLayout? = null
    private var emojiContainer: LinearLayout? = null
    private var inputPreview: TextView? = null
    private var translationView: TextView? = null
    private var translateButton: TextView? = null
    private var replaceButton: TextView? = null
    private var sourceLangView: TextView? = null
    private var targetLangView: TextView? = null
    private var translationRow: LinearLayout? = null
    private var actionRow: LinearLayout? = null

    // Timers
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
        if (!restarting) clearAll()
        if (showingEmoji) hideEmojiPicker()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stopBackspaceRepeat()
    }

    override fun onDestroy() {
        stopBackspaceRepeat()
        handler.removeCallbacksAndMessages(null)
        flutterEngine?.destroy()
        flutterEngine = null
        cachedView = null
        super.onDestroy()
    }

    // ── View Creation ──────────────────────────────────────

    override fun onCreateInputView(): View {
        cachedView?.let { return it }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KB.BG)
            // FIX #1: Apply window insets for nav bar
            fitsSystemWindows = true
        }
        rootLayout = root

        // Translation strip
        root.addView(buildTranslationStrip())

        // Keyboard container
        val kbContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(6))
        }
        buildKeyboardRows(kbContainer)
        keyboardContainer = kbContainer
        root.addView(kbContainer)

        // Emoji container (hidden by default)
        val emojiCont = buildEmojiPicker()
        emojiCont.visibility = View.GONE
        emojiContainer = emojiCont
        root.addView(emojiCont)

        cachedView = root
        return root
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

        sourceLangView = TextView(this).apply {
            text = "Auto"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.TEXT_LIGHT)
            background = roundRect(Color.parseColor("#E8E8EC"), dp(6).toFloat())
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        langBar.addView(sourceLangView)

        langBar.addView(TextView(this).apply {
            text = " \u21C4 "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(KB.PRIMARY)
            gravity = Gravity.CENTER
        })

        targetLangView = TextView(this).apply {
            updateTargetDisplay()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(KB.PRIMARY)
            background = roundRect(KB.SEND_BG, dp(6).toFloat())
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { showLanguageDropdown(it) }
        }
        langBar.addView(targetLangView)

        langBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })

        // Translate button (#3 — manual translation)
        translateButton = TextView(this).apply {
            text = " Translate \u25B6 "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = roundRect(KB.TRANSLATE_BG, dp(14).toFloat())
            setPadding(dp(10), dp(5), dp(10), dp(5))
            gravity = Gravity.CENTER
            visibility = View.GONE
            setOnClickListener { doTranslate() }
        }
        langBar.addView(translateButton)

        // Clear button
        langBar.addView(TextView(this).apply {
            text = "\u2715"
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
            maxLines = 2
            setPadding(dp(12), dp(2), dp(12), dp(2))
            visibility = View.GONE
        }
        strip.addView(inputPreview)

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
        strip.addView(transRow)

        // Action row: Replace button (#3)
        val actRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(2), dp(8), dp(4))
            visibility = View.GONE
        }
        actionRow = actRow

        replaceButton = TextView(this).apply {
            text = " \u21C4 Replace original "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = roundRect(KB.ENTER_BG, dp(14).toFloat())
            setPadding(dp(12), dp(6), dp(12), dp(6))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { replaceWithTranslation() }
        }
        actRow.addView(replaceButton)

        actRow.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(6), 0)
        })

        // Copy translation button
        actRow.addView(TextView(this).apply {
            text = " Send \u25B6 "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = roundRect(KB.TRANSLATE_BG, dp(14).toFloat())
            setPadding(dp(12), dp(6), dp(12), dp(6))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { sendTranslation() }
        })

        strip.addView(actRow)
        strip.addView(divider())

        return strip
    }

    // ── Emoji Picker (#2) ──────────────────────────────────

    private fun buildEmojiPicker(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KB.EMOJI_BG)
        }

        // Category tabs
        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.WHITE)
        }
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        for ((index, cat) in EMOJI_CATEGORIES.withIndex()) {
            val tab = TextView(this).apply {
                text = cat.icon
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = if (index == 0) roundRect(KB.SEND_BG, dp(8).toFloat()) else null
                setOnClickListener { selectEmojiCategory(index, tabRow) }
                tag = index
            }
            tabRow.addView(tab)
        }

        // ABC button to return to keyboard
        tabRow.addView(TextView(this).apply {
            text = "ABC"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(KB.PRIMARY)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(4), dp(12), dp(4))
            background = roundRect(KB.SEND_BG, dp(8).toFloat())
            setOnClickListener { hideEmojiPicker() }
        })

        tabScroll.addView(tabRow)
        container.addView(tabScroll)

        // Emoji grid in scroll view
        val gridScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(180)
            )
        }

        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            tag = "emoji_grid"
        }
        buildEmojiGrid(gridContainer, 0)
        gridScroll.addView(gridContainer)
        container.addView(gridScroll)

        // Bottom row with backspace
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), dp(6))
            setBackgroundColor(KB.BG)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
            )
        }

        bottomRow.addView(buildKey(KeyData("\u232B", KeyType.BACKSPACE, 1.5f)))
        bottomRow.addView(buildKey(KeyData(" ", KeyType.SPACE, 5f)))
        bottomRow.addView(buildKey(KeyData("\u23CE", KeyType.ENTER, 1.5f)))
        container.addView(bottomRow)

        return container
    }

    private fun buildEmojiGrid(container: LinearLayout, categoryIndex: Int) {
        container.removeAllViews()
        val emojis = EMOJI_CATEGORIES[categoryIndex].emojis
        val columns = 8

        var row: LinearLayout? = null
        for ((i, emoji) in emojis.withIndex()) {
            if (i % columns == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                container.addView(row)
            }
            val emojiView = TextView(this).apply {
                text = emoji
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(6), dp(4), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    currentInputConnection?.commitText(emoji, 1)
                }
            }
            row?.addView(emojiView)
        }
        // Fill remaining cells in last row
        val remaining = columns - (emojis.size % columns)
        if (remaining < columns) {
            for (j in 0 until remaining) {
                row?.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }
        }
    }

    private fun selectEmojiCategory(index: Int, tabRow: LinearLayout) {
        currentEmojiCategory = index
        // Update tab highlights
        for (i in 0 until tabRow.childCount - 1) { // -1 to skip ABC button
            val tab = tabRow.getChildAt(i) as? TextView ?: continue
            tab.background = if (i == index) roundRect(KB.SEND_BG, dp(8).toFloat()) else null
        }
        // Rebuild grid
        val gridContainer = emojiContainer?.findViewWithTag<LinearLayout>("emoji_grid")
        if (gridContainer != null) {
            buildEmojiGrid(gridContainer, index)
        }
    }

    private fun showEmojiPicker() {
        showingEmoji = true
        keyboardContainer?.visibility = View.GONE
        emojiContainer?.visibility = View.VISIBLE
    }

    private fun hideEmojiPicker() {
        showingEmoji = false
        emojiContainer?.visibility = View.GONE
        keyboardContainer?.visibility = View.VISIBLE
    }

    // ── Language Dropdown ──────────────────────────────────

    private fun showLanguageDropdown(anchor: View) {
        val popup = PopupMenu(this, anchor)
        for ((index, lang) in LANGUAGES.withIndex()) {
            popup.menu.add(0, index, index, "${lang.flag} ${lang.name}")
        }
        popup.setOnMenuItemClickListener { item ->
            targetLangIndex = item.itemId
            targetLangView?.updateTargetDisplay()
            true
        }
        popup.show()
    }

    // ── Keyboard Rows ──────────────────────────────────────

    private fun buildKeyboardRows(container: LinearLayout) {
        container.removeAllViews()
        for ((rowIndex, row) in currentLayout.withIndex()) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
                ).apply { bottomMargin = dp(2) }
            }
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
            KeyType.SPACE -> if (currentLayoutId == "arabic") "\u0645\u0633\u0627\u0641\u0629" else "space"
            KeyType.CHAR -> if (isShifted && currentLayoutId == "qwerty") key.label.uppercase() else key.label
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
                currentLayoutId = "numbers"; isShifted = false; refreshKeyboard()
            }
            KeyType.TO_ALPHA -> {
                currentLayoutId = baseLayoutId; isShifted = false; refreshKeyboard()
            }
            KeyType.TO_SYM -> {
                currentLayoutId = "symbols"; isShifted = false; refreshKeyboard()
            }
            KeyType.GLOBE -> {
                baseLayoutId = if (baseLayoutId == "qwerty") "arabic" else "qwerty"
                currentLayoutId = baseLayoutId
                isShifted = false
                refreshKeyboard()
            }
            KeyType.EMOJI -> {
                if (showingEmoji) hideEmojiPicker() else showEmojiPicker()
            }
        }
    }

    private fun typeChar(ch: String) {
        currentInputConnection?.commitText(ch, 1)
        typedBuffer.append(ch)
        updateInputPreview()
        // #3: Show translate button when there's text (no auto-translate)
        updateTranslateButtonVisibility()
    }

    private fun doBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (typedBuffer.isNotEmpty()) {
            typedBuffer.deleteCharAt(typedBuffer.length - 1)
        }
        updateInputPreview()
        if (typedBuffer.isEmpty()) {
            clearTranslation()
        }
        updateTranslateButtonVisibility()
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

    // ── Translation (#3 — manual trigger) ──────────────────

    private fun updateTranslateButtonVisibility() {
        val hasText = typedBuffer.length >= 2
        translateButton?.visibility = if (hasText && translatedText == null) View.VISIBLE else View.GONE
    }

    private fun doTranslate() {
        val text = typedBuffer.toString().trim()
        if (text.length < 2) return

        val channel = translationChannel ?: run {
            translationError = "Translation service not available"
            updateTranslationUI()
            return
        }
        val targetLang = LANGUAGES[targetLangIndex].code

        isTranslating = true
        translationError = null
        translatedText = null
        translateButton?.visibility = View.GONE
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
                    updateTranslateButtonVisibility()
                    updateSourceLangDisplay()
                }
            }

            override fun error(code: String, msg: String?, details: Any?) {
                handler.post {
                    isTranslating = false
                    translationError = msg ?: "Translation failed"
                    updateTranslationUI()
                    updateTranslateButtonVisibility()
                }
            }

            override fun notImplemented() {
                handler.post {
                    isTranslating = false
                    translationError = "Translation engine starting\u2026"
                    updateTranslationUI()
                    handler.postDelayed({
                        if (typedBuffer.isNotEmpty()) doTranslate()
                    }, 2000)
                }
            }
        })
    }

    // #3: Replace original text with translation
    private fun replaceWithTranslation() {
        val translation = translatedText ?: return
        val conn = currentInputConnection ?: return
        conn.deleteSurroundingText(typedBuffer.length, 0)
        conn.commitText(translation, 1)
        typedBuffer.clear()
        typedBuffer.append(translation)
        updateInputPreview()
        translatedText = null
        updateTranslationUI()
        updateTranslateButtonVisibility()
    }

    // Send translation (clear everything after)
    private fun sendTranslation() {
        val translation = translatedText ?: return
        val conn = currentInputConnection ?: return
        conn.deleteSurroundingText(typedBuffer.length, 0)
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
        updateTranslateButtonVisibility()
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
        val actRow = actionRow ?: return

        when {
            isTranslating -> {
                transRow.visibility = View.VISIBLE
                translationView?.text = "Translating\u2026"
                translationView?.setTextColor(KB.TEXT_LIGHT)
                translationView?.setTypeface(null, Typeface.ITALIC)
                actRow.visibility = View.GONE
            }
            translationError != null -> {
                transRow.visibility = View.VISIBLE
                translationView?.text = translationError
                translationView?.setTextColor(KB.ERROR)
                translationView?.setTypeface(null, Typeface.NORMAL)
                actRow.visibility = View.GONE
            }
            translatedText != null -> {
                transRow.visibility = View.VISIBLE
                translationView?.text = translatedText
                translationView?.setTextColor(KB.PRIMARY)
                translationView?.setTypeface(null, Typeface.BOLD)
                actRow.visibility = View.VISIBLE
            }
            else -> {
                transRow.visibility = View.GONE
                actRow.visibility = View.GONE
            }
        }
    }

    private fun updateSourceLangDisplay() {
        val lang = LANGUAGES.find { it.code == detectedLangCode }
        sourceLangView?.text = if (lang != null) "${lang.flag} ${lang.name}" else "Auto"
    }

    private fun TextView.updateTargetDisplay() {
        val lang = LANGUAGES[targetLangIndex]
        text = "${lang.flag} ${lang.name} \u25BC"
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
