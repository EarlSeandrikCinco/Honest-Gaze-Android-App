package com.example.honestgaze;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryScreen extends AppCompatActivity {

    private DatabaseReference database;
    private LinearLayout quizContainer;
    private ImageButton backButton;
    private EditText searchInput;

    private Map<String, Quiz> allQuizzesMap = new HashMap<>(); // Map quizKey to Quiz object
    private Map<String, LinearLayout> quizCardMap = new HashMap<>(); // Map quizKey to card view
    private Map<String, ValueEventListener> listenerMap = new HashMap<>(); // Map quizKey to listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_screen);

        database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("quizzes");

        quizContainer = findViewById(R.id.buttonContainer);
        backButton = findViewById(R.id.backButton);
        searchInput = findViewById(R.id.searchInput);

        backButton.setOnClickListener(v -> finish());

        loadQuizzes();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuizzes(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all listeners when activity is destroyed
        for (Map.Entry<String, ValueEventListener> entry : listenerMap.entrySet()) {
            database.child(entry.getKey()).child("isActive").removeEventListener(entry.getValue());
        }
        listenerMap.clear();
    }

    private void loadQuizzes() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Remove old listeners
                for (Map.Entry<String, ValueEventListener> entry : listenerMap.entrySet()) {
                    database.child(entry.getKey()).child("isActive").removeEventListener(entry.getValue());
                }
                listenerMap.clear();
                quizCardMap.clear();
                allQuizzesMap.clear();
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    Quiz quiz = data.getValue(Quiz.class);
                    String quizKey = data.getKey();
                    if (quiz != null && quizKey != null) {
                        allQuizzesMap.put(quizKey, quiz);
                    }
                }
                displayQuizzes(allQuizzesMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void filterQuizzes(String query) {
        Map<String, Quiz> filtered = new HashMap<>();
        for (Map.Entry<String, Quiz> entry : allQuizzesMap.entrySet()) {
            Quiz quiz = entry.getValue();
            if (quiz.getQuizName().toLowerCase().contains(query.toLowerCase())) {
                filtered.put(entry.getKey(), quiz);
            }
        }
        displayQuizzes(filtered);
    }

    private void displayQuizzes(Map<String, Quiz> quizzesMap) {
        quizContainer.removeAllViews();
        quizCardMap.clear();

        for (Map.Entry<String, Quiz> entry : quizzesMap.entrySet()) {
            String quizKey = entry.getKey();
            Quiz quiz = entry.getValue();

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(25, 25, 25, 25);
            card.setBackgroundColor(0xFFE8E8E8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 25, 0, 0);
            card.setLayoutParams(params);

            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            card.setForeground(ContextCompat.getDrawable(this, outValue.resourceId));

            TextView quizLabel = new TextView(this);
            String dateTimeText = quiz.getDateTime() != null ? quiz.getDateTime() : "No date";
            String dateText = quiz.getDate() != null ? quiz.getDate() : "";
            quizLabel.setText(quiz.getQuizName() + "\n" + (dateText.isEmpty() ? dateTimeText : dateText));
            quizLabel.setTextSize(18);
            quizLabel.setTextColor(0xFF000000);
            card.addView(quizLabel);

            // Store card reference
            quizCardMap.put(quizKey, card);

            // Set initial state based on isActive
            updateCardState(card, quiz.getIsActive(), quiz.getRoomId(), quizKey);

            // Listen for isActive changes in real-time
            ValueEventListener isActiveListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean isActive = snapshot.getValue(Boolean.class);
                    if (isActive == null) isActive = true;
                    updateCardState(card, isActive, quiz.getRoomId(), quizKey);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            
            database.child(quizKey).child("isActive").addValueEventListener(isActiveListener);
            listenerMap.put(quizKey, isActiveListener);

            quizContainer.addView(card);
        }
    }

    private void updateCardState(LinearLayout card, boolean isActive, String roomId, String quizKey) {
        if (isActive) {
            // Session is active - disable button
            card.setAlpha(0.5f);
            card.setEnabled(false);
            card.setClickable(false);
            
            // Update text to show status
            TextView quizLabel = (TextView) card.getChildAt(0);
            String currentText = quizLabel.getText().toString();
            if (!currentText.contains("(Active)")) {
                quizLabel.setText(currentText + "\n(Active - Summary unavailable)");
            }
        } else {
            // Session ended - enable button
            card.setAlpha(1.0f);
            card.setEnabled(true);
            card.setClickable(true);
            
            // Update text
            TextView quizLabel = (TextView) card.getChildAt(0);
            String currentText = quizLabel.getText().toString();
            currentText = currentText.replace("\n(Active - Summary unavailable)", "");
            quizLabel.setText(currentText);
            
            // Set click listener to navigate to SessionSummaryActivity
            card.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(SummaryScreen.this, SessionSummaryActivity.class);
                intent.putExtra("QUIZ_KEY", quizKey);
                intent.putExtra("ROOM_ID", roomId);
                startActivity(intent);
            });
        }
    }

    private void showSummaryDialog(String roomId) {
        if (roomId == null || roomId.isEmpty()) return;

        DatabaseReference roomRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("rooms").child(roomId);

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String quizKey = snapshot.child("quizKey").getValue(String.class);
                if (quizKey == null) return;

                // Fetch quiz node to get maxWarnings
                FirebaseDatabase.getInstance(
                                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
                        ).getReference("quizzes").child(quizKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot quizSnapshot) {
                                int maxWarnings = 3; // fallback default
                                if (quizSnapshot.child("maxWarnings").exists()) {
                                    try {
                                        maxWarnings = Integer.parseInt(quizSnapshot.child("maxWarnings").getValue(String.class));
                                    } catch (Exception ignored) {}
                                }

                                buildSummary(snapshot.child("students"), roomId, maxWarnings);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void buildSummary(DataSnapshot studentsSnapshot, String roomId, int maxWarnings) {
        StringBuilder summary = new StringBuilder();
        StringBuilder csvData = new StringBuilder();
        csvData.append("Name,Warnings Left,Event\n");

        for (DataSnapshot studentSnap : studentsSnapshot.getChildren()) {
            String name = studentSnap.child("name").getValue(String.class);
            if (name == null) name = "Unknown";

            Object totalObj = studentSnap.child("totalWarnings").getValue();
            int totalWarnings = 0;
            if (totalObj instanceof Long) totalWarnings = ((Long) totalObj).intValue();
            else if (totalObj instanceof Integer) totalWarnings = (Integer) totalObj;
            else if (totalObj instanceof String) {
                try { totalWarnings = Integer.parseInt((String) totalObj); } catch (Exception ignored) {}
            }

            int currentWarningsLeft = maxWarnings - totalWarnings;
            if (currentWarningsLeft < 0) currentWarningsLeft = 0;

            DataSnapshot eventsSnap = studentSnap.child("events");
            int warningsForDisplay = maxWarnings;
            if (eventsSnap.exists()) {
                for (DataSnapshot eventSnap : eventsSnap.getChildren()) {
                    String event = eventSnap.getValue(String.class);
                    if (event == null) event = "Unknown event";

                    summary.append(name).append(" ").append(warningsForDisplay)
                            .append(" ").append(event).append("\n");

                    csvData.append(name).append(",").append(warningsForDisplay).append(",")
                            .append(event.replace(",", " ")).append("\n");

                    warningsForDisplay--;
                    if (warningsForDisplay < 0) warningsForDisplay = 0;
                }
            } else {
                summary.append(name).append(" ").append(warningsForDisplay).append(" No events\n");
                csvData.append(name).append(",").append(warningsForDisplay).append(",No events\n");
            }

            summary.append("\n");
        }

        // Display summary dialog
        TextView summaryView = new TextView(SummaryScreen.this);
        summaryView.setText(summary.toString());
        summaryView.setPadding(30, 30, 30, 30);

        ScrollView scrollView = new ScrollView(SummaryScreen.this);
        scrollView.addView(summaryView);

        new AlertDialog.Builder(SummaryScreen.this)
                .setTitle("Exam Summary")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNegativeButton("Export CSV", (dialog, which) -> saveCsvToDownloads(roomId, csvData.toString()))
                .show();
    }

    private void saveCsvToDownloads(String roomId, String csvContent) {
        String fileName = "exam_summary_" + roomId + ".csv";

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        os.write(csvContent.getBytes());
                    }
                    Toast.makeText(this, "CSV saved to Downloads.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to create file.", Toast.LENGTH_LONG).show();
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File csvFile = new File(downloadsDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(csvFile)) {
                    fos.write(csvContent.getBytes());
                }
                Toast.makeText(this, "CSV saved to Downloads: " + csvFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save CSV.", Toast.LENGTH_LONG).show();
        }
    }
}
