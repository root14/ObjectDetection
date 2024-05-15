package com.root14.detectionsdk.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {
    /**
     * @param context for getting permission from user
     * granting camera permission for sdk functionalities
     */
    fun requestPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(android.Manifest.permission.CAMERA), 101
            )
        }
    }

    /**
     * @param context for checking if permission
     * @return true if camera permission is granted, false otherwise
     */
    fun checkPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

}