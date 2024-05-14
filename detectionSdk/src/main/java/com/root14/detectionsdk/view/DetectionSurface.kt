package com.root14.detectionsdk.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.root14.detectionsdk.R
import com.root14.detectionsdk.ml.Detect
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class DetectionSurface(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    lateinit var labels: List<String>
    var colors = listOf<Int>(
        Color.BLUE,
        Color.GREEN,
        Color.RED,
        Color.CYAN,
        Color.GRAY,
        Color.BLACK,
        Color.DKGRAY,
        Color.MAGENTA,
        Color.YELLOW,
        Color.RED
    )
    private val paint = Paint()
    private lateinit var bitmap: Bitmap
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var handler: Handler
    private lateinit var model: Detect

    init {
        inflate(context, R.layout.detection_surface, this)

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)

        labels = FileUtil.loadLabels(context, "labelmap.txt")


        val compatList = CompatibilityList()
        //gpu acceleration
        val options = if (compatList.isDelegateSupportedOnThisDevice) {
            // if the device has a supported GPU, add the GPU delegate
            Model.Options.Builder().setDevice(Model.Device.GPU).build()
        } else {
            // if the GPU is not supported, run on 4 threads
            Model.Options.Builder().setNumThreads(4).build()
        }

        model = Detect.newInstance(context, options)

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        //todo asagisi objeye gidecek generic
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

                //locations
                val locations = outputs.outputFeature0AsTensorBuffer.floatArray
                Log.d("TAG", "outputFeature0: $locations")

                //classes
                val classes = outputs.outputFeature1AsTensorBuffer.floatArray
                Log.d("TAG", "outputFeature1: $classes")

                //scores
                val scores = outputs.outputFeature2AsTensorBuffer.floatArray
                Log.d("TAG", "outputFeature2: $scores")

                //number of detections
                val numberOfDetection = outputs.outputFeature3AsTensorBuffer.floatArray
                Log.d("TAG", "outputFeature3: $numberOfDetection")


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

                imageView.setImageBitmap(mutable)

            }

        }

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.detection_surface, this)
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    cameraDevice = p0

                    var surfaceTexture = textureView.surfaceTexture
                    var surface = Surface(surfaceTexture)

                    var captureRequest =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                p0.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {
                            }
                        }, handler
                    )
                }

                override fun onDisconnected(p0: CameraDevice) {

                }

                override fun onError(p0: CameraDevice, p1: Int) {

                }
            }, handler
        )
    }


}