package com.katoaapps.openminilaunch.ui.minkspace.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculationResult
import com.katoaapps.openminilaunch.features.minkspace.calculator.CalculatorEngine
import com.katoaapps.openminilaunch.ui.theme.Dimens

@Composable
internal fun CalculatorKeypad(
    draft: CalculatorDraftState,
    onHistoryAdded: (expression: String, result: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    fun evaluate() {
        if (draft.evaluate(onHistoryAdded)) keyboard?.hide()
    }

    Column(
        modifier = modifier.padding(horizontal = Dimens.dp10, vertical = Dimens.dp4),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp5),
    ) {
        OutlinedTextField(
            value = draft.expression,
            onValueChange = draft::updateExpression,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleLarge,
            supportingText = {
                when {
                    draft.invalidExpression -> Text(stringResource(R.string.calculator_invalid_expression))
                    draft.result.isNotEmpty() -> Text("= ${draft.result}")
                }
            },
            trailingIcon = {
                IconButton(
                    onClick = draft::deleteLastCharacter,
                    enabled = draft.expression.isNotEmpty(),
                ) {
                    Icon(Icons.Default.Backspace, stringResource(R.string.delete_last_character))
                }
            },
            isError = draft.invalidExpression,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { evaluate() }),
        )
        CALCULATOR_KEYS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp5),
            ) {
                row.forEach { key ->
                    CalculatorKey(
                        label = key,
                        accent = key in OPERATOR_KEYS || key == "=",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        when (key) {
                            "C" -> draft.clear()
                            "=" -> evaluate()
                            "±" -> draft.append("-")
                            else -> draft.append(key)
                        }
                    }
                }
            }
        }
    }
}

internal class CalculatorDraftState {
    var expression by mutableStateOf("")
        private set
    var result by mutableStateOf("")
        private set
    var invalidExpression by mutableStateOf(false)
        private set
    private var justEvaluated = false

    fun updateExpression(value: String) {
        expression = value
        resetResult()
    }

    fun append(value: String) {
        val startsNewExpression = value.firstOrNull()?.let {
            it.isDigit() || it == '.' || it == '('
        } == true
        expression = when {
            justEvaluated && startsNewExpression -> value
            justEvaluated && result.isNotEmpty() -> result + value
            else -> expression + value
        }
        resetResult()
    }

    fun deleteLastCharacter() {
        expression = expression.dropLast(1)
        resetResult()
    }

    fun clear() {
        expression = ""
        resetResult()
    }

    fun evaluate(onSuccess: (expression: String, result: String) -> Unit): Boolean {
        if (expression.isBlank()) return false
        return when (val calculation = CalculatorEngine.evaluate(expression)) {
            CalculationResult.InvalidExpression -> {
                invalidExpression = true
                false
            }
            is CalculationResult.Success -> {
                result = calculation.value
                invalidExpression = false
                justEvaluated = true
                onSuccess(expression.trim(), calculation.value)
                true
            }
        }
    }

    private fun resetResult() {
        result = ""
        invalidExpression = false
        justEvaluated = false
    }
}

@Composable
private fun CalculatorKey(
    label: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.dp12),
        color = if (accent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
            )
        }
    }
}

private val CALCULATOR_KEYS = listOf(
    listOf("C", "(", ")", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("±", "0", ".", "="),
)

private val OPERATOR_KEYS = setOf("÷", "×", "−", "+")
