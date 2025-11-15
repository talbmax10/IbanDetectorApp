package com.ibandetector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ibandetector.databinding.ActivityCameraScanBinding
import com.ibandetector.utils.IbanValidator
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var isProcessing = false
    private var lastDetectedIban: String? = null

    companion object {
        private const val TAG = "CameraScanActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupViews()
        checkPermissions()
    }

    private fun setupViews() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.useDetectedIbanButton.setOnClickListener {
            lastDetectedIban?.let { iban ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("IBAN", iban)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    private fun checkPermissions() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.camera_permission_required))
            .setMessage(getString(R.string.camera_permission_required))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                checkPermissions()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, IbanTextAnalyzer())
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                hideLoading()

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                showError()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private inner class IbanTextAnalyzer : ImageAnalysis.Analyzer {
        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            if (isProcessing) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                isProcessing = true
                showLoading()

                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        val ibans = IbanValidator.extractIbanFromText(text)

                        if (ibans.isNotEmpty()) {
                            val iban = ibans.first()
                            val result = IbanValidator.validateIban(iban, this@CameraScanActivity)
                            
                            if (result.isValid) {
                                runOnUiThread {
                                    showDetectedIban(iban)
                                }
                            } else {
                                runOnUiThread {
                                    updateDetectionStatus(getString(R.string.camera_instruction))
                                    hideLoading()
                                }
                            }
                        } else {
                            runOnUiThread {
                                updateDetectionStatus(getString(R.string.camera_instruction))
                                hideLoading()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Text recognition failed", e)
                        runOnUiThread {
                            hideLoading()
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                        isProcessing = false
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private fun showDetectedIban(iban: String) {
        lastDetectedIban = iban
        val formattedIban = IbanValidator.formatIban(iban)
        
        binding.detectedIbanText.text = formattedIban
        binding.detectedIbanCard.visibility = View.VISIBLE
        binding.detectionStatus.text = getString(R.string.iban_detected)
        
        // إيقاف الكاميرا مؤقتاً
        imageAnalyzer?.clearAnalyzer()
        
        hideLoading()
    }

    private fun updateDetectionStatus(status: String) {
        binding.detectionStatus.text = status
        binding.detectedIbanCard.visibility = View.GONE
    }

    private fun showLoading() {
        binding.loadingIndicator.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingIndicator.visibility = View.GONE
    }

    private fun showError() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.error_scanning))
            .setMessage(getString(R.string.camera_not_available))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
