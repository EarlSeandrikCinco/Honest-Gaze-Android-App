package com.example.honestgaze;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionSummaryActivity extends AppCompatActivity {

    private Button btnExportCsv;
    private TextView tvStudentTable;
    private String roomId;
    private String quizKey;
    private StringBuilder csvData = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_summary);

        btnExportCsv = findViewById(R.id.btnExportCsv);
        tvStudentTable = findViewById(R.id.tvStudentTable);

        roomId = getIntent().getStringExtra("ROOM_ID");
        quizKey = getIntent().getStringExtra("QUIZ_KEY");

        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Invalid room ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnExportCsv.setOnClickListener(v -> exportCsv());

        loadSessionSummary();
    }

    private void loadSessionSummary() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/");
        
        DatabaseReference roomRef = database.getReference("rooms").child(roomId);
        
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvStudentTable.setText("No session data found.");
                    return;
                }

                StringBuilder summaryText = new StringBuilder();
                csvData.setLength(0);
                csvData.append("Name,Warnings,Event Count\n");

                DataSnapshot studentsSnap = snapshot.child("students");
                if (studentsSnap.exists()) {
                    summaryText.append("Name\t\tWarnings\tEvents\n");
                    summaryText.append("----------------------------------------\n");

                    for (DataSnapshot studentSnap : studentsSnap.getChildren()) {
                        String studentName = studentSnap.child("name").getValue(String.class);
                        if (studentName == null) studentName = "Unknown";

                        Object totalWarningsObj = studentSnap.child("totalWarnings").getValue();
                        int totalWarnings = 0;
                        if (totalWarningsObj instanceof Long) {
                            totalWarnings = ((Long) totalWarningsObj).intValue();
                        } else if (totalWarningsObj instanceof Integer) {
                            totalWarnings = (Integer) totalWarningsObj;
                        }

                        int eventCount = 0;
                        DataSnapshot eventsSnap = studentSnap.child("events");
                        if (eventsSnap.exists()) {
                            eventCount = (int) eventsSnap.getChildrenCount();
                        }

                        summaryText.append(studentName).append("\t\t")
                                .append(totalWarnings).append("\t\t")
                                .append(eventCount).append("\n");

                        csvData.append(studentName).append(",")
                                .append(totalWarnings).append(",")
                                .append(eventCount).append("\n");
                    }
                } else {
                    summaryText.append("No students found in this session.");
                }

                tvStudentTable.setText(summaryText.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SessionSummaryActivity.this, "Failed to load summary", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exportCsv() {
        String fileName = "session_summary_" + (roomId != null ? roomId : "unknown") + ".csv";
        String fullCsv = csvData.toString();
        
        if (fullCsv.isEmpty() || fullCsv.equals("Name,Warnings,Event Count\n")) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            OutputStream fos;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ scoped storage
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Failed to create file URI");

                fos = getContentResolver().openOutputStream(uri);
            } else {
                // Older Android
                java.io.File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(path, fileName);
                fos = new java.io.FileOutputStream(file);
            }

            if (fos != null) {
                fos.write(fullCsv.getBytes());
                fos.close();
            }

            Toast.makeText(this, "CSV exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error exporting CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
