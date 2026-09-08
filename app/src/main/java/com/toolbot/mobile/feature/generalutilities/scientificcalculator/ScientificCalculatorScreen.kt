package com.toolbot.mobile.feature.generalutilities.scientificcalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var expression by rememberSaveable { mutableStateOf("") }
    var angleMode by rememberSaveable { mutableStateOf(AngleMode.DEG) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun appendToExpression(value: String) {
        if (expression == "Error") {
            expression = ""
        }
        if (errorMessage != null) {
            errorMessage = null
        }
        expression += value
    }

    fun clearExpression() {
        expression = ""
        errorMessage = null
    }

    fun removeLast() {
        if (expression == "Error") {
            expression = ""
            errorMessage = null
            return
        }
        expression = expression.dropLast(1)
        if (errorMessage != null) {
            errorMessage = null
        }
    }

    fun toggleSign() {
        if (expression.isBlank()) {
            expression = "-"
            return
        }
        if (expression == "Error") {
            expression = "-"
            errorMessage = null
            return
        }
        if (expression.last() == '(' || expression.last() in listOf('+', '-', '*', '/', '^', '%')) {
            expression += "-"
            return
        }

        val lastOperatorIndex = expression.lastIndexOfAny(listOf('+', '-', '*', '/', '^', '%', '('))
        val startIndex = if (lastOperatorIndex == -1) 0 else lastOperatorIndex + 1
        val currentToken = expression.substring(startIndex)
        val updatedToken = if (currentToken.startsWith("-")) {
            currentToken.removePrefix("-")
        } else {
            "-$currentToken"
        }
        expression = expression.substring(0, startIndex) + updatedToken
    }

    val functionButtons = listOf(
        "sin(", "cos(", "tan(", "log(", "ln(", "sqrt(", "x²", "xʸ", "π", "e", "!", "(", ")",
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Scientific Calculator") },
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
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = angleMode == AngleMode.DEG,
                                onClick = { angleMode = AngleMode.DEG },
                                label = { Text("DEG") },
                            )
                            FilterChip(
                                selected = angleMode == AngleMode.RAD,
                                onClick = { angleMode = AngleMode.RAD },
                                label = { Text("RAD") },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        Text(
                            text = if (expression.isBlank()) "0" else expression,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "Invalid expression",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                functionButtons.forEach { buttonText ->
                    OutlinedButton(
                        onClick = {
                            if (buttonText == "x²") {
                                appendToExpression("^2")
                            } else if (buttonText == "xʸ") {
                                appendToExpression("^")
                            } else if (buttonText == "π") {
                                appendToExpression("π")
                            } else if (buttonText == "e") {
                                appendToExpression("e")
                            } else if (buttonText == "!") {
                                appendToExpression("!")
                            } else if (buttonText == "(") {
                                appendToExpression("(")
                            } else if (buttonText == ")") {
                                appendToExpression(")")
                            } else {
                                appendToExpression(buttonText)
                            }
                        },
                        modifier = Modifier.width(72.dp),
                    ) {
                        Text(buttonText)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { clearExpression() },
                    modifier = Modifier.weight(1f),
                ) { Text("AC") }
                Button(
                    onClick = { removeLast() },
                    modifier = Modifier.weight(1f),
                ) { Text("⌫") }
                Button(
                    onClick = { toggleSign() },
                    modifier = Modifier.weight(1f),
                ) { Text("±") }
                Button(
                    onClick = { appendToExpression("%") },
                    modifier = Modifier.weight(1f),
                ) { Text("%") }
            }

            val keypadButtons = listOf(
                listOf("7", "8", "9", "÷"),
                listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "-"),
                listOf("0", ".", "+", ")"),
            )

            keypadButtons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { buttonText ->
                        val buttonModifier = Modifier.weight(1f).aspectRatio(1.25f)
                        Button(
                            onClick = {
                                when (buttonText) {
                                    "÷" -> appendToExpression("/")
                                    "×" -> appendToExpression("*")
                                    "-" -> appendToExpression("-")
                                    "+" -> appendToExpression("+")
                                    else -> appendToExpression(buttonText)
                                }
                            },
                            modifier = buttonModifier,
                        ) {
                            Text(buttonText)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val result = ScientificCalculatorEngine.evaluate(expression, angleMode)
                    result.onSuccess { value ->
                        val formatted = formatDouble(value)
                        expression = formatted
                        errorMessage = null
                    }.onFailure {
                        expression = "Error"
                        errorMessage = it.message ?: "Invalid expression"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("=")
            }
        }
    }
}

private fun formatDouble(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        val normalized = BigDecimal(value.toString()).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        normalized.toPlainString()
    }
}

private fun String.lastIndexOfAny(chars: List<Char>): Int {
    var result = -1
    for (char in chars) {
        val index = this.lastIndexOf(char)
        if (index > result) {
            result = index
        }
    }
    return result
}
