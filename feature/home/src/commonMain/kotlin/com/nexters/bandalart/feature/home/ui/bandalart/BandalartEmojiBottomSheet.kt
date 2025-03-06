package com.nexters.bandalart.feature.home.ui.bandalart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexters.bandalart.core.domain.entity.UpdateBandalartEmojiEntity
import com.nexters.bandalart.feature.home.viewmodel.HomeUiAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandalartEmojiBottomSheet(
    bandalartId: Long,
    cellId: Long,
    currentEmoji: String?,
    onHomeUiAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            onHomeUiAction(HomeUiAction.OnDismiss)
        },
        modifier = modifier.wrapContentSize(),
        sheetState = bottomSheetState,
        dragHandle = null,
    ) {
        Column {
            BandalartEmojiPicker(
                currentEmoji = currentEmoji,
                isBottomSheet = true,
                onEmojiSelect = { selectedEmoji ->
                    onHomeUiAction(
                        HomeUiAction.OnEmojiSelected(
                            bandalartId,
                            cellId,
                            UpdateBandalartEmojiEntity(profileEmoji = selectedEmoji),
                        ),
                    )
                },
            )
        }
    }
}

// @ComponentPreview
// @Composable
// private fun BandalartEmojiBottomSheetPreview() {
//     BandalartTheme {
//         BandalartEmojiBottomSheet(
//             bandalartId = 0L,
//             cellId = 0L,
//             currentEmoji = "😎",
//             onHomeUiAction = {},
//        )
//     }
// }
