package com.root14.objectdetection

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.root14.detectionsdk.DetectionSdk
import com.root14.detectionsdk.PermissionUtil
import com.root14.detectionsdk.data.DetectionSdkLogger
import com.root14.detectionsdk.view.DetectionSurface

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        PermissionUtil.grantPermission(this)

        DetectionSdk.init(object :DetectionSdkLogger{
            override fun log(message: String) {
                TODO("Not yet implemented")
            }

        })

        val a0 = DetectionSdk.getObjectDetector()
    }
}