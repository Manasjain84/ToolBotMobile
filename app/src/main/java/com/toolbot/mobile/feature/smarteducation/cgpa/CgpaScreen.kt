package com.toolbot.mobile.feature.smarteducation.cgpa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgpaScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val semesters = remember { mutableStateListOf(SemesterInput(id = UUID.randomUUID().toString())) }
    var calculationResult by remember { mutableStateOf<CgpaCalculationResult?>(null) }

    fun updateSemester(semesterId: String, mutate: (SemesterInput) -> SemesterInput) {
        val index = semesters.indexOfFirst { it.id == semesterId }
        if (index != -1) {
            semesters[index] = mutate(semesters[index])
            calculationResult = null
        }
    }

    fun addSemester() {
        semesters.add(SemesterInput(id = UUID.randomUUID().toString()))
        calculationResult = null
    }

    fun removeSemester(semesterId: String) {
        if (semesters.size > 1) {
            semesters.removeAll { it.id == semesterId }
            calculationResult = null
        }
    }

    fun resetCalculator() {
        semesters.clear()
        semesters.add(SemesterInput(id = UUID.randomUUID().toString()))
        calculationResult = null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("CGPA Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track your semester performance and calculate your cumulative result.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            semesters.forEachIndexed { index, semester ->
                val validationIssues = validateSemester(semester)
                val nameIssue = validationIssues.firstOrNull { it.field == "name" }?.message
                val sgpaIssue = validationIssues.firstOrNull { it.field == "sgpa" }?.message
                val creditsIssue = validationIssues.firstOrNull { it.field == "credits" }?.message

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Semester ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (semesters.size > 1) {
                                IconButton(onClick = { removeSemester(semester.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Remove semester",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = semester.name,
                            onValueChange = { newValue ->
                                updateSemester(semester.id) { it.copy(name = newValue) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Semester name") },
                            singleLine = true,
                            isError = nameIssue != null,
                        )
                        if (nameIssue != null) {
                            Text(
                                text = nameIssue,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        OutlinedTextField(
                            value = semester.sgpaText,
                            onValueChange = { newValue ->
                                val sanitized = newValue.filter { it.isDigit() || it == '.' || it == '-' }
                                updateSemester(semester.id) { it.copy(sgpaText = sanitized) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("SGPA") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = sgpaIssue != null,
                        )
                        if (sgpaIssue != null) {
                            Text(
                                text = sgpaIssue,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        OutlinedTextField(
                            value = semester.creditsText,
                            onValueChange = { newValue ->
                                val sanitized = newValue.filter { it.isDigit() || it == '.' }
                                updateSemester(semester.id) { it.copy(creditsText = sanitized) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Credits") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = creditsIssue != null,
                        )
                        if (creditsIssue != null) {
                            Text(
                                text = creditsIssue,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { addSemester() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Add Semester")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        calculationResult = calculateCgpa(semesters)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Calculate CGPA")
                }
                TextButton(
                    onClick = { resetCalculator() },
                    modifier = Modifier.weight(0.6f),
                ) {
                    Text("Clear")
                }
            }

            val result = calculationResult
            if (result != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isValid) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (result.isValid && result.cgpa != null) {
                            Text(
                                text = "CGPA",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = String.format("%.2f", result.cgpa),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Total Credits",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = if (result.totalCredits % 1.0 == 0.0) {
                                        result.totalCredits.toInt().toString()
                                    } else {
                                        String.format("%.2f", result.totalCredits)
                                    },
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Number of Semesters",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = result.semesterCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        } else {
                            Text(
                                text = "Please correct the semester values and try again.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            result.validationIssues.firstOrNull()?.let { issue ->
                                Text(
                                    text = issue.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
