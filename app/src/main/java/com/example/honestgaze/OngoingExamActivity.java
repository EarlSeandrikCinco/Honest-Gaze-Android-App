package com.example.honestgaze;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OngoingExamActivity extends AppCompatActivity {

    private PreviewView cameraPreview;
    private TextView warningText;

    private FaceDetector detector;
    private ExecutorService cameraExecutor;

    private long lastWarningTime = 0;
    private final long WARNING_COOLDOWN_MS = 2500; // avoid spam warnings

    private static final int PERMISSION_CODE = 100;
    private static final String TAG = "HonestGazeExam";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_exam);

        cameraPreview = findViewById(R.id.cameraPreview);
        warningText = findViewById(R.id.warningText);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (!permissionsGranted()) {
            requestPermissions();
        } else {
            startCamera();
        }

        setupMLKit();
    }

    private boolean permissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (permissionsGranted()) {
                startCamera();
            } else {
                finish(); // Cannot run without camera permissions
            }
        }
    }

    private void setupMLKit() {

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .build();

        detector = FaceDetection.getClient(options);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        analysis
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        detector.process(image)
                .addOnSuccessListener(faces -> handleDetections(faces))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleDetections(List<Face> faces) {

        if (faces.size() == 0) {
            showWarning("No face detected");
            return;
        }

        if (faces.size() > 1) {
            showWarning("Multiple faces detected");
            return;
        }

        Face face = faces.get(0);

        float headEulerY = face.getHeadEulerAngleY(); // left/right (negative = right, positive = left)
        float headEulerX = face.getHeadEulerAngleX(); // up/down

        boolean lookingAway = (Math.abs(headEulerY) > 20) || (Math.abs(headEulerX) > 20);

        if (lookingAway) {
            showWarning("Looking away from the screen");
        }
    }

    private void showWarning(String msg) {

        long now = System.currentTimeMillis();
        if (now - lastWarningTime < WARNING_COOLDOWN_MS) return;

        lastWarningTime = now;

        runOnUiThread(() -> {
            warningText.setText("Warning: " + msg);
            warningText.setVisibility(View.VISIBLE);
            warningText.postDelayed(() -> warningText.setVisibility(View.GONE), 2000);
        });

        Log.w(TAG, "Suspicious Behavior: " + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (detector != null) detector.close();
    }
}