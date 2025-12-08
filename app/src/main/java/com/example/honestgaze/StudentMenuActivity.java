package com.example.honestgaze;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StudentMenuActivity extends AppCompatActivity {

    private EditText etQuizLink;
    private Button btnEnterQuiz, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_menu);

        etQuizLink = findViewById(R.id.etQuizLink);
        btnEnterQuiz = findViewById(R.id.btnEnterQuiz);
        btnHistory = findViewById(R.id.btnHistory);

        btnEnterQuiz.setOnClickListener(v -> {
            String quizLink = etQuizLink.getText().toString().trim();

            if (quizLink.isEmpty()) {
                Toast.makeText(this, "Please enter a quiz link", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if it's a valid URL
            if (!quizLink.startsWith("http://") && !quizLink.startsWith("https://")) {
                quizLink = "https://" + quizLink;
            }

            // Open quiz with camera monitoring
            Intent intent = new Intent(StudentMenuActivity.this, QuizTakingActivity.class);
            intent.putExtra("quizLink", quizLink);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(StudentMenuActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}