package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OngoingExamActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_exam);

        Button btnEndSession = findViewById(R.id.btnEndSession);
        btnEndSession.setOnClickListener(v -> {
            startActivity(new Intent(OngoingExamActivity.this, SessionSummaryActivity.class));
        });
    }
}
