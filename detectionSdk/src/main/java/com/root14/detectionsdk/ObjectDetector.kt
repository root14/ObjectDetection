package com.root14.detectionsdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import com.root14.detectionsdk.ml.Detect
import com.root14.detectionsdk.util.ColorUtils
import com.root14.detectionsdk.util.PermissionUtil
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.IOException
import java.util.UUID

class ObjectDetector internal constructor(
    private var context: Context,
    private var textureView: TextureView,
    private var customView: Boolean = false,
    private var detectionLabel: ImageView?
) {
    //set true after initialized
    private val initialized = false
    private lateinit var labels: List<String>
    private val paint = Paint()
    private val colors = ColorUtils.getColors()
    private lateinit var bitmap: Bitmap
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraManager: CameraManager
    private lateinit var handler: Handler
    private lateinit var model: Detect
    private lateinit var mediaRecorder: MediaRecorder


    //should be builder
    data class Builder(
        var context: Context? = null,
        var textureView: TextureView? = null,
        var customView: Boolean = false,
        var detectionLabel: ImageView? = null,
    ) {
        fun addContext(context: Context) = apply { this.context = context }
        fun addDetectionLabel(detectionLabel: ImageView) =
            apply { this.detectionLabel = detectionLabel }

        fun withTextureView(textureView: TextureView) = apply { this.textureView = textureView }
        fun whitCustomView(customView: Boolean) = apply { this.customView = customView }
        fun build(): ObjectDetector {
            return if (context != null) {
                if (!customView) {
                    if (textureView != null) {
                        ObjectDetector(context!!, textureView!!, false, detectionLabel)
                    } else {
                        throw Exception("detectionsdk TextureView cannot be null!")
                    }

                } else {
                    ObjectDetector(context!!, textureView!!, true, detectionLabel)
                }

            } else {
                throw Exception("detectionsdk Context cannot be null!")
            }
        }
    }

    init {
        if (PermissionUtil.checkPermission(context)) {
            loadLabels()
            initModel()
            initHandlerThread()
            initMediaRecorder()
        } else {
            throw Exception("detection-sdk Grant Camera permission first!")
        }
    }

    private fun loadLabels() {
        labels = FileUtil.loadLabels(context, "labelmap.txt")
    }

    private fun getModelOptions(): Model.Options {
        val compatList = CompatibilityList()
        return if (compatList.isDelegateSupportedOnThisDevice) {
            // if the device has a supported GPU, add the GPU delegate
            Model.Options.Builder().setDevice(Model.Device.GPU).build()
        } else {
            // if the GPU is not supported, run on 4 threads
            Model.Options.Builder().setNumThreads(4).build()
        }
    }

    private fun initModel() {
        model = Detect.newInstance(context, getModelOptions())
    }

    private fun initHandlerThread() {
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    private fun drawCanvas(scores: FloatArray, locations: FloatArray, classes: FloatArray) {
        if (detectionLabel != null) {
            val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutable)

            val h = mutable.height
            val w = mutable.width
            paint.textSize = h / 30f
            paint.strokeWidth = h / 170f
            var x = 0
            scores.forEachIndexed { index, fl ->
                x = index
                x *= 4
                if (fl > 0.5) {
                    paint.setColor(colors.get(index))
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(
                        RectF(
                            locations[x + 1] * w,
                            locations[x] * h,
                            locations[x + 3] * w,
                            locations[x + 2] * h
                        ), paint
                    )
                    paint.style = Paint.Style.FILL
                    canvas.drawText(
                        labels[classes[index].toInt()] + " " + fl.toString(),
                        locations[x + 1] * w,
                        locations[x] * h,
                        paint
                    )
                }
            }

            detectionLabel!!.setImageBitmap(mutable)
        } else {
            throw Exception("detection-sdk DetectionLabel cannot be null!")
        }
    }

    fun initMediaRecorder() {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "${UUID.randomUUID()}.mp4"
        )
        mediaRecorder = MediaRecorder()
        val previewSize = calculatePreviewSize()

        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(previewSize!!.width, previewSize.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            prepare()
        }
    }

    fun calculatePreviewSize(): Size? {
        val characteristics =
            cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[0])
        val map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = map?.getOutputSizes(SurfaceTexture::class.java)

        val targetWidth = 1920
        val targetHeight = 1080

        var chosenSize: Size? = null
        sizes?.forEach { size ->
            if (size.width <= targetWidth && size.height <= targetHeight) {
                if (chosenSize == null || size.width > chosenSize!!.width) {
                    chosenSize = size
                }
            }
        }

        val previewSize = chosenSize ?: sizes?.get(0)
        return previewSize
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera

                    val previewSize = calculatePreviewSize()

                    val surfaceTexture = textureView.surfaceTexture
                    surfaceTexture?.setDefaultBufferSize(previewSize!!.width, previewSize.height)
                    val surface = Surface(surfaceTexture)

                    val captureRequestBuilder =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                    captureRequestBuilder.addTarget(surface)
                    captureRequestBuilder.addTarget(mediaRecorder.surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface, mediaRecorder.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(
                                    captureRequestBuilder.build(), null, handler
                                )
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                // Configuration failed
                            }
                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {
                    // Camera disconnected
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    // Camera error
                }
            }, handler
        )
    }


    fun pauseRecord() {
        mediaRecorder.pause()
    }

    fun resumeRecord() {
        mediaRecorder.resume()
    }

    fun stopRecord() {
        mediaRecorder.stop()
        mediaRecorder.reset()
        mediaRecorder.release()
    }

    fun startRecording() {
        try {
            //mediaRecorder.prepare()
            mediaRecorder.start()
        } catch (e: IllegalStateException) {
            Log.e("TAG", "IllegalStateException while preparing MediaRecorder: ${e.message}")
        } catch (e: IOException) {
            Log.e("TAG", "IOException while preparing MediaRecorder: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e("TAG", "RuntimeException while preparing MediaRecorder: ${e.message}")
        }
    }


    fun bindToSurface() {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
                Log.d("TAG", "Surface texture available")
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                // Releases model resources if no longer used.
                model.close()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                //process image
                val imageResult = TensorImage.fromBitmap(bitmap)

                val imageProcessor =
                    ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
                        .build()

                val result = imageProcessor.process(imageResult)

                // Creates inputs for reference.
                val inputFeature0 =
                    TensorBuffer.createFixedSize(intArrayOf(1, 300, 300, 3), DataType.UINT8)
                inputFeature0.loadBuffer(result.buffer)

                // Runs model inference and gets result.
                val outputs = model.process(inputFeature0)

                val locations = outputs.outputFeature0AsTensorBuffer.floatArray
                val classes = outputs.outputFeature1AsTensorBuffer.floatArray
                val scores = outputs.outputFeature2AsTensorBuffer.floatArray
                val numberOfDetection = outputs.outputFeature3AsTensorBuffer.floatArray


                drawCanvas(scores, locations, classes)
            }
        }
    }

}