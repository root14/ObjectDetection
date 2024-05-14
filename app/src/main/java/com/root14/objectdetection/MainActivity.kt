package com.root14.objectdetection

import android.os.Bundle
import android.view.TextureView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.root14.detectionsdk.DetectionSdk
import com.root14.detectionsdk.ObjectDetector
import com.root14.detectionsdk.util.PermissionUtil
import com.root14.detectionsdk.data.DetectionSdkLogger
import com.root14.detectionsdk.view.DetectionSurface

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        PermissionUtil.grantPermission(this)

        DetectionSdk.init(object : DetectionSdkLogger {
            override fun log(message: String) {
                TODO("Not yet implemented")
            }
        })

        /*val detectionSurface = findViewById<DetectionSurface>(R.id.detection_surface)

        val objectDetector =
            ObjectDetector.Builder().withDetectionSurface(detectionSurface).enableMediaButtons(true)
                .build()

        objectDetector.bindToSurface()*/

        val textureView = findViewById<TextureView>(R.id.textureView)
        val detectionLabel = findViewById<ImageView>(R.id.detectionLabel)

        val objectDetector =
            ObjectDetector.Builder().withTextureView(textureView).addDetectionLabel(detectionLabel)
                .addContext(this).build()

        objectDetector.bindToSurface()
    }
}