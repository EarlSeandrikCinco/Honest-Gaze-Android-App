package com.example.honestgaze;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ExamSummaryActivity extends AppCompatActivity {

    private DatabaseReference roomRef;
    private LinearLayout studentSummaryContainer;
    private ImageButton backButton;
    private String roomId;
    private String persistentStudentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_summary);

        studentSummaryContainer = findViewById(R.id.studentSummaryContainer);
        backButton = findViewById(R.id.backButton);

        roomId = getIntent().getStringExtra("ROOM_ID");
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(this, "Invalid room ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get persistent student ID
        SharedPreferences prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        persistentStudentId = prefs.getString("studentId", null);

        roomRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("rooms").child(roomId);

        backButton.setOnClickListener(v -> finish());

        loadStudentSummary();
    }

    private void loadStudentSummary() {
        roomRef.child("students").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentSummaryContainer.removeAllViews();

                // Find the current student's record
                boolean foundStudent = false;
                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String studentId = studentSnap.getKey();
                    
                    // Try to match by checking if this student's name matches or if we can identify them
                    // Since we don't have a direct mapping, we'll show all students but highlight the current one
                    // For now, let's show the student's own stats if we can find them
                    
                    String studentName = studentSnap.child("name").getValue(String.class);
                    Object totalWarningsObj = studentSnap.child("totalWarnings").getValue();
                    int totalWarnings = 0;
                    
                    if (totalWarningsObj instanceof Long) {
                        totalWarnings = ((Long) totalWarningsObj).intValue();
                    } else if (totalWarningsObj instanceof Integer) {
                        totalWarnings = (Integer) totalWarningsObj;
                    }
                    
                    // Count events
                    int eventCount = 0;
                    DataSnapshot eventsSnap = studentSnap.child("events");
                    if (eventsSnap.exists()) {
                        eventCount = (int) eventsSnap.getChildrenCount();
                    }

                    // Create summary card for this student
                    TextView studentText = new TextView(ExamSummaryActivity.this);
                    String summaryText = "Name: " + (studentName != null ? studentName : "Unknown") + "\n" +
                            "Total Warnings: " + totalWarnings + "\n" +
                            "Warning Events: " + eventCount;
                    
                    studentText.setText(summaryText);
                    studentText.setTextSize(18);
                    studentText.setPadding(20, 20, 20, 20);
                    studentText.setBackgroundColor(0xFFE8E8E8);
                    
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 10, 0, 10);
                    studentText.setLayoutParams(params);

                    studentSummaryContainer.addView(studentText);
                    foundStudent = true;
                }
                
                if (!foundStudent) {
                    TextView noDataText = new TextView(ExamSummaryActivity.this);
                    noDataText.setText("No student data found for this exam.");
                    noDataText.setTextSize(16);
                    noDataText.setPadding(20, 20, 20, 20);
                    studentSummaryContainer.addView(noDataText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExamSummaryActivity.this, "Failed to load summary", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
