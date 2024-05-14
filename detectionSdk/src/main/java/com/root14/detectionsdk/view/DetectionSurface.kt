package com.root14.detectionsdk.view

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.root14.detectionsdk.R


class DetectionSurface(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    var textureView: TextureView
    var imageView: ImageView


    init {
        inflate(context, R.layout.detection_surface, this)
        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)
    }
}