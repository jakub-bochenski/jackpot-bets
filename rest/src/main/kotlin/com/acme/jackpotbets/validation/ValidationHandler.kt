package com.acme.jackpotbets.validation

import com.acme.jackpotbets.exception.BadArgumentException
import com.fasterxml.jackson.core.type.TypeReference
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.RoutingContext
import jakarta.validation.Validator
import javax.inject.Inject

class ValidationHandler @Inject constructor(
    private val validator: Validator
) {
    fun initValidator(rc: RoutingContext) {
        rc.put(VALIDATOR_ATTRIBUTE, validator)
        rc.next()
    }
}

fun RoutingContext.validator(): Validator =
    get(VALIDATOR_ATTRIBUTE)
        ?: error("No validator found, did you forget to call ValidationHandler.initValidator()?")

inline fun <reified T> RoutingContext.bodyAsPojo(): T =
    DatabindCodec
        .mapper() // https://github.com/eclipse-vertx/vert.x/issues/5461
        .readValue(
            DatabindCodec.createParser(body().buffer()),
            object : TypeReference<T>() {}
        )
        .apply {
            validator()
                .validate(this)
                .takeIf { it.isNotEmpty() }
                ?.run { joinToString { "${it.propertyPath}: ${it.message}" } }
                ?.run { throw BadArgumentException(this) }
        }

private const val VALIDATOR_ATTRIBUTE = "jakarta.validation.Validator"
