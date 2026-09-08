package com.toolbot.mobile.feature.smarteducation.gpa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaSgpaScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val subjects = remember { mutableStateListOf(GpaSubjectInput(id = UUID.randomUUID().toString())) }
    var calculationResult by remember { mutableStateOf<GpaSgpaCalculationResult?>(null) }
    val dropdownExpanded = remember { mutableStateMapOf<String, Boolean>() }

    fun updateSubject(subjectId: String, mutate: (GpaSubjectInput) -> GpaSubjectInput) {
        val index = subjects.indexOfFirst { it.id == subjectId }
        if (index != -1) {
            subjects[index] = mutate(subjects[index])
            calculationResult = null
        }
    }

    fun addSubject() {
        subjects.add(GpaSubjectInput(id = UUID.randomUUID().toString()))
        calculationResult = null
    }

    fun removeSubject(subjectId: String) {
        if (subjects.size > 1) {
            subjects.removeAll { it.id == subjectId }
            dropdownExpanded.remove(subjectId)
            calculationResult = null
        }
    }

    fun resetCalculator() {
        subjects.clear()
        subjects.add(GpaSubjectInput(id = UUID.randomUUID().toString()))
        calculationResult = null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("GPA / SGPA Calculator") },
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
                text = "Calculate your semester GPA using subject credits and grades.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (subjects.isNotEmpty()) {
                subjects.forEachIndexed { index, subject ->
                    val validationIssues = validateSubject(subject)
                    val nameIssue = validationIssues.firstOrNull { it.field == "name" }?.message
                    val creditsIssue = validationIssues.firstOrNull { it.field == "credits" }?.message
                    val gradeIssue = validationIssues.firstOrNull { it.field == "grade" }?.message
                    val expanded = dropdownExpanded[subject.id] == true

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
                                    text = "Subject ${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (subjects.size > 1) {
                                    IconButton(onClick = { removeSubject(subject.id) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Remove subject",
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = subject.subjectName,
                                onValueChange = { newValue ->
                                    updateSubject(subject.id) { it.copy(subjectName = newValue) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Subject name") },
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
                                value = subject.creditsText,
                                onValueChange = { newValue ->
                                    val sanitized = newValue.filter { it.isDigit() || it == '.' || it == '-' }
                                    updateSubject(subject.id) { it.copy(creditsText = sanitized) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Credits") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                ),
                                isError = creditsIssue != null,
                            )
                            if (creditsIssue != null) {
                                Text(
                                    text = creditsIssue,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { dropdownExpanded[subject.id] = !expanded },
                            ) {
                                OutlinedTextField(
                                    value = subject.grade?.label ?: "Select grade",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    label = { Text("Grade") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    isError = gradeIssue != null,
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { dropdownExpanded[subject.id] = false },
                                ) {
                                    Grade.entries.forEach { grade ->
                                        DropdownMenuItem(
                                            text = { Text(grade.label) },
                                            onClick = {
                                                updateSubject(subject.id) { it.copy(grade = grade) }
                                                dropdownExpanded[subject.id] = false
                                            },
                                        )
                                    }
                                }
                            }
                            if (gradeIssue != null) {
                                Text(
                                    text = gradeIssue,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { addSubject() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ Add Subject")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        val result = calculateGpaSgpa(subjects)
                        calculationResult = result
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Calculate SGPA")
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
                        if (result.isValid && result.sgpa != null) {
                            Text(
                                text = "SGPA",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = String.format("%.2f", result.sgpa),
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
                                    text = "Number of Subjects",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = result.subjectCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        } else {
                            Text(
                                text = "Please fix the highlighted fields and try again.",
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
