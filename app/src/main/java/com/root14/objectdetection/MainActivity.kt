package com.root14.objectdetection

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.root14.detectionsdk.PermissionUtil
import com.root14.detectionsdk.view.DetectionSurface

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        PermissionUtil.grantPermission(this)
    }
}