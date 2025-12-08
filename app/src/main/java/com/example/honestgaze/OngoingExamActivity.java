package com.example.honestgaze;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Size;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceDetection;

import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.concurrent.ExecutionException;

public class OngoingExamActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;

    // UI
    private PreviewView previewView;
    private TextView txtDebug, warningCounterText;
    private LinearLayout calibrationOverlay;
    private Button btnStartCalibration, btnCancelCalibration;
    private TextView calibrationMessage;

    // ML Kit
    private FaceDetector faceDetector;

    // Calibration
    private boolean isCalibrating = false;
    private int calibrationSamples = 0;
    private float calibratedCenterX = 0;
    private float calibratedCenterY = 0;

    // Exam warnings
    private int remainingWarnings = 3;
    private boolean isLookingAway = false;

    private long gazeAwayStartTime = 0;
    private static final long WARNING_DELAY_MS = 2500; // 2.5 seconds grace period

    // SoundPool for warning sound
    private SoundPool soundPool;
    private int calibrationDoneSoundId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_exam);

        bindViews();
        setupFaceDetector();
        setupSoundPool();

        btnStartCalibration.setOnClickListener(v -> startCalibration());
        btnCancelCalibration.setOnClickListener(v -> {
            calibrationOverlay.setVisibility(LinearLayout.GONE);
            Toast.makeText(this, "Calibration cancelled", Toast.LENGTH_SHORT).show();
        });

        calibrationOverlay.setVisibility(LinearLayout.VISIBLE);

        if (hasCameraPermission()) startCamera();
        else requestCameraPermission();
    }

    private void setupSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build();

        calibrationDoneSoundId = soundPool.load(this, R.raw.calibrationdone, 1);
    }

    private void playCalibrationDoneSound() {
        soundPool.play(calibrationDoneSoundId, 1f, 1f, 1, 0, 1f);
    }


    @Override
    protected void onDestroy() {
        if (soundPool != null) {
            soundPool.release();
        }
        super.onDestroy();
    }

    // ---------------------------------------------------
    // Bind Views
    // ---------------------------------------------------
    private void bindViews() {
        previewView = findViewById(R.id.previewView);
        txtDebug = findViewById(R.id.txtDebug);
        warningCounterText = findViewById(R.id.warningCounterText);

        calibrationOverlay = findViewById(R.id.calibrationOverlay);
        calibrationMessage = findViewById(R.id.calibrationMessage);
        btnStartCalibration = findViewById(R.id.btnStartCalibration);
        btnCancelCalibration = findViewById(R.id.btnCancelCalibration);
    }

    // ---------------------------------------------------
    // ML Kit Face Detector
    // ---------------------------------------------------
    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    // ---------------------------------------------------
    // Permissions
    // ---------------------------------------------------
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------
    // CameraX Setup
    // ---------------------------------------------------
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);

        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = providerFuture.get();
                bindCamera(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCamera(ProcessCameraProvider provider) {
        provider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                this::analyzeFrame
        );

        provider.bindToLifecycle(this, selector, preview, analysis);
    }

    // ---------------------------------------------------
    // Frame Analysis
    // ---------------------------------------------------
    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeFrame(ImageProxy proxy) {
        if (proxy.getImage() == null) {
            proxy.close();
            return;
        }

        InputImage img = InputImage.fromMediaImage(
                proxy.getImage(),
                proxy.getImageInfo().getRotationDegrees()
        );

        faceDetector.process(img)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) processFace(faces.get(0));
                    else txtDebug.setText("No face detected");
                })
                .addOnCompleteListener(t -> proxy.close());
    }

    // ---------------------------------------------------
    // Eye Center Helper
    // ---------------------------------------------------
    private PointF getEyeCenter(Face face, int contourType) {
        FaceContour contour = face.getContour(contourType);
        if (contour == null || contour.getPoints().isEmpty()) return null;

        float sx = 0, sy = 0;
        for (PointF p : contour.getPoints()) {
            sx += p.x;
            sy += p.y;
        }
        int n = contour.getPoints().size();
        return new PointF(sx / n, sy / n);
    }

    // ---------------------------------------------------
    // Processing Face + Gaze
    // ---------------------------------------------------
    private void processFace(Face face) {
        PointF left = getEyeCenter(face, FaceContour.LEFT_EYE);
        PointF right = getEyeCenter(face, FaceContour.RIGHT_EYE);

        if (left == null || right == null) {
            txtDebug.setText("Eye contours missing");
            return;
        }

        float cx = (left.x + right.x) / 2f;
        float cy = (left.y + right.y) / 2f;

        txtDebug.setText("CX: " + cx + "\nCY: " + cy);

        if (isCalibrating) {
            calibratedCenterX += cx;
            calibratedCenterY += cy;
            calibrationSamples++;

            calibrationMessage.setText("Calibrating... (" + calibrationSamples + "/30)");

            if (calibrationSamples >= 30) finishCalibration();
            return;
        }

        detectGaze(cx, cy);
    }

    // ---------------------------------------------------
    // Calibration
    // ---------------------------------------------------
    private void startCalibration() {
        calibrationSamples = 0;
        calibratedCenterX = 0;
        calibratedCenterY = 0;
        isCalibrating = true;

        calibrationMessage.setText("Please keep looking directly at your camera…");
    }

    private void finishCalibration() {
        isCalibrating = false;

        calibratedCenterX /= calibrationSamples;
        calibratedCenterY /= calibrationSamples;

        calibrationOverlay.setVisibility(LinearLayout.GONE);
        Toast.makeText(this, "Calibration complete!", Toast.LENGTH_SHORT).show();
        playCalibrationDoneSound(); // play calibration sound
    }


    // ---------------------------------------------------
    // Gaze Detection + Warning System
    // ---------------------------------------------------
    private void detectGaze(float cx, float cy) {
        float dx = Math.abs(cx - calibratedCenterX);
        float threshold = 25f; // adjust sensitivity

        if (dx > threshold) {
            if (gazeAwayStartTime == 0) {
                gazeAwayStartTime = System.currentTimeMillis();
            } else {
                long elapsed = System.currentTimeMillis() - gazeAwayStartTime;
                if (elapsed >= WARNING_DELAY_MS && !isLookingAway) {
                    issueWarning();
                    isLookingAway = true;
                }
            }
        } else {
            gazeAwayStartTime = 0;
            isLookingAway = false;
        }
    }

    private void issueWarning() {
        remainingWarnings--;
        warningCounterText.setText("Remaining warnings: " + remainingWarnings);

        if (remainingWarnings <= 0) {
            Toast.makeText(this, "Exam ended (too many warnings).", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
