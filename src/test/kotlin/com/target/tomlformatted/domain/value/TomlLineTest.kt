package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlFillerWhitespace
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TomlLineTest {
    @Test
    fun `TomlValue with filler`() {
        val value =
            TomlValue(
                prefixFiller = TomlFillerWhitespace("\t"),
                value = TomlBasicString("Word"),
                suffixFiller = TomlFillerWhitespace(" "),
            )

        assertSoftly(value) {
            toString() shouldBe "Word"
            toTomlString() shouldBe "\t\"Word\" "
            toValue() shouldBe "Word"
        }
    }

    @Test
    fun `TomlValue with no filler`() {
        val value =
            TomlValue(
                value = TomlBasicString("Word"),
            )

        assertSoftly(value) {
            toString() shouldBe "Word"
            toTomlString() shouldBe "\"Word\""
            toValue() shouldBe "Word"
        }
    }
}
