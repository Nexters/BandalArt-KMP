/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.feature.complete

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.save_bandalart_image
import com.nexters.bandalart.core.common.ImageHandlerProvider
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString

@Composable
internal fun HandleCompleteEffects(
    state: CompleteScreen.State,
    imageHandlerProvider: ImageHandlerProvider,
) {
    LaunchedEffect(state.sideEffect) {
        when (val sideEffect = state.sideEffect) {
            is CompleteScreen.SideEffect.SaveImage -> {
                imageHandlerProvider.saveUriToGallery(sideEffect.imageUri)
                showToast(getString(Res.string.save_bandalart_image))
            }

            is CompleteScreen.SideEffect.ShareImage -> {
                imageHandlerProvider.shareImage(sideEffect.imageUri)
            }

            null -> Unit
        }

        if (state.sideEffect != null) {
            state.eventSink(CompleteScreen.Event.ClearSideEffect)
        }
    }
}
