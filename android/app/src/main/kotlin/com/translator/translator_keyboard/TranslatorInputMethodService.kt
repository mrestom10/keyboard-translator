package com.translator.translator_keyboard

import android.content.ClipboardManager
import android.content.Context
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
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.inputmethodservice.InputMethodService
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Timer
import java.util.TimerTask

private const val TAG = "TranslatorIME"

data class LangInfo(val code: String, val name: String, val flag: String)
private val LANGS = listOf(
    LangInfo("en","English","\uD83C\uDDEC\uD83C\uDDE7"),
    LangInfo("ar","Arabic","\uD83C\uDDF8\uD83C\uDDE6"),
    LangInfo("fr","French","\uD83C\uDDEB\uD83C\uDDF7"),
    LangInfo("de","German","\uD83C\uDDE9\uD83C\uDDEA"),
)

enum class KT { CH, BS, SHIFT, SPACE, ENTER, NUM, ALPHA, SYM, GLOBE, EMOJI }
data class K(val l: String, val t: KT = KT.CH, val w: Float = 1f)

// ── Long-press alternatives (#2) ───────────────────────────
private val ALTS = mapOf(
    // English
    "a" to "àáâäæãåā","e" to "èéêëēėę","i" to "ìíîïīį","o" to "òóôöœõōø",
    "u" to "ùúûüūů","s" to "śšşß","c" to "çćč","n" to "ñń","l" to "ł","y" to "ÿý",
    "z" to "žźż","d" to "đð","r" to "ř","t" to "ťþ","g" to "ğ",
    // Arabic
    "\u0627" to "\u0622\u0623\u0625\u0671",    // ا → آ أ إ ٱ
    "\u062D" to "\u062C\u062E",                  // ح → ج خ
    "\u0643" to "\u0637\u0638\u06AF",            // ك → ط ظ گ
    "\u064A" to "\u0626\u0649",                  // ي → ئ ى
    "\u0648" to "\u0624",                        // و → ؤ
    "\u0647" to "\u0629",                        // ه → ة
    "\u0627\u0644" to "\u0644\u0627",            // ال → لا
    "\u0630" to "\u0638",                        // ذ → ظ
    "\u062F" to "\u0636",                        // د → ض (nearby)
    "\u0633" to "\u0634",                        // س → ش
    "\u062A" to "\u0629",                        // ت → ة
    "\u0631" to "\u0632",                        // ر → ز
    // Punctuation
    "." to "…·•°","," to ";:'","?" to "¿‽","!" to "¡",
    "-" to "–—~","/" to "\\|","'" to "'\u2018\u2019`","\"" to "\u201C\u201D\u00AB\u00BB",
    "$" to "€£¥₹¢","&" to "§","0" to "°","(" to "[{<",") " to "]}>",
    "#" to "№","%" to "‰","*" to "†‡","=" to "≠≈±",
)

private val NUM_ROW = listOf(K("1"),K("2"),K("3"),K("4"),K("5"),K("6"),K("7"),K("8"),K("9"),K("0"))

