package com.acme.jackpotbets.validation

import dagger.Module
import dagger.Provides
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import javax.inject.Singleton

@Module
object ValidationModule {

    @Provides
    fun validator(factory: ValidatorFactory): Validator = factory.validator

    @Provides
    @Singleton
    fun validatorFactory(): ValidatorFactory = Validation
        .byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()
}
