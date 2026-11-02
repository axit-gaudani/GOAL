package com.goal.ieltsreading.domain

import kotlin.math.roundToInt

/**
 * Estimates an IELTS Academic Reading band.
 * Real tests have 40 questions; shorter practice tests are scaled to 40 first.
 * This is only an estimate for practice, not an official score.
 */
object BandCalculator {

    fun bandFor(correct: Int, total: Int): Double {
        if (total <= 0) return 0.0
        val outOf40 = (correct.toDouble() / total * 40).roundToInt()
        return when {
            outOf40 >= 39 -> 9.0
            outOf40 >= 37 -> 8.5
            outOf40 >= 35 -> 8.0
            outOf40 >= 33 -> 7.5
            outOf40 >= 30 -> 7.0
            outOf40 >= 27 -> 6.5
            outOf40 >= 23 -> 6.0
            outOf40 >= 19 -> 5.5
            outOf40 >= 15 -> 5.0
            outOf40 >= 13 -> 4.5
            outOf40 >= 10 -> 4.0
            outOf40 >= 8 -> 3.5
            outOf40 >= 6 -> 3.0
            outOf40 >= 4 -> 2.5
            else -> 2.0
        }
    }

    fun format(band: Double): String = if (band % 1.0 == 0.0) band.toInt().toString() else band.toString()
}
