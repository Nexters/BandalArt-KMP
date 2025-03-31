package com.nexters.bandalart.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import com.nexters.bandalart.buildlogic.configure.applyPlugins
import com.nexters.bandalart.buildlogic.configure.libs
import org.gradle.kotlin.dsl.configure

class SpotlessPlugin : BuildLogicPlugin(
    {
        applyPlugins("com.diffplug.spotless")

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/**/*.kt")
                licenseHeader(licenseHeaderKotlin)
                ktlint(libs.versions.ktlint.get())
            }
            format("kts") {
                target("**/*.kts")
                targetExclude("**/build/**/*.kts")
                licenseHeader(licenseHeaderKotlin, "(^(?![\\/ ]\\*).*$)")
            }
            format("xml") {
                target("**/*.xml")
                targetExclude("**/build/**/*.xml")
                licenseHeader(licenseHeaderXml, "(^(?![\\/ ]\\*).*$)")
            }
        }
    },
)

private val licenseHeaderKotlin =
    buildString {
        append("/*\n")
        append(" * Copyright \$YEAR easyhooon\n")
        append(" *\n")
        append(" * Licensed under the Apache License, Version 2.0 (the \"License\");\n")
        append(" * you may not use this file except in compliance with the License.\n")
        append(" * You may obtain a copy of the License at\n")
        append(" *\n")
        append(" *     http://www.apache.org/licenses/LICENSE-2.0\n")
        append(" *\n")
        append(" * Unless required by applicable law or agreed to in writing, software\n")
        append(" * distributed under the License is distributed on an \"AS IS\" BASIS,\n")
        append(" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n")
        append(" * See the License for the specific language governing permissions and\n")
        append(" * limitations under the License.\n")
        append(" */\n")
        append("\n")
    }

private val licenseHeaderXml =
    buildString {
        append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        append("<!--\n")
        append("     Copyright \$YEAR easyhooon\n")
        append("     Licensed under the Apache License, Version 2.0 (the \"License\");\n")
        append("     you may not use this file except in compliance with the License.\n")
        append("     You may obtain a copy of the License at\n")
        append("          http://www.apache.org/licenses/LICENSE-2.0\n")
        append("     Unless required by applicable law or agreed to in writing, software\n")
        append("     distributed under the License is distributed on an \"AS IS\" BASIS,\n")
        append("     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n")
        append("     See the License for the specific language governing permissions and\n")
        append("     limitations under the License.\n")
        append("-->")
    }
