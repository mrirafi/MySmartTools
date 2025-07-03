package com.meghpy.mysmarttools;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageDecoder;
import android.media.AudioManager;
import android.media.Image;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 200;

    private PreviewView previewView;
    private TextView textResult;
    private MaterialButton btnFlash,btnPickImage;
    private Button btnScanAgain ;
    private RadioGroup scanTypeGroup;
    private RadioButton radioQR, radioBarcode;
    private ImageView btnCopy;

    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean beepPlayed = false;
    private boolean isFlashOn = false;
    private boolean allowScan = true;
    private Camera camera;
    private String lastScannedText = "";

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) scanFromGalleryImage(uri);
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        previewView = findViewById(R.id.previewView);
        textResult = findViewById(R.id.textResult);
        btnFlash = findViewById(R.id.btnFlash);
        btnScanAgain = findViewById(R.id.btnScanAgain);
        btnPickImage = findViewById(R.id.btnPickImage);
        scanTypeGroup = findViewById(R.id.scanTypeGroup);
        radioQR = findViewById(R.id.radioQR);
        radioBarcode = findViewById(R.id.radioBarcode);
        btnCopy = findViewById(R.id.btnCopy);

        textResult.setOnClickListener(v -> copyToClipboard());
        btnPickImage.setOnClickListener(v -> pickImage());
        btnFlash.setOnClickListener(v -> toggleFlash());
        btnScanAgain.setOnClickListener(v -> resetScanner());

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyToClipboard();
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
        setupScanner();
        requestCameraPermission();
    }

    private void setupScanner() {
        BarcodeScannerOptions options;

        if (radioQR.isChecked()) {
            options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();
        } else {
            options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                    .build();
        }

        scanner = BarcodeScanning.getClient(options);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startCamera();
        }

        scanTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            setupScanner();   // Reconfigure scanner with new type
            startCamera();    // Restart the camera with new scanner
        });

    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (allowScan) processImageProxy(imageProxy);
                    else imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        @androidx.camera.core.ExperimentalGetImage
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            handleBarcodes(barcodes);
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        String scannedText = barcodes.get(0).getRawValue();
        if (scannedText == null || scannedText.equals(lastScannedText)) return;

        lastScannedText = scannedText;
        allowScan = false;

        runOnUiThread(() -> {
            textResult.setText(scannedText);
            playBeep();

            // Check if scannedText is a URL and make TextView clickable to open link
            if (scannedText.startsWith("http://") || scannedText.startsWith("https://")) {
                textResult.setOnClickListener(v -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(scannedText));
                    startActivity(browserIntent);
                });
            } else {
                // If not a URL, set default click listener to copy text
                textResult.setOnClickListener(v -> copyToClipboard());
            }
        });

    }

    private void playBeep() {
        if (beepPlayed) return;
        beepPlayed = true;

        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200);

        previewView.postDelayed(() -> beepPlayed = false, 1500);
    }

    private void resetScanner() {
        allowScan = true;
        textResult.setText("Scan result will appear here");
        lastScannedText = "";
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            btnFlash.setIconResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void scanFromGalleryImage(Uri uri) {
        try {
            InputImage image;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                image = InputImage.fromBitmap(ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(getContentResolver(), uri)), 0);
            } else {
                image = InputImage.fromFilePath(this, uri);
            }

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            handleBarcodes(barcodes);
                        } else {
                            Toast.makeText(this, "No barcode found in image.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Error scanning image", Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyToClipboard() {
        if (lastScannedText.isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Scan Result", lastScannedText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}