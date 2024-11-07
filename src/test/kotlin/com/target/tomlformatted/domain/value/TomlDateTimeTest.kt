package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test

class TomlDateTimeTest {
    // 1979-05-27T07:32:00.123Z
    private val epochMillis = 296638320123L
    private val instant = Instant.fromEpochMilliseconds(epochMillis)
    private val localDateTime =
        LocalDateTime(
            year = 1979,
            monthNumber = 5,
            dayOfMonth = 27,
            hour = 7,
            minute = 32,
            second = 0,
            nanosecond = 123_000_000,
        )

    private val localDate =
        LocalDate(
            year = 1979,
            monthNumber = 5,
            dayOfMonth = 27,
        )

    private val localTime =
        LocalTime(
            hour = 7,
            minute = 32,
            second = 0,
            nanosecond = 123_000_000,
        )

    @Test
    fun `Test candidateDates dateTimes`() {
        assertSoftly {
            getCandidateDate("", DATE_TIME_REGEX_STR, TOML_DATE_TIME) shouldBe null
            getCandidateDate("1234-05-17", DATE_TIME_REGEX_STR, TOML_DATE_TIME) shouldBe null
            getCandidateDate("1234-05-17 Hello", DATE_TIME_REGEX_STR, TOML_DATE_TIME) shouldBe null
            getCandidateDate(
                "1234-05-17T07:21:58Z",
                DATE_TIME_REGEX_STR,
                TOML_DATE_TIME,
            ) shouldBe "1234-05-17T07:21:58Z"
            getCandidateDate(
                "1234-05-17 07:21:58+07:00 ",
                DATE_TIME_REGEX_STR,
                TOML_DATE_TIME,
            ) shouldBe "1234-05-17 07:21:58+07:00"
            getCandidateDate(
                "1234-05-17T07:21:58z Hello",
                DATE_TIME_REGEX_STR,
                TOML_DATE_TIME,
            ) shouldBe "1234-05-17T07:21:58z"
            getCandidateDate(" 1234-05-17T07:21:58Z", DATE_TIME_REGEX_STR, TOML_DATE_TIME) shouldBe null
        }
    }

    @Test
    fun `Test candidateDates local date time`() {
        assertSoftly {
            getCandidateDate("", DATE_TIME_REGEX_STR, TOML_LOCAL_DATE_TIME) shouldBe null
            getCandidateDate("1234-05-17", DATE_TIME_REGEX_STR, TOML_LOCAL_DATE_TIME) shouldBe null
            getCandidateDate("1234-05-17 Hello", DATE_TIME_REGEX_STR, TOML_LOCAL_DATE_TIME) shouldBe null
            getCandidateDate(
                "1234-05-17T07:21:58",
                DATE_TIME_REGEX_STR,
                TOML_LOCAL_DATE_TIME,
            ) shouldBe "1234-05-17T07:21:58"
            getCandidateDate(
                "1234-05-17 07:21:58 Hello",
                DATE_TIME_REGEX_STR,
                TOML_LOCAL_DATE_TIME,
            ) shouldBe "1234-05-17 07:21:58"
            getCandidateDate("1234-05-17T07:21:58z Hello", DATE_TIME_REGEX_STR, TOML_LOCAL_DATE_TIME) shouldBe null
            getCandidateDate(" 1234-05-17T07:21:58", DATE_TIME_REGEX_STR, TOML_LOCAL_DATE_TIME) shouldBe null
        }
    }

    @Test
    fun `Test candidateDates local date`() {
        assertSoftly {
            getCandidateDate("", DATE_REGEX_STR, TOML_LOCAL_DATE) shouldBe null
            getCandidateDate("1234-05-17T07:21:58", DATE_REGEX_STR, TOML_LOCAL_DATE) shouldBe null
            getCandidateDate("1234-05-17", DATE_REGEX_STR, TOML_LOCAL_DATE) shouldBe "1234-05-17"
            getCandidateDate("1234-05-17 Hello", DATE_REGEX_STR, TOML_LOCAL_DATE) shouldBe "1234-05-17"
            getCandidateDate("1234-05-17 07:21:58 Hello", DATE_REGEX_STR, TOML_LOCAL_DATE) shouldBe "1234-05-17"
            getCandidateDate(" 1234-05-17", DATE_REGEX_STR, TOML_LOCAL_DATE) shouldBe null
        }
    }

