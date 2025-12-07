package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Set up click listeners for each quiz button
        findViewById(R.id.btnQuiz1).setOnClickListener(v ->
                openQuizDetails("Quiz 1"));

        findViewById(R.id.btnQuiz2).setOnClickListener(v ->
                openQuizDetails("Quiz 2"));

        findViewById(R.id.btnQuiz3).setOnClickListener(v ->
                openQuizDetails("Quiz 3"));

        findViewById(R.id.btnQuiz4).setOnClickListener(v ->
                openQuizDetails("Quiz 4"));

        findViewById(R.id.btnQuiz5).setOnClickListener(v ->
                openQuizDetails("Quiz 5"));

        findViewById(R.id.btnQuiz6).setOnClickListener(v ->
                openQuizDetails("Quiz 6"));

        findViewById(R.id.btnQuiz7).setOnClickListener(v ->
                openQuizDetails("Quiz 7"));
    }

    private void openQuizDetails(String quizName) {
        Intent intent = new Intent(HistoryActivity.this, QuizDetailsActivity.class);
        intent.putExtra("quizName", quizName);
        startActivity(intent);
    }
}