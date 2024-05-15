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
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.root14.detectionsdk.data.Events
import com.root14.detectionsdk.ml.Detect
import com.root14.detectionsdk.util.ColorUtils
import com.root14.detectionsdk.util.PermissionUtil
import com.root14.detectionsdk.view.DetectionSurface
import com.root14.detectionsdk.viewmodel.MainViewModel
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
    private var context: Context? = null,
    private var textureView: TextureView? = null,
    private var mediaButtons: Boolean? = false,
    private var detectionLabel: ImageView? = null,
    private var detectionSurface: DetectionSurface? = null,
) {
    private lateinit var labels: List<String>
    private val paint = Paint()
    private val colors = ColorUtils.getColors()
    private lateinit var bitmap: Bitmap
    private lateinit var cameraDevice: CameraDevice
    private var cameraManager: CameraManager
    private lateinit var handler: Handler
    private lateinit var model: Detect
    private lateinit var mediaRecorder: MediaRecorder
    private var viewModel: MainViewModel = DetectionSdk.viewModel

    //should be builder
    data class Builder(
        var context: Context? = null,
        var textureView: TextureView? = null,
        var mediaButtons: Boolean = false,
        var detectionLabel: ImageView? = null,
        var detectionSurface: DetectionSurface? = null
    ) {
        fun addContext(context: Context) = apply { this.context = context }
        fun addDetectionLabel(detectionLabel: ImageView) =
            apply { this.detectionLabel = detectionLabel }

        fun withTextureView(textureView: TextureView) = apply { this.textureView = textureView }
        fun withDetectionSurface(detectionSurface: DetectionSurface) =
            apply { this.detectionSurface = detectionSurface }

        fun enableMediaButtons(mediaButtons: Boolean) = apply { this.mediaButtons = mediaButtons }

        //eger detectionSurface ve textureview set edilmisse detectionSurface oncelik verilir
        fun build(): ObjectDetector {
            if (detectionSurface != null) {
                return ObjectDetector(
                    mediaButtons = mediaButtons, detectionSurface = detectionSurface
                )
            } else {
                if (context != null) {
                    if (textureView != null && detectionLabel != null) {
                        return ObjectDetector(
                            context!!, textureView!!, mediaButtons, detectionLabel
                        )
                    } else {
                        throw Exception("detection-sdk TextureView cannot be null!")
                    }
                } else {
                    throw Exception("detection-sdk Context cannot be null!")
                }
            }
        }
    }

    init {
        if (detectionSurface != null) {
            this.context = detectionSurface!!.context
            this.textureView = detectionSurface!!.textureView
            this.detectionLabel = detectionSurface!!.imageView
            if (mediaButtons!!) {
                enableMediaButtons()
            }
        }

        if (PermissionUtil.checkPermission(context!!)) {
            cameraManager = context!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            loadLabels()
            initModel()
            initHandlerThread()
        } else {
            throw Exception("detection-sdk Grant Camera permission first!")
        }
    }

    private fun loadLabels() {
        labels = FileUtil.loadLabels(context!!, "labelmap.txt")
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
        model = Detect.newInstance(context!!, getModelOptions())
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
                    paint.setColor(colors[index])
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

    private fun initMediaRecorder() {
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

    private fun calculatePreviewSize(): Size? {
        val characteristics = cameraManager.getCameraCharacteristics(cameraManager.cameraIdList[0])
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
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
    private fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera

                    val previewSize = calculatePreviewSize()
                    initMediaRecorder()

                    val surfaceTexture = textureView!!.surfaceTexture
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

    private fun pauseRecord() {
        try {
            mediaRecorder.pause()
            Toast.makeText(context, "recording paused", Toast.LENGTH_SHORT).show()
            viewModel.pushEvent(Events.PAUSE_RECORD)
        } catch (e: IllegalStateException) {
            Log.e("TAG", "IllegalStateException: ${e.message}")
        } catch (e: IOException) {
            Log.e("TAG", "IOException: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e("TAG", "RuntimeException: ${e.message}")
        }
    }

    private fun resumeRecord() {
        try {
            mediaRecorder.resume()
            Toast.makeText(context, "resuming to record", Toast.LENGTH_SHORT).show()
            viewModel.pushEvent(Events.RESUME_RECORD)
        } catch (e: IllegalStateException) {
            Log.e("TAG", "IllegalStateException: ${e.message}")
        } catch (e: IOException) {
            Log.e("TAG", "IOException: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e("TAG", "RuntimeException: ${e.message}")
        }
    }

    private fun stopRecord() {
        try {
            mediaRecorder.stop()
            mediaRecorder.reset()
            mediaRecorder.release()
            Toast.makeText(context, "record stopped", Toast.LENGTH_SHORT).show()
            viewModel.pushEvent(Events.STOP_RECORD)
            openCamera()
        } catch (e: IllegalStateException) {
            Log.e("TAG", "IllegalStateException: ${e.message}")
        } catch (e: IOException) {
            Log.e("TAG", "IOException: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e("TAG", "RuntimeException: ${e.message}")
        }
    }

    private fun startRecording() {
        try {
            mediaRecorder.start()
            Toast.makeText(context, "record started", Toast.LENGTH_SHORT).show()
            viewModel.pushEvent(Events.START_RECORD)
        } catch (e: IllegalStateException) {
            Log.e("TAG", "IllegalStateException while preparing MediaRecorder: ${e.message}")
        } catch (e: IOException) {
            Log.e("TAG", "IOException while preparing MediaRecorder: ${e.message}")
        } catch (e: RuntimeException) {
            Log.e("TAG", "RuntimeException while preparing MediaRecorder: ${e.message}")
        }
    }

    /**
     * media buttons can only usable on detectionSurface component
     */
    private fun enableMediaButtons() {
        if (detectionSurface != null) {
            detectionSurface!!.findViewById<Button>(R.id.btn_stop).setOnClickListener {
                stopRecord()
            }

            detectionSurface!!.findViewById<Button>(R.id.btn_start).setOnClickListener {
                startRecording()
            }

            detectionSurface!!.findViewById<Button>(R.id.btn_pause).setOnClickListener {
                pauseRecord()
            }

            detectionSurface!!.findViewById<Button>(R.id.btn_resume).setOnClickListener {
                resumeRecord()
            }
        }
    }

    fun bindToSurface() {
        textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
                bitmap = textureView!!.bitmap!!
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