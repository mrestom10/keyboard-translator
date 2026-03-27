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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.inputmethodservice.InputMethodService
import android.view.inputmethod.EditorInfo
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
    LangInfo("en", "English", "\uD83C\uDDEC\uD83C\uDDE7"),
    LangInfo("ar", "Arabic", "\uD83C\uDDF8\uD83C\uDDE6"),
    LangInfo("fr", "French", "\uD83C\uDDEB\uD83C\uDDF7"),
    LangInfo("de", "German", "\uD83C\uDDE9\uD83C\uDDEA"),
)

enum class KT { CH, BS, SHIFT, SPACE, ENTER, NUM, ALPHA, SYM, GLOBE, EMOJI }
data class K(val l: String, val t: KT = KT.CH, val w: Float = 1f)

private val NUM_ROW = listOf(K("1"),K("2"),K("3"),K("4"),K("5"),K("6"),K("7"),K("8"),K("9"),K("0"))

private val QWERTY = listOf(
    listOf(K("q"),K("w"),K("e"),K("r"),K("t"),K("y"),K("u"),K("i"),K("o"),K("p")),
    listOf(K("a"),K("s"),K("d"),K("f"),K("g"),K("h"),K("j"),K("k"),K("l")),
    listOf(K("\u21E7",KT.SHIFT,1.5f),K("z"),K("x"),K("c"),K("v"),K("b"),K("n"),K("m"),K("\u232B",KT.BS,1.5f)),
    listOf(K("123",KT.NUM,1.3f),K(","),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)

// (#1) Arabic: all 28 letters + hamza forms + taa marbuta + alef maqsura
private val ARABIC_LO = listOf(
    listOf(K("\u0636"),K("\u0635"),K("\u062B"),K("\u0642"),K("\u0641"),K("\u063A"),K("\u0639"),K("\u0647"),K("\u062E"),K("\u062D"),K("\u062C")),
    listOf(K("\u0634"),K("\u0633"),K("\u064A"),K("\u0628"),K("\u0644"),K("\u0627"),K("\u062A"),K("\u0646"),K("\u0645"),K("\u0643"),K("\u0637")),
    listOf(K("\u21E7",KT.SHIFT,1.3f),K("\u0630"),K("\u062F"),K("\u0626"),K("\u0621"),K("\u0624"),K("\u0631"),K("\u0649"),K("\u0629"),K("\u0648"),K("\u0632"),K("\u0638"),K("\u232B",KT.BS,1.3f)),
    listOf(K("123",KT.NUM,1.3f),K("\u060C"),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)
private val ARABIC_HI = listOf(
    listOf(K("\u064E"),K("\u064B"),K("\u064F"),K("\u064C"),K("\u0650"),K("\u064D"),K("\u0651"),K("\u0652"),K("\u0622"),K("\u0623"),K("\u0625")),
    listOf(K("\u0644\u0627"),K("\u0640"),K("\u00AB"),K("\u00BB"),K("{"),K("}"),K("["),K("]"),K("\u061B"),K(":"),K("\u061F")),
    listOf(K("\u21E7",KT.SHIFT,1.3f),K("!"),K("@"),K("#"),K("&"),K("-"),K("+"),K("="),K("/"),K("\\"),K("%"),K("_"),K("\u232B",KT.BS,1.3f)),
    listOf(K("123",KT.NUM,1.3f),K("\u060C"),K(" ",KT.SPACE,4.5f),K("."),K("\u23CE",KT.ENTER,1.3f)),
)

// (#4) More special chars on first screen, common ones like / \ { } [ ] moved here
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

// ── Emoji + Stickers Data ──────────────────────────────────
private data class EC(val icon: String, val label: String, val items: List<String>)
private val EMOJIS = listOf(
    EC("\uD83D\uDE00","Smileys",listOf("\uD83D\uDE00","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE01","\uD83D\uDE06","\uD83D\uDE05","\uD83E\uDD23","\uD83D\uDE02","\uD83D\uDE42","\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE07","\uD83E\uDD70","\uD83D\uDE0D","\uD83E\uDD29","\uD83D\uDE18","\uD83D\uDE1A","\uD83D\uDE19","\uD83D\uDE0B","\uD83D\uDE1B","\uD83D\uDE1C","\uD83E\uDD2A","\uD83D\uDE1D","\uD83E\uDD14","\uD83D\uDE10","\uD83D\uDE0F","\uD83D\uDE12","\uD83D\uDE44","\uD83D\uDE24","\uD83D\uDE20","\uD83D\uDE21","\uD83D\uDE22","\uD83D\uDE2D","\uD83D\uDE25","\uD83D\uDE28","\uD83D\uDE31","\uD83D\uDE33","\uD83D\uDE34","\uD83D\uDE0E","\uD83E\uDD71")),
    EC("\u2764\uFE0F","Love",listOf("\u2764\uFE0F","\uD83E\uDDE1","\uD83D\uDC9B","\uD83D\uDC9A","\uD83D\uDC99","\uD83D\uDC9C","\uD83D\uDDA4","\uD83D\uDC94","\uD83D\uDC95","\uD83D\uDC93","\uD83D\uDC97","\uD83D\uDC96","\uD83D\uDC4D","\uD83D\uDC4E","\u270C\uFE0F","\uD83E\uDD1E","\uD83E\uDD18","\uD83D\uDC4C","\uD83D\uDC4B","\uD83D\uDC4F","\uD83D\uDE4F","\uD83D\uDCAA","\uD83E\uDD1D","\uD83D\uDC4A")),
    EC("\uD83D\uDC31","Animals",listOf("\uD83D\uDC36","\uD83D\uDC31","\uD83D\uDC2D","\uD83D\uDC39","\uD83D\uDC30","\uD83E\uDD8A","\uD83D\uDC3B","\uD83D\uDC3C","\uD83D\uDC28","\uD83D\uDC2F","\uD83E\uDD81","\uD83D\uDC2E","\uD83D\uDC37","\uD83D\uDC35","\uD83D\uDC14","\uD83D\uDC27","\uD83D\uDC26","\uD83D\uDC1D","\uD83E\uDD8B","\uD83D\uDC20","\uD83D\uDC19","\uD83E\uDD80","\uD83D\uDC22","\uD83E\uDD8E")),
    EC("\uD83C\uDF4E","Food",listOf("\uD83C\uDF4E","\uD83C\uDF4A","\uD83C\uDF4B","\uD83C\uDF4C","\uD83C\uDF49","\uD83C\uDF47","\uD83C\uDF53","\uD83C\uDF45","\uD83C\uDF54","\uD83C\uDF55","\uD83C\uDF2E","\uD83C\uDF73","\uD83C\uDF5E","\uD83C\uDF70","\uD83C\uDF69","\uD83C\uDF66","\u2615","\uD83C\uDF7A","\uD83C\uDF77","\uD83E\uDD42","\uD83C\uDF75","\uD83E\uDDC3","\uD83E\uDD64","\uD83C\uDF7D\uFE0F")),
    EC("\u26BD","Sports",listOf("\u26BD","\uD83C\uDFC0","\uD83C\uDFC8","\uD83C\uDFBE","\uD83C\uDFC9","\uD83C\uDFB1","\u26F3","\uD83C\uDFAE","\uD83C\uDFB5","\uD83C\uDFB6","\uD83C\uDFA4","\uD83C\uDFAC","\uD83C\uDFA8","\uD83C\uDFC6","\uD83E\uDD47","\uD83E\uDD48","\uD83E\uDD49","\uD83C\uDFAF","\uD83C\uDFB2","\uD83C\uDFAD","\uD83C\uDFAA","\uD83C\uDFBF","\uD83C\uDFC2","\uD83C\uDFC4")),
    EC("\uD83D\uDE97","Travel",listOf("\uD83D\uDE97","\uD83D\uDE95","\uD83D\uDE8C","\uD83D\uDE93","\uD83D\uDE91","\uD83D\uDEB2","\u2708\uFE0F","\uD83D\uDE80","\uD83D\uDE82","\u26F5","\uD83C\uDFE0","\uD83C\uDFEB","\u26EA","\uD83D\uDD4C","\uD83C\uDFF0","\uD83D\uDDFC","\u2600\uFE0F","\uD83C\uDF08","\u2B50","\uD83C\uDF19","\u2744\uFE0F","\uD83C\uDF3B","\uD83C\uDF34","\uD83C\uDF0A")),
    // (#5) Stickers / Kaomoji
    EC("\u0028\u00B4\u2022\u1D17\u2022\u0060\u0029","Stickers",listOf(
        "( \u0361\u00B0 \u035C\u0296 \u0361\u00B0)","(\u256F\u00B0\u25A1\u00B0)\u256F\uFE35 \u253B\u2501\u253B","\u252C\u2500\u252C\u30CE( \u00BA _ \u00BA\u30CE)","\u00AF\\_(\u30C4)_/\u00AF","(\u0E07'\u0300-'\u0301)\u0E07",
        "(\u2310\u25A0_\u25A0)","(\u25CF\u00B4\u03C9\uFF40\u25CF)","(\u3065\uFFE3 \u00B3\uFFE3)\u3065","\u0295\u2022\u1D25\u2022\u0294","(=\uFF3E\u30FB\u2978\u30FB\uFF3E=)",
        "\u0028\u002F\u002F\u002F\u2229\u005F\u2229\u005C\u005C\u005C\u0029","(\u2727\u03C9\u2727)","(\u2267\u25E1\u2266)","(\u00F2\u03C9\u00F3)","(\u2060\u2060\u256F\u00B0\u2060\u25A1\u00B0\u2060\uFF09\u256F",
        "UwU","OwO","(^_^)","(-_-)","(T_T)","(>_<)","\u2661","(._.)","\\(^o^)/","(*_*)",
        "\u2606*:..\u00B0\u2606","~(\u2267\u25BD\u2266)~","\u0295\u2022\u1D25\u2022\u0294\u2283\u2661\u2282\u0295\u2022\u1D25\u2022\u0294","d(\u2060-_\u2060\u25E0\u2060)b","\u0028 \u0361~ \u035C\u0296 \u0361\u00B0\u0029",
        "\u261D\uFE0F\uD83E\uDD13","XD",":3",">:(","\u2764\u200D\uD83D\uDD25","\uD83D\uDE4F\uD83C\uDFFB","\uD83E\uDEE1","\uD83E\uDEE0","\uD83E\uDD7A","\uD83D\uDE4C",
    )),
)

private object C {
    val BG = Color.parseColor("#C8CAD0")
    val KEY = Color.WHITE
    val KEY_P = Color.parseColor("#B8BAC0")
    val SP = Color.parseColor("#A4A8B4")
    val SP_P = Color.parseColor("#9498A4")
    val ENT = Color.parseColor("#4285F4")
    val ENT_P = Color.parseColor("#3275E4")
    val TXT = Color.parseColor("#1B1B1F")
    val TXT_L = Color.parseColor("#5F6368")
    val SFC = Color.parseColor("#F5F5F7")
    val PRI = Color.parseColor("#4285F4")
    val SEND = Color.parseColor("#E8F0FE")
    val DIV = Color.parseColor("#DADCE0")
    val ERR = Color.parseColor("#EA4335")
    val GRN = Color.parseColor("#34A853")
    val TB = Color.parseColor("#ECEDF1")
    val EMOJI_SEL = Color.parseColor("#DCE4F8")
    val NUM_BG = Color.parseColor("#E8E9ED")
    val NUM_P = Color.parseColor("#D0D1D6")
}

class TranslatorInputMethodService : InputMethodService() {

    private var cachedView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var shifted = false
    private var layoutId = "qwerty"
    private var baseLay = "qwerty"
    private var tgtIdx = 1
    private var detLang: String? = null
    private var result: String? = null
    private var busy = false
    private var err: String? = null
    private var emojiOn = false
    private var numRowOn = true
    private var emojiCat = 0
    private var hasTyped = false  // tracks if user has typed anything

    private var kbWrap: LinearLayout? = null
    private var numRowView: LinearLayout? = null
    private var kbRows: LinearLayout? = null
    private var emojiWrap: LinearLayout? = null
    private var transView: TextView? = null
    private var transBtn: TextView? = null
    private var transRow: LinearLayout? = null
    private var actRow: LinearLayout? = null
    private var srcLang: TextView? = null
    private var tgtLang: TextView? = null
    private var bsTimer: Timer? = null

    private val KH = 48; private val KR = 8f; private val KM = 3; private val NKH = 38

    private val lay: List<List<K>> get() = when (layoutId) {
        "arabic" -> if (shifted) ARABIC_HI else ARABIC_LO
        "nums" -> NUMS; "syms" -> SYMS; else -> QWERTY
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) clearAll()
        if (emojiOn) hideEmoji()
    }

    // (#2) Detect when host app clears its input (e.g. after sending a message)
    override fun onUpdateSelection(osS: Int, osE: Int, nsS: Int, nsE: Int, cS: Int, cE: Int) {
        super.onUpdateSelection(osS, osE, nsS, nsE, cS, cE)
        // If cursor reset to 0 with no selection, the host app probably cleared its field
        if (nsS == 0 && nsE == 0 && osS > 0) {
            clearAll()
        }
    }

    override fun onFinishInputView(f: Boolean) { super.onFinishInputView(f); stopBs() }
    override fun onDestroy() { stopBs(); handler.removeCallbacksAndMessages(null); cachedView = null; super.onDestroy() }

    override fun onCreateInputView(): View {
        cachedView?.let { return it }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C.BG); fitsSystemWindows = true }
        root.addView(buildStrip())
        root.addView(buildToolbar())
        val kw = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; kbWrap = kw
        val nr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(4),dp(2),dp(4),0) }
        buildNumRow(nr); numRowView = nr; kw.addView(nr)
        val kr = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(3),dp(3),dp(3),dp(8)) }
        buildRows(kr); kbRows = kr; kw.addView(kr); root.addView(kw)
        val ew = buildEmoji(); ew.visibility = View.GONE; emojiWrap = ew; root.addView(ew)
        cachedView = root; return root
    }

    private fun buildToolbar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(C.TB); setPadding(dp(6),dp(4),dp(6),dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36))
            addView(tbBtn("\uD83C\uDF10") { toggleLang() })
            addView(tbBtn("\uD83D\uDE00") { if (emojiOn) hideEmoji() else showEmoji() })
            addView(tbBtn("\uD83D\uDCCB") { pasteFromClipboard() })
            addView(View(this@TranslatorInputMethodService).apply { layoutParams = LinearLayout.LayoutParams(0,0,1f) })
            addView(tbBtn("123") { numRowOn = !numRowOn; numRowView?.visibility = if (numRowOn) View.VISIBLE else View.GONE })
        }
    }

    private fun tbBtn(label: String, action: () -> Unit) = TextView(this).apply {
        text = label; setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length <= 2) 18f else 13f)
        setTextColor(C.TXT_L); gravity = Gravity.CENTER; setPadding(dp(10),dp(2),dp(10),dp(2))
        setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); action() }
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this).toString()
            if (text.isNotEmpty()) { currentInputConnection?.commitText(text, 1); hasTyped = true; updateTBtn() }
        }
    }

    // ── Translation Strip (#3: no input preview, just translate button + result) ──

    private fun buildStrip(): View {
        val s = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C.SFC) }
        val lb = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(6),dp(8),dp(4)) }

        srcLang = TextView(this).apply { text = "Auto"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(C.TXT_L); background = rr(C.DIV, dp(6).toFloat()); setPadding(dp(8),dp(4),dp(8),dp(4)) }
        lb.addView(srcLang)
        lb.addView(TextView(this).apply { text = " \u21C4 "; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTextColor(C.PRI) })
        tgtLang = TextView(this).apply { updTgt(); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(C.PRI); background = rr(C.SEND, dp(6).toFloat()); setPadding(dp(8),dp(4),dp(8),dp(4)); setOnClickListener { showLangMenu(it) } }
        lb.addView(tgtLang)
        lb.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0,0,1f) })

        transBtn = TextView(this).apply {
            text = " Translate \u25B6 "; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
            background = rr(C.GRN, dp(14).toFloat()); setPadding(dp(10),dp(5),dp(10),dp(5)); gravity = Gravity.CENTER; visibility = View.GONE
            setOnClickListener { doTranslate() }
        }
        lb.addView(transBtn)
        lb.addView(TextView(this).apply { text = "\u2715"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextColor(C.ERR); gravity = Gravity.CENTER; setPadding(dp(8),0,dp(4),0); setOnClickListener { clearAll() } })
        s.addView(lb); s.addView(div())

        val tr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12),dp(4),dp(8),dp(4)); visibility = View.GONE }
        transRow = tr
        transView = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextColor(C.PRI); setTypeface(null, Typeface.BOLD); maxLines = 3; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        tr.addView(transView); s.addView(tr)

        val ar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(2),dp(8),dp(4)); visibility = View.GONE }
        actRow = ar
        ar.addView(actBtn("\u21C4 Replace", C.PRI) { replaceTrans() })
        ar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(6),0) })
        ar.addView(actBtn("Send \u25B6", C.GRN) { sendTrans() })
        s.addView(ar); s.addView(div())
        return s
    }

    private fun actBtn(label: String, color: Int, action: () -> Unit) = TextView(this).apply {
        text = " $label "; setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
        background = rr(color, dp(14).toFloat()); setPadding(dp(12),dp(6),dp(12),dp(6)); gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { action() }
    }

    private fun showLangMenu(a: View) {
        val p = PopupMenu(this, a)
        for ((i, l) in LANGS.withIndex()) p.menu.add(0, i, i, "${l.flag} ${l.name}")
        p.setOnMenuItemClickListener { tgtIdx = it.itemId; tgtLang?.updTgt(); true }; p.show()
    }

    private fun buildNumRow(c: LinearLayout) {
        c.removeAllViews()
        for (k in NUM_ROW) c.addView(TextView(this).apply {
            text = k.l; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); setTextColor(C.TXT); gravity = Gravity.CENTER
            background = keyBg(C.NUM_BG, C.NUM_P)
            layoutParams = LinearLayout.LayoutParams(0, dp(NKH), 1f).apply { marginStart = dp(KM); marginEnd = dp(KM) }
            isClickable = true; isFocusable = true
            setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); typeCh(k.l) }
        })
    }

    private fun buildRows(c: LinearLayout) {
        c.removeAllViews()
        for ((ri, row) in lay.withIndex()) {
            val rv = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(KH)).apply { bottomMargin = dp(KM) } }
            if (ri == 1 && (layoutId == "qwerty" || layoutId == "arabic")) rv.setPadding(dp(if (layoutId == "arabic") 4 else 16), 0, dp(if (layoutId == "arabic") 4 else 16), 0)
            for (k in row) rv.addView(mkKey(k))
            c.addView(rv)
        }
    }

    private fun mkKey(k: K): View {
        val spec = k.t !in listOf(KT.CH, KT.SPACE)
        val ent = k.t == KT.ENTER
        val bgN = when { ent -> C.ENT; spec -> C.SP; else -> C.KEY }
        val bgP = when { ent -> C.ENT_P; spec -> C.SP_P; else -> C.KEY_P }
        val tc = when { ent -> Color.WHITE; k.t == KT.SHIFT && shifted -> C.PRI; else -> C.TXT }
        val lbl = when (k.t) { KT.SPACE -> if (layoutId == "arabic") "\u0645\u0633\u0627\u0641\u0629" else "space"; KT.CH -> if (shifted && layoutId == "qwerty") k.l.uppercase() else k.l; else -> k.l }
        val fs = when { k.t == KT.SPACE -> 13f; spec -> 15f; layoutId == "arabic" -> 18f; else -> 20f }

        return TextView(this).apply {
            text = lbl; setTextSize(TypedValue.COMPLEX_UNIT_SP, fs); setTextColor(tc); gravity = Gravity.CENTER; background = keyBg(bgN, bgP)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, k.w).apply { marginStart = dp(KM); marginEnd = dp(KM) }
            isClickable = true; isFocusable = true
            if (k.t == KT.BS) {
                setOnTouchListener { v, ev -> when (ev.action) {
                    MotionEvent.ACTION_DOWN -> { v.isPressed = true; v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); hk(k); startBs(); true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; stopBs(); true }
                    else -> false
                } }
            } else { setOnClickListener { v -> v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); hk(k) } }
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

    private fun typeCh(ch: String) { currentInputConnection?.commitText(ch, 1); hasTyped = true; updateTBtn() }

    private fun doBs() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (sel != null && sel.isNotEmpty()) { ic.commitText("", 1) } else { ic.deleteSurroundingText(1, 0) }
        // Check if input is now empty
        val remaining = ic.getTextBeforeCursor(1, 0)
        if (remaining == null || remaining.isEmpty()) { hasTyped = false }
        updateTBtn()
    }

    private fun startBs() { stopBs(); bsTimer = Timer().apply { schedule(object : TimerTask() { override fun run() { handler.post { doBs() } } }, 400, 60) } }
    private fun stopBs() { bsTimer?.cancel(); bsTimer = null }

    // ── Translation (#3: reads directly from input field, no buffer copy) ──

    private fun doTranslate() {
        val ic = currentInputConnection ?: return
        val text = (ic.getTextBeforeCursor(5000, 0)?.toString() ?: "").trim()
        if (text.length < 2) return

        busy = true; err = null; result = null; transBtn?.visibility = View.GONE; updateTUI()
        val tl = LANGS[tgtIdx].code

        Thread {
            var res: String? = null; var det: String? = null; var e: String? = null
            try {
                val enc = URLEncoder.encode(text, "UTF-8")
                val u = URL("https://api.mymemory.translated.net/get?q=$enc&langpair=autodetect|$tl&de=translator-keyboard@app.com")
                val c = (u.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                if (c.responseCode == 200) {
                    val j = JSONObject(c.inputStream.bufferedReader().readText())
                    if (j.optInt("responseStatus") == 200) { val rd = j.getJSONObject("responseData"); val t = rd.optString("translatedText", ""); if (t.isNotEmpty()) { res = t; det = rd.optString("detectedLanguage", null) } }
                }; c.disconnect()
            } catch (x: Exception) { Log.d(TAG, "MyMemory: ${x.message}") }

            if (res == null) try {
                val enc = URLEncoder.encode(text, "UTF-8")
                val u = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$tl&dt=t&q=$enc")
                val c = (u.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
                if (c.responseCode == 200) {
                    val a = JSONArray(c.inputStream.bufferedReader().readText()); val segs = a.getJSONArray(0); val sb = StringBuilder()
                    for (i in 0 until segs.length()) sb.append(segs.getJSONArray(i).optString(0, "")); if (sb.isNotEmpty()) { res = sb.toString(); det = a.optString(2, null) }
                }; c.disconnect()
            } catch (x: Exception) { Log.d(TAG, "Google: ${x.message}"); e = "Translation failed" }

            val fr = res; val fd = det; val fe = e
            handler.post { busy = false; if (fr != null) { result = fr; detLang = fd; err = null } else { err = fe ?: "Translation failed"; result = null }; updateTUI(); updateTBtn(); updateSrc() }
        }.start()
    }

    // (#3) Replace reads current text length from input field
    private fun replaceTrans() {
        val t = result ?: return; val ic = currentInputConnection ?: return
        val currentLen = (ic.getTextBeforeCursor(5000, 0)?.length ?: 0)
        ic.deleteSurroundingText(currentLen, 0); ic.commitText(t, 1)
        result = null; updateTUI(); updateTBtn()
    }

    // (#2) Send clears everything including our state
    private fun sendTrans() {
        val t = result ?: return; val ic = currentInputConnection ?: return
        val currentLen = (ic.getTextBeforeCursor(5000, 0)?.length ?: 0)
        ic.deleteSurroundingText(currentLen, 0); ic.commitText(t, 1); clearAll()
    }

    // ── Emoji + Stickers Panel (#5) ────────────────────────

    private fun buildEmoji(): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(C.SFC) }
        val ts = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setBackgroundColor(Color.WHITE) }
        val tr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(4),dp(4),dp(4),dp(4)) }
        for ((i, cat) in EMOJIS.withIndex()) {
            tr.addView(TextView(this).apply {
                text = cat.icon; setTextSize(TypedValue.COMPLEX_UNIT_SP, if (cat.label == "Stickers") 14f else 20f)
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
        c.removeAllViews(); val cat = EMOJIS[ci]; val isSticker = cat.label == "Stickers"; val cols = if (isSticker) 4 else 8
        var row: LinearLayout? = null
        for ((i, e) in cat.items.withIndex()) {
            if (i % cols == 0) { row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_HORIZONTAL }; c.addView(row) }
            row?.addView(TextView(this).apply {
                text = e; setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isSticker) 16f else 28f); gravity = Gravity.CENTER
                setPadding(dp(4), dp(if (isSticker) 10 else 7), dp(4), dp(if (isSticker) 10 else 7))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (isSticker) { setTextColor(C.TXT); background = rr(C.NUM_BG, dp(8).toFloat()) }
                setOnClickListener { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); currentInputConnection?.commitText(e, 1) }
            })
        }
        val rem = cols - (cat.items.size % cols); if (rem < cols) for (j in 0 until rem) row?.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0,0,1f) })
    }

    private fun selECat(i: Int, tr: LinearLayout) {
        emojiCat = i; for (x in 0 until tr.childCount - 1) (tr.getChildAt(x) as? TextView)?.background = if (x == i) rr(C.EMOJI_SEL, dp(8).toFloat()) else null
        emojiWrap?.findViewWithTag<LinearLayout>("eg")?.let { buildEGrid(it, i) }
    }

    private fun showEmoji() { emojiOn = true; kbWrap?.visibility = View.GONE; emojiWrap?.visibility = View.VISIBLE }
    private fun hideEmoji() { emojiOn = false; emojiWrap?.visibility = View.GONE; kbWrap?.visibility = View.VISIBLE }

    private fun updateTBtn() { transBtn?.visibility = if (hasTyped && result == null && !busy) View.VISIBLE else View.GONE }
    private fun updateTUI() {
        val tr = transRow ?: return; val ar = actRow ?: return
        when {
            busy -> { tr.visibility = View.VISIBLE; transView?.text = "Translating\u2026"; transView?.setTextColor(C.TXT_L); transView?.setTypeface(null, Typeface.ITALIC); ar.visibility = View.GONE }
            err != null -> { tr.visibility = View.VISIBLE; transView?.text = err; transView?.setTextColor(C.ERR); transView?.setTypeface(null, Typeface.NORMAL); ar.visibility = View.GONE }
            result != null -> { tr.visibility = View.VISIBLE; transView?.text = result; transView?.setTextColor(C.PRI); transView?.setTypeface(null, Typeface.BOLD); ar.visibility = View.VISIBLE }
            else -> { tr.visibility = View.GONE; ar.visibility = View.GONE }
        }
    }
    private fun updateSrc() { val l = LANGS.find { it.code == detLang }; srcLang?.text = if (l != null) "${l.flag} ${l.name}" else "Auto" }
    private fun TextView.updTgt() { val l = LANGS[tgtIdx]; text = "${l.flag} ${l.name} \u25BC" }
    private fun clearAll() { hasTyped = false; result = null; err = null; detLang = null; busy = false; clearTrans(); updateTBtn(); updateSrc() }
    private fun clearTrans() { result = null; err = null; busy = false; updateTUI() }
    private fun reK() { kbRows?.let { buildRows(it) } }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun rr(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun keyBg(n: Int, p: Int): StateListDrawable { val r = dp(KR.toInt()).toFloat(); return StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_pressed), rr(p, r)); addState(intArrayOf(), rr(n, r)) } }
    private fun div() = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1); setBackgroundColor(C.DIV) }
}
