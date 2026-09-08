package com.toolbot.mobile.feature.smarteducation.percentage

data class PercentageValidationIssue(
    val field: String,
    val message: String,
)

data class PercentageGradeResult(
    val percentage: Double?,
    val grade: String,
    val obtainedMarks: Double,
    val maximumMarks: Double,
    val validationIssues: List<PercentageValidationIssue>,
    val isValid: Boolean,
)

fun getLetterGrade(percentage: Double): String {
    return when {
        percentage >= 90.0 -> "A+"
        percentage >= 80.0 -> "A"
        percentage >= 70.0 -> "B+"
        percentage >= 60.0 -> "B"
        percentage >= 50.0 -> "C+"
        percentage >= 40.0 -> "C"
        percentage >= 33.0 -> "D"
        else -> "F"
    }
}

fun calculatePercentageGrade(
    obtainedText: String,
    maximumText: String,
): PercentageGradeResult {
    val issues = mutableListOf<PercentageValidationIssue>()

    val maximumMarks = maximumText.trim().toDoubleOrNull()
    if (maximumMarks == null || maximumMarks <= 0.0) {
        issues += PercentageValidationIssue("maximum", "Maximum marks must be greater than 0.")
    }

    val obtainedMarks = obtainedText.trim().toDoubleOrNull()
    if (obtainedMarks == null || obtainedMarks < 0.0) {
        issues += PercentageValidationIssue("obtained", "Marks obtained must be a valid non-negative number.")
    }

    if (obtainedMarks != null && maximumMarks != null && obtainedMarks > maximumMarks) {
        issues += PercentageValidationIssue("obtained", "Marks obtained cannot exceed maximum marks.")
    }

    if (issues.isNotEmpty()) {
        return PercentageGradeResult(
            percentage = null,
            grade = "F",
            obtainedMarks = obtainedMarks ?: 0.0,
            maximumMarks = maximumMarks ?: 0.0,
            validationIssues = issues,
            isValid = false,
        )
    }

    val totalMarks = maximumMarks!!
    val scoredMarks = obtainedMarks!!
    val percentage = (scoredMarks / totalMarks) * 100.0

    return PercentageGradeResult(
        percentage = percentage,
        grade = getLetterGrade(percentage),
        obtainedMarks = scoredMarks,
        maximumMarks = totalMarks,
        validationIssues = emptyList(),
        isValid = true,
    )
}
