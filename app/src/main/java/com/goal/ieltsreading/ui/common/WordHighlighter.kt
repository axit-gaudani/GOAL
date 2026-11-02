package com.goal.ieltsreading.ui.common

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.annotation.ColorInt
import com.goal.ieltsreading.data.db.VocabEntity
import java.util.regex.Pattern

/**
 * Makes the lesson's vocabulary words inside a passage bold, coloured and tappable.
 * Tapping a word calls [onWordClick] so the screen can show its Gujarati meaning.
 */
object WordHighlighter {

    fun highlight(
        text: String,
        words: List<VocabEntity>,
        @ColorInt color: Int,
        onWordClick: (VocabEntity) -> Unit
    ): SpannableString {
        val spannable = SpannableString(text)
        val taken = BooleanArray(text.length)
        // Longer words first so "climate change" wins over "climate".
        for (word in words.sortedByDescending { it.word.length }) {
            if (word.word.isBlank()) continue
            val pattern = Pattern.compile("\\b" + Pattern.quote(word.word) + "\\b", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if ((start until end).any { taken[it] }) continue
                for (i in start until end) taken[i] = true
                spannable.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = onWordClick(word)
                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = color
                        ds.isUnderlineText = false
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }
}