    @Test
    fun `Test candidateDates local time`() {
        assertSoftly {
            getCandidateDate("", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe null
            getCandidateDate("1234-05-17", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe null
            getCandidateDate("1234-05-17 07:21:58", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe null
            getCandidateDate("07:21:58", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe "07:21:58"
            getCandidateDate("07:21:58 Hello", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe "07:21:58"
            getCandidateDate("07:21:58z Hello", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe null
            getCandidateDate(" 07:21:58", TIME_REGEX_STR, TOML_LOCAL_TIME) shouldBe null
        }
    }

    @Test
    fun `TomlOffsetDateTime T code Z zone`() {
        assertSoftly(TomlOffsetDateTime("1979-05-27T07:32:00.123Z")) {
            toInstant() shouldBe instant
            toTomlString() shouldBe "1979-05-27T07:32:00.123Z"
            toString() shouldBe "1979-05-27T07:32:00.123Z"
        }
    }

    @Test
    fun `TomlOffsetDateTime no time code, Z zone`() {
        assertSoftly(TomlOffsetDateTime("1979-05-27 07:32:00.123Z")) {
            toInstant() shouldBe instant
            toTomlString() shouldBe "1979-05-27 07:32:00.123Z"
            toString() shouldBe "1979-05-27T07:32:00.123Z"
        }
    }

    @Test
    fun `TomlOffsetDateTime T code offset time`() {
        assertSoftly(TomlOffsetDateTime("1979-05-27T00:32:00.123-07:00")) {
            toInstant() shouldBe instant
            toTomlString() shouldBe "1979-05-27T00:32:00.123-07:00"
            toString() shouldBe "1979-05-27T00:32:00.123-07:00"
        }
    }

    @Test
    fun `TomlOffsetDateTime T code positive offset time`() {
        assertSoftly(TomlOffsetDateTime("1979-05-27T14:32:00.123+07:00")) {
            toInstant() shouldBe instant
            toTomlString() shouldBe "1979-05-27T14:32:00.123+07:00"
            toString() shouldBe "1979-05-27T14:32:00.123+07:00"
        }
    }

    @Test
    fun `TomlOffsetDateTime parse valid`() {
        val result = TomlOffsetDateTime.parse("1979-05-27T14:32:00.123+07:00 Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlOffsetDateTime>>()
            value shouldBe TomlOffsetDateTime("1979-05-27T14:32:00.123+07:00")
            remainingBody shouldBe " Rest of body"
        }
    }

    @Test
    fun `TomlOffsetDateTime parse invalid`() {
        val result = TomlOffsetDateTime.parse("1979-05-27T14:32:00.123 Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlOffsetDateTime>>()
        }
    }

    @Test
    fun `TomlLocalDateTime T Code`() {
        assertSoftly(TomlLocalDateTime("1979-05-27T07:32:00.123")) {
            toLocalDateTime() shouldBe localDateTime
            toTomlString() shouldBe "1979-05-27T07:32:00.123"
            toString() shouldBe "1979-05-27T07:32:00.123"
        }
    }

    @Test
    fun `TomlLocalDateTime no time Code`() {
        assertSoftly(TomlLocalDateTime("1979-05-27 07:32:00.123")) {
            toLocalDateTime() shouldBe localDateTime
            toTomlString() shouldBe "1979-05-27 07:32:00.123"
            toString() shouldBe "1979-05-27T07:32:00.123"
        }
    }

    @Test
    fun `TomlLocalDateTime parse valid`() {
        val result = TomlLocalDateTime.parse("1979-05-27T07:32:00.123 Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlLocalDateTime>>()
            value shouldBe TomlLocalDateTime("1979-05-27T07:32:00.123")
            remainingBody shouldBe " Rest of body"
        }
    }

    @Test
    fun `TomlLocalDateTime parse invalid`() {
        val result = TomlLocalDateTime.parse("1979-05-27T14:32:00.123Z Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlLocalDateTime>>()
        }
    }

    @Test
    fun `TomlLocalDate test`() {
        assertSoftly(TomlLocalDate("1979-05-27")) {
            toLocalDate() shouldBe localDate
            toTomlString() shouldBe "1979-05-27"
            toString() shouldBe "1979-05-27"
        }
    }

    @Test
    fun `TomlLocalDate parse valid`() {
        val result = TomlLocalDate.parse("1979-05-27 Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlLocalDate>>()
            value shouldBe TomlLocalDate("1979-05-27")
            remainingBody shouldBe " Rest of body"
        }
    }

    @Test
    fun `TomlLocalDate parse invalid`() {
        val result = TomlLocalDate.parse("1979-05-27T14:32:00.123Z Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlLocalDate>>()
        }
    }

    @Test
    fun `TomlLocalTime test`() {
        assertSoftly(TomlLocalTime("07:32:00.123")) {
            toLocalTime() shouldBe localTime
            toTomlString() shouldBe "07:32:00.123"
            toString() shouldBe "07:32:00.123"
        }
    }

    @Test
    fun `TomlLocalTime parse valid`() {
        val result = TomlLocalTime.parse("07:32:00.123 Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlLocalTime>>()
            value shouldBe TomlLocalTime("07:32:00.123")
            remainingBody shouldBe " Rest of body"
        }
    }

    @Test
    fun `TomlLocalTime parse invalid`() {
        val result = TomlLocalTime.parse("14:32:00.123Z Rest of body")
        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlLocalTime>>()
        }
    }

    @Test
    fun `ITomlDateTime Parse`() {
        assertSoftly {
            ITomlDateTime.parse("1979-05-27T14:32:00.123+07:00 Rest of body") shouldBe
                TomlParseResult.Success(
                    value = TomlOffsetDateTime("1979-05-27T14:32:00.123+07:00"),
                    remainingBody = " Rest of body",
                )
            ITomlDateTime.parse("1979-05-27T07:32:00.123 Rest of body") shouldBe
                TomlParseResult.Success(
                    value = TomlLocalDateTime("1979-05-27T07:32:00.123"),
                    remainingBody = " Rest of body",
                )

            ITomlDateTime.parse("1979-05-27 Rest of body") shouldBe
                TomlParseResult.Success(
                    value = TomlLocalDate("1979-05-27"),
                    remainingBody = " Rest of body",
                )

            ITomlDateTime.parse("07:32:00.123 Rest of body") shouldBe
                TomlParseResult.Success(
                    value = TomlLocalTime("07:32:00.123"),
                    remainingBody = " Rest of body",
                )
        }
    }
}
