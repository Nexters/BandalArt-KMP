package com.nexters.bandalart.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.home_share
import bandalart.core.designsystem.generated.resources.ic_share
import bandalart.core.designsystem.generated.resources.share_description
import com.nexters.bandalart.core.common.extension.clickableSingle
import com.nexters.bandalart.core.designsystem.theme.Gray200
import com.nexters.bandalart.core.designsystem.theme.Gray900
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun HomeShareButton(
    onShareButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Gray200)
            .clickableSingle { onShareButtonClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_share),
                contentDescription = stringResource(Res.string.share_description),
                tint = Color.Unspecified,
            )
            Text(
                text = stringResource(Res.string.home_share),
                color = Gray900,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// @ComponentPreview
// @Composable
// private fun HomeShareButtonPreview() {
//     BandalartTheme {
//         HomeShareButton(
//             onShareButtonClick = {},
//         )
//     }
// }
