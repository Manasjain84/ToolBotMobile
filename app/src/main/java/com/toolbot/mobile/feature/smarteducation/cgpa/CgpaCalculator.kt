package com.toolbot.mobile.feature.smarteducation.cgpa

import java.util.UUID
import kotlin.math.roundToInt

data class SemesterInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sgpaText: String = "",
    val creditsText: String = "",
)

data class CgpaValidationIssue(
    val field: String,
    val message: String,
)

data class CgpaCalculationResult(
    val cgpa: Double?,
    val totalCredits: Double,
    val semesterCount: Int,
    val validationIssues: List<CgpaValidationIssue>,
    val isValid: Boolean,
)

fun validateSemester(semester: SemesterInput): List<CgpaValidationIssue> {
    val issues = mutableListOf<CgpaValidationIssue>()

    if (semester.name.trim().isEmpty()) {
        issues += CgpaValidationIssue("name", "Semester name is required.")
    }

    val sgpaText = semester.sgpaText.trim()
    if (sgpaText.isEmpty()) {
        issues += CgpaValidationIssue("sgpa", "SGPA is required.")
    } else {
        val sgpaValue = sgpaText.toDoubleOrNull()
        when {
            sgpaValue == null -> {
                issues += CgpaValidationIssue("sgpa", "SGPA must be a valid number.")
            }
            sgpaValue < 0.0 || sgpaValue > 10.0 -> {
                issues += CgpaValidationIssue("sgpa", "SGPA must be between 0 and 10.")
            }
        }
    }

    val creditsText = semester.creditsText.trim()
    if (creditsText.isEmpty()) {
        issues += CgpaValidationIssue("credits", "Credits are required.")
    } else {
        val creditsValue = creditsText.toDoubleOrNull()
        when {
            creditsValue == null -> {
                issues += CgpaValidationIssue("credits", "Credits must be a valid number.")
            }
            creditsValue <= 0.0 -> {
                issues += CgpaValidationIssue("credits", "Credits must be greater than 0.")
            }
        }
    }

    return issues
}

fun calculateCgpa(semesters: List<SemesterInput>): CgpaCalculationResult {
    if (semesters.isEmpty()) {
        return CgpaCalculationResult(
            cgpa = null,
            totalCredits = 0.0,
            semesterCount = 0,
            validationIssues = listOf(CgpaValidationIssue("semesters", "At least one valid semester is required.")),
            isValid = false,
        )
    }

    var weightedSum = 0.0
    var totalCredits = 0.0
    var validSemesters = 0
    val validationIssues = mutableListOf<CgpaValidationIssue>()

    for (semester in semesters) {
        val issues = validateSemester(semester)
        if (issues.isNotEmpty()) {
            validationIssues += issues
            continue
        }

        val sgpa = semester.sgpaText.trim().toDouble()
        val credits = semester.creditsText.trim().toDouble()

        weightedSum += sgpa * credits
        totalCredits += credits
        validSemesters += 1
    }

    if (validSemesters == 0 || totalCredits <= 0.0) {
        return CgpaCalculationResult(
            cgpa = null,
            totalCredits = totalCredits,
            semesterCount = semesters.size,
            validationIssues = if (validationIssues.isEmpty()) {
                listOf(CgpaValidationIssue("semesters", "At least one valid semester is required."))
            } else {
                validationIssues
            },
            isValid = false,
        )
    }

    val cgpa = weightedSum / totalCredits
    val roundedCgpa = ((cgpa * 100.0).roundToInt().toDouble() / 100.0)

    return CgpaCalculationResult(
        cgpa = roundedCgpa,
        totalCredits = totalCredits,
        semesterCount = validSemesters,
        validationIssues = emptyList(),
        isValid = true,
    )
}
