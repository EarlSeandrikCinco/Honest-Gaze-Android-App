package com.example.honestgaze;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CreateQuiz extends AppCompatActivity {

    private EditText quizNameInput, graceInput, maxWarningsInput;
    private Button btnCreate, btnCopyLink, btnEnterRoom;
    private ImageButton btnBackCreate, btnMore2, btnMore4;

    private DatabaseReference quizzesRef;
    private DatabaseReference roomsRef;

    private String roomId = "";
    private String quizKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        quizzesRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("quizzes");
        roomsRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("rooms");

        btnBackCreate = findViewById(R.id.btnBackCreate);
        quizNameInput = findViewById(R.id.inputField);
        graceInput = findViewById(R.id.inputField2);
        maxWarningsInput = findViewById(R.id.inputField4);

        btnCreate = findViewById(R.id.button2);
        btnCopyLink = findViewById(R.id.btnCopyLink);
        btnEnterRoom = findViewById(R.id.btnEnterRoom);
        btnMore2 = findViewById(R.id.btnMore2);
        btnMore4 = findViewById(R.id.btnMore4);

        btnCopyLink.setVisibility(View.GONE);
        btnEnterRoom.setVisibility(View.GONE);

        btnBackCreate.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> createQuiz());
        btnCopyLink.setOnClickListener(v -> copyRoomLink());
        btnEnterRoom.setOnClickListener(v -> enterRoom());
        
        // Three Dots Helper Dialogs
        btnMore2.setOnClickListener(v -> showGracePeriodDialog());
        btnMore4.setOnClickListener(v -> showWarningLimitDialog());
    }

    private void createQuiz() {
        String quizName = quizNameInput.getText().toString().trim();
        String graceStr = graceInput.getText().toString().trim();
        String maxWarningsStr = maxWarningsInput.getText().toString().trim();

        if (quizName.isEmpty()) {
            Toast.makeText(this, "Enter quiz name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (graceStr.isEmpty() || maxWarningsStr.isEmpty()) {
            Toast.makeText(this, "Please fill all quiz options", Toast.LENGTH_SHORT).show();
            return;
        }

        int grace, maxWarnings;
        try {
            grace = Integer.parseInt(graceStr);
            maxWarnings = Integer.parseInt(maxWarningsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate numeric ranges
        if (grace <= 0 || grace > 10) { // set your max grace seconds here
            Toast.makeText(this, "Grace period must be greater than 0 and less than 10", Toast.LENGTH_SHORT).show();
            return;
        }

        if (maxWarnings <= 0 || maxWarnings > 10) { // set max warnings limit
            Toast.makeText(this, "Number of warnings must be greater than 0 and less than 10", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate room and quiz key only if all valid
        roomId = generateRoomId();
        quizKey = quizzesRef.push().getKey();
        if (quizKey == null) {
            Toast.makeText(this, "Error generating quiz", Toast.LENGTH_SHORT).show();
            return;
        }

        saveQuizToFirebase(quizName, String.valueOf(grace), String.valueOf(maxWarnings));
    }


    private void saveQuizToFirebase(String quizName, String grace, String maxWarnings) {
        String dateTime = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date());
        long startTime = System.currentTimeMillis();

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("quizName", quizName);
        quizData.put("gracePeriod", Integer.parseInt(grace));
        quizData.put("numberOfWarnings", Integer.parseInt(maxWarnings));
        quizData.put("roomId", roomId);
        quizData.put("dateTime", dateTime); // ensure date/time is always present
        quizData.put("timestamp", startTime);
        quizData.put("isActive", true);
        quizData.put("startTime", startTime);
        quizData.put("date", date);

        quizzesRef.child(quizKey).setValue(quizData)
                .addOnSuccessListener(aVoid -> {
                    setupRoomInFirebase();
                    btnCopyLink.setVisibility(View.VISIBLE);
                    btnEnterRoom.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Quiz created successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to create quiz", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupRoomInFirebase() {
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("quizKey", quizKey);
        roomData.put("active", true);
        roomData.put("createdAt", System.currentTimeMillis());

        roomsRef.child(roomId).setValue(roomData);
    }

    private void copyRoomLink() {
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Room not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Room Link", roomId);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Room ID copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void enterRoom() {
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Room not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(CreateQuiz.this, RoomMonitorActivity.class);
        intent.putExtra("ROOM_ID", roomId);
        startActivity(intent);
    }

    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void showGracePeriodDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Grace Period")
                .setMessage("Enter allowable grace period (e.g., 3 seconds). This is the time allowed before a student is warned for gazing.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showWarningLimitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Warning Limit")
                .setMessage("Enter maximum number of warnings (e.g., 3 warnings). If the student exceeds this, the student will be disconnected to the room")
                .setPositiveButton("OK", null)
                .show();
    }
}

