
# ObjectDetection



[![](https://jitpack.io/v/root14/ObjectDetection.svg)](https://jitpack.io/#root14/ObjectDetection)



##### Android SDK object detection with Tensorflow Lite.




## Features



- Object detection with given Tensorflow Lite model

- Uses Koin for dependency injection

- Modular structure with builder pattern

- Notify user with events

- Video Capture, Object Detection and Camera Preview




It is aimed to provide creating a custom Android SDK/Framework for implementing a custom camera view with advanced functionalities.



The main goal of this SDK is to provide developers with a customizable camera view that handles camera permission management internally and supports two modes: object detection and video capture, with an option to combine both functionalities.








## Installation



Dont forget to ad Jitpack to root build.gradle

```kotlin

dependencyResolutionManagement {

repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

repositories {

mavenCentral()

maven { url 'https://jitpack.io' }
	}
}

```

Add to project build.gradle

```kotlin

dependencies {
implementation("org.tensorflow:tensorflow-lite-gpu:2.3.0")
implementation("org.tensorflow:tensorflow-lite-support:0.1.0")
implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")
implementation ("io.insert-koin:koin-android:3.5.6")
implementation ("com.github.root14:ObjectDetection:1.0")
}

```



## Usage



There are two types of usage styles
### With CustomView
You can use media buttons with customView.
```kotlin
val detectionSurface = findViewById<DetectionSurface>(R.id.detectionSurface)

DetectionSdk.init(this, object : DetectionSdkLogger {  
    override fun eventCallback(events: Events) {  
        if (events == Events.INIT_SUCCESS) {  
            val objectDetector =  
                ObjectDetector.Builder().withDetectionSurface(detectionSurface)  
                    .enableMediaButtons(true).build()  
  
            objectDetector.bindToSurface()  
        }   
    }  
})

```
##### XML file should look like that
```XML
<com.root14.detectionsdk.view.DetectionSurface  
  android:id="@+id/detectionSurface"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent" />
  ```

### With Views
```kotlin

val textureView = findViewById<TextureView>(R.id.textureView)
val detectionLabel = findViewById<ImageView>(R.id.detectionLabel)

DetectionSdk.init(this, object : DetectionSdkLogger {  
    override fun eventCallback(events: Events) {  
        if (events == Events.INIT_SUCCESS) {  
	        val objectDetector =
	            ObjectDetector.Builder().withTextureView(textureView).addDetectionLabel(detectionLabel)
	                .addContext(this).build()

	        objectDetector.bindToSurface() 
        }   
    }  
})
```
When an object is found, DetectionLabel draws a rectangle around it.
#### XML file should look like that

```XML

        <TextureView
            android:id="@+id/textureView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <ImageView
            android:id="@+id/detectionLabel"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="#000" />
    </androidx.constraintlayout.widget.ConstraintLayout>

```

#### If you want to handle events, basically

```kotlin
when (events) {  
    Events.START_RECORD -> Log.d("detection-sdk", "start  recording.")  
    Events.PAUSE_RECORD -> Log.d("detection-sdk", "pause  recording.")  
    Events.RESUME_RECORD -> Log.d("detection-sdk", "resume  recording.")  
    Events.STOP_RECORD -> Log.d("detection-sdk", "stop  recording.")  
    Events.INIT_SUCCESS -> Log.d("detection-sdk", "init successfully")  
    Events.INIT_FAIL -> Log.d("detection-sdk", "init  fail.")  
}
```

## Compatibility
**minSdk** version is  **30**.

## Known Issues
When video recording is finished, sometimes it may take time(may minutes) for the video to be displayed on the device.