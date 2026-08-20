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

package com.nexters.bandalart.ads

import com.nexters.bandalart.core.common.RewardedAdPurpose
import com.nexters.bandalart.core.common.RewardedAdResult
import platform.UIKit.UIView

/** Implemented by the Swift app target, where Google Mobile Ads is linked through SPM. */
interface IosAdsBridge {
    fun makeBannerView(): UIView

    fun showRewarded(
        requestId: Long,
        purpose: RewardedAdPurpose,
        completion: (RewardedAdResult) -> Unit,
    )

    fun consumeRewarded(requestId: Long)
}
