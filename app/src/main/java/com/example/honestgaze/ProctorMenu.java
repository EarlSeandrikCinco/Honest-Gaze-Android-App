package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;

public class ProctorMenu extends AppCompatActivity {

    private CardView cardButton1, cardButton2;
    private ImageButton btnProfile;
    private TextView tvProctorTitle, tvHeader, tvFooter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proctor_menu);

        cardButton1 = findViewById(R.id.cardButton1);
        cardButton2 = findViewById(R.id.cardButton2);
        btnProfile = findViewById(R.id.btnProfile);
        tvProctorTitle = findViewById(R.id.tvProctorTitle);
        tvHeader = findViewById(R.id.tvHeader);
        tvFooter = findViewById(R.id.tvFooter);

        // Card Buttons Navigation
        cardButton1.setOnClickListener(v -> {
            Intent intent = new Intent(ProctorMenu.this, CreateQuiz.class);
            startActivity(intent);
        });

        cardButton2.setOnClickListener(v -> {
            Intent intent = new Intent(ProctorMenu.this, SummaryScreen.class);
            startActivity(intent);
        });
    }
}
