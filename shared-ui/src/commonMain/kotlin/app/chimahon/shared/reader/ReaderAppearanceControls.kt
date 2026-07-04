package app.chimahon.shared.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.BooleanSegmentedControlState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.SliderControlState
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.SliderSpec
import tachiyomi.domain.reader.service.NovelReaderAppearanceSheetPolicy.SwitchControlState

@Composable
fun ReaderAppearanceSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun ReaderAppearanceSwitchRow(
    state: SwitchControlState,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            state.label,
            style = if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
        )
        Switch(
            checked = state.checked,
            onCheckedChange = onCheckedChange,
            modifier = if (compact) Modifier.scale(0.85f) else Modifier,
        )
    }
}

@Composable
fun ReaderAppearanceSlider(
    state: SliderControlState,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val textStyle = if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            }
            Text(
                state.label,
                style = textStyle,
            )
            Text(
                state.valueText,
                style = textStyle,
            )
        }
        Slider(
            value = state.value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = state.spec.toFloatRange(),
            steps = state.spec.steps,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppearanceBooleanSegmentedControl(
    state: BooleanSegmentedControlState,
    onOptionSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            state.label,
            style = labelStyle,
            color = labelColor,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            state.options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option.selected,
                    onClick = { onOptionSelected(option.value) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = state.options.size,
                    ),
                ) {
                    Text(option.label)
                }
            }
        }
    }
}

private fun SliderSpec.toFloatRange(): ClosedFloatingPointRange<Float> {
    return min.toFloat()..max.toFloat()
}
