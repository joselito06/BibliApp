package com.jbncode.bibliaapp.domain.usecase

import java.time.LocalDate

object StreakCalculator {
    /** Racha viva: cuenta hacia atrás desde hoy (o ayer, si hoy aún no se lee). */
    fun current(days: Set<LocalDate>, today: LocalDate): Int {
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }

    fun best(days: Set<LocalDate>): Int {
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run); prev = d
        }
        return best
    }
}
