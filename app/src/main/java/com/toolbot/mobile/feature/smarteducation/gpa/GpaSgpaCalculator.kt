package com.toolbot.mobile.feature.smarteducation.gpa

import java.util.UUID
import kotlin.math.roundToInt

enum class Grade(val label: String, val gradePoint: Double) {
    A_PLUS("A+", 10.0),
    A("A", 9.0),
    B_PLUS("B+", 8.0),
    B("B", 7.0),
    C_PLUS("C+", 6.0),
    C("C", 5.0),
    D("D", 4.0),
    F("F", 0.0),
}

data class GpaSubjectInput(
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String = "",
    val creditsText: String = "",
    val grade: Grade? = null,
)

data class GpaValidationIssue(
    val field: String,
    val message: String,
)

data class GpaSgpaCalculationResult(
    val sgpa: Double?,
    val totalCredits: Double,
    val subjectCount: Int,
    val validSubjectCount: Int,
    val validationIssues: List<GpaValidationIssue>,
    val isValid: Boolean,
)

fun validateSubject(subject: GpaSubjectInput): List<GpaValidationIssue> {
    val issues = mutableListOf<GpaValidationIssue>()

    if (subject.subjectName.trim().isEmpty()) {
        issues += GpaValidationIssue("name", "Subject name is required.")
    }

    val credits = subject.creditsText.trim()
    if (credits.isEmpty()) {
        issues += GpaValidationIssue("credits", "Credits are required.")
    } else {
        val creditsValue = credits.toDoubleOrNull()
        when {
            creditsValue == null -> {
                issues += GpaValidationIssue("credits", "Credits must be a valid number.")
            }
            creditsValue <= 0.0 -> {
                issues += GpaValidationIssue("credits", "Credits must be greater than 0.")
            }
        }
    }

    if (subject.grade == null) {
        issues += GpaValidationIssue("grade", "Please select a grade.")
    }

    return issues
}

fun calculateGpaSgpa(subjects: List<GpaSubjectInput>): GpaSgpaCalculationResult {
    if (subjects.isEmpty()) {
        return GpaSgpaCalculationResult(
            sgpa = null,
            totalCredits = 0.0,
            subjectCount = 0,
            validSubjectCount = 0,
            validationIssues = listOf(GpaValidationIssue("subjects", "At least one subject is required.")),
            isValid = false,
        )
    }

    var weightedGradeSum = 0.0
    var totalCredits = 0.0
    var validSubjectCount = 0
    val validationIssues = mutableListOf<GpaValidationIssue>()

    for (subject in subjects) {
        val issues = validateSubject(subject)
        if (issues.isNotEmpty()) {
            validationIssues += issues
            continue
        }

        val credits = subject.creditsText.trim().toDouble()
        val grade = subject.grade ?: continue

        weightedGradeSum += credits * grade.gradePoint
        totalCredits += credits
        validSubjectCount += 1
    }

    if (validSubjectCount == 0 || totalCredits <= 0.0) {
        return GpaSgpaCalculationResult(
            sgpa = null,
            totalCredits = totalCredits,
            subjectCount = subjects.size,
            validSubjectCount = validSubjectCount,
            validationIssues = if (validationIssues.isEmpty()) {
                listOf(GpaValidationIssue("subjects", "At least one valid subject is required."))
            } else {
                validationIssues
            },
            isValid = false,
        )
    }

    val sgpa = weightedGradeSum / totalCredits
    val roundedSgpa = ((sgpa * 100.0).roundToInt().toDouble() / 100.0)

    return GpaSgpaCalculationResult(
        sgpa = roundedSgpa,
        totalCredits = totalCredits,
        subjectCount = subjects.size,
        validSubjectCount = validSubjectCount,
        validationIssues = emptyList(),
        isValid = true,
    )
}
