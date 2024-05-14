package com.root14.detectionsdk.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer


object BitmapUtils {

     fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 300 * 300 * 3)
        byteBuffer.rewind()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        resizedBitmap.getPixels(
            IntArray(300 * 300),
            0,
            300,
            0,
            0,
            300,
            300
        )
        for (y in 0 until 300) {
            for (x in 0 until 300) {
                val px = resizedBitmap.getPixel(x, y)
                byteBuffer.put((px shr 16 and 0xFF).toByte())
                byteBuffer.put((px shr 8 and 0xFF).toByte())
                byteBuffer.put((px and 0xFF).toByte())
            }
        }
        return byteBuffer
    }


    /**
     * Converts bitmap to byte array in PNG format
     * @param bitmap source bitmap
     * @return result byte array
     */
    fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        var baos: ByteArrayOutputStream? = null
        try {
            baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            return baos.toByteArray()
        } finally {
            if (baos != null) {
                try {
                    baos.close()
                } catch (e: IOException) {
                    Log.e(
                        BitmapUtils::class.java.simpleName, "ByteArrayOutputStream was not closed"
                    )
                }
            }
        }
    }

    /**
     * Converts bitmap to the byte array without compression
     * @param bitmap source bitmap
     * @return result byte array
     */
    fun convertBitmapToByteArrayUncompressed(bitmap: Bitmap): ByteArray {
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()
        return byteBuffer.array()
    }

    /**
     * Converts compressed byte array to bitmap
     * @param src source array
     * @return result bitmap
     */
    fun convertCompressedByteArrayToBitmap(src: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(src, 0, src.size)
    }
}