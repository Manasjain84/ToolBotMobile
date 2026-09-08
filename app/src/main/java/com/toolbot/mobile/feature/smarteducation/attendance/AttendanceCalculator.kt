package com.toolbot.mobile.feature.smarteducation.attendance

import kotlin.math.ceil

data class AttendanceValidationIssue(
    val field: String,
    val message: String,
)

data class AttendanceCalculationResult(
    val isValid: Boolean,
    val currentAttendance: Double?,
    val targetAttendance: Double,
    val requiredFutureClasses: Int?,
    val statusMessage: String,
    val validationIssues: List<AttendanceValidationIssue>,
)

fun calculateAttendance(
    conductedText: String,
    attendedText: String,
    targetText: String,
): AttendanceCalculationResult {
    val validationIssues = mutableListOf<AttendanceValidationIssue>()

    val conducted = conductedText.trim().toDoubleOrNull()
    if (conducted == null || conducted <= 0.0) {
        validationIssues += AttendanceValidationIssue("conducted", "Total classes conducted must be greater than 0.")
    }

    val attended = attendedText.trim().toDoubleOrNull()
    if (attended == null || attended < 0.0) {
        validationIssues += AttendanceValidationIssue("attended", "Classes attended must be a valid number.")
    }

    val target = targetText.trim().toDoubleOrNull()
    if (target == null || target < 0.0 || target > 100.0) {
        validationIssues += AttendanceValidationIssue("target", "Target attendance must be between 0 and 100.")
    }

    if (conducted != null && attended != null && attended > conducted) {
        validationIssues += AttendanceValidationIssue("attended", "Classes attended cannot exceed classes conducted.")
    }

    if (validationIssues.isNotEmpty()) {
        return AttendanceCalculationResult(
            isValid = false,
            currentAttendance = null,
            targetAttendance = target ?: 0.0,
            requiredFutureClasses = null,
            statusMessage = "Please fix the invalid inputs.",
            validationIssues = validationIssues,
        )
    }

    val totalClasses = conducted!!
    val classesAttended = attended!!
    val targetPercentage = target!!

    val currentAttendance = (classesAttended / totalClasses) * 100.0

    if (currentAttendance >= targetPercentage) {
        return AttendanceCalculationResult(
            isValid = true,
            currentAttendance = currentAttendance,
            targetAttendance = targetPercentage,
            requiredFutureClasses = null,
            statusMessage = "You have already reached your target.",
            validationIssues = emptyList(),
        )
    }

    if (targetPercentage >= 100.0) {
        return AttendanceCalculationResult(
            isValid = true,
            currentAttendance = currentAttendance,
            targetAttendance = targetPercentage,
            requiredFutureClasses = null,
            statusMessage = "Reaching 100% may require attending every future class.",
            validationIssues = emptyList(),
        )
    }

    val required = ceil(
        ((targetPercentage * totalClasses) - (100.0 * classesAttended)) / (100.0 - targetPercentage)
    )

    return AttendanceCalculationResult(
        isValid = true,
        currentAttendance = currentAttendance,
        targetAttendance = targetPercentage,
        requiredFutureClasses = if (required > 0) required.toInt() else 0,
        statusMessage = "Below target",
        validationIssues = emptyList(),
    )
}
