package com.root14.detectionsdk.di

import com.root14.detectionsdk.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val baseModule = module {
    viewModel { MainViewModel() }
}