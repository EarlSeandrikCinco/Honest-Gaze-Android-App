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
    private ImageButton backButton;

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

        backButton = findViewById(R.id.backButton);
        quizNameInput = findViewById(R.id.inputField);
        graceInput = findViewById(R.id.inputField2);
        maxWarningsInput = findViewById(R.id.inputField4);

        btnCreate = findViewById(R.id.button2);
        btnCopyLink = findViewById(R.id.btnCopyLink);
        btnEnterRoom = findViewById(R.id.btnEnterRoom);

        btnCopyLink.setVisibility(View.GONE);
        btnEnterRoom.setVisibility(View.GONE);

        backButton.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> createQuiz());
        btnCopyLink.setOnClickListener(v -> copyRoomLink());
        btnEnterRoom.setOnClickListener(v -> enterRoom());
    }

    private void createQuiz() {
        String quizName = quizNameInput.getText().toString().trim();
        String grace = graceInput.getText().toString().trim();
        String maxWarnings = maxWarningsInput.getText().toString().trim();

        if (quizName.isEmpty()) {
            Toast.makeText(this, "Enter quiz name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (grace.isEmpty() || maxWarnings.isEmpty()) {
            Toast.makeText(this, "Please fill all quiz options", Toast.LENGTH_SHORT).show();
            return;
        }

        roomId = generateRoomId();
        quizKey = quizzesRef.push().getKey();

        if (quizKey == null) {
            Toast.makeText(this, "Error generating quiz", Toast.LENGTH_SHORT).show();
            return;
        }

        saveQuizToFirebase(quizName, grace, maxWarnings);
    }

    private void saveQuizToFirebase(String quizName, String grace, String maxWarnings) {
        String dateTime = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        Map<String, Object> quizData = new HashMap<>();
        quizData.put("quizName", quizName);
        quizData.put("gracePeriod", Integer.parseInt(grace));
        quizData.put("numberOfWarnings", Integer.parseInt(maxWarnings));
        quizData.put("roomId", roomId);
        quizData.put("dateTime", dateTime); // ensure date/time is always present
        quizData.put("timestamp", System.currentTimeMillis());

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
}
