package com.example.honestgaze;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class QuizDetailActivity extends AppCompatActivity {

    private LinearLayout studentLogsContainer;
    private DatabaseReference roomRef;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Simple ScrollView with LinearLayout programmatically
        ScrollView scrollView = new ScrollView(this);
        studentLogsContainer = new LinearLayout(this);
        studentLogsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(studentLogsContainer);
        setContentView(scrollView);

        // Get roomId from intent
        roomId = getIntent().getStringExtra("ROOM_ID");

        // Firebase room reference
        roomRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("rooms").child(roomId);

        // Listen to students data in real-time
        roomRef.child("students").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentLogsContainer.removeAllViews();

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String studentName = studentSnap.child("name").getValue(String.class);
                    Long totalWarnings = studentSnap.child("totalWarnings").getValue(Long.class);
                    if (totalWarnings == null) totalWarnings = 0L;

                    // Student header
                    TextView studentHeader = new TextView(QuizDetailActivity.this);
                    studentHeader.setText(studentName + " - Total Warnings: " + totalWarnings);
                    studentHeader.setTextSize(18);
                    studentHeader.setPadding(20, 20, 20, 10);
                    studentLogsContainer.addView(studentHeader);

                    // Student events/logs
                    LinearLayout logsLayout = new LinearLayout(QuizDetailActivity.this);
                    logsLayout.setOrientation(LinearLayout.VERTICAL);
                    logsLayout.setPadding(40, 0, 20, 20);

                    for (DataSnapshot eventSnap : studentSnap.child("events").getChildren()) {
                        TextView eventLabel = new TextView(QuizDetailActivity.this);
                        eventLabel.setText("• " + eventSnap.getValue(String.class));
                        eventLabel.setTextSize(14);
                        logsLayout.addView(eventLabel);
                    }

                    studentLogsContainer.addView(logsLayout);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
