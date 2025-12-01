package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    Button btnStartCall, btnViewReplay, btnOngoingExam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        btnStartCall = findViewById(R.id.btnStartCall);
        btnViewReplay = findViewById(R.id.btnViewReplay);
        btnOngoingExam = findViewById(R.id.btnOngoingExam);

        btnStartCall.setOnClickListener(v ->
                startActivity(new Intent(this, VideoCallActivity.class))
        );

        btnViewReplay.setOnClickListener(v -> {
            Intent i = new Intent(this, ReplayViewerActivity.class);
            // Paste your Firebase video URL here
            i.putExtra("videoUrl", "PASTE_FIREBASE_VIDEO_URL_HERE");
            startActivity(i);
        });

        btnOngoingExam.setOnClickListener(v -> {
            Intent i = new Intent(MainMenuActivity.this, OngoingExamActivity.class);
            startActivity(i);
        });
    }
}
