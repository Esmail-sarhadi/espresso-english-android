package ir.esmail.espresso.ui

import java.util.Locale

/** Persian digits and human friendly durations. */
object Fa {

    private val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun digits(input: String): String = buildString(input.length) {
        for (ch in input) {
            if (ch in '0'..'9') append(digits[ch - '0']) else append(ch)
        }
    }

    fun num(value: Int): String = digits(value.toString())

    fun num(value: Long): String = digits(value.toString())

    /** 1:05 or 12:30 – always at least one minute digit. */
    fun clock(millis: Long): String {
        val total = (millis.coerceAtLeast(0L) / 1000L)
        val minutes = total / 60
        val seconds = total % 60
        return digits(String.format(Locale.US, "%d:%02d", minutes, seconds))
    }

    fun minutes(millis: Long): String {
        val value = (millis.coerceAtLeast(0L) / 60_000L)
        return if (value <= 0) "کمتر از یک دقیقه" else "${num(value)} دقیقه"
    }

    fun percent(value: Float): String = digits("${(value * 100).toInt()}٪")

    fun level(level: String): String = when (level.lowercase(Locale.US)) {
        "beginner" -> "مقدماتی"
        "intermediate" -> "متوسط"
        "advanced" -> "پیشرفته"
        else -> level
    }

    fun category(category: String): String = when (category.lowercase(Locale.US)) {
        "vocabulary" -> "واژگان"
        "grammar" -> "گرامر"
        "phrases" -> "عبارت‌ها"
        "pronunciation" -> "تلفظ"
        "idioms" -> "اصطلاح‌ها"
        else -> category
    }

    fun partOfSpeech(pos: String): String = when (pos.lowercase(Locale.US)) {
        "noun" -> "اسم"
        "verb" -> "فعل"
        "adjective", "adj" -> "صفت"
        "adverb", "adv" -> "قید"
        "pronoun" -> "ضمیر"
        "preposition" -> "حرف اضافه"
        "conjunction" -> "حرف ربط"
        "phrase" -> "عبارت"
        "idiom" -> "اصطلاح"
        "expression" -> "اصطلاح"
        else -> pos
    }

    fun speed(value: Float): String {
        val text = if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
        return digits(text) + "×"
    }
}
