package com.example.honestgaze;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateQuiz extends AppCompatActivity {

    private static final String TAG = "CreateQuizDebug";

    private EditText inputField, inputField2, inputField3, inputField4;
    private ImageButton moreInput2, moreInput3, moreInput4, backButton;
    private Button createQuizButton;
    private DatabaseReference database;

    private int gracePeriod = 3;
    private int warningCooldown = 1;
    private int numberOfWarnings = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        // Firebase reference
        database = FirebaseDatabase.getInstance("https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("quizzes");


        // Inputs
        inputField = findViewById(R.id.inputField);
        inputField2 = findViewById(R.id.inputField2);
        inputField3 = findViewById(R.id.inputField3);
        inputField4 = findViewById(R.id.inputField4);

        // More buttons
        moreInput2 = findViewById(R.id.btnMore2);
        moreInput3 = findViewById(R.id.btnMore3);
        moreInput4 = findViewById(R.id.btnMore4);

        // Back button
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupMoreButtons();

        // Create quiz button
        createQuizButton = findViewById(R.id.button2);
        createQuizButton.setOnClickListener(v -> {
            Log.d(TAG, "Create Quiz button clicked");
            if (validateInputs()) {
                Toast.makeText(this, "Valid inputs. Saving to Firebase...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Inputs valid, calling saveQuizToFirebase()");
                saveQuizToFirebase();
            } else {
                Log.d(TAG, "Validation failed");
            }
        });
    }

    private void setupMoreButtons() {
        moreInput2.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, moreInput2);
            popup.getMenu().add("Set Grace Period (1-30s)");
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(this, "Set Grace Period clicked", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        moreInput3.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, moreInput3);
            popup.getMenu().add("Set Warning Cooldown (≥ Grace Period, ≤ 60s)");
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(this, "Set Warning Cooldown clicked", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        moreInput4.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, moreInput4);
            popup.getMenu().add("Set Number of Warnings (2-10)");
            popup.setOnMenuItemClickListener(item -> {
                Toast.makeText(this, "Set Number of Warnings clicked", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });
    }

    private boolean validateInputs() {
        // Grace Period
        try {
            int val2 = Integer.parseInt(inputField2.getText().toString().trim());
            if (val2 < 1 || val2 > 30) {
                Toast.makeText(this, "Grace Period must be 1-30 seconds", Toast.LENGTH_SHORT).show();
                return false;
            }
            gracePeriod = val2;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Grace Period", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Warning Cooldown
        try {
            int val3 = Integer.parseInt(inputField3.getText().toString().trim());
            if (val3 < gracePeriod || val3 > 60) {
                Toast.makeText(this, "Warning Cooldown must be ≥ Grace Period and ≤ 60 seconds", Toast.LENGTH_SHORT).show();
                return false;
            }
            warningCooldown = val3;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Warning Cooldown", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Number of Warnings
        try {
            int val4 = Integer.parseInt(inputField4.getText().toString().trim());
            if (val4 < 2 || val4 > 10) {
                Toast.makeText(this, "Number of Warnings must be 2-10", Toast.LENGTH_SHORT).show();
                return false;
            }
            numberOfWarnings = val4;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Number of Warnings", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Quiz Name
        if (inputField.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a quiz name", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveQuizToFirebase() {
        String quizName = inputField.getText().toString().trim();
        String dateTime = new java.text.SimpleDateFormat(
                "MMM dd, yyyy HH:mm", java.util.Locale.getDefault()
        ).format(new java.util.Date());

        Quiz quiz = new Quiz(quizName, dateTime, gracePeriod, warningCooldown, numberOfWarnings);

        String key = database.push().getKey();
        Log.d(TAG, "Generated key: " + key);

        if (key == null) {
            Toast.makeText(this, "Unexpected error: key is null", Toast.LENGTH_SHORT).show();
            return;
        }

        database.child(key).setValue(quiz)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateQuiz.this, "Quiz created successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Quiz saved successfully to Firebase");
                    inputField.setText("");
                    inputField2.setText("");
                    inputField3.setText("");
                    inputField4.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateQuiz.this, "Failed to create quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to save quiz", e);
                });
    }
}
