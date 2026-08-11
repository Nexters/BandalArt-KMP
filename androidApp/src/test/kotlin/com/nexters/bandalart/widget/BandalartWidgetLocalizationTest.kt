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

package com.nexters.bandalart.widget

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BandalartWidgetLocalizationTest {
    @Test
    fun `widget resources cover Korean English and Japanese`() {
        val requiredKeys =
            setOf(
                "bandalart_widget_name",
                "bandalart_widget_description",
                "bandalart_widget_loading",
                "bandalart_widget_unconfigured",
                "bandalart_widget_deleted",
                "bandalart_widget_completion_ratio",
                "bandalart_widget_no_subgoal",
                "bandalart_widget_no_tasks",
                "bandalart_widget_unnamed_goal",
                "bandalart_widget_config_title",
                "bandalart_widget_config_section_title",
                "bandalart_widget_config_choose_goal",
                "bandalart_widget_config_create_first",
                "bandalart_widget_config_subgoal",
                "bandalart_widget_config_subgoal_description",
                "bandalart_widget_config_none",
                "bandalart_widget_config_unnamed_subgoal",
                "bandalart_widget_config_saving",
                "bandalart_widget_config_save",
            )

        val translations =
            listOf("values", "values-en", "values-ja").associateWith { directory ->
                resourceStrings(directory)
            }
        translations.forEach { (directory, strings) ->
            assertTrue(strings.keys.containsAll(requiredKeys), "$directory is missing widget translations")
            assertTrue(requiredKeys.all { strings.getValue(it).isNotBlank() }, "$directory has blank widget translations")
        }
        assertEquals(
            3,
            translations.values
                .map { it.getValue("bandalart_widget_unconfigured") }
                .toSet()
                .size,
        )
    }

    private fun resourceStrings(directory: String): Map<String, String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File("src/main/res/$directory/strings.xml"))
        val strings = document.getElementsByTagName("string")
        return (0 until strings.length).associate { index ->
            val node = strings.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
}
