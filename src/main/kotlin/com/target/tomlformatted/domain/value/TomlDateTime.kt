package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.parseAggregate
import com.target.tomlformatted.parse.valueSetRegexString
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeComponents.Companion.Format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DateTimeFormatBuilder
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional

fun DateTimeFormatBuilder.WithDateTimeComponents.addTomlDate() {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
}

fun DateTimeFormatBuilder.WithDateTimeComponents.addTomlTime() {
    hour()
    char(':')
    minute()
    char(':')
    second()
    optional {
        char('.')
        secondFraction(1, 9)
    }
}

fun DateTimeFormatBuilder.WithDateTimeComponents.addTomlDateTime() {
    addTomlDate()
    alternativeParsing(
        { char(' ') },
        { char('t') },
    ) {
        char('T')
    }
    addTomlTime()
}

val TOML_LOCAL_DATE_TIME: DateTimeFormat<DateTimeComponents> =
    Format {
        addTomlDateTime()
    }

val TOML_LOCAL_DATE: DateTimeFormat<DateTimeComponents> =
    Format {
        addTomlDate()
    }

val TOML_LOCAL_TIME: DateTimeFormat<DateTimeComponents> =
    Format {
        addTomlTime()
    }

val TOML_DATE_TIME: DateTimeFormat<DateTimeComponents> =
    Format {
        addTomlDateTime()
        alternativeParsing({
            offsetHours()
        }) {
            offset(UtcOffset.Formats.ISO)
        }
    }

const val DATE_REGEX_STR = "[\\d-]+"
const val TIME_REGEX_STR = "[\\d:\\.Zz+-]+"
const val DATE_TIME_REGEX_STR = "$DATE_REGEX_STR[Tt ]$TIME_REGEX_STR"

fun getCandidateDate(
    currentBody: String,
    regexStr: String,
    dateTimeFormat: DateTimeFormat<DateTimeComponents>,
): String? {
    val matchResult =
        "^($regexStr)${valueSetRegexString()}".toRegex().find(currentBody)
            ?: return null
    val candidate = matchResult.groupValues[1]

    return if (dateTimeFormat.parseOrNull(candidate) != null) {
        candidate
    } else {
        null
    }
}

interface ITomlDateTime<T> : ITomlValue<T> {
    companion object {
        private val options =
            listOf(
                { body: String -> TomlOffsetDateTime.parse(body) },
                { body: String -> TomlLocalDateTime.parse(body) },
                { body: String -> TomlLocalDate.parse(body) },
                { body: String -> TomlLocalTime.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlDateTime<*>> = parseAggregate(options, currentBody)
    }
}

data class TomlOffsetDateTime(
    val value: String,
) : ITomlDateTime<Instant> {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlOffsetDateTime> {
            val candidate = getCandidateDate(currentBody, DATE_TIME_REGEX_STR, TOML_DATE_TIME)
            return if (candidate != null) {
                val remainingBody = currentBody.drop(candidate.length)
                TomlParseResult.Success(TomlOffsetDateTime(candidate), remainingBody)
            } else {
                TomlParseResult.Failure("No OffsetDateTime found")
            }
        }
    }

    private val components = TOML_DATE_TIME.parse(value)

    override fun toString(): String = TOML_DATE_TIME.format(components)

    override fun toTomlString(): String = value

    fun toInstant(): Instant = components.toInstantUsingOffset()

    override fun toValue(): Instant = toInstant()
}

data class TomlLocalDateTime(
    val value: String,
) : ITomlDateTime<LocalDateTime> {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlLocalDateTime> {
            val candidate = getCandidateDate(currentBody, DATE_TIME_REGEX_STR, TOML_LOCAL_DATE_TIME)
            return if (candidate != null) {
                val remainingBody = currentBody.drop(candidate.length)
                TomlParseResult.Success(TomlLocalDateTime(candidate), remainingBody)
            } else {
                TomlParseResult.Failure("No OffsetDateTime found")
            }
        }
    }

    private val components = TOML_LOCAL_DATE_TIME.parse(value)

    override fun toString(): String = TOML_LOCAL_DATE_TIME.format(components)

    override fun toTomlString(): String = value

    fun toLocalDateTime(): LocalDateTime = components.toLocalDateTime()

    override fun toValue(): LocalDateTime = toLocalDateTime()
}

data class TomlLocalDate(
    val value: String,
) : ITomlDateTime<LocalDate> {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlLocalDate> {
            val candidate = getCandidateDate(currentBody, DATE_REGEX_STR, TOML_LOCAL_DATE)
            return if (candidate != null) {
                val remainingBody = currentBody.drop(candidate.length)
                TomlParseResult.Success(TomlLocalDate(candidate), remainingBody)
            } else {
                TomlParseResult.Failure("No OffsetDateTime found")
            }
        }
    }

    private val components = TOML_LOCAL_DATE.parse(value)

    override fun toString(): String = TOML_LOCAL_DATE.format(components)

    override fun toTomlString(): String = value

    fun toLocalDate(): LocalDate = components.toLocalDate()

    override fun toValue(): LocalDate = toLocalDate()
}

data class TomlLocalTime(
    val value: String,
) : ITomlDateTime<LocalTime> {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlLocalTime> {
            val candidate = getCandidateDate(currentBody, TIME_REGEX_STR, TOML_LOCAL_TIME)
            return if (candidate != null) {
                val remainingBody = currentBody.drop(candidate.length)
                TomlParseResult.Success(TomlLocalTime(candidate), remainingBody)
            } else {
                TomlParseResult.Failure("No OffsetDateTime found")
            }
        }
    }

    private val components = TOML_LOCAL_TIME.parse(value)

    override fun toString(): String = TOML_LOCAL_TIME.format(components)

    override fun toTomlString(): String = value

    fun toLocalTime(): LocalTime = components.toLocalTime()

    override fun toValue(): LocalTime = toLocalTime()
}
