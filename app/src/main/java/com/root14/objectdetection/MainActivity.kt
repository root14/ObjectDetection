package com.root14.objectdetection

import android.os.Bundle
import android.view.TextureView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.root14.detectionsdk.DetectionSdk
import com.root14.detectionsdk.ObjectDetector
import com.root14.detectionsdk.util.PermissionUtil
import com.root14.detectionsdk.data.DetectionSdkLogger
import com.root14.detectionsdk.data.Events
import com.root14.detectionsdk.view.DetectionSurface

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        /*val detectionSurface = findViewById<DetectionSurface>(R.id.detection_surface)

        val objectDetector =
            ObjectDetector.Builder().withDetectionSurface(detectionSurface).enableMediaButtons(true)
                .build()

        objectDetector.bindToSurface()*/

        val detectionSurface = findViewById<DetectionSurface>(R.id.detectionSurface)

        DetectionSdk.init(this, object : DetectionSdkLogger {
            override fun eventCallback(events: Events) {
                println("gelen vaar ${events}")
                if (events == Events.INIT_SUCCESS) {
                    val objectDetector =
                        ObjectDetector.Builder().withDetectionSurface(detectionSurface)
                            .enableMediaButtons(true).build()

                    objectDetector.bindToSurface()
                }
            }
        })

        /*
        val textureView = findViewById<TextureView>(R.id.textureView)
        val detectionLabel = findViewById<ImageView>(R.id.detectionLabel)

        DetectionSdk.init(this, object : DetectionSdkLogger {
        override fun eventCallback(events: Events) {
            if (events == Events.INIT_SUCCESS) {
                val objectDetector =
                    ObjectDetector.Builder().withTextureView(textureView)
                        .addDetectionLabel(detectionLabel)
                        .addContext(this@MainActivity).build()

                objectDetector.bindToSurface()
            }
        }
    })*/
    }
}