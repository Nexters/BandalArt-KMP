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

package com.nexters.bandalart

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.nexters.bandalart.di.metro.AppGraph
import com.nexters.bandalart.di.metro.createAndroidAppGraph
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class BandalartApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()

        appGraph = createAndroidAppGraph(this)

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        Firebase.initialize(this)

        multiplatform.network.cmptoast.AppContext
            .apply { set(applicationContext) }
    }
}
