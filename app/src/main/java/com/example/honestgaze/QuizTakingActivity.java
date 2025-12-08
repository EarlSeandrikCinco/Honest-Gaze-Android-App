package com.example.honestgaze;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizTakingActivity extends AppCompatActivity {

    private WebView webView;
    private PreviewView cameraPreview;
    private TextView tvTimer, tvWarningCount, warningText;
    private View warningOverlay;
    private Button btnSubmit;

    private FaceDetector detector;
    private ExecutorService cameraExecutor;

    private long lookAwayStartTime = 0;
    private boolean isLookingAway = false;
    private final long GRACE_PERIOD_MS = 1000;

    private int warningCount = 0;
    private long lastWarningTime = 0;
    private final long WARNING_COOLDOWN_MS = 2500;

    private CountDownTimer quizTimer;
    private long timeRemainingInMillis = 3600000; // 60 minutes default

    private static final int PERMISSION_CODE = 100;
    private static final String TAG = "QuizTaking";

    private String quizName = "Quiz"; // Store quiz name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_taking);

        webView = findViewById(R.id.webView);
        cameraPreview = findViewById(R.id.cameraPreview);
        tvTimer = findViewById(R.id.tvTimer);
        tvWarningCount = findViewById(R.id.tvWarningCount);
        warningText = findViewById(R.id.warningText);
        warningOverlay = findViewById(R.id.warningOverlay);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Get quiz link from intent
        String quizLink = getIntent().getStringExtra("quizLink");
        if (quizLink == null || quizLink.isEmpty()) {
            Toast.makeText(this, "No quiz link provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup WebView
        setupWebView(quizLink);

        // Setup camera and face detection
        cameraExecutor = Executors.newSingleThreadExecutor();
        if (!permissionsGranted()) {
            requestPermissions();
        } else {
            startCamera();
        }
        setupMLKit();

        // Start timer
        startQuizTimer();

        // Submit button
        btnSubmit.setOnClickListener(v -> showSubmitDialog());
    }

    private void setupWebView(String url) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    private void startQuizTimer() {
        quizTimer = new CountDownTimer(timeRemainingInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingInMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                showTimeUpDialog();
            }
        }.start();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeRemainingInMillis / 1000) / 60;
        int seconds = (int) (timeRemainingInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void showTimeUpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Time's Up!")
                .setMessage("Your quiz time has ended.")
                .setCancelable(false)
                .setPositiveButton("View Results", (dialog, which) -> navigateToResults())
                .show();
    }

    private void showSubmitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Submit Quiz?")
                .setMessage("Are you sure you want to submit? You cannot change your answers after submitting.")
                .setPositiveButton("Submit", (dialog, which) -> navigateToResults())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToResults() {
        // Navigate to QuizDetailsActivity with results
        Intent intent = new Intent(QuizTakingActivity.this, QuizDetailsActivity.class);
        intent.putExtra("quizName", quizName);
        intent.putExtra("warningCount", warningCount);
        intent.putExtra("duration", calculateDuration());
        startActivity(intent);
        finish();
    }

    private String calculateDuration() {
        long elapsedMillis = 3600000 - timeRemainingInMillis;
        int minutes = (int) (elapsedMillis / 1000) / 60;
        return minutes + " minutes";
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
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
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
                .addOnSuccessListener(this::handleDetections)
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleDetections(List<Face> faces) {
        if (faces.size() == 0) {
            resetGazeState();
            showWarning("No face detected");
            return;
        }

        if (faces.size() > 1) {
            resetGazeState();
            showWarning("Multiple faces detected");
            return;
        }

        Face face = faces.get(0);
        float headEulerY = face.getHeadEulerAngleY();
        float headEulerX = face.getHeadEulerAngleX();

        String direction = null;

        if (headEulerY < -20) direction = "right";
        else if (headEulerY > 20) direction = "left";
        if (headEulerX < -20) direction = "down";

        if (direction == null) {
            resetGazeState();
            return;
        }

        long now = System.currentTimeMillis();

        if (!isLookingAway) {
            isLookingAway = true;
            lookAwayStartTime = now;
            return;
        }

        if (now - lookAwayStartTime >= GRACE_PERIOD_MS) {
            showWarning("You looked " + direction);
            resetGazeState();
        }
    }

    private void resetGazeState() {
        isLookingAway = false;
        lookAwayStartTime = 0;
    }

    private void showWarning(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastWarningTime < WARNING_COOLDOWN_MS) return;

        lastWarningTime = now;
        warningCount++;

        runOnUiThread(() -> {
            tvWarningCount.setText("Warnings: " + warningCount);
            warningText.setText("⚠️ " + msg);
            warningText.setVisibility(View.VISIBLE);
            warningOverlay.setVisibility(View.VISIBLE);

            warningText.postDelayed(() -> {
                warningText.setVisibility(View.GONE);
                warningOverlay.setVisibility(View.GONE);
            }, 2000);
        });

        Log.w(TAG, "Warning #" + warningCount + ": " + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizTimer != null) quizTimer.cancel();
        cameraExecutor.shutdown();
        if (detector != null) detector.close();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Quiz?")
                .setMessage("Are you sure you want to exit? Your progress may not be saved.")
                .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Cancel", null)
                .show();
    }
}