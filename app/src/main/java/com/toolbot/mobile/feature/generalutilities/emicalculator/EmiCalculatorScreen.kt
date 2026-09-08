package com.toolbot.mobile.feature.generalutilities.emicalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiCalculatorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var principalText by rememberSaveable { mutableStateOf("") }
    var rateText by rememberSaveable { mutableStateOf("") }
    var tenureText by rememberSaveable { mutableStateOf("") }
    var tenureUnit by rememberSaveable { mutableStateOf(TenureUnit.MONTHS) }
    var calculationResult by remember { mutableStateOf<EmiCalculationResult?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        val principal = principalText.toDoubleOrNull()
        val annualRate = rateText.toDoubleOrNull()
        val tenureValue = tenureText.toDoubleOrNull()

        when {
            principal == null -> validationMessage = "Principal must be a valid number."
            principal <= 0.0 -> validationMessage = "Principal must be greater than 0."
            annualRate == null -> validationMessage = "Interest rate must be a valid number."
            annualRate < 0.0 -> validationMessage = "Interest rate cannot be negative."
            tenureValue == null -> validationMessage = "Tenure must be a valid number."
            tenureValue <= 0.0 -> validationMessage = "Tenure must be greater than 0."
            else -> {
                val result = calculateEmi(principal, annualRate, tenureValue, tenureUnit)
                if (result == null) {
                    validationMessage = "Please check your values and try again."
                    calculationResult = null
                } else {
                    validationMessage = null
                    calculationResult = result
                }
            }
        }
    }

    fun reset() {
        principalText = ""
        rateText = ""
        tenureText = ""
        tenureUnit = TenureUnit.MONTHS
        calculationResult = null
        validationMessage = null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("EMI Calculator") },
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Plan your loan and understand the monthly repayment clearly.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = principalText,
                onValueChange = { principalText = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Loan amount / Principal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Annual interest rate (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            OutlinedTextField(
                value = tenureText,
                onValueChange = { tenureText = it.filter { char -> char.isDigit() || char == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tenure") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = tenureUnit == TenureUnit.MONTHS,
                    onClick = { tenureUnit = TenureUnit.MONTHS },
                    label = { Text("Months") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = tenureUnit == TenureUnit.YEARS,
                    onClick = { tenureUnit = TenureUnit.YEARS },
                    label = { Text("Years") },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Calculate EMI")
                }
                Button(
                    onClick = { reset() },
                    modifier = Modifier.weight(0.8f),
                ) {
                    Text("Reset")
                }
            }

            if (validationMessage != null) {
                Text(
                    text = validationMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            calculationResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "EMI Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )

                        ResultRow(label = "Monthly EMI", value = formatCurrency(result.monthlyEmi))
                        ResultRow(label = "Total interest", value = formatCurrency(result.totalInterest))
                        ResultRow(label = "Total amount", value = formatCurrency(result.totalAmount))

                        val principalRatio = (result.principal / result.totalAmount.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)
                        val interestRatio = 1.0f - principalRatio.toFloat()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Principal vs interest",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { principalRatio.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Principal ${formatPercentage(principalRatio)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = "Interest ${formatPercentage(interestRatio.toDouble())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 2
    return formatter.format(value)
}

private fun formatPercentage(value: Double): String {
    val percentage = value * 100.0
    return "${String.format("%.1f", percentage)}%"
}
