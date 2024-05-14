package com.root14.detectionsdk

import com.root14.detectionsdk.data.DetectionSdkLogger
import com.root14.detectionsdk.di.IsolatedKoinContext
import com.root14.detectionsdk.di.baseModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

object DetectionSdk {

    private var initialized = false

    private val koinApplication: KoinApplication by lazy {
        startKoin {
            modules(baseModule)
        }
    }
    //TODO check permissions

    //TODO add state logs
    fun init(detectionSdkLogger: DetectionSdkLogger) {
        val dynamicModule = module {
            single {
                detectionSdkLogger
            }
        }
        IsolatedKoinContext.koinApp.koin.loadModules(listOf(dynamicModule))
        initialized = true
    }

    //builder object can be
    fun getObjectDetector(): ObjectDetector {
        return koinApplication.koin.get()
    }

}