package com.root14.detectionsdk

import androidx.lifecycle.ViewModel

class MainViewModel(private val name: String) : ViewModel() {
    fun sayHello(): String {
        return "hey douglas! from $name"
    }
}