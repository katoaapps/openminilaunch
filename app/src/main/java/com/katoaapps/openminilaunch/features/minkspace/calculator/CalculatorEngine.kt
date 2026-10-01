package com.katoaapps.openminilaunch.features.minkspace.calculator

import java.math.BigDecimal
import java.math.MathContext

internal sealed interface CalculationResult {
    data class Success(val value: String) : CalculationResult
    data object InvalidExpression : CalculationResult
}

/** Small offline expression parser kept separate from the calculator UI for reuse and testing. */
internal object CalculatorEngine {
    fun evaluate(expression: String): CalculationResult = runCatching {
        val normalized = normalize(expression)
        val value = Parser(normalized).parse()
        CalculationResult.Success(value.stripTrailingZeros().toPlainString())
    }.getOrElse { CalculationResult.InvalidExpression }

    /** Accepts the compact notation people commonly use on physical calculator keyboards. */
    private fun normalize(expression: String): String {
        val symbols = expression
            .filterNot { it.isWhitespace() }
            .replace('×', '*')
            .replace('x', '*')
            .replace('X', '*')
            .replace('÷', '/')
            .replace('−', '-')
        return buildString {
            symbols.forEach { character ->
                val previous = lastOrNull()
                val implicitMultiplication = when {
                    character == '(' -> previous?.isDigit() == true || previous == '.' || previous == ')'
                    character.isDigit() || character == '.' -> previous == ')'
                    else -> false
                }
                if (implicitMultiplication) append('*')
                append(character)
            }
        }
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parse(): BigDecimal {
            val result = expression()
            skipWhitespace()
            require(index == source.length)
            return result
        }

        private fun expression(): BigDecimal {
            var value = term()
            while (true) {
                value = when {
                    consume('+') -> value.add(term(), PRECISION)
                    consume('-') -> value.subtract(term(), PRECISION)
                    else -> return value
                }
            }
        }

        private fun term(): BigDecimal {
            var value = factor()
            while (true) {
                value = when {
                    consume('*') -> value.multiply(factor(), PRECISION)
                    consume('/') -> value.divide(factor(), PRECISION)
                    else -> return value
                }
            }
        }

        private fun factor(): BigDecimal {
            skipWhitespace()
            if (consume('+')) return factor()
            if (consume('-')) return factor().negate(PRECISION)
            if (consume('(')) {
                val value = expression()
                require(consume(')'))
                return value
            }
            return number()
        }

        private fun number(): BigDecimal {
            skipWhitespace()
            val start = index
            var decimalSeen = false
            while (index < source.length) {
                val character = source[index]
                when {
                    character.isDigit() -> index++
                    character == '.' && !decimalSeen -> {
                        decimalSeen = true
                        index++
                    }
                    else -> break
                }
            }
            require(index > start)
            return source.substring(start, index).toBigDecimal(PRECISION)
        }

        private fun consume(expected: Char): Boolean {
            skipWhitespace()
            if (source.getOrNull(index) != expected) return false
            index++
            return true
        }

        private fun skipWhitespace() {
            while (source.getOrNull(index)?.isWhitespace() == true) index++
        }
    }

    private val PRECISION = MathContext.DECIMAL64
}
