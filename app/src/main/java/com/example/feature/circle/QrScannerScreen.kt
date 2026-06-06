package com.example.feature.circle

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.i18n.t
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
  onResult: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
  var scanned by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (!cameraPermission.status.isGranted) {
      cameraPermission.launchPermissionRequest()
    }
  }

  Scaffold(
    topBar = {
      TextButton(onClick = onDismiss, modifier = Modifier.padding(8.dp)) {
        Text(t("Close", "Đóng"))
      }
    },
  ) { padding ->
    if (!cameraPermission.status.isGranted) {
      Text(
        t("Camera permission is required to scan QR codes.", "Cần quyền camera để quét mã QR."),
        modifier = Modifier.padding(padding).padding(24.dp),
      )
      return@Scaffold
    }

    AndroidView(
      modifier = Modifier.fillMaxSize().padding(padding),
      factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
          val cameraProvider = cameraProviderFuture.get()
          val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
          }
          val analyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
          val scanner = BarcodeScanning.getClient()
          analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            if (scanned) {
              imageProxy.close()
              return@setAnalyzer
            }
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
              val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
              scanner.process(image)
                .addOnSuccessListener { barcodes ->
                  val value = barcodes.firstOrNull()?.rawValue
                  if (!value.isNullOrBlank()) {
                    scanned = true
                    onResult(value)
                  }
                }
                .addOnCompleteListener { imageProxy.close() }
            } else {
              imageProxy.close()
            }
          }
          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analyzer,
          )
        }, ContextCompat.getMainExecutor(ctx))
        previewView
      },
    )
  }
}
