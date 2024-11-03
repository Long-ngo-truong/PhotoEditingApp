package de.yanneckreiss.cameraxtutorial.ui.features.camera.photo_capture

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import de.yanneckreiss.cameraxtutorial.core.utils.rotateBitmap
import de.yanneckreiss.cameraxtutorial.ui.gallery_screen.GalleryScreen
import org.koin.androidx.compose.koinViewModel
import java.util.concurrent.Executor


@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel()
) {
    val cameraState: CameraState by viewModel.state.collectAsStateWithLifecycle()
    var isRecording by remember { mutableStateOf(false) }
    CameraContent(
        onPhotoCaptured = viewModel::storePhotoInGallery,
        lastCapturedPhoto = cameraState.capturedImage,
        isRecording = isRecording,
    )
}

private fun capturePhoto(context: Context, cameraController: LifecycleCameraController, onPhotoCaptured: (Bitmap) -> Unit) {
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    cameraController.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val correctedBitmap: Bitmap = image
                .toBitmap()
                .rotateBitmap(image.imageInfo.rotationDegrees)

            onPhotoCaptured(correctedBitmap)
            image.close()
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("CameraContent", "Error capturing image", exception)
        }
    })
}

@Composable
fun LastPhotoPreview(modifier: Modifier, lastCapturedPhoto: Bitmap) {
    val capturedPhoto: ImageBitmap = remember(lastCapturedPhoto.hashCode()) { lastCapturedPhoto.asImageBitmap() }
    Card(
        modifier = modifier
            .size(100.dp)
            .padding(16.dp)
            .clickable(onClick = {
            }),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Image(
            bitmap = capturedPhoto,
            contentDescription = "Last captured photo",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }

}

@Composable
fun onSelectPhoto() {
    GalleryScreen()
}

@Composable
private fun CameraContent(
    onPhotoCaptured: (Bitmap) -> Unit,
    lastCapturedPhoto: Bitmap? = null,
    isRecording: Boolean
) {

    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setBackgroundColor(Color.BLACK)
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_START
                }.also { previewView ->
                    previewView.controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also {
                    it.controller = cameraController
                }
            }
        )

        FloatingActionButton(
            onClick = { capturePhoto(context, cameraController, onPhotoCaptured) },
            containerColor = androidx.compose.ui.graphics.Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(100.dp)
                .padding(15.dp)
                .border(4.dp, androidx.compose.ui.graphics.Color.Gray, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Capture Photo",
                tint = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }

        FloatingActionButton(
            onClick = { recordingVideo(context, isRecording) },
            containerColor = if (isRecording) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(100.dp)
                .padding(15.dp)
                .border(4.dp, androidx.compose.ui.graphics.Color.Gray, CircleShape)
                .align(alignment = Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = if (isRecording) "Stop recording" else "Record video",
                tint = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }

        if (lastCapturedPhoto != null) {
            LastPhotoPreview(
                modifier = Modifier.align(alignment = BottomStart),
                lastCapturedPhoto = lastCapturedPhoto
            )
        }

        IconButton(
            onClick = { switchCamera(cameraController) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}




fun recordingVideo(context: Context, isRecording: Boolean) {
    if (isRecording) {
        //stopRecording()
    } else {
        //startRecording(context)
    }
    !isRecording
}


fun switchCamera(cameraController: LifecycleCameraController) {
    val currentSelector = cameraController.cameraSelector
    val newSelector = if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    try {
        cameraController.cameraSelector = newSelector
    } catch (e: Exception) {
        Log.e("CameraSwitch", "Failed to switch camera: ${e.message}")
    }
}

@Preview
@Composable
private fun Preview_CameraContent() {
    CameraContent(
        onPhotoCaptured = {},
        isRecording = false,
    )
}
