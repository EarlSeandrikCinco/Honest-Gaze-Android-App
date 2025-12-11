package com.example.honestgaze;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceDetection;

import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class OngoingExamActivity extends AppCompatActivity {

    private Handler heartbeatHandler = new Handler();
    private Runnable heartbeatRunnable;

    private static final int CAMERA_PERMISSION_CODE = 101;

    private DatabaseReference roomStatusRef;
    private PreviewView previewView;
    private TextView txtDebug, warningCounterText;
    private LinearLayout calibrationOverlay;
    private Button btnStartCalibration, btnCancelCalibration;
    private TextView calibrationMessage;
    private TextView popupWarning;

    private FaceDetector faceDetector;

    private boolean isCalibrating = false;
    private boolean isCalibrated = false;
    private int calibrationSamples = 0;
    private float calibratedCenterX = 0;
    private float calibratedCenterY = 0;

    private int remainingWarnings;
    private boolean isLookingAway = false;
    private long gazeAwayStartTime = 0;
    private static long WARNING_DELAY_MS = 2500;
    private boolean hadFaceLastFrame = false;

    private SoundPool soundPool;
    private int calibrationDoneSoundId, calibrationStartSoundId, warningBeepSoundId;
    private final int calibrationFrames = 100;

    private DatabaseReference roomRef;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_exam);

        bindViews();
        setupFaceDetector();
        setupSoundPool();

        String roomId = getIntent().getStringExtra("ROOM_ID");
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Invalid room. Cannot enter exam.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("rooms").child(roomId);

        studentId = "student_" + System.currentTimeMillis();
        String studentName = getIntent().getStringExtra("STUDENT_NAME");
        if (studentName == null || studentName.isEmpty()) studentName = "Student";

        roomRef.child("students").child(studentId).child("name").setValue(studentName);
        roomRef.child("students").child(studentId).child("totalWarnings").setValue(0);
        roomRef.child("status").setValue("active");

        // Create studentHistory record
        createStudentHistoryRecord(roomId);

        roomStatusRef = roomRef.child("active");
        listenForRoomEnd();

        // Fetch quiz settings first
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String quizKey = snapshot.child("quizKey").getValue(String.class);
                    if (quizKey != null) {
                        FirebaseDatabase.getInstance(
                                        "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app"
                                ).getReference("quizzes").child(quizKey)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot quizSnapshot) {
                                        if (quizSnapshot.exists()) {
                                            // Defaults
                                            long graceMs = 2500;
                                            int maxWarn = 3;

                                            // Grace period
                                            Object graceObj = quizSnapshot.child("gracePeriod").getValue();
                                            if (graceObj instanceof Long) graceMs = (Long) graceObj;
                                            else if (graceObj instanceof String) {
                                                try { graceMs = Long.parseLong((String) graceObj); } catch (Exception ignored) {}
                                            }

                                            // Max warnings
                                            Object warnObj = quizSnapshot.child("numberOfWarnings").getValue();
                                            if (warnObj instanceof Long) maxWarn = ((Long) warnObj).intValue();
                                            else if (warnObj instanceof String) {
                                                try { maxWarn = Integer.parseInt((String) warnObj); } catch (Exception ignored) {}
                                            }

                                            WARNING_DELAY_MS = graceMs;
                                            remainingWarnings = maxWarn;
                                            warningCounterText.setText("Warnings left: " + remainingWarnings);

                                            // Start camera & calibration
                                            calibrationOverlay.setVisibility(LinearLayout.VISIBLE);
                                            if (hasCameraPermission()) startCamera();
                                            else requestCameraPermission();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (roomRef != null) {
                    roomRef.child("students").child(studentId)
                            .child("lastActive").setValue(ServerValue.TIMESTAMP)
                            .addOnFailureListener(e -> heartbeatHandler.postDelayed(this, 5000));
                }
                heartbeatHandler.postDelayed(this, 5000);
            }
        };
        heartbeatHandler.post(heartbeatRunnable);

        btnStartCalibration.setOnClickListener(v -> startCalibration());
        btnCancelCalibration.setOnClickListener(v -> {
            isCalibrating = false;
            isCalibrated = false;
            calibrationSamples = 0;
            calibratedCenterX = 0;
            calibratedCenterY = 0;
            calibrationOverlay.setVisibility(LinearLayout.GONE);
            Toast.makeText(this, "Calibration cancelled", Toast.LENGTH_SHORT).show();
            finish();
        });
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
    }

    private void listenForRoomEnd() {
        roomStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean active = snapshot.getValue(Boolean.class);
                if (active != null && !active) finish();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
        if (soundPool != null) soundPool.release();
        super.onDestroy();
    }

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
        popupWarning.bringToFront();
        popupWarning.animate()
                .alpha(0f)
                .setDuration(8000)
                .withEndAction(() -> popupWarning.setVisibility(TextView.GONE))
                .start();
    }

    private void setupFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(options);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==CAMERA_PERMISSION_CODE && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startCamera();
        else Toast.makeText(this,"Camera permission required",Toast.LENGTH_SHORT).show();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                bindCamera(providerFuture.get());
            } catch (ExecutionException | InterruptedException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void bindCamera(ProcessCameraProvider provider) {
        provider.unbindAll();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(480,640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeFrame);
        provider.bindToLifecycle(this, selector, preview, analysis);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeFrame(ImageProxy proxy) {
        if(proxy.getImage()==null){proxy.close();return;}
        InputImage img = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());
        faceDetector.process(img)
                .addOnSuccessListener(faces -> {
                    if(!faces.isEmpty()){ hadFaceLastFrame=true; processFace(faces.get(0)); }
                    else if(isCalibrating){ txtDebug.setText("Face lost during calibration — restarting calibration"); restartCalibration(); return;}
                    else if(!isCalibrated){ txtDebug.setText("No face detected (waiting for calibration)"); gazeAwayStartTime=0; isLookingAway=false; return;}
                    else if(hadFaceLastFrame){
                        txtDebug.setText("Face lost – treating as looking away");
                        if(gazeAwayStartTime==0) gazeAwayStartTime=System.currentTimeMillis();
                        else if(System.currentTimeMillis()-gazeAwayStartTime>=WARNING_DELAY_MS && !isLookingAway){ issueWarning("face lost"); isLookingAway=true; }
                    } else {gazeAwayStartTime=0; isLookingAway=false;}
                }).addOnFailureListener(Throwable::printStackTrace)
                .addOnCompleteListener(task -> proxy.close());
    }

    private PointF getEyeCenter(Face face,int contourType){
        FaceContour contour = face.getContour(contourType);
        if(contour==null||contour.getPoints().isEmpty()) return null;
        float sx=0,sy=0;
        for(PointF p:contour.getPoints()){ sx+=p.x; sy+=p.y; }
        int n=contour.getPoints().size();
        return new PointF(sx/n,sy/n);
    }

    private void processFace(Face face){
        PointF left=getEyeCenter(face,FaceContour.LEFT_EYE);
        PointF right=getEyeCenter(face,FaceContour.RIGHT_EYE);
        if(left==null||right==null){ txtDebug.setText("Eye contours missing"); return; }
        float cx=(left.x+right.x)/2f;
        float cy=(left.y+right.y)/2f;
        txtDebug.setText("CX: "+cx+"\nCY: "+cy);
        if(isCalibrating){
            if(calibrationSamples>0){
                float dx=cx-(calibratedCenterX/calibrationSamples);
                float dy=cy-(calibratedCenterY/calibrationSamples);
                boolean lookedAway=dx<-35f||dx>35f||dy<-50f||dy>50f;
                if(lookedAway){ restartCalibration(); return; }
            }
            calibratedCenterX+=cx;
            calibratedCenterY+=cy;
            calibrationSamples++;
            calibrationMessage.setText("Calibrating... ("+calibrationSamples+"/"+calibrationFrames+")");
            if(calibrationSamples>=calibrationFrames) finishCalibration();
            return;
        }
        if(isCalibrated) detectGaze(cx,cy);
    }

    private void startCalibration(){ playCalibrationStartSound(); calibrationSamples=0; calibratedCenterX=0; calibratedCenterY=0; isCalibrating=true; btnStartCalibration.setVisibility(Button.GONE); calibrationMessage.setText("Please keep looking directly at your exam…"); }
    private void restartCalibration(){ isCalibrating=false; calibrationSamples=0; calibratedCenterX=0; calibratedCenterY=0; calibrationMessage.setText("Calibration needs to restart. Press OK to begin."); btnStartCalibration.setVisibility(Button.VISIBLE);}
    private void finishCalibration(){ isCalibrating=false; isCalibrated=true; calibratedCenterX/=calibrationSamples; calibratedCenterY/=calibrationSamples; calibrationOverlay.setVisibility(LinearLayout.GONE); Toast.makeText(this,"Calibration complete!",Toast.LENGTH_SHORT).show(); playCalibrationDoneSound(); }

    private void detectGaze(float cx,float cy){
        float dx = calibratedCenterX - cx; // flip sign for front camera
        float dy = cy - calibratedCenterY;
        String direction = null;
        if(dx < -25f) direction="left";
        else if(dx > 25f) direction="right";
        else if(dy < -55f) direction="up";
        else if(dy > 40f) direction="down";

        if(direction != null){
            if(gazeAwayStartTime==0) gazeAwayStartTime=System.currentTimeMillis();
            else if(System.currentTimeMillis()-gazeAwayStartTime>=WARNING_DELAY_MS && !isLookingAway){
                issueWarning("looked "+direction+" for too long");
                isLookingAway=true;
            }
        } else { gazeAwayStartTime=0; isLookingAway=false; }
    }


    private void createStudentHistoryRecord(String roomId) {
        // Get or create a persistent student identifier
        SharedPreferences prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        String persistentStudentId = prefs.getString("studentId", null);
        if (persistentStudentId == null) {
            persistentStudentId = "student_" + System.currentTimeMillis();
            prefs.edit().putString("studentId", persistentStudentId).apply();
        }

        // Create final temporary variable for use in inner class
        final String finalPersistentStudentId = persistentStudentId;

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/");

        // Get quizKey from room
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String quizKey = snapshot.child("quizKey").getValue(String.class);
                if (quizKey != null) {
                    // Create final temporary variable for use in nested inner class
                    final String finalQuizKey = quizKey;
                    
                    // Get quiz details
                    database.getReference("quizzes").child(finalQuizKey)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot quizSnapshot) {
                                    Quiz quiz = quizSnapshot.getValue(Quiz.class);
                                    if (quiz != null) {
                                        // Create studentHistory record
                                        DatabaseReference historyRef = database.getReference("studentHistory")
                                                .child(finalPersistentStudentId).child(finalQuizKey);
                                        
                                        Map<String, Object> historyData = new HashMap<>();
                                        historyData.put("quizName", quiz.getQuizName());
                                        historyData.put("roomId", roomId);
                                        historyData.put("date", quiz.getDate() != null ? quiz.getDate() : quiz.getDateTime());
                                        historyData.put("joinedAt", System.currentTimeMillis());
                                        historyData.put("quizKey", finalQuizKey);
                                        
                                        historyRef.setValue(historyData);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void issueWarning(String direction){
        playWarningBeep();
        showPopupWarning(direction);
        remainingWarnings--;
        warningCounterText.setText("Warnings left: "+remainingWarnings);
        if(roomRef!=null){
            long timestamp=System.currentTimeMillis();
            roomRef.child("students").child(studentId).child("events").child(String.valueOf(timestamp)).setValue(direction);
            roomRef.child("students").child(studentId).child("totalWarnings").setValue(ServerValue.increment(1));
        }
        if(remainingWarnings<=0){
            Toast.makeText(this,"Exam ended due to too many warnings.",Toast.LENGTH_LONG).show();
            if(roomRef!=null) roomRef.child("status").setValue("ended");
            Intent intent=new Intent(this,StudentMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }
}
