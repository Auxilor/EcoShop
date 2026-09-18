package com.willfp.ecoshop.sell

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Units sold and value paid, with value split by price type (ConfiguredPrice identifier). */
data class SellTotals(
    val units: Long = 0,
    val values: Map<String, Double> = emptyMap()
) {
    val isEmpty: Boolean
        get() = units == 0L && values.isEmpty()

    operator fun plus(other: SellTotals) = SellTotals(
        units + other.units,
        (values.keys + other.values.keys).associateWith { (values[it] ?: 0.0) + (other.values[it] ?: 0.0) }
    )

    /** `units;type=value;type=value`, with types URL-encoded. Stored in item and block PDC. */
    fun encode(): String = buildString {
        append(units)
        for ((type, value) in values) {
            append(';').append(URLEncoder.encode(type, StandardCharsets.UTF_8)).append('=').append(value)
        }
    }

    companion object {
        val EMPTY = SellTotals()

        fun decode(raw: String?): SellTotals {
            if (raw.isNullOrEmpty()) return EMPTY
            val parts = raw.split(';')
            val units = parts[0].toLongOrNull() ?: return EMPTY
            val values = parts.drop(1).mapNotNull { part ->
                val type = part.substringBefore('=', "")
                val value = part.substringAfter('=', "").toDoubleOrNull()
                if (type.isEmpty() || value == null) null else URLDecoder.decode(type, StandardCharsets.UTF_8) to value
            }.toMap()
            return SellTotals(units, values)
        }

        fun of(result: SellResult): SellTotals =
            result.paid.fold(EMPTY) { acc, line -> acc + line(line.candidate, line.units, line.value) }

        /** Estimated totals for a quote. Excludes the event multiplier, which is only known at commit. */
        fun of(quote: SellQuote): SellTotals =
            quote.lines.fold(EMPTY) { acc, line -> acc + line(line.candidate, line.units, line.baseValue * line.priceMultiplier) }

        private fun line(candidate: SellCandidate, units: Int, value: Double): SellTotals {
            val type = candidate.sellPrice?.identifier ?: return SellTotals(units.toLong())
            return SellTotals(units.toLong(), mapOf(type to value))
        }
    }
}
