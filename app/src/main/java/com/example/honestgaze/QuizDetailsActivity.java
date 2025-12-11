package com.example.honestgaze;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class QuizDetailsActivity extends AppCompatActivity {

    private TextView tvQuizTitle, tvDuration, tvWarnings, tvStartTime;
    private String roomId, studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_quiz_details);

        tvQuizTitle = findViewById(R.id.tvQuizTitle);
        tvDuration = findViewById(R.id.tvDuration);
        tvWarnings = findViewById(R.id.tvWarnings);
        tvStartTime = findViewById(R.id.tvTimestamp);

        roomId = getIntent().getStringExtra("ROOM_ID");
        studentId = getIntent().getStringExtra("STUDENT_ID");

        loadQuizDetails();
    }

    private void loadQuizDetails() {
        if (roomId == null || studentId == null) return;

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
                            public void onDataChange(@NonNull DataSnapshot quizSnap) {
                                String quizName = quizSnap.child("quizName").getValue(String.class);
                                tvQuizTitle.setText(quizName + " Results");

                                DataSnapshot studentSnap = snapshot.child("students").child(studentId);
                                long startTime = 0, endTime = System.currentTimeMillis();
                                if (studentSnap.child("startTime").getValue() instanceof Long)
                                    startTime = (Long) studentSnap.child("startTime").getValue();
                                if (studentSnap.child("endTime").getValue() instanceof Long)
                                    endTime = (Long) studentSnap.child("endTime").getValue();

                                tvDuration.setText("Duration: " + ((endTime - startTime) / 60000) + " min");

                                int totalWarnings = 0;
                                Object warnObj = studentSnap.child("totalWarnings").getValue();
                                if (warnObj instanceof Long) totalWarnings = ((Long) warnObj).intValue();
                                else if (warnObj instanceof String) {
                                    try { totalWarnings = Integer.parseInt((String) warnObj); } catch (Exception ignored) {}
                                }
                                tvWarnings.setText("Total Warnings: " + totalWarnings);

                                tvStartTime.setText("Started: " + android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", startTime));
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
