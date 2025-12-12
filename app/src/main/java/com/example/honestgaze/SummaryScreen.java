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
import java.util.Collections;
import java.util.List;

public class SummaryScreen extends AppCompatActivity {

    private DatabaseReference database;
    private LinearLayout quizContainer;
    private ImageButton backButton;
    private ImageButton btnBackSummary;
    private EditText searchInput;

    private List<Quiz> allQuizzes = new ArrayList<>();

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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuizzes(s.toString().trim());
            }
        });
    }

    private void loadQuizzes() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allQuizzes.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Quiz quiz = data.getValue(Quiz.class);
                    if (quiz != null) allQuizzes.add(quiz);
                }

                // **THE FIX — reverse so newest appears first**
                Collections.reverse(allQuizzes);

                displayQuizzes(allQuizzes);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterQuizzes(String query) {
        List<Quiz> filtered = new ArrayList<>();
        for (Quiz quiz : allQuizzes) {
            if (quiz.getQuizName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(quiz);
            }
        }

        // keep order consistent
        Collections.reverse(filtered);

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

            TextView warningsLabel = new TextView(this);
            int totalWarnings = quiz.getNumberOfWarnings();
            warningsLabel.setText("Total Warnings: " + totalWarnings);
            warningsLabel.setTextSize(14);
            warningsLabel.setTextColor(0xFFFF0000);
            LinearLayout.LayoutParams warningsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            warningsParams.setMargins(0, 8, 0, 0);
            warningsLabel.setLayoutParams(warningsParams);
            card.addView(warningsLabel);

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

                FirebaseDatabase.getInstance(
                                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
                        ).getReference("quizzes").child(quizKey)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot quizSnapshot) {

                                int maxWarnings = 0;

                                Object maxObj = quizSnapshot.child("maxWarnings").getValue();
                                if (maxObj instanceof Long) maxWarnings = ((Long) maxObj).intValue();
                                else if (maxObj instanceof String) {
                                    try { maxWarnings = Integer.parseInt((String) maxObj); } catch (Exception ignored) {}
                                }

                                if (maxWarnings == 0) {
                                    Object oldObj = quizSnapshot.child("numberOfWarnings").getValue();
                                    if (oldObj instanceof Long) maxWarnings = ((Long) oldObj).intValue();
                                    else if (oldObj instanceof String) {
                                        try { maxWarnings = Integer.parseInt((String) oldObj); } catch (Exception ignored) {}
                                    }
                                }

                                if (maxWarnings == 0) maxWarnings = 3;

                                buildSummary(snapshot.child("students"), roomId, maxWarnings);
                            }

                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void buildSummary(DataSnapshot studentsSnapshot, String roomId, int maxWarnings) {
        StringBuilder summary = new StringBuilder();
        summary.append("Name | Warning Number | Event\n");
        summary.append("------------------------------------\n");

        StringBuilder csvData = new StringBuilder();
        csvData.append("Name,Warning Number,Event\n");

        for (DataSnapshot studentSnap : studentsSnapshot.getChildren()) {
            String name = studentSnap.child("name").getValue(String.class);
            if (name == null) name = "Unknown";

            DataSnapshot eventsSnap = studentSnap.child("events");

            int warningsLeft = maxWarnings;

            if (eventsSnap.exists()) {
                for (DataSnapshot eventSnap : eventsSnap.getChildren()) {
                    String event = eventSnap.getValue(String.class);
                    if (event == null) event = "Unknown event";

                    summary.append(name).append(" | ")
                            .append(warningsLeft).append(" | ")
                            .append(event).append("\n");

                    csvData.append(name).append(",")
                            .append(warningsLeft).append(",")
                            .append(event.replace(",", " ")).append("\n");

                    warningsLeft--;
                    if (warningsLeft < 1) warningsLeft = 1;
                }
            } else {
                summary.append(name).append(" | ").append(warningsLeft).append(" | No events\n");
                csvData.append(name).append(",").append(warningsLeft).append(",No events\n");
            }

            summary.append("\n");
        }

        TextView summaryView = new TextView(SummaryScreen.this);
        summaryView.setText(summary.toString());
        summaryView.setPadding(30, 30, 30, 30);

        ScrollView scrollView = new ScrollView(SummaryScreen.this);
        scrollView.addView(summaryView);

        new AlertDialog.Builder(SummaryScreen.this)
                .setTitle("Exam Summary")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNegativeButton("Export CSV",
                        (dialog, which) -> saveCsvToDownloads(roomId, csvData.toString()))
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
