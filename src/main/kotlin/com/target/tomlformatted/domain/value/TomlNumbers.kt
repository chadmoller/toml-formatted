package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlPiece
import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.VALUE_TERMINATOR_SET
import com.target.tomlformatted.parse.parseAggregate
import com.target.tomlformatted.parse.valueSetRegexString
import java.math.BigDecimal
import java.math.BigInteger

interface ITomlNumber<T : Number> : ITomlValue<T> {
    companion object {
        private val options =
            listOf(
                { body: String -> ITomlInteger.parse(body) },
                { body: String -> TomlFloat.parse(body) },
                { body: String -> ITomlDouble.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlNumber<*>> = parseAggregate(options, currentBody)
    }
}

interface ITomlInteger : ITomlNumber<BigInteger> {
    fun toBigInteger(): BigInteger

    companion object {
        private val options =
            listOf(
                { body: String -> TomlDecimalInteger.parse(body) },
                { body: String -> TomlNonDecimalInteger.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlInteger> = parseAggregate(options, currentBody)
    }
}

interface ITomlDouble : ITomlNumber<Double> {
    fun toDouble(): Double

    companion object {
        private val options =
            listOf(
                { body: String -> TomlInfinity.parse(body) },
                { body: String -> TomlNan.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlDouble> = parseAggregate(options, currentBody)
    }
}

enum class Sign(private val sign: Char?) {
    POSITIVE('+'),
    NEGATIVE('-'),
    UNSIGNED(null),
    ;

    companion object {
        fun parse(currentBody: String): TomlParseResult.Success<Sign> =
            when {
                currentBody.startsWith("+") ->
                    TomlParseResult.Success(value = POSITIVE, currentBody.substring(1))
                currentBody.startsWith("-") ->
                    TomlParseResult.Success(value = NEGATIVE, currentBody.substring(1))
                else ->
                    TomlParseResult.Success(value = UNSIGNED, currentBody)
            }
    }

    override fun toString(): String = sign?.toString() ?: ""
}

private val DECIMAL_REGEX = "\\d+".toRegex()
private val HEX_REGEX = "[A-Fa-f\\d]+".toRegex()
private val OCTAL_REGEX = "[0-7]+".toRegex()
private val BINARY_REGEX = "[0-1]+".toRegex()

private fun Regex.toPiece(): Regex = "^($pattern)_".toRegex()

private fun Regex.toEndPiece(terminators: Set<String> = VALUE_TERMINATOR_SET): Regex =
    "^($pattern)${valueSetRegexString(
        terminators,
    )}".toRegex()

private fun <T : ITomlInteger> parseInt(
    currentBody: String,
    regex: Regex,
    terminators: Set<String> = VALUE_TERMINATOR_SET,
    builder: (List<String>, String) -> TomlParseResult<T>,
): TomlParseResult<T> {
    val pieceRegex = regex.toPiece()
    val endPieceRegex = regex.toEndPiece(terminators)

    var remainingBody = currentBody
    val pieces = mutableListOf<String>()
    var matchResult = pieceRegex.find(remainingBody)

    while (matchResult != null) {
        pieces.add(matchResult.groupValues[1])
        remainingBody = remainingBody.substring(matchResult.value.length)
        matchResult = pieceRegex.find(remainingBody)
    }

    matchResult = endPieceRegex.find(remainingBody)
    return if (matchResult != null) {
        pieces.add(matchResult.groupValues[1])
        remainingBody = remainingBody.substring(matchResult.groupValues[1].length)
        builder(pieces, remainingBody)
    } else {
        TomlParseResult.Failure(
            "$pieces were valid, but $remainingBody didn't terminate properly",
        )
    }
}

data class TomlDecimalInteger(
    val sign: Sign = Sign.UNSIGNED,
    val pieces: List<String>,
    val forbidLeadingZero: Boolean = true,
) : ITomlInteger {
    init {
        require(pieces.isNotEmpty()) { "Some pieces are required for a number" }
        require(pieces.all { DECIMAL_REGEX.matches(it) }) { "All pieces must follow the regex $DECIMAL_REGEX" }
        if (forbidLeadingZero) {
            if (pieces.first().startsWith("0")) {
                require(pieces.size == 1) { "Leading zeros aren't allowed on a decimal number" }
                require(pieces.first() == "0") { "Leading zeros aren't allowed on a decimal number" }
            }
        }
    }

    companion object {
        fun parse(
            currentBody: String,
            forbidLeadingZero: Boolean = true,
            terminators: Set<String> = VALUE_TERMINATOR_SET,
        ): TomlParseResult<TomlDecimalInteger> {
            val signResult = Sign.parse(currentBody)

            return parseInt(
                currentBody = signResult.remainingBody,
                regex = DECIMAL_REGEX,
                terminators = terminators,
                builder = { pieces: List<String>, remainingBody: String ->
                    TomlParseResult.Success(
                        value =
                            TomlDecimalInteger(
                                sign = signResult.value,
                                pieces = pieces,
                                forbidLeadingZero = forbidLeadingZero,
                            ),
                        remainingBody = remainingBody,
                    )
                },
            )
        }
    }

    override fun toTomlString(): String = "$sign${pieces.joinToString("_")}"

    override fun toString(): String = "$sign${pieces.joinToString("")}"

    override fun toBigInteger(): BigInteger = toString().toBigInteger()

    override fun toValue(): BigInteger = toBigInteger()
}

data class TomlNonDecimalInteger(
    val pieces: List<String>,
    val base: Base,
) : ITomlInteger {
    enum class Base(val radix: Int, val code: Char, val regex: Regex) {
        HEX(16, 'x', HEX_REGEX),
        OCTAL(8, 'o', OCTAL_REGEX),
        BINARY(2, 'b', BINARY_REGEX),
        ;

        companion object {
            fun parse(currentBody: String): TomlParseResult<Base> =
                when {
                    currentBody.startsWith("0x") ->
                        TomlParseResult.Success(value = HEX, currentBody.substring(2))
                    currentBody.startsWith("0o") ->
                        TomlParseResult.Success(value = OCTAL, currentBody.substring(2))
                    currentBody.startsWith("0b") ->
                        TomlParseResult.Success(value = BINARY, currentBody.substring(2))
                    else ->
                        TomlParseResult.Failure("No nonDecimal starter")
                }
        }
    }

    init {
        require(pieces.isNotEmpty()) { "Some pieces are required for a number" }
        require(pieces.all { base.regex.matches(it) }) { "All pieces must follow the regex ${base.regex}" }
    }

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlNonDecimalInteger> {
            val baseResult = Base.parse(currentBody)
            if (baseResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure(baseResult.message)
            }
            baseResult as TomlParseResult.Success
            return parseInt(
                currentBody = baseResult.remainingBody,
                regex = baseResult.value.regex,
                builder = { pieces: List<String>, remainingBody: String ->
                    TomlParseResult.Success(
                        value =
                            TomlNonDecimalInteger(
                                pieces = pieces,
                                base = baseResult.value,
                            ),
                        remainingBody = remainingBody,
                    )
                },
            )
        }
    }

    override fun toTomlString(): String = "0${base.code}${pieces.joinToString("_")}"

    override fun toString(): String = pieces.joinToString("")

    override fun toBigInteger(): BigInteger = toString().toBigInteger(base.radix)

    override fun toValue(): BigInteger = toBigInteger()
}

data class TomlFloat(
    val intPart: TomlDecimalInteger,
    val fractionPart: TomlDecimalInteger? = null,
    val exponent: Exponent? = null,
) : ITomlNumber<BigDecimal> {
    data class Exponent(
        val power: TomlDecimalInteger,
        val marker: ExponentMarker,
    ) : TomlPiece {
        companion object {
            fun parse(currentBody: String): TomlParseResult<Exponent> =
                when (val markerResult = ExponentMarker.parse(currentBody)) {
                    is TomlParseResult.Failure<ExponentMarker> -> TomlParseResult.Failure("Failed to find exponent ${markerResult.message}")
                    is TomlParseResult.Success -> {
                        when (val powerResult = TomlDecimalInteger.parse(markerResult.remainingBody, false)) {
                            is TomlParseResult.Failure<TomlDecimalInteger> ->
                                TomlParseResult.Failure(
                                    "Failed to find exponent int ${powerResult.message}",
                                )
                            is TomlParseResult.Success ->
                                TomlParseResult.Success(
                                    value = Exponent(marker = markerResult.value, power = powerResult.value),
                                    remainingBody = powerResult.remainingBody,
                                )
                        }
                    }
                }
        }

        override fun toTomlString(): String = "${marker}${power.toTomlString()}"

        override fun toString(): String = "${ExponentMarker.E}$power"
    }

    enum class ExponentMarker(private val chr: Char) {
        E('E'),
        LOWER_E('e'),
        ;

        companion object {
            fun parse(currentBody: String): TomlParseResult<ExponentMarker> =
                when {
                    currentBody.startsWith(E.chr) ->
                        TomlParseResult.Success(value = E, currentBody.substring(1))
                    currentBody.startsWith(LOWER_E.chr) ->
                        TomlParseResult.Success(value = LOWER_E, currentBody.substring(1))
                    else ->
                        TomlParseResult.Failure("")
                }
        }

        override fun toString(): String = "$chr"
    }

    init {
        if (fractionPart != null) {
            require(fractionPart.sign == Sign.UNSIGNED) { "After the Decimal point, another sign is not allowed" }
        }
    }

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlFloat> =
            when (val intPartResult = TomlDecimalInteger.parse(currentBody, true, VALUE_TERMINATOR_SET + "." + "e" + "E")) {
                is TomlParseResult.Failure -> TomlParseResult.Failure("Failed to find a starting integer")
                is TomlParseResult.Success -> {
                    val intPart = intPartResult.value
                    var fractionPart: TomlDecimalInteger? = null
                    var exponent: Exponent? = null
                    var result: TomlParseResult<TomlFloat>? = null

                    var workingBody = intPartResult.remainingBody
                    if (workingBody.startsWith(".")) {
                        workingBody = workingBody.drop(1)
                        when (val fractionPartResult = TomlDecimalInteger.parse(workingBody, false, VALUE_TERMINATOR_SET + "e" + "E")) {
                            is TomlParseResult.Failure ->
                                result =
                                    TomlParseResult.Failure(
                                        "Decimal point existed without decimal value after",
                                    )
                            is TomlParseResult.Success -> {
                                fractionPart = fractionPartResult.value
                                workingBody = fractionPartResult.remainingBody
                            }
                        }
                    }
                    if (result == null && workingBody.startsWith('e', ignoreCase = true)) {
                        when (val exponentPartResult = Exponent.parse(workingBody)) {
                            is TomlParseResult.Failure -> result = TomlParseResult.Failure("Exponent failed to parse")
                            is TomlParseResult.Success -> {
                                exponent = exponentPartResult.value
                                workingBody = exponentPartResult.remainingBody
                            }
                        }
                    }
                    result
                        ?: TomlParseResult.Success(
                            value =
                                TomlFloat(
                                    intPart = intPart,
                                    fractionPart = fractionPart,
                                    exponent = exponent,
                                ),
                            remainingBody = workingBody,
                        )
                }
            }
    }

    override fun toTomlString(): String {
        val intStr = intPart.toTomlString()
        val fractionStr = if (fractionPart != null) ".${fractionPart.toTomlString()}" else ""
        val exponentStr = exponent?.toTomlString() ?: ""

        return "$intStr$fractionStr$exponentStr"
    }

    override fun toString(): String {
        val intStr = intPart.toString()
        val fractionStr = if (fractionPart != null) ".$fractionPart" else ""
        val exponentStr = exponent?.toString() ?: ""

        return "$intStr$fractionStr$exponentStr"
    }

    fun toBigDecimal(): BigDecimal = toString().toBigDecimal()

    override fun toValue(): BigDecimal = toBigDecimal()
}

data class TomlInfinity(
    val sign: Sign = Sign.UNSIGNED,
) : ITomlDouble {
    companion object {
        private const val VALUE = "inf"

        fun parse(currentBody: String): TomlParseResult<TomlInfinity> {
            val signResult = Sign.parse(currentBody)
            val workingBody = signResult.remainingBody
            return if (workingBody.startsWith(VALUE)) {
                TomlParseResult.Success(TomlInfinity(signResult.value), workingBody.drop(VALUE.length))
            } else {
                TomlParseResult.Failure("Incompatible infinity")
            }
        }
    }

    override fun toTomlString(): String = "$sign$VALUE"

    override fun toString(): String = "$sign∞"

    override fun toDouble(): Double = if (sign == Sign.NEGATIVE) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY

    override fun toValue(): Double = toDouble()
}

data class TomlNan(
    val sign: Sign = Sign.UNSIGNED,
) : ITomlDouble {
    companion object {
        private const val VALUE = "nan"

        fun parse(currentBody: String): TomlParseResult<TomlNan> {
            val signResult = Sign.parse(currentBody)
            val workingBody = signResult.remainingBody
            return if (workingBody.startsWith(VALUE)) {
                TomlParseResult.Success(TomlNan(signResult.value), workingBody.drop(VALUE.length))
            } else {
                TomlParseResult.Failure("Incompatible infinity")
            }
        }
    }

    override fun toTomlString(): String = "$sign$VALUE"

    override fun toString(): String = "NaN"

    override fun toDouble(): Double = Double.NaN

    override fun toValue(): Double = toDouble()
}
