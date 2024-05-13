package com.root14.detectionsdk.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.root14.detectionsdk.R

class DetectionSurface(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.detection_surface, this)
    }

}