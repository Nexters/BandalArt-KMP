package com.nexters.bandalart

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

internal actual fun Modifier.enableTestTagsAsResourceId(): Modifier = semantics { testTagsAsResourceId = true }
