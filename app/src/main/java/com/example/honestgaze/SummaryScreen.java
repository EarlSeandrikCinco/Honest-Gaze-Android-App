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
import java.util.List;

public class SummaryScreen extends AppCompatActivity {

    private DatabaseReference database;
    private LinearLayout quizContainer;
    private ImageButton backButton;
    private ImageButton btnBackSummary;
    private EditText searchInput;

    private List<Quiz> allQuizzes = new ArrayList<>(); // store all quizzes for filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_screen);

        database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("quizzes");

        quizContainer = findViewById(R.id.buttonContainer);
        backButton = findViewById(R.id.backButton);
        btnBackSummary = findViewById(R.id.btnBackSummary);
        searchInput = findViewById(R.id.searchInput);

        backButton.setOnClickListener(v -> finish());
        btnBackSummary.setOnClickListener(v -> finish());

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

    private void loadQuizzes() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allQuizzes.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Quiz quiz = data.getValue(Quiz.class);
                    if (quiz != null) {
                        allQuizzes.add(quiz);
                    }
                }
                displayQuizzes(allQuizzes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void filterQuizzes(String query) {
        List<Quiz> filtered = new ArrayList<>();
        for (Quiz quiz : allQuizzes) {
            if (quiz.getQuizName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(quiz);
            }
        }
        displayQuizzes(filtered);
    }

    private void displayQuizzes(List<Quiz> quizzes) {
        quizContainer.removeAllViews();

        for (Quiz quiz : quizzes) {
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
            quizLabel.setText(quiz.getQuizName() + "\n" + dateTimeText);
            quizLabel.setTextSize(18);
            quizLabel.setTextColor(0xFF000000);
            card.addView(quizLabel);

            // Show summary when clicked
            card.setOnClickListener(v -> showSummaryDialog(quiz.getRoomId()));

            quizContainer.addView(card);
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
