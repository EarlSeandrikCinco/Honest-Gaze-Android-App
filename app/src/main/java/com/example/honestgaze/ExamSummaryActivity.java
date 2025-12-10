package com.example.honestgaze;

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

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String studentName = studentSnap.child("name").getValue(String.class);
                    Long totalWarnings = studentSnap.child("totalWarnings").getValue(Long.class);

                    TextView studentText = new TextView(ExamSummaryActivity.this);
                    studentText.setText(studentName + " - Warnings: " + (totalWarnings != null ? totalWarnings : 0));
                    studentText.setTextSize(18);
                    studentText.setPadding(10, 10, 10, 10);

                    studentSummaryContainer.addView(studentText);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExamSummaryActivity.this, "Failed to load summary", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
