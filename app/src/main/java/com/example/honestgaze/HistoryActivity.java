package com.example.honestgaze;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private String studentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // BACK BUTTON
        ImageButton btnBackHistory = findViewById(R.id.btnBackHistory);
        btnBackHistory.setOnClickListener(v -> finish());

        historyContainer = findViewById(R.id.historyContainer);

        studentId = getIntent().getStringExtra("STUDENT_ID");
        if (studentId == null) studentId = "student_default";

        loadStudentHistory();
    }

    private void loadStudentHistory() {
        DatabaseReference quizzesRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("quizzes");

        quizzesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyContainer.removeAllViews();

                for (DataSnapshot quizSnap : snapshot.getChildren()) {

                    String quizName = quizSnap.child("quizName").getValue(String.class);
                    String dateTime = quizSnap.child("dateTime").getValue(String.class);

                    Object warningsObj = quizSnap.child("numberOfWarnings").getValue();
                    int totalWarnings = 0;

                    if (warningsObj instanceof Long)
                        totalWarnings = ((Long) warningsObj).intValue();
                    else if (warningsObj instanceof String) {
                        try {
                            totalWarnings = Integer.parseInt((String) warningsObj);
                        } catch (Exception ignored) {}
                    }

                    TextView tv = new TextView(HistoryActivity.this);

                    String text = quizName != null ? quizName : "Quiz";
                    if (dateTime != null) text += "\nStarted: " + dateTime;

                    text += "\nTotal Warnings: " + totalWarnings;

                    tv.setText(text);
                    tv.setPadding(20, 20, 20, 20);
                    tv.setBackgroundColor(0xFFE8E8E8);

                    historyContainer.addView(tv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
