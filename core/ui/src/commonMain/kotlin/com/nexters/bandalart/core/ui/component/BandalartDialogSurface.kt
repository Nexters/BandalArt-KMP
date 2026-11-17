package com.nexters.bandalart.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousRoundedRectangle
import zone.ien.hig.adaptive.ExperimentalAdaptiveApi
import zone.ien.hig.adaptive.Theme
import zone.ien.hig.adaptive.currentTheme
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.systemRed

/**
 * 다이얼로그 뒤에 비칠 화면 기록. 콘텐츠를 `layerBackdrop`으로 기록하는 화면(iOS 홈)만 제공한다.
 * 시트가 떠 있으면 홈 기록과 시트 기록을 합성한 값이라 바로 뒤의 시트가 비친다.
 */
val LocalDialogBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private val DialogShape = RoundedCornerShape(16.dp)

/** iOS 유리 다이얼로그 모양. 네이티브 alert와 hig alert·드롭다운처럼 연속 곡률 34dp. */
private val GlassDialogShape = ContinuousRoundedRectangle(34.dp)

/** 다이얼로그 아이콘·제목·본문의 가로 정렬. iOS는 네이티브 alert처럼 왼쪽, 그 외는 가운데. */
@OptIn(ExperimentalAdaptiveApi::class)
@Composable
fun bandalartDialogContentAlignment(): Alignment.Horizontal = if (currentTheme == Theme.Cupertino) Alignment.Start else Alignment.CenterHorizontally

/** 다이얼로그 제목·본문의 글자 정렬. iOS는 네이티브 alert처럼 왼쪽, 그 외는 가운데. */
@OptIn(ExperimentalAdaptiveApi::class)
@Composable
fun bandalartDialogTextAlign(): TextAlign = if (currentTheme == Theme.Cupertino) TextAlign.Start else TextAlign.Center

/**
 * 삭제·초기화처럼 되돌릴 수 없는 확인 버튼의 글자색.
 * iOS는 네이티브 alert의 destructive 버튼처럼 시스템 빨강, 그 외는 [default]를 그대로 쓴다.
 */
@OptIn(ExperimentalAdaptiveApi::class)
@Composable
fun bandalartDialogDestructiveTextColor(default: Color): Color = if (currentTheme == Theme.Cupertino) CupertinoColors.systemRed else default

/**
 * 알림 다이얼로그의 바탕 면.
 * iOS(Cupertino)에서 [LocalDialogBackdrop]이 있으면 뒤 화면이 비치는 Liquid Glass 면으로, 그 외는 기존 Material [Surface]로 그린다.
 */
@Composable
fun BandalartDialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalDialogBackdrop.current
    if (isBandalartGlassEnabled() && backdrop != null) {
        Box(modifier = modifier.bandalartGlassSurface(backdrop = backdrop, shape = GlassDialogShape)) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = DialogShape,
            color = MaterialTheme.colorScheme.surface,
            content = content,
        )
    }
}
