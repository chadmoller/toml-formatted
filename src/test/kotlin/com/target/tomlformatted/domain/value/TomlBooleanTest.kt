package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TomlBooleanTest {
    @Test
    fun `TomlBoolean true`() {
        assertSoftly(TomlBoolean.TRUE) {
            toString() shouldBe "True"
            toTomlString() shouldBe "true"
            toBoolean() shouldBe true
        }
    }

    @Test
    fun `TomlBoolean false`() {
        assertSoftly(TomlBoolean.FALSE) {
            toString() shouldBe "False"
            toTomlString() shouldBe "false"
            toBoolean() shouldBe false
        }
    }

    @Test
    fun `Parse TomlBoolean true`() {
        assertSoftly(TomlBoolean.parse("true other text")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlBoolean>>()
            value shouldBe TomlBoolean.TRUE
            remainingBody shouldBe " other text"
        }
    }

    @Test
    fun `Parse TomlBoolean last value`() {
        assertSoftly(TomlBoolean.parse("true")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlBoolean>>()
            value shouldBe TomlBoolean.TRUE
            remainingBody shouldBe ""
        }
    }

    @Test
    fun `Parse TomlBoolean false`() {
        assertSoftly(TomlBoolean.parse("false other text")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlBoolean>>()
            value shouldBe TomlBoolean.FALSE
            remainingBody shouldBe " other text"
        }
    }

    @Test
    fun `Parse TomlBoolean fail`() {
        assertSoftly(TomlBoolean.parse("other text")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlBoolean>>()
        }
    }
}
