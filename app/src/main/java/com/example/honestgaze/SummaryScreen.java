package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SummaryScreen extends AppCompatActivity {

    private LinearLayout buttonContainer;
    private ImageButton backButton;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_screen);

        buttonContainer = findViewById(R.id.buttonContainer);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> {
            startActivity(new Intent(SummaryScreen.this, ProctorMenu.class));
            finish();
        });

        // Firebase reference
        String proctorEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (proctorEmail == null) proctorEmail = "default_proctor";
        database = FirebaseDatabase.getInstance().getReference("quizzes").child(proctorEmail.replace(".", "_"));

        // Listen for changes
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                buttonContainer.removeAllViews(); // clear old buttons

                for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                    Quiz quiz = quizSnapshot.getValue(Quiz.class);
                    if (quiz != null) {
                        Button quizButton = new Button(SummaryScreen.this);
                        quizButton.setText(quiz.getQuizName() + "\n" + quiz.getDateTime());
                        quizButton.setAllCaps(false);
                        quizButton.setTextSize(16f);

                        // You can add more styling here if you want
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(0, 16, 0, 0);
                        quizButton.setLayoutParams(params);

                        // For now, button does nothing on click
                        quizButton.setOnClickListener(v -> {
                            // Placeholder for future functionality
                        });

                        buttonContainer.addView(quizButton);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optional: handle errors
            }
        });
    }
}
