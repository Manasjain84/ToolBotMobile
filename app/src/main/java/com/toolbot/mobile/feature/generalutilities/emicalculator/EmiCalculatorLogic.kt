package com.toolbot.mobile.feature.generalutilities.emicalculator

import kotlin.math.pow

enum class TenureUnit {
    MONTHS,
    YEARS,
}

data class EmiCalculationResult(
    val monthlyEmi: Double,
    val totalInterest: Double,
    val totalAmount: Double,
    val principal: Double,
    val months: Int,
)

fun calculateEmi(
    principal: Double,
    annualRate: Double,
    tenureValue: Double,
    tenureUnit: TenureUnit,
): EmiCalculationResult? {
    if (principal <= 0.0) return null
    if (annualRate < 0.0) return null
    if (tenureValue <= 0.0) return null

    val months = if (tenureUnit == TenureUnit.MONTHS) {
        tenureValue.toInt()
    } else {
        (tenureValue * 12.0).toInt()
    }

    if (months <= 0) return null

    val monthlyRate = annualRate / 12.0 / 100.0

    val emi = if (monthlyRate == 0.0) {
        principal / months.toDouble()
    } else {
        principal * monthlyRate * ((1.0 + monthlyRate).pow(months.toDouble())) /
            (((1.0 + monthlyRate).pow(months.toDouble())) - 1.0)
    }

    val totalAmount = emi * months.toDouble()
    val totalInterest = totalAmount - principal

    return EmiCalculationResult(
        monthlyEmi = emi,
        totalInterest = totalInterest,
        totalAmount = totalAmount,
        principal = principal,
        months = months,
    )
}
