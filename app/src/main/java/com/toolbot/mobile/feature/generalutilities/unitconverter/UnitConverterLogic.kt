package com.toolbot.mobile.feature.generalutilities.unitconverter

import kotlin.math.pow

enum class UnitCategoryType {
    LENGTH,
    WEIGHT,
    TEMPERATURE,
    AREA,
    VOLUME,
    SPEED,
    TIME,
    DATA,
}

data class UnitDefinition(
    val name: String,
    val shortName: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

data class UnitCategory(
    val type: UnitCategoryType,
    val label: String,
    val units: List<UnitDefinition>,
) {
    val defaultFrom: UnitDefinition get() = units.first()
    val defaultTo: UnitDefinition get() = units.getOrNull(1) ?: units.first()
}

val unitCategories = listOf(
    UnitCategory(
        type = UnitCategoryType.LENGTH,
        label = "Length",
        units = listOf(
            UnitDefinition("Millimeter", "mm", { it / 1000.0 }, { it * 1000.0 }),
            UnitDefinition("Centimeter", "cm", { it / 100.0 }, { it * 100.0 }),
            UnitDefinition("Meter", "m", { it }, { it }),
            UnitDefinition("Kilometer", "km", { it * 1000.0 }, { it / 1000.0 }),
            UnitDefinition("Inch", "in", { it * 0.0254 }, { it / 0.0254 }),
            UnitDefinition("Foot", "ft", { it * 0.3048 }, { it / 0.3048 }),
            UnitDefinition("Yard", "yd", { it * 0.9144 }, { it / 0.9144 }),
            UnitDefinition("Mile", "mi", { it * 1609.344 }, { it / 1609.344 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.WEIGHT,
        label = "Weight / Mass",
        units = listOf(
            UnitDefinition("Milligram", "mg", { it / 1_000_000.0 }, { it * 1_000_000.0 }),
            UnitDefinition("Gram", "g", { it / 1000.0 }, { it * 1000.0 }),
            UnitDefinition("Kilogram", "kg", { it }, { it }),
            UnitDefinition("Ounce", "oz", { it * 0.028349523125 }, { it / 0.028349523125 }),
            UnitDefinition("Pound", "lb", { it * 0.45359237 }, { it / 0.45359237 }),
            UnitDefinition("Tonne", "t", { it * 1000.0 }, { it / 1000.0 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.TEMPERATURE,
        label = "Temperature",
        units = listOf(
            UnitDefinition("Celsius", "°C", { it }, { it }),
            UnitDefinition("Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 }, { (it * 9.0 / 5.0) + 32.0 }),
            UnitDefinition("Kelvin", "K", { it - 273.15 }, { it + 273.15 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.AREA,
        label = "Area",
        units = listOf(
            UnitDefinition("Square meter", "m²", { it }, { it }),
            UnitDefinition("Square kilometer", "km²", { it * 1_000_000.0 }, { it / 1_000_000.0 }),
            UnitDefinition("Square foot", "ft²", { it * 0.09290304 }, { it / 0.09290304 }),
            UnitDefinition("Square yard", "yd²", { it * 0.83612736 }, { it / 0.83612736 }),
            UnitDefinition("Acre", "ac", { it * 4046.8564224 }, { it / 4046.8564224 }),
            UnitDefinition("Hectare", "ha", { it * 10000.0 }, { it / 10000.0 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.VOLUME,
        label = "Volume",
        units = listOf(
            UnitDefinition("Milliliter", "mL", { it / 1000.0 }, { it * 1000.0 }),
            UnitDefinition("Liter", "L", { it }, { it }),
            UnitDefinition("Cubic meter", "m³", { it * 1000.0 }, { it / 1000.0 }),
            UnitDefinition("Cubic foot", "ft³", { it * 28.316846592 }, { it / 28.316846592 }),
            UnitDefinition("Gallon", "gal", { it * 3.785411784 }, { it / 3.785411784 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.SPEED,
        label = "Speed",
        units = listOf(
            UnitDefinition("m/s", "m/s", { it }, { it }),
            UnitDefinition("km/h", "km/h", { it * 0.2777777777777778 }, { it / 0.2777777777777778 }),
            UnitDefinition("mph", "mph", { it * 0.44704 }, { it / 0.44704 }),
            UnitDefinition("knot", "kn", { it * 0.5144444444444445 }, { it / 0.5144444444444445 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.TIME,
        label = "Time",
        units = listOf(
            UnitDefinition("Millisecond", "ms", { it / 1000.0 }, { it * 1000.0 }),
            UnitDefinition("Second", "s", { it }, { it }),
            UnitDefinition("Minute", "min", { it * 60.0 }, { it / 60.0 }),
            UnitDefinition("Hour", "h", { it * 3600.0 }, { it / 3600.0 }),
            UnitDefinition("Day", "d", { it * 86400.0 }, { it / 86400.0 }),
            UnitDefinition("Week", "wk", { it * 604800.0 }, { it / 604800.0 }),
        ),
    ),
    UnitCategory(
        type = UnitCategoryType.DATA,
        label = "Data Storage",
        units = listOf(
            UnitDefinition("Byte", "B", { it }, { it }),
            UnitDefinition("KB", "KB", { it * 1000.0 }, { it / 1000.0 }),
            UnitDefinition("MB", "MB", { it * 1000.0.pow(2) }, { it / 1000.0.pow(2) }),
            UnitDefinition("GB", "GB", { it * 1000.0.pow(3) }, { it / 1000.0.pow(3) }),
            UnitDefinition("TB", "TB", { it * 1000.0.pow(4) }, { it / 1000.0.pow(4) }),
        ),
    ),
)

fun convertValue(value: Double, from: UnitDefinition, to: UnitDefinition): Double {
    val baseValue = from.toBase(value)
    return to.fromBase(baseValue)
}

fun formatConvertedValue(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    return when {
        value == 0.0 -> "0"
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> {
            val text = value.toString()
            if (text.contains("E") || text.contains("e")) {
                val formatted = String.format("%.10f", value)
                formatted.trimEnd('0').trimEnd('.')
            } else {
                text.trimEnd('0').trimEnd('.')
            }
        }
    }
}
