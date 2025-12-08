package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProctorMenu extends AppCompatActivity {

    private ImageButton imageButton3;
    private ImageButton imageButton5;
    private ImageButton imageButton4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proctor_menu);

        imageButton3 = findViewById(R.id.imageButton3);
        imageButton5 = findViewById(R.id.imageButton5);
        imageButton4 = findViewById(R.id.imageButton4);
        TextView textView = findViewById(R.id.textView);

        // Top button → CreateQuiz
        imageButton3.setOnClickListener(v -> {
            Intent intent = new Intent(ProctorMenu.this, CreateQuiz.class);
            startActivity(intent);
        });

        // Bottom button → SummaryScreen
        imageButton5.setOnClickListener(v -> {
            Intent intent = new Intent(ProctorMenu.this, SummaryScreen.class);
            startActivity(intent);
        });

        // Optional small button
        imageButton4.setOnClickListener(v -> {
            // Add action here if needed
        });
    }

}
