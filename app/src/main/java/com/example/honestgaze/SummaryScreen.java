package com.example.honestgaze;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SummaryScreen extends AppCompatActivity {

    private DatabaseReference database;
    private LinearLayout quizContainer;
    private ImageButton backButton;
    private EditText searchInput;

    private List<Quiz> allQuizzes = new ArrayList<>(); // store all quizzes for filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_screen);

        // Firebase reference (use full URL!)
        database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("quizzes");

        quizContainer = findViewById(R.id.buttonContainer); // LinearLayout in XML
        backButton = findViewById(R.id.backButton);
        searchInput = findViewById(R.id.searchInput); // search EditText in XML

        backButton.setOnClickListener(v -> finish());

        loadQuizzes();

        // Search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuizzes(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void loadQuizzes() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allQuizzes.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Quiz quiz = data.getValue(Quiz.class);
                    if (quiz != null) {
                        allQuizzes.add(quiz);
                    }
                }
                // Show all quizzes initially
                displayQuizzes(allQuizzes);
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    private void filterQuizzes(String query) {
        List<Quiz> filtered = new ArrayList<>();
        for (Quiz quiz : allQuizzes) {
            if (quiz.getQuizName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(quiz);
            }
        }
        displayQuizzes(filtered);
    }

    private void displayQuizzes(List<Quiz> quizzes) {
        quizContainer.removeAllViews();
        for (Quiz quiz : quizzes) {
            TextView quizLabel = new TextView(this);
            quizLabel.setText(quiz.getQuizName() + "\n" + quiz.getDateTime());
            quizLabel.setTextSize(18);
            quizLabel.setPadding(25, 25, 25, 25);
            quizLabel.setBackgroundColor(0xFFE8E8E8);
            quizLabel.setTextColor(0xFF000000);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 25, 0, 0);
            quizLabel.setLayoutParams(params);

            quizContainer.addView(quizLabel);
        }
    }
}
