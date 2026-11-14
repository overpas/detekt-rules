package by.overpas.detekt.architecture

import dev.detekt.api.Rule
import kotlin.properties.ReadOnlyProperty

internal fun decimalConfig(defaultValue: Double): ReadOnlyProperty<Rule, Double> =
    ReadOnlyProperty { rule, property ->
        when (val value = rule.config.valueOrNull<Any>(property.name)) {
            null -> defaultValue
            is Number -> value.toDouble()
            else -> checkNotNull(value.toString().toDoubleOrNull()) { "'${property.name}' must be a number." }
        }
    }
