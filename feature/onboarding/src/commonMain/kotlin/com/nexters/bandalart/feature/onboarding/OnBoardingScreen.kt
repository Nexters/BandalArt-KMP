/*
 * Copyright 2025 easyhooon
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

package com.nexters.bandalart.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavOptions
import bandalart.core.designsystem.generated.resources.Res
import bandalart.core.designsystem.generated.resources.delete_description
import bandalart.core.designsystem.generated.resources.ic_onboarding_en
import bandalart.core.designsystem.generated.resources.ic_onboarding_kr
import bandalart.core.designsystem.generated.resources.onboarding_first_title
import bandalart.core.designsystem.generated.resources.onboarding_second_title
import bandalart.core.designsystem.generated.resources.onboarding_start
import com.nexters.bandalart.core.common.Language
import com.nexters.bandalart.core.common.extension.aspectRatioBasedOnOrientation
import com.nexters.bandalart.core.common.getLocale
import com.nexters.bandalart.core.common.utils.ObserveAsEvents
import com.nexters.bandalart.core.designsystem.theme.BandalartTheme
import com.nexters.bandalart.core.designsystem.theme.Gray50
import com.nexters.bandalart.core.designsystem.theme.Gray900
import com.nexters.bandalart.core.designsystem.theme.pretendardFontFamily
import com.nexters.bandalart.core.navigation.Route
import com.nexters.bandalart.core.ui.component.BandalartButton
import com.nexters.bandalart.core.ui.component.LottieImage
import com.nexters.bandalart.core.ui.component.PagerIndicator
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

private const val ONBOARDING_KR_LOTTIE_FILE = "files/onboarding_kr.json"
private const val ONBOARDING_EN_LOTTIE_FILE = "files/onboarding_en.json"

@Composable
internal fun OnBoardingRoute(
    navigateToHome: (NavOptions) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    ObserveAsEvents(flow = viewModel.uiEvent) { event ->
        when (event) {
            is OnBoardingUiEvent.NavigateToHome -> {
                val options = NavOptions.Builder()
                    .setPopUpTo(Route.Onboarding, inclusive = true)
                    .build()
                navigateToHome(options)
            }
        }
    }

    OnBoardingScreen(
        setOnboardingCompletedStatus = viewModel::setOnboardingCompletedStatus,
        modifier = modifier,
    )
}

@Composable
internal fun OnBoardingScreen(
    setOnboardingCompletedStatus: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // val configuration = LocalConfiguration.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Gray50,
    ) {
        val pageCount = 2
        val pagerState = rememberPagerState(pageCount = { pageCount })

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            PagerIndicator(
                pageCount = pageCount,
                pagerState = pagerState,
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(modifier = Modifier.height(50.dp))
                            Text(
                                text = stringResource(Res.string.onboarding_first_title),
                                modifier = modifier,
                                color = Gray900,
                                fontFamily = pretendardFontFamily(),
                                fontWeight = FontWeight.W700,
                                fontSize = 22.sp,
                                lineHeight = 30.8.sp,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(50.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatioBasedOnOrientation(1f)
                                        .background(Gray50),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when (getLocale().language) {
                                        Language.KOREAN -> {
                                            Image(
                                                imageVector = vectorResource(Res.drawable.ic_onboarding_kr),
                                                contentDescription = stringResource(Res.string.delete_description),
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }

                                        Language.ENGLISH -> {
                                            Image(
                                                imageVector = vectorResource(Res.drawable.ic_onboarding_en),
                                                contentDescription = stringResource(Res.string.delete_description),
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }

                                        else -> {
                                            Image(
                                                imageVector = vectorResource(Res.drawable.ic_onboarding_en),
                                                contentDescription = stringResource(Res.string.delete_description),
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Box {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(modifier = Modifier.height(50.dp))
                                Text(
                                    text = stringResource(Res.string.onboarding_second_title),
                                    modifier = modifier,
                                    color = Gray900,
                                    fontFamily = pretendardFontFamily(),
                                    fontWeight = FontWeight.W700,
                                    fontSize = 22.sp,
                                    lineHeight = 30.8.sp,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(50.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatioBasedOnOrientation(1f)
                                            .background(Gray50),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        LottieImage(
                                            jsonString = when (getLocale().language) {
                                                Language.KOREAN -> ONBOARDING_KR_LOTTIE_FILE
                                                Language.ENGLISH -> ONBOARDING_EN_LOTTIE_FILE
                                                else -> ONBOARDING_EN_LOTTIE_FILE
                                            },
                                            iterations = Int.MAX_VALUE,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                            BandalartButton(
                                onClick = { setOnboardingCompletedStatus(true) },
                                text = stringResource(Res.string.onboarding_start),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                                    .clip(shape = RoundedCornerShape(50.dp))
                                    .background(Gray900),
                            )
//                            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                                BandalartButton(
//                                    onClick = { setOnboardingCompletedStatus(true) },
//                                    text = stringResource(Res.string.onboarding_start),
//                                    modifier = Modifier
//                                        .wrapContentWidth()
//                                        .align(Alignment.BottomEnd)
//                                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
//                                        .clip(shape = RoundedCornerShape(50.dp))
//                                        .background(Gray900),
//                                )
//                            } else {
//                                BandalartButton(
//                                    onClick = { setOnboardingCompletedStatus(true) },
//                                    text = stringResource(Res.string.onboarding_start),
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .align(Alignment.BottomCenter)
//                                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
//                                        .clip(shape = RoundedCornerShape(50.dp))
//                                        .background(Gray900),
//                                )
//                            }
                        }
                    }
                }
            }
        }
    }
}

// @DevicePreview
@Preview
@Composable
private fun OnBoardingScreenPreview() {
    BandalartTheme {
        OnBoardingScreen(
            setOnboardingCompletedStatus = {},
        )
    }
}
