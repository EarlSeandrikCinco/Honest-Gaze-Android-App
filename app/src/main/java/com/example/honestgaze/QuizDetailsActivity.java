package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class QuizDetailsActivity extends AppCompatActivity {

    private TextView tvQuizTitle, tvScore, tvStatus, tvDuration, tvWarnings, tvTimestamp;
    private Button btnViewReplay, btnBackToHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_quiz_details);

        // Initialize views
        tvQuizTitle = findViewById(R.id.tvQuizTitle);
        tvScore = findViewById(R.id.tvScore);
        tvStatus = findViewById(R.id.tvStatus);
        tvDuration = findViewById(R.id.tvDuration);
        tvWarnings = findViewById(R.id.tvWarnings);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        btnViewReplay = findViewById(R.id.btnViewReplay);
        btnBackToHistory = findViewById(R.id.btnBackToHistory);

        // Get quiz name from intent
        String quizName = getIntent().getStringExtra("quizName");
        if (quizName == null) quizName = "Quiz";

        // Set quiz details (placeholder data)
        tvQuizTitle.setText(quizName + " Results");
        tvScore.setText("Score: 85/100");
        tvStatus.setText("Status: Passed");
        tvDuration.setText("Duration: 45 minutes");
        tvWarnings.setText("Warnings: 2");
        tvTimestamp.setText("Completed: Dec 8, 2025 at 3:45 PM");

        // View Replay button
        btnViewReplay.setOnClickListener(v -> {
            Intent intent = new Intent(QuizDetailsActivity.this, ReplayViewerActivity.class);
            // Pass the video URL if available
            intent.putExtra("videoUrl", "PASTE_FIREBASE_VIDEO_URL_HERE");
            startActivity(intent);
        });

        // Back to History button
        btnBackToHistory.setOnClickListener(v -> {
            finish(); // Goes back to HistoryActivity
        });
    }
}