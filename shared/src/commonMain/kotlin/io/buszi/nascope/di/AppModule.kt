package io.buszi.nascope.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

/**
 * Root Koin dependency injection module scanning for annotated definitions across the shared module.
 */
@Module
@Configuration
@ComponentScan("io.buszi.nascope")
class AppModule

/**
 * Root Koin application descriptor for compile-time dependency graph resolution.
 */
@KoinApplication
object NascopeApp
