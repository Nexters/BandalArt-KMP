package com.nexters.bandalart.core.common.extension

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import com.nexters.bandalart.core.common.utils.MultipleEventsCutter
import com.nexters.bandalart.core.common.utils.get

inline fun Modifier.noRippleClickable(crossinline onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
    ) {
        onClick()
    }
}

fun Modifier.clickableSingle(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
    },
) {
    val multipleEventsCutter = remember { MultipleEventsCutter.get() }
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = { multipleEventsCutter.processEvent { onClick() } },
        role = role,
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() },
    )
}

// fun Modifier.clearFocusOnKeyboardDismiss(): Modifier = composed {
//     var isFocused by remember { mutableStateOf(false) }
//     var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }
//     if (isFocused) {
//         val imeIsVisible = WindowInsets.isImeVisible
//         val focusManager = LocalFocusManager.current
//         LaunchedEffect(imeIsVisible) {
//             if (imeIsVisible) {
//                 keyboardAppearedSinceLastFocused = true
//             } else if (keyboardAppearedSinceLastFocused) {
//                 focusManager.clearFocus()
//             }
//         }
//     }
//     onFocusEvent {
//         if (isFocused != it.isFocused) {
//             isFocused = it.isFocused
//             if (isFocused) {
//                 keyboardAppearedSinceLastFocused = false
//             }
//         }
//     }
// }

fun Modifier.clearFocusOnKeyboardDismiss(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }

    if (isFocused) {
        val density = LocalDensity.current
        val imeIsVisible = WindowInsets.ime.getBottom(density) > 0
        val focusManager = LocalFocusManager.current

        LaunchedEffect(imeIsVisible) {
            if (imeIsVisible) {
                keyboardAppearedSinceLastFocused = true
            } else if (keyboardAppearedSinceLastFocused) {
                focusManager.clearFocus()
            }
        }
    }

    onFocusEvent {
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                keyboardAppearedSinceLastFocused = false
            }
        }
    }
}

fun Modifier.aspectRatioBasedOnOrientation(aspectRatio: Float): Modifier {
    return this.then(
        object : LayoutModifier {
            override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
                val width = constraints.maxWidth
                val height = constraints.maxHeight

                val targetWidth: Int
                val targetHeight: Int

                if (width <= height) {
                    targetWidth = width
                    targetHeight = (width / aspectRatio).toInt()
                } else {
                    targetHeight = height
                    targetWidth = (height * aspectRatio).toInt()
                }

                val placeable = measurable.measure(
                    Constraints.fixed(targetWidth, targetHeight),
                )

                return layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
        },
    )
}

fun Modifier.captureToGraphicsLayer(
    graphicsLayer: GraphicsLayer,
) = this.drawWithContent {
    graphicsLayer.record { this@drawWithContent.drawContent() }
    drawLayer(graphicsLayer)
}
