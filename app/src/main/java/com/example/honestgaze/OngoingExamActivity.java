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

    private boolean isCalibrated = false;

    private int calibrationSamples = 0;
    private float calibratedCenterX = 0;
    private float calibratedCenterY = 0;

    // Exam warnings
    private int remainingWarnings = 3;
    private boolean isLookingAway = false;

    private long gazeAwayStartTime = 0;
    private static final long WARNING_DELAY_MS = 2500; // 2.5 seconds grace period

    private boolean hadFaceLastFrame = false;

    private TextView popupWarning;


    // SoundPool for warning sound
    private SoundPool soundPool;
    private int calibrationDoneSoundId;
    private int calibrationStartSoundId;
    private int warningBeepSoundId;





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
                .setMaxStreams(2)
                .setAudioAttributes(attributes)
                .build();

        calibrationDoneSoundId = soundPool.load(this, R.raw.calibrationdone, 1);
        warningBeepSoundId = soundPool.load(this, R.raw.warningbeep, 1);
        calibrationStartSoundId = soundPool.load(this, R.raw.calibrationstart, 1);

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status != 0) {
                Toast.makeText(this, "Failed to load sound: " + sampleId, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void playWarningBeep() {
        soundPool.play(warningBeepSoundId, 1f, 1f, 1, 0, 1f);
    }

    private void playCalibrationStartSound() {
        soundPool.play(calibrationStartSoundId, 1f, 1f, 1, 0, 1f);
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

        popupWarning = findViewById(R.id.popupWarning);
    }

    private void showPopupWarning(String message) {
        popupWarning.setText(message);

        popupWarning.setAlpha(1f);
        popupWarning.setVisibility(TextView.VISIBLE);
        popupWarning.bringToFront(); // <-- ensures it's above all other views

        popupWarning.animate()
                .alpha(0f)
                .setDuration(8000)
                .withEndAction(() -> popupWarning.setVisibility(TextView.GONE))
                .start();
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
                    if (!faces.isEmpty()) {
                        hadFaceLastFrame = true;
                        processFace(faces.get(0));
                    } else {
                        txtDebug.setText("Face lost – treating as looking down/up");

                        // Treat missing face as looking away
                        boolean lookingAway = true;

                        if (gazeAwayStartTime == 0) {
                            gazeAwayStartTime = System.currentTimeMillis();
                        } else {
                            long elapsed = System.currentTimeMillis() - gazeAwayStartTime;
                            if (elapsed >= WARNING_DELAY_MS && !isLookingAway) {
                                issueWarning();
                                isLookingAway = true;
                            }
                        }
                    }


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

        // Only detect gaze if calibration is done
        if (isCalibrated) {
            detectGaze(cx, cy);
        }
    }


    // ---------------------------------------------------
    // Calibration
    // ---------------------------------------------------
    private void startCalibration() {
        playCalibrationStartSound();

        calibrationSamples = 0;
        calibratedCenterX = 0;
        calibratedCenterY = 0;
        isCalibrating = true;

        calibrationMessage.setText("Please keep looking directly at your camera…");
    }


    private void finishCalibration() {
        isCalibrating = false;
        isCalibrated = true; // calibration is done
        calibratedCenterX /= calibrationSamples;
        calibratedCenterY /= calibrationSamples;

        calibrationOverlay.setVisibility(LinearLayout.GONE);
        Toast.makeText(this, "Calibration complete!", Toast.LENGTH_SHORT).show();
        playCalibrationDoneSound();
    }


    // ---------------------------------------------------
    // Gaze Detection + Warning System
    // ---------------------------------------------------
    private void detectGaze(float cx, float cy) {

        float dx = cx - calibratedCenterX;
        float dy = cy - calibratedCenterY;

        // Directional thresholds
        float thresholdLeft  = -25f;  // negative dx
        float thresholdRight =  25f;  // positive dx

        float thresholdUp    = -55f;  // very lenient
        float thresholdDown  =  40f;  // lenient

        boolean lookingAway = false;

        // Horizontal detection
        if (dx < thresholdLeft) {
            lookingAway = true;   // too far left
        } else if (dx > thresholdRight) {
            lookingAway = true;   // too far right
        }

        // Vertical detection
        if (dy < thresholdUp) {
            lookingAway = true;   // looking too far up
        } else if (dy > thresholdDown) {
            lookingAway = true;   // looking too far down
        }

        // Timer behavior
        if (lookingAway) {
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
        playWarningBeep();
        showPopupWarning("Looking away!");

        remainingWarnings--;
        warningCounterText.setText("Remaining warnings: " + remainingWarnings);

        if (remainingWarnings <= 0) {
            Toast.makeText(this, "Exam ended (too many warnings).", Toast.LENGTH_LONG).show();
            finish();
        }
    }

}
