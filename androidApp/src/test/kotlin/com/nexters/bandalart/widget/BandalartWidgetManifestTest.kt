/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.widget

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BandalartWidgetManifestTest {
    @Test
    fun `receiver stays private while configuration activity stays exported`() {
        val manifest =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(File("src/main/AndroidManifest.xml"))
        val androidNamespace = "http://schemas.android.com/apk/res/android"

        fun exported(
            componentTag: String,
            className: String
        ): String? {
            val components = manifest.getElementsByTagName(componentTag)
            return (0 until components.length)
                .map { components.item(it) }
                .firstOrNull { it.attributes.getNamedItemNS(androidNamespace, "name")?.nodeValue == className }
                ?.attributes
                ?.getNamedItemNS(androidNamespace, "exported")
                ?.nodeValue
        }

        assertEquals("false", exported("receiver", BandalartGlanceWidgetReceiver::class.java.name))
        assertEquals("true", exported("activity", BandalartWidgetConfigurationActivity::class.java.name))
    }
}
