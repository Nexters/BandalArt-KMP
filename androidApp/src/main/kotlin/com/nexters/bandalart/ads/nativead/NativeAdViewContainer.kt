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

package com.nexters.bandalart.ads.nativead

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesView
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

private class NativeAdScope(
    val nativeAdView: NativeAdView,
    val onMediaViewCreated: (MediaView) -> Unit,
)

private val LocalNativeAdScope = staticCompositionLocalOf<NativeAdScope?> { null }

@Composable
internal fun NativeAdViewContainer(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val nativeAdViewRef = remember { mutableStateOf<NativeAdView?>(null) }
    var mediaView by remember { mutableStateOf<MediaView?>(null) }
    val parentCompositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)

    AndroidView(
        factory = { context ->
            val composeView =
                ComposeView(context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
            NativeAdView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                addView(composeView)
                nativeAdViewRef.value = this
            }
        },
        modifier = modifier,
        update = { nativeAdView ->
            val composeView = nativeAdView.getChildAt(0) as? ComposeView ?: return@AndroidView
            composeView.setParentCompositionContext(parentCompositionContext)
            composeView.setContent {
                val scope =
                    remember(nativeAdView) {
                        NativeAdScope(nativeAdView) { view -> mediaView = view }
                    }
                CompositionLocalProvider(LocalNativeAdScope provides scope) {
                    currentContent()
                }
            }
        },
    )

    val currentNativeAd by rememberUpdatedState(nativeAd)
    val currentNativeAdView = nativeAdViewRef.value
    val currentMediaView = mediaView
    SideEffect {
        registerNativeAdWhenReady(
            nativeAdView = currentNativeAdView,
            nativeAd = currentNativeAd,
            mediaView = currentMediaView,
            register = NativeAdView::registerNativeAd,
        )
    }
}

internal inline fun <AdView, Ad, Media> registerNativeAdWhenReady(
    nativeAdView: AdView?,
    nativeAd: Ad,
    mediaView: Media?,
    register: (AdView, Ad, Media) -> Unit,
) {
    if (nativeAdView != null && mediaView != null) {
        register(nativeAdView, nativeAd, mediaView)
    }
}

@Composable
internal fun NativeAdHeadlineView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NativeAdAssetView(modifier, content) { adView, view -> adView.headlineView = view }
}

@Composable
internal fun NativeAdBodyView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NativeAdAssetView(modifier, content) { adView, view -> adView.bodyView = view }
}

@Composable
internal fun NativeAdCallToActionView(
    rippleColor: Color,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalInspectionMode.current) {
        Box(modifier) { content() }
        return
    }
    val scope = requireNativeAdScope()
    NativeAdRippleAssetView(
        rippleColor = rippleColor,
        cornerRadius = cornerRadius,
        register = { view -> scope.nativeAdView.callToActionView = view },
        modifier = modifier,
        content = content,
    )
}

@Composable
internal fun NativeAdMediaView(
    mediaContent: MediaContent?,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(modifier)
        return
    }
    val scope = requireNativeAdScope()
    AndroidView(
        factory = { context ->
            MediaView(context).apply {
                imageScaleType = ImageView.ScaleType.FIT_CENTER
                scope.onMediaViewCreated(this)
            }
        },
        modifier = modifier,
        update = { view -> mediaContent?.let { view.mediaContent = it } },
    )
}

@Composable
internal fun NativeAdChoicesView(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier)
        return
    }
    val scope = requireNativeAdScope()
    AndroidView(
        factory = { context -> AdChoicesView(context) },
        modifier = modifier,
        update = { view -> scope.nativeAdView.adChoicesView = view },
    )
}

@Composable
private fun NativeAdAssetView(
    modifier: Modifier,
    content: @Composable () -> Unit,
    register: (NativeAdView, ComposeView) -> Unit,
) {
    if (LocalInspectionMode.current) {
        Box(modifier) { content() }
        return
    }
    val scope = requireNativeAdScope()
    val parentCompositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            register(scope.nativeAdView, view)
            view.setParentCompositionContext(parentCompositionContext)
            view.setContent { currentContent() }
        },
    )
}

@Composable
private fun NativeAdRippleAssetView(
    rippleColor: Color,
    cornerRadius: Dp,
    register: (View) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val parentCompositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    val ripple =
        remember(rippleColor, radiusPx) {
            RippleDrawable(
                ColorStateList.valueOf(rippleColor.toArgb()),
                null,
                GradientDrawable().apply {
                    setColor(Color.White.toArgb())
                    this.cornerRadius = radiusPx
                },
            )
        }
    AndroidView(
        factory = { context ->
            NativeAdRippleLayout(context).apply {
                addView(
                    ComposeView(context),
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        },
        modifier = modifier,
        update = { view ->
            view.foreground = ripple
            register(view)
            val composeView = view.getChildAt(0) as ComposeView
            composeView.setParentCompositionContext(parentCompositionContext)
            composeView.setContent { currentContent() }
        },
    )
}

private class NativeAdRippleLayout(
    context: Context
) : FrameLayout(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                foreground?.setHotspot(event.x, event.y)
                isPressed = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.x < 0 || event.x >= width || event.y < 0 || event.y >= height) {
                    isPressed = false
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> isPressed = false
        }
        return super.dispatchTouchEvent(event)
    }
}

@Composable
private fun requireNativeAdScope(): NativeAdScope =
    checkNotNull(LocalNativeAdScope.current) {
        "Native ad assets must be placed inside NativeAdViewContainer."
    }
