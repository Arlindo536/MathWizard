package com.arlindodishi.mathwizard.engine

import kotlin.math.abs

/** Exact rational number, always normalised (positive denominator, lowest terms). */
class Fraction private constructor(val num: Int, val den: Int) : Comparable<Fraction> {

    operator fun plus(o: Fraction) = of(num * o.den + o.num * den, den * o.den)
    operator fun minus(o: Fraction) = of(num * o.den - o.num * den, den * o.den)
    operator fun times(o: Fraction) = of(num * o.num, den * o.den)
    operator fun div(o: Fraction) = of(num * o.den, den * o.num)

    val isWhole: Boolean get() = den == 1
    val isNegative: Boolean get() = num < 0

    override fun compareTo(other: Fraction): Int = (num.toLong() * other.den).compareTo(other.num.toLong() * den)
    override fun equals(other: Any?) = other is Fraction && num == other.num && den == other.den
    override fun hashCode() = 31 * num + den

    /** "3/4", or "2" for whole numbers. */
    override fun toString() = if (den == 1) "$num" else "$num/$den"

    companion object {
        fun of(num: Int, den: Int = 1): Fraction {
            require(den != 0) { "Zero denominator" }
            val g = gcd(abs(num), abs(den)).coerceAtLeast(1)
            val sign = if (den < 0) -1 else 1
            return Fraction(sign * num / g, sign * den / g)
        }

        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

        fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
    }
}
