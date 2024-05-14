package com.root14.detectionsdk.di

import org.koin.dsl.koinApplication

object IsolatedKoinContext {
    val koinApp = koinApplication {
        modules(baseModule)
    }

    val koin = koinApp.koin
}