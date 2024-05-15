package com.root14.detectionsdk

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.root14.detectionsdk.data.DetectionSdkLogger
import com.root14.detectionsdk.data.Events
import com.root14.detectionsdk.di.baseModule
import com.root14.detectionsdk.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

object DetectionSdk {
    private val koinApplication: KoinApplication by lazy {
        startKoin {
            modules(baseModule)
        }
    }
    lateinit var viewModel: MainViewModel

    fun init(context: Context, detectionSdkLogger: DetectionSdkLogger) {
        val dynamicModule = module {
            single {
                detectionSdkLogger
            }
        }
        koinApplication.koin.loadModules(listOf(dynamicModule))

        viewModel = provideMainViewModel()

        viewModel.grantPermission(context)
        viewModel.viewModelScope.launch {
            viewModel.permissionGranted.collect {
                detectionSdkLogger.eventCallback(Events.INIT_SUCCESS)
            }
        }

        viewModel.viewModelScope.launch {
            //Notify the user
            viewModel.pushEventFlow.collect { events ->
                if (events != null) {
                    detectionSdkLogger.eventCallback(events)
                }
            }
        }


    }

    private fun provideMainViewModel(): MainViewModel {
        return koinApplication.koin.get()
    }

}