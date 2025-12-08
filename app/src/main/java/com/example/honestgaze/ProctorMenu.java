package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProctorMenu extends AppCompatActivity {

    private ImageButton imageButton3;
    private ImageButton imageButton5;
    private ImageButton imageButton4;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proctor_menu);   // Make sure this matches your XML file name

        imageButton3 = findViewById(R.id.imageButton3);
        imageButton5 = findViewById(R.id.imageButton5);
        imageButton4 = findViewById(R.id.imageButton4);
        textView = findViewById(R.id.textView);

        // Set "Hello, Name"
        setUserName();

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

    private void setUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String email = user.getEmail();

            if (email != null && email.contains("@")) {
                // Extract before '@'
                String beforeAt = email.substring(0, email.indexOf("@"));

                // Remove known prefixes
                beforeAt = beforeAt.replace("prof_", "")
                        .replace("student_", "");

                // Add spaces before capital letters (CamelCase / PascalCase)
                String spacedName = beforeAt.replaceAll("(?<!^)([A-Z])", " $1");

                // Display
                textView.setText("Hello, " + spacedName);
            }
        }
    }
}
