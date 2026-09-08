package com.toolbot.mobile.feature.generalutilities.scientificcalculator

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class AngleMode {
    DEG,
    RAD,
}

object ScientificCalculatorEngine {
    fun evaluate(expression: String, angleMode: AngleMode): Result<Double> {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter an expression"))
        }

        return runCatching {
            ScientificExpressionParser(trimmed, angleMode).parse()
        }
    }
}

private class ScientificExpressionParser(
    private val text: String,
    private val angleMode: AngleMode,
) {
    private var index = 0

    fun parse(): Double {
        val value = parseExpression()
        skipWhitespace()
        if (index != text.length) {
            throw IllegalArgumentException("Invalid expression")
        }
        if (value.isNaN() || value.isInfinite()) {
            throw IllegalArgumentException("Invalid expression")
        }
        return value
    }

    private fun parseExpression(): Double = parseAdditive()

    private fun parseAdditive(): Double {
        var value = parseMultiplicative()
        while (true) {
            skipWhitespace()
            if (index >= text.length) return value
            when (text[index]) {
                '+' -> {
                    index++
                    value += parseMultiplicative()
                }
                '-' -> {
                    index++
                    value -= parseMultiplicative()
                }
                else -> return value
            }
        }
    }

    private fun parseMultiplicative(): Double {
        var value = parsePower()
        while (true) {
            skipWhitespace()
            if (index >= text.length) return value
            when (text[index]) {
                '*' -> {
                    index++
                    value *= parsePower()
                }
                '/' -> {
                    index++
                    val divisor = parsePower()
                    if (abs(divisor) < 1e-12) {
                        throw IllegalArgumentException("Division by zero")
                    }
                    value /= divisor
                }
                '%' -> {
                    index++
                    val percent = parsePower()
                    value = (value * percent) / 100.0
                }
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        var value = parseUnary()
        skipWhitespace()
        if (index < text.length && text[index] == '^') {
            index++
            val exponent = parsePower()
            value = value.pow(exponent)
        }
        return value
    }

    private fun parseUnary(): Double {
        skipWhitespace()
        if (index < text.length && text[index] == '-') {
            index++
            return -parseUnary()
        }
        if (index < text.length && text[index] == '+') {
            index++
            return parseUnary()
        }
        return parsePostfix()
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (true) {
            skipWhitespace()
            if (index >= text.length) return value
            when (text[index]) {
                '!' -> {
                    index++
                    value = factorial(value)
                }
                '%' -> {
                    index++
                    value = value / 100.0
                }
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Double {
        skipWhitespace()
        if (index >= text.length) {
            throw IllegalArgumentException("Invalid expression")
        }

        val ch = text[index]

        return when {
            ch.isDigit() || ch == '.' -> parseNumber()
            ch == '(' -> {
                index++
                val inner = parseExpression()
                skipWhitespace()
                if (index >= text.length || text[index] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                index++
                inner
            }
            text.startsWith("sin", index) && isAlphaBoundaryBefore(index) && isAlphaBoundaryAfter(index + 3) -> {
                index += 3
                val argument = parseFunctionArgument()
                applyTrigonometricFunction("sin", argument)
            }
            text.startsWith("cos", index) && isAlphaBoundaryBefore(index) && isAlphaBoundaryAfter(index + 3) -> {
                index += 3
                val argument = parseFunctionArgument()
                applyTrigonometricFunction("cos", argument)
            }
            text.startsWith("tan", index) && isAlphaBoundaryBefore(index) && isAlphaBoundaryAfter(index + 3) -> {
                index += 3
                val argument = parseFunctionArgument()
                applyTrigonometricFunction("tan", argument)
            }
            text.startsWith("log", index) && isAlphaBoundaryBefore(index) && isAlphaBoundaryAfter(index + 3) -> {
                index += 3
                val argument = parseFunctionArgument()
                if (argument <= 0.0) throw IllegalArgumentException("Log requires positive input")
                log10(argument)
            }
            text.startsWith("ln", index) && isAlphaBoundaryBefore(index) && isAlphaBoundaryAfter(index + 2) -> {
                index += 2
                val argument = parseFunctionArgument()
                if (argument <= 0.0) throw IllegalArgumentException("Natural log requires positive input")
                ln(argument)
            }
            text.startsWith("sqrt", index) && isAlphaBoundaryBefore(index) && isAlphaBoundaryAfter(index + 4) -> {
                index += 4
                val argument = parseFunctionArgument()
                if (argument < 0.0) throw IllegalArgumentException("Square root requires non-negative input")
                sqrt(argument)
            }
            ch == 'π' -> {
                index++
                PI
            }
            ch == 'e' -> {
                index++
                E
            }
            else -> throw IllegalArgumentException("Invalid expression")
        }
    }

    private fun parseFunctionArgument(): Double {
        skipWhitespace()
        if (index < text.length && text[index] == '(') {
            index++
            val argument = parseExpression()
            skipWhitespace()
            if (index >= text.length || text[index] != ')') {
                throw IllegalArgumentException("Missing closing parenthesis")
            }
            index++
            return argument
        }
        return parseUnary()
    }

    private fun parseNumber(): Double {
        val start = index
        var seenDot = false
        while (index < text.length) {
            val current = text[index]
            if (current.isDigit() || (current == '.' && !seenDot)) {
                if (current == '.') seenDot = true
                index++
            } else {
                break
            }
        }

        val numberText = text.substring(start, index)
        if (numberText.count { it == '.' } > 1) {
            throw IllegalArgumentException("Invalid numeric value")
        }

        return numberText.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid expression")
    }

    private fun factorial(value: Double): Double {
        val integerValue = value.roundToLong()
        if (value < 0.0 || integerValue.toDouble() != value) {
            throw IllegalArgumentException("Factorial is only valid for non-negative integers")
        }
        var result = 1.0
        for (i in 2..integerValue.toInt()) {
            result *= i.toDouble()
        }
        return result
    }

    private fun applyTrigonometricFunction(name: String, input: Double): Double {
        val radians = if (angleMode == AngleMode.DEG) {
            Math.toRadians(input)
        } else {
            input
        }
        return when (name) {
            "sin" -> sin(radians)
            "cos" -> cos(radians)
            "tan" -> tan(radians)
            else -> throw IllegalArgumentException("Unsupported function")
        }
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) {
            index++
        }
    }

    private fun isAlphaBoundaryBefore(position: Int): Boolean {
        if (position == 0) return true
        return !text[position - 1].isLetterOrDigit() && text[position - 1] != '_'
    }

    private fun isAlphaBoundaryAfter(position: Int): Boolean {
        if (position >= text.length) return true
        return !text[position].isLetterOrDigit() && text[position] != '_'
    }
}

private fun Double.roundToLong(): Long = floor(this).toLong()