// ── Layouts (Arabic reduced to 10 keys/row, extras on long-press) ──
private val QWERTY = listOf(
    listOf(K("q"),K("w"),K("e"),K("r"),K("t"),K("y"),K("u"),K("i"),K("o"),K("p")),
    listOf(K("a"),K("s"),K("d"),K("f"),K("g"),K("h"),K("j"),K("k"),K("l")),
    listOf(K("\u21E7",KT.SHIFT,1.5f),K("z"),K("x"),K("c"),K("v"),K("b"),K("n"),K("m"),K("\u232B",KT.BS,1.5f)),
    listOf(K("123",KT.NUM,1.3f),K(","),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)

// (#1) Arabic: 10 keys per row (same as English), less-common chars via long-press
private val ARABIC_LO = listOf(
    listOf(K("\u0636"),K("\u0635"),K("\u062B"),K("\u0642"),K("\u0641"),K("\u063A"),K("\u0639"),K("\u0647"),K("\u062E"),K("\u062D")),
    listOf(K("\u0634"),K("\u0633"),K("\u064A"),K("\u0628"),K("\u0644"),K("\u0627"),K("\u062A"),K("\u0646"),K("\u0645"),K("\u0643")),
    listOf(K("\u21E7",KT.SHIFT,1.5f),K("\u0630"),K("\u062F"),K("\u0621"),K("\u0631"),K("\u0649"),K("\u0629"),K("\u0648"),K("\u0632"),K("\u232B",KT.BS,1.5f)),
    listOf(K("123",KT.NUM,1.3f),K("\u060C"),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)
private val ARABIC_HI = listOf(
    listOf(K("\u064E"),K("\u064B"),K("\u064F"),K("\u064C"),K("\u0650"),K("\u064D"),K("\u0651"),K("\u0652"),K("\u0622"),K("\u0623")),
    listOf(K("\u0625"),K("\u0644\u0627"),K("\u0626"),K("\u0624"),K("{"),K("}"),K("["),K("]"),K("\u061B"),K(":")),
    listOf(K("\u21E7",KT.SHIFT,1.5f),K("!"),K("@"),K("#"),K("-"),K("+"),K("="),K("/"),K("\u061F"),K("\u232B",KT.BS,1.5f)),
    listOf(K("123",KT.NUM,1.3f),K("\u060C"),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)

private val NUMS = listOf(
    listOf(K("@"),K("#"),K("$"),K("%"),K("&"),K("*"),K("-"),K("+"),K("=")),
    listOf(K("/"),K("\\"),K("|"),K("{"),K("}"),K("["),K("]"),K("<"),K(">")),
    listOf(K("#+=",KT.SYM,1.5f),K("\""),K("'"),K(":"),K(";"),K("!"),K("?"),K("_"),K("\u232B",KT.BS,1.5f)),
    listOf(K("ABC",KT.ALPHA,1.3f),K(","),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)
private val SYMS = listOf(
    listOf(K("~"),K("`"),K("\u00A3"),K("\u20AC"),K("\u00A5"),K("^"),K("\u00B0"),K("\u00A9"),K("\u00AE")),
    listOf(K("\u2022"),K("\u221A"),K("\u03C0"),K("\u00F7"),K("\u00D7"),K("\u2026"),K("\u00BF"),K("\u00A1"),K("\u2122")),
    listOf(K("123",KT.NUM,1.5f),K("\u00AB"),K("\u00BB"),K("\u201C"),K("\u201D"),K("\u2018"),K("\u2019"),K("\u2013"),K("\u232B",KT.BS,1.5f)),
    listOf(K("ABC",KT.ALPHA,1.3f),K(","),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)

// ── Emoji + Stickers ───────────────────────────────────────
private data class EC(val icon: String, val label: String, val items: List<String>)
private val EMOJIS = listOf(
    EC("\uD83D\uDE00","Smileys",listOf("\uD83D\uDE00","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE01","\uD83D\uDE06","\uD83D\uDE05","\uD83E\uDD23","\uD83D\uDE02","\uD83D\uDE42","\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE07","\uD83E\uDD70","\uD83D\uDE0D","\uD83E\uDD29","\uD83D\uDE18","\uD83D\uDE1A","\uD83D\uDE19","\uD83D\uDE0B","\uD83D\uDE1B","\uD83D\uDE1C","\uD83E\uDD2A","\uD83D\uDE1D","\uD83E\uDD14","\uD83D\uDE10","\uD83D\uDE0F","\uD83D\uDE12","\uD83D\uDE44","\uD83D\uDE24","\uD83D\uDE20","\uD83D\uDE21","\uD83D\uDE22","\uD83D\uDE2D","\uD83D\uDE25","\uD83D\uDE28","\uD83D\uDE31","\uD83D\uDE33","\uD83D\uDE34","\uD83D\uDE0E","\uD83E\uDD71")),
    EC("\u2764\uFE0F","Love",listOf("\u2764\uFE0F","\uD83E\uDDE1","\uD83D\uDC9B","\uD83D\uDC9A","\uD83D\uDC99","\uD83D\uDC9C","\uD83D\uDDA4","\uD83D\uDC94","\uD83D\uDC95","\uD83D\uDC93","\uD83D\uDC97","\uD83D\uDC96","\uD83D\uDC4D","\uD83D\uDC4E","\u270C\uFE0F","\uD83E\uDD1E","\uD83E\uDD18","\uD83D\uDC4C","\uD83D\uDC4B","\uD83D\uDC4F","\uD83D\uDE4F","\uD83D\uDCAA","\uD83E\uDD1D","\uD83D\uDC4A")),
    EC("\uD83D\uDC31","Animals",listOf("\uD83D\uDC36","\uD83D\uDC31","\uD83D\uDC2D","\uD83D\uDC39","\uD83D\uDC30","\uD83E\uDD8A","\uD83D\uDC3B","\uD83D\uDC3C","\uD83D\uDC28","\uD83D\uDC2F","\uD83E\uDD81","\uD83D\uDC2E","\uD83D\uDC37","\uD83D\uDC35","\uD83D\uDC14","\uD83D\uDC27","\uD83D\uDC26","\uD83D\uDC1D","\uD83E\uDD8B","\uD83D\uDC20","\uD83D\uDC19","\uD83E\uDD80","\uD83D\uDC22","\uD83E\uDD8E")),
    EC("\uD83C\uDF4E","Food",listOf("\uD83C\uDF4E","\uD83C\uDF4A","\uD83C\uDF4B","\uD83C\uDF4C","\uD83C\uDF49","\uD83C\uDF47","\uD83C\uDF53","\uD83C\uDF45","\uD83C\uDF54","\uD83C\uDF55","\uD83C\uDF2E","\uD83C\uDF73","\uD83C\uDF5E","\uD83C\uDF70","\uD83C\uDF69","\uD83C\uDF66","\u2615","\uD83C\uDF7A","\uD83C\uDF77","\uD83E\uDD42","\uD83C\uDF75","\uD83E\uDDC3","\uD83E\uDD64","\uD83C\uDF7D\uFE0F")),
    EC("\u26BD","Sports",listOf("\u26BD","\uD83C\uDFC0","\uD83C\uDFC8","\uD83C\uDFBE","\uD83C\uDFC9","\uD83C\uDFB1","\u26F3","\uD83C\uDFAE","\uD83C\uDFB5","\uD83C\uDFB6","\uD83C\uDFA4","\uD83C\uDFAC","\uD83C\uDFA8","\uD83C\uDFC6","\uD83E\uDD47","\uD83E\uDD48","\uD83E\uDD49","\uD83C\uDFAF","\uD83C\uDFB2","\uD83C\uDFAD","\uD83C\uDFAA","\uD83C\uDFBF","\uD83C\uDFC2","\uD83C\uDFC4")),
    EC("\uD83D\uDE97","Travel",listOf("\uD83D\uDE97","\uD83D\uDE95","\uD83D\uDE8C","\uD83D\uDE93","\uD83D\uDE91","\uD83D\uDEB2","\u2708\uFE0F","\uD83D\uDE80","\uD83D\uDE82","\u26F5","\uD83C\uDFE0","\uD83C\uDFEB","\u26EA","\uD83D\uDD4C","\uD83C\uDFF0","\uD83D\uDDFC","\u2600\uFE0F","\uD83C\uDF08","\u2B50","\uD83C\uDF19","\u2744\uFE0F","\uD83C\uDF3B","\uD83C\uDF34","\uD83C\uDF0A")),
    // (#5) Sticker combos - popular emoji combinations
    EC("\uD83C\uDF1F","Stickers",listOf(
        "\uD83D\uDE0D\u2764\uFE0F","\uD83D\uDE4F\u2728","\uD83D\uDD25\uD83D\uDCAF","\uD83C\uDF89\uD83C\uDF8A","\uD83D\uDC4D\uD83D\uDCAA",
        "\uD83D\uDE02\uD83D\uDE02\uD83D\uDE02","\u2764\uFE0F\uD83D\uDD25","\uD83E\uDD29\uD83C\uDF1F","\uD83D\uDE0E\uD83D\uDC4D","\uD83E\uDD17\u2764\uFE0F",
        "\uD83C\uDF39\u2764\uFE0F","\uD83D\uDE18\uD83D\uDC95","\uD83D\uDC4B\uD83D\uDE00","\uD83D\uDE4C\uD83C\uDF89","\uD83D\uDCAF\u2705",
        "\uD83D\uDC9C\uD83E\uDDE1\uD83D\uDC9B\uD83D\uDC9A\uD83D\uDC99","\u2728\uD83C\uDF1F\u2B50","\uD83C\uDF1E\uD83C\uDF3B\uD83C\uDF3A","\uD83C\uDF08\u2728\uD83C\uDF1F","\uD83D\uDE80\uD83C\uDF1F\u2728",
        "\uD83E\uDD23\uD83D\uDC80","\uD83D\uDE31\uD83D\uDE31\uD83D\uDE31","\uD83E\uDD7A\uD83D\uDC94","\uD83D\uDE4F\uD83C\uDFFB\u2764\uFE0F","\uD83D\uDCAA\uD83D\uDD25",
        "\u270B\uD83C\uDFFB\uD83D\uDE0A","GG \uD83C\uDFC6","\uD83C\uDF82\uD83C\uDF89\uD83C\uDF88","\uD83C\uDF1D\u2728","LOL \uD83D\uDE02",
        "OMG \uD83D\uDE31","BRB \uD83D\uDC4B","TY \u2764\uFE0F","GM \u2600\uFE0F","GN \uD83C\uDF19\u2728",
        "ILY \u2764\uFE0F\uD83D\uDD25","\uD83D\uDC68\u200D\uD83D\uDCBB","\uD83D\uDC69\u200D\uD83D\uDCBB","\uD83E\uDDD1\u200D\uD83C\uDFA8","\uD83C\uDFC3\u200D\u2642\uFE0F\uD83D\uDCA8",
    )),
    // GIF tab handled separately via GIPHY API
    EC("GIF","GIF", emptyList()),
)

// GIPHY API (free beta key for development — 100 requests/hour)
private const val GIPHY_KEY = "dc6zaTOxFJmzC"
private const val GIPHY_BASE = "https://api.giphy.com/v1/gifs"

private object C {
    val BG = Color.parseColor("#C8CAD0"); val KEY = Color.WHITE; val KEY_P = Color.parseColor("#B8BAC0")
    val SP = Color.parseColor("#A4A8B4"); val SP_P = Color.parseColor("#9498A4")
    val ENT = Color.parseColor("#4285F4"); val ENT_P = Color.parseColor("#3275E4")
    val TXT = Color.parseColor("#1B1B1F"); val TXT_L = Color.parseColor("#5F6368")
    val SFC = Color.parseColor("#F5F5F7"); val PRI = Color.parseColor("#4285F4")
    val SEND = Color.parseColor("#E8F0FE"); val DIV = Color.parseColor("#DADCE0")
    val ERR = Color.parseColor("#EA4335"); val GRN = Color.parseColor("#34A853")
    val TB = Color.parseColor("#ECEDF1"); val EMOJI_SEL = Color.parseColor("#DCE4F8")
    val NUM_BG = Color.parseColor("#E8E9ED"); val NUM_P = Color.parseColor("#D0D1D6")
    val POPUP_BG = Color.parseColor("#333333"); val POPUP_TXT = Color.WHITE
}

class TranslatorInputMethodService : InputMethodService() {

    private var cachedView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var shifted = false; private var layoutId = "qwerty"; private var baseLay = "qwerty"
    private var tgtIdx = 1; private var detLang: String? = null; private var result: String? = null
    private var busy = false; private var err: String? = null; private var emojiOn = false
    private var numRowOn = true; private var emojiCat = 0; private var hasTyped = false
    private var altPopup: PopupWindow? = null
    private var gifGrid: LinearLayout? = null
    private var gifDebounce: Timer? = null

    private var kbWrap: LinearLayout? = null; private var numRowView: LinearLayout? = null
    private var kbRows: LinearLayout? = null; private var emojiWrap: LinearLayout? = null
    private var transView: TextView? = null; private var transBtn: TextView? = null
    private var transRow: LinearLayout? = null; private var actRow: LinearLayout? = null
    private var srcLang: TextView? = null; private var tgtLang: TextView? = null
    private var bsTimer: Timer? = null

    private val KH = 48; private val KR = 8f; private val KM = 3; private val NKH = 38
    private val lay: List<List<K>> get() = when (layoutId) {
        "arabic" -> if (shifted) ARABIC_HI else ARABIC_LO
        "nums" -> NUMS; "syms" -> SYMS; else -> QWERTY
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting); if (!restarting) clearAll(); if (emojiOn) hideEmoji()
    }
    override fun onUpdateSelection(osS: Int, osE: Int, nsS: Int, nsE: Int, cS: Int, cE: Int) {
        super.onUpdateSelection(osS, osE, nsS, nsE, cS, cE)
        if (nsS == 0 && nsE == 0 && osS > 0) clearAll()
        if (nsS > osS + 1 && nsS == nsE) { hasTyped = true; updateTBtn() }
    }
    override fun onFinishInputView(f: Boolean) { super.onFinishInputView(f); stopBs(); dismissAlt() }
    override fun onDestroy() { stopBs(); dismissAlt(); gifDebounce?.cancel(); handler.removeCallbacksAndMessages(null); cachedView = null; super.onDestroy() }

    override fun onCreateInputView(): View {
        cachedView?.let { return it }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C.BG); fitsSystemWindows = true }
        root.addView(buildStrip()); root.addView(buildToolbar())
        val kw = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; kbWrap = kw
        val nr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(4),dp(2),dp(4),0) }
        buildNumRow(nr); numRowView = nr; kw.addView(nr)
        val kr = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3),dp(3),dp(3),dp(8)) }
        buildRows(kr); kbRows = kr; kw.addView(kr); root.addView(kw)
        val ew = buildEmoji(); ew.visibility = View.GONE; emojiWrap = ew; root.addView(ew)
        cachedView = root; return root
    }

    private fun buildToolbar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(C.TB); setPadding(dp(6),dp(4),dp(6),dp(4))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36))
        addView(tbBtn("\uD83C\uDF10") { toggleLang() })
        addView(tbBtn("\uD83D\uDE00") { if (emojiOn) hideEmoji() else showEmoji() })
        addView(tbBtn("\uD83D\uDCCB") { pasteClip() })
        addView(View(this@TranslatorInputMethodService).apply { layoutParams = LinearLayout.LayoutParams(0,0,1f) })
        addView(tbBtn("123") { numRowOn = !numRowOn; numRowView?.visibility = if (numRowOn) View.VISIBLE else View.GONE })
    }

    private fun tbBtn(l: String, a: () -> Unit) = TextView(this).apply {
        text = l; setTextSize(TypedValue.COMPLEX_UNIT_SP, if (l.length <= 2) 18f else 13f)
        setTextColor(C.TXT_L); gravity = Gravity.CENTER; setPadding(dp(10),dp(2),dp(10),dp(2))
        setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); a() }
    }

    private fun pasteClip() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount > 0) { val t = clip.getItemAt(0).coerceToText(this).toString(); if (t.isNotEmpty()) { currentInputConnection?.commitText(t,1); hasTyped = true; updateTBtn() } }
    }

    // ── Translation Strip ──────────────────────────────────

    private fun buildStrip(): View {
        val s = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C.SFC) }
        val lb = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(6),dp(8),dp(4)) }
        srcLang = TextView(this).apply { text = "Auto"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(C.TXT_L); background = rr(C.DIV, dp(6).toFloat()); setPadding(dp(8),dp(4),dp(8),dp(4)) }
        lb.addView(srcLang)
        lb.addView(TextView(this).apply { text = " \u21C4 "; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTextColor(C.PRI) })
        tgtLang = TextView(this).apply { updTgt(); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(C.PRI); background = rr(C.SEND, dp(6).toFloat()); setPadding(dp(8),dp(4),dp(8),dp(4)); setOnClickListener { showLMenu(it) } }
        lb.addView(tgtLang)
        lb.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0,0,1f) })
        transBtn = TextView(this).apply { text = " Translate \u25B6 "; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); background = rr(C.GRN, dp(14).toFloat()); setPadding(dp(10),dp(5),dp(10),dp(5)); gravity = Gravity.CENTER; visibility = View.GONE; setOnClickListener { doTranslate() } }
        lb.addView(transBtn)
        lb.addView(TextView(this).apply { text = "\u2715"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextColor(C.ERR); gravity = Gravity.CENTER; setPadding(dp(8),0,dp(4),0); setOnClickListener { clearAll() } })
        s.addView(lb); s.addView(div())

        val tr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12),dp(4),dp(8),dp(4)); visibility = View.GONE }
        transRow = tr
        transView = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextColor(C.PRI); setTypeface(null, Typeface.BOLD); maxLines = 3; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        tr.addView(transView); s.addView(tr)

        val ar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(2),dp(8),dp(4)); visibility = View.GONE }
        actRow = ar
        ar.addView(aBtn("\u21C4 Replace", C.PRI) { replaceTrans() })
        ar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(6),0) })
        ar.addView(aBtn("Send \u25B6", C.GRN) { sendTrans() })
        s.addView(ar); s.addView(div()); return s
    }

    private fun aBtn(l: String, c: Int, a: () -> Unit) = TextView(this).apply {
        text = " $l "; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
        background = rr(c, dp(14).toFloat()); setPadding(dp(12),dp(6),dp(12),dp(6)); gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { a() }
    }
    private fun showLMenu(a: View) { val p = PopupMenu(this, a); for ((i, l) in LANGS.withIndex()) p.menu.add(0,i,i,"${l.flag} ${l.name}"); p.setOnMenuItemClickListener { tgtIdx = it.itemId; tgtLang?.updTgt(); true }; p.show() }

    // ── Long-press alternatives popup (#2) ─────────────────

    private fun showAltPopup(anchor: View, key: String) {
        val alts = ALTS[key.lowercase()] ?: return
        dismissAlt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rr(C.POPUP_BG, dp(10).toFloat())
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        for (ch in alts) {
            row.addView(TextView(this).apply {
                text = if (shifted && key[0].isLetter()) ch.uppercase() else ch.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTextColor(C.POPUP_TXT)
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(8), dp(14), dp(8))
                background = rr(Color.TRANSPARENT, dp(6).toFloat())
                setOnClickListener { v ->
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val txt = (v as TextView).text.toString()
                    currentInputConnection?.commitText(txt, 1)
                    hasTyped = true; updateTBtn()
                    dismissAlt()
                }
            })
        }

        val pw = PopupWindow(row, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        pw.isOutsideTouchable = true
        pw.setOnDismissListener { altPopup = null }

        try {
            val xOff = -(row.childCount * dp(20)) / 2 + anchor.width / 2
            pw.showAsDropDown(anchor, xOff, -(anchor.height + dp(54)))
            altPopup = pw
        } catch (e: Exception) {
            Log.d(TAG, "Popup show failed: ${e.message}")
        }
    }

    private fun dismissAlt() { altPopup?.dismiss(); altPopup = null }

    // ── Number Row ─────────────────────────────────────────

    private fun buildNumRow(c: LinearLayout) {
        c.removeAllViews()
        for (k in NUM_ROW) c.addView(TextView(this).apply {
            text = k.l; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTextColor(C.TXT); gravity = Gravity.CENTER
            background = keyBg(C.NUM_BG, C.NUM_P)
            layoutParams = LinearLayout.LayoutParams(0, dp(NKH), 1f).apply { marginStart = dp(KM); marginEnd = dp(KM) }
            isClickable = true; isFocusable = true
            setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); typeCh(k.l) }
            setOnLongClickListener { showAltPopup(it, k.l); true }
        })
    }

    // ── Keyboard Rows ──────────────────────────────────────

    private fun buildRows(c: LinearLayout) {
        c.removeAllViews()
        for ((ri, row) in lay.withIndex()) {
            val rv = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(KH)).apply { bottomMargin = dp(KM) } }
            if (ri == 1 && layoutId in listOf("qwerty","arabic")) rv.setPadding(dp(16),0,dp(16),0)
            for (k in row) rv.addView(mkKey(k))
            c.addView(rv)
        }
    }

    private fun mkKey(k: K): View {
        val spec = k.t !in listOf(KT.CH, KT.SPACE); val ent = k.t == KT.ENTER
        val bgN = when { ent -> C.ENT; spec -> C.SP; else -> C.KEY }
        val bgP = when { ent -> C.ENT_P; spec -> C.SP_P; else -> C.KEY_P }
        val tc = when { ent -> Color.WHITE; k.t == KT.SHIFT && shifted -> C.PRI; else -> C.TXT }
        val lbl = when (k.t) { KT.SPACE -> if (layoutId == "arabic") "\u0645\u0633\u0627\u0641\u0629" else "space"; KT.CH -> if (shifted && layoutId == "qwerty") k.l.uppercase() else k.l; else -> k.l }
        val fs = when { k.t == KT.SPACE -> 13f; spec -> 15f; layoutId == "arabic" -> 20f; else -> 20f }

        return TextView(this).apply {
            text = lbl; setTextSize(TypedValue.COMPLEX_UNIT_SP, fs); setTextColor(tc); gravity = Gravity.CENTER
            background = keyBg(bgN, bgP)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, k.w).apply { marginStart = dp(KM); marginEnd = dp(KM) }
            isClickable = true; isFocusable = true

            if (k.t == KT.BS) {
                setOnTouchListener { v, ev -> when (ev.action) {
                    MotionEvent.ACTION_DOWN -> { v.isPressed = true; v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); hk(k); startBs(); true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; stopBs(); true }
                    else -> false
                } }
            } else {
                setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); hk(k) }
                // (#2) Long-press for alternatives on character, comma, dot, etc.
                if (k.t == KT.CH || k.t == KT.SPACE) {
                    if (ALTS.containsKey(k.l.lowercase()) || ALTS.containsKey(k.l)) {
                        setOnLongClickListener { v -> showAltPopup(v, k.l); true }
                    }
                }
            }
        }
    }

    private fun hk(k: K) {
        when (k.t) {
            KT.CH -> { typeCh(if (shifted && layoutId == "qwerty") k.l.uppercase() else k.l); if (shifted && layoutId != "arabic") { shifted = false; reK() } }
            KT.SPACE -> typeCh(" ")
            KT.BS -> doBs()
            KT.ENTER -> currentInputConnection?.commitText("\n", 1)
            KT.SHIFT -> { shifted = !shifted; reK() }
            KT.NUM -> { layoutId = "nums"; shifted = false; reK() }
            KT.ALPHA -> { layoutId = baseLay; shifted = false; reK() }
            KT.SYM -> { layoutId = "syms"; shifted = false; reK() }
            KT.GLOBE -> toggleLang()
            KT.EMOJI -> { if (emojiOn) hideEmoji() else showEmoji() }
        }
    }

    private fun toggleLang() { baseLay = if (baseLay == "qwerty") "arabic" else "qwerty"; layoutId = baseLay; shifted = false; reK() }
    private fun typeCh(ch: String) { currentInputConnection?.commitText(ch,1); hasTyped = true; updateTBtn() }

    private fun doBs() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (sel != null && sel.isNotEmpty()) ic.commitText("",1) else ic.deleteSurroundingText(1,0)
        val rem = ic.getTextBeforeCursor(1,0); if (rem == null || rem.isEmpty()) hasTyped = false
        updateTBtn()
    }
    private fun startBs() { stopBs(); bsTimer = Timer().apply { schedule(object : TimerTask() { override fun run() { handler.post { doBs() } } }, 400, 60) } }
    private fun stopBs() { bsTimer?.cancel(); bsTimer = null }

    // ── Translation ────────────────────────────────────────

    private fun doTranslate() {
        val ic = currentInputConnection ?: return
        val text = (ic.getTextBeforeCursor(5000,0)?.toString() ?: "").trim()
        if (text.length < 2) return
        busy = true; err = null; result = null; transBtn?.visibility = View.GONE; updateTUI()
        val tl = LANGS[tgtIdx].code

        Thread {
            var res: String? = null; var det: String? = null; var e: String? = null
            try {
                val enc = URLEncoder.encode(text, "UTF-8")
                val u = URL("https://api.mymemory.translated.net/get?q=$enc&langpair=autodetect|$tl&de=translator-keyboard@app.com")
                val c = (u.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                if (c.responseCode == 200) { val j = JSONObject(c.inputStream.bufferedReader().readText()); if (j.optInt("responseStatus") == 200) { val rd = j.getJSONObject("responseData"); val t = rd.optString("translatedText",""); if (t.isNotEmpty()) { res = t; det = rd.optString("detectedLanguage",null) } } }; c.disconnect()
            } catch (x: Exception) { Log.d(TAG, "MyMemory: ${x.message}") }
            if (res == null) try {
                val enc = URLEncoder.encode(text, "UTF-8")
                val u = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$tl&dt=t&q=$enc")
                val c = (u.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                if (c.responseCode == 200) { val a = JSONArray(c.inputStream.bufferedReader().readText()); val segs = a.getJSONArray(0); val sb = StringBuilder(); for (i in 0 until segs.length()) sb.append(segs.getJSONArray(i).optString(0,"")); if (sb.isNotEmpty()) { res = sb.toString(); det = a.optString(2,null) } }; c.disconnect()
            } catch (x: Exception) { Log.d(TAG, "Google: ${x.message}"); e = "Translation failed" }
            val fr = res; val fd = det; val fe = e
            handler.post { busy = false; if (fr != null) { result = fr; detLang = fd; err = null } else { err = fe ?: "Translation failed"; result = null }; updateTUI(); updateTBtn(); updateSrc() }
        }.start()
    }

    private fun replaceTrans() { val t = result ?: return; val ic = currentInputConnection ?: return; val cl = (ic.getTextBeforeCursor(5000,0)?.length ?: 0); ic.deleteSurroundingText(cl,0); ic.commitText(t,1); result = null; updateTUI(); updateTBtn() }
    private fun sendTrans() { val t = result ?: return; val ic = currentInputConnection ?: return; val cl = (ic.getTextBeforeCursor(5000,0)?.length ?: 0); ic.deleteSurroundingText(cl,0); ic.commitText(t,1); clearAll() }

    // ── Emoji + Stickers + GIF Panel ───────────────────────

    private fun buildEmoji(): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C.SFC) }
        val ts = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setBackgroundColor(Color.WHITE) }
        val tr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4),dp(4),dp(4),dp(4)) }
        for ((i, cat) in EMOJIS.withIndex()) {
            tr.addView(TextView(this).apply {
                text = cat.icon; setTextSize(TypedValue.COMPLEX_UNIT_SP, if (cat.label == "GIF") 13f else if (cat.label == "Stickers") 14f else 20f)
                if (cat.label == "GIF") setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER; setPadding(dp(8),dp(4),dp(8),dp(4))
                background = if (i == 0) rr(C.EMOJI_SEL, dp(8).toFloat()) else null; tag = i
                setOnClickListener { selECat(i, tr) }
            })
        }
        tr.addView(TextView(this).apply { text = "ABC"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextColor(C.PRI); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(dp(12),dp(4),dp(12),dp(4)); background = rr(C.SEND, dp(8).toFloat()); setOnClickListener { hideEmoji() } })
        ts.addView(tr); c.addView(ts)

        val gs = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190)) }
        val g = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4),dp(4),dp(4),dp(4)); tag = "eg" }
        buildEGrid(g, 0); gs.addView(g); c.addView(gs)

        val br = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(4),dp(2),dp(4),dp(8)); setBackgroundColor(C.BG); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(KH)) }
        br.addView(mkKey(K("\u232B",KT.BS,1.5f))); br.addView(mkKey(K(" ",KT.SPACE,5f))); br.addView(mkKey(K("\u23CE",KT.ENTER,1.5f)))
        c.addView(br); return c
    }

    private fun buildEGrid(c: LinearLayout, ci: Int) {
        c.removeAllViews(); val cat = EMOJIS[ci]
        val isSticker = cat.label == "Stickers"; val isGif = cat.label == "GIF"

        if (isGif) {
            buildGifPanel(c)
            return
        }

        val cols = if (isSticker) 4 else 8
        var row: LinearLayout? = null
        for ((i, e) in cat.items.withIndex()) {
            if (i % cols == 0) { row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0,dp(2),0,dp(2)) }; c.addView(row) }
            row?.addView(TextView(this).apply {
                text = e; gravity = Gravity.CENTER
                if (isSticker) { setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f); setPadding(dp(6),dp(10),dp(6),dp(10)); background = rr(C.NUM_BG, dp(8).toFloat()); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) } }
                else { setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f); setPadding(dp(4),dp(7),dp(4),dp(7)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); currentInputConnection?.commitText(e,1) }
            })
        }
        val rem = cols - (cat.items.size % cols); if (rem in 1 until cols) for (j in 0 until rem) row?.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0,0,1f) })
    }

    // ── GIF Search Panel (GIPHY API + Glide) ───────────────

    private fun buildGifPanel(container: LinearLayout) {
        // Search bar
        val searchBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            gravity = Gravity.CENTER_VERTICAL
        }

        val searchIcon = TextView(this).apply {
            text = "\uD83D\uDD0D"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(8), 0, dp(4), 0)
        }
        searchBar.addView(searchIcon)

        val searchInput = EditText(this).apply {
            hint = "Search GIFs..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(C.TXT); setHintTextColor(C.TXT_L)
            background = rr(Color.WHITE, dp(20).toFloat())
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            // Don't let this EditText steal focus from the host app
            isFocusable = true
            isFocusableInTouchMode = true
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                gifDebounce?.cancel()
                val query = s?.toString()?.trim() ?: ""
                gifDebounce = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            handler.post {
                                if (query.length >= 2) searchGifs(query)
                                else loadTrendingGifs()
                            }
                        }
                    }, 500)
                }
            }
        })
        searchBar.addView(searchInput)
        container.addView(searchBar)

        // GIF grid container
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        gifGrid = grid
        container.addView(grid)

        // Load trending GIFs on open
        loadTrendingGifs()
    }

    private fun loadTrendingGifs() {
        fetchGifs("$GIPHY_BASE/trending?api_key=$GIPHY_KEY&limit=18&rating=pg")
    }

    private fun searchGifs(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        fetchGifs("$GIPHY_BASE/search?api_key=$GIPHY_KEY&q=$encoded&limit=18&rating=pg")
    }

    private fun fetchGifs(url: String) {
        val grid = gifGrid ?: return

        // Show loading
        handler.post {
            grid.removeAllViews()
            grid.addView(TextView(this).apply {
                text = "Loading GIFs..."; setTextColor(C.TXT_L)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER; setPadding(0, dp(20), 0, dp(20))
            })
        }

        Thread {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 8000
                }
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val data = json.getJSONArray("data")
                    val gifs = mutableListOf<Pair<String, String>>() // (previewUrl, fullUrl)

                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val images = item.getJSONObject("images")
                        val preview = images.getJSONObject("fixed_width_small").optString("url", "")
                        val full = images.getJSONObject("fixed_width").optString("url", "")
                        if (preview.isNotEmpty() && full.isNotEmpty()) {
                            gifs.add(Pair(preview, full))
                        }
                    }

                    handler.post { displayGifs(gifs) }
                } else {
                    handler.post { showGifError("Failed to load GIFs") }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.d(TAG, "GIF fetch error: ${e.message}")
                handler.post { showGifError("Network error") }
            }
        }.start()
    }

    private fun displayGifs(gifs: List<Pair<String, String>>) {
        val grid = gifGrid ?: return
        grid.removeAllViews()

        if (gifs.isEmpty()) {
            grid.addView(TextView(this).apply {
                text = "No GIFs found"; setTextColor(C.TXT_L)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER; setPadding(0, dp(20), 0, dp(20))
            })
            return
        }

        val cols = 3
        var row: LinearLayout? = null

        for ((i, gif) in gifs.withIndex()) {
            if (i % cols == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(0, dp(2), 0, dp(2))
                }
                grid.addView(row)
            }

            val imgView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(80), 1f).apply {
                    marginStart = dp(2); marginEnd = dp(2)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rr(C.NUM_BG, dp(6).toFloat())
                clipToOutline = false

                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    // Insert GIF URL as text (apps that support rich content will render it)
                    currentInputConnection?.commitText(gif.second, 1)
                }
            }

            try {
                Glide.with(applicationContext)
                    .asGif()
                    .load(gif.first)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imgView)
            } catch (e: Exception) {
                Log.d(TAG, "Glide load failed: ${e.message}")
            }

            row?.addView(imgView)
        }

        // Fill last row
        val rem = cols - (gifs.size % cols)
        if (rem in 1 until cols) {
            for (j in 0 until rem) {
                row?.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(80), 1f)
                })
            }
        }

        // GIPHY attribution (required by TOS)
        grid.addView(TextView(this).apply {
            text = "Powered by GIPHY"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(C.TXT_L)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(4))
        })
    }

    private fun showGifError(msg: String) {
        val grid = gifGrid ?: return
        grid.removeAllViews()
        grid.addView(TextView(this).apply {
            text = msg; setTextColor(C.ERR)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER; setPadding(0, dp(20), 0, dp(20))
        })
    }

    private fun selECat(i: Int, tr: LinearLayout) { emojiCat = i; for (x in 0 until tr.childCount - 1) (tr.getChildAt(x) as? TextView)?.background = if (x == i) rr(C.EMOJI_SEL, dp(8).toFloat()) else null; emojiWrap?.findViewWithTag<LinearLayout>("eg")?.let { buildEGrid(it, i) } }
    private fun showEmoji() { emojiOn = true; kbWrap?.visibility = View.GONE; emojiWrap?.visibility = View.VISIBLE }
    private fun hideEmoji() { emojiOn = false; emojiWrap?.visibility = View.GONE; kbWrap?.visibility = View.VISIBLE }

    private fun updateTBtn() { transBtn?.visibility = if (hasTyped && result == null && !busy) View.VISIBLE else View.GONE }
    private fun updateTUI() { val tr = transRow ?: return; val ar = actRow ?: return; when { busy -> { tr.visibility = View.VISIBLE; transView?.text = "Translating\u2026"; transView?.setTextColor(C.TXT_L); transView?.setTypeface(null, Typeface.ITALIC); ar.visibility = View.GONE }; err != null -> { tr.visibility = View.VISIBLE; transView?.text = err; transView?.setTextColor(C.ERR); transView?.setTypeface(null, Typeface.NORMAL); ar.visibility = View.GONE }; result != null -> { tr.visibility = View.VISIBLE; transView?.text = result; transView?.setTextColor(C.PRI); transView?.setTypeface(null, Typeface.BOLD); ar.visibility = View.VISIBLE }; else -> { tr.visibility = View.GONE; ar.visibility = View.GONE } } }
    private fun updateSrc() { val l = LANGS.find { it.code == detLang }; srcLang?.text = if (l != null) "${l.flag} ${l.name}" else "Auto" }
    private fun TextView.updTgt() { val l = LANGS[tgtIdx]; text = "${l.flag} ${l.name} \u25BC" }
    private fun clearAll() { hasTyped = false; result = null; err = null; detLang = null; busy = false; clearTrans(); updateTBtn(); updateSrc() }
    private fun clearTrans() { result = null; err = null; busy = false; updateTUI() }
    private fun reK() { kbRows?.let { buildRows(it) } }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun rr(c: Int, r: Float) = GradientDrawable().apply { setColor(c); cornerRadius = r }
    private fun keyBg(n: Int, p: Int): StateListDrawable { val r = dp(KR.toInt()).toFloat(); return StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_pressed), rr(p, r)); addState(intArrayOf(), rr(n, r)) } }
    private fun div() = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(C.DIV) }
}
