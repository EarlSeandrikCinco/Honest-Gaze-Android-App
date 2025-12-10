package com.example.honestgaze;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

        database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("quizzes");

        quizContainer = findViewById(R.id.buttonContainer);
        backButton = findViewById(R.id.backButton);
        searchInput = findViewById(R.id.searchInput);

        backButton.setOnClickListener(v -> finish());

        loadQuizzes();

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
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allQuizzes.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Quiz quiz = data.getValue(Quiz.class);
                    if (quiz != null) {
                        allQuizzes.add(quiz);
                    }
                }
                displayQuizzes(allQuizzes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
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
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(25, 25, 25, 25);
            card.setBackgroundColor(0xFFE8E8E8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 25, 0, 0);
            card.setLayoutParams(params);

            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            card.setForeground(ContextCompat.getDrawable(this, outValue.resourceId));

            TextView quizLabel = new TextView(this);
            quizLabel.setText(quiz.getQuizName() + "\n" + quiz.getDateTime());
            quizLabel.setTextSize(18);
            quizLabel.setTextColor(0xFF000000);
            card.addView(quizLabel);

            // Show summary when clicked
            card.setOnClickListener(v -> showSummaryDialog(quiz.getRoomId()));

            quizContainer.addView(card);
        }
    }

    private void showSummaryDialog(String roomId) {
        if (roomId == null || roomId.isEmpty()) {
            // Room ID invalid, show toast
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error")
                    .setMessage("Invalid room ID. Cannot fetch summary.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        DatabaseReference roomRef = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("rooms").child(roomId).child("students");

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SummaryScreen.this);
                    builder.setTitle("No Data")
                            .setMessage("No students found for this room.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                StringBuilder summary = new StringBuilder();

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String name = studentSnap.child("name").getValue(String.class);
                    Long totalWarnings = studentSnap.child("totalWarnings").getValue(Long.class);
                    summary.append(name != null ? name : "Unknown")
                            .append(" - Warnings: ")
                            .append(totalWarnings != null ? totalWarnings : 0)
                            .append("\n");

                    DataSnapshot eventsSnap = studentSnap.child("events");
                    if (eventsSnap.exists()) {
                        summary.append("Events:\n");
                        for (DataSnapshot event : eventsSnap.getChildren()) {
                            summary.append("  • ").append(event.getValue(String.class)).append("\n");
                        }
                    }
                    summary.append("\n");
                }

                // Scrollable TextView
                TextView summaryView = new TextView(SummaryScreen.this);
                summaryView.setText(summary.toString());
                summaryView.setTextSize(16);
                summaryView.setPadding(30, 30, 30, 30);

                ScrollView scrollView = new ScrollView(SummaryScreen.this);
                scrollView.addView(summaryView);

                AlertDialog.Builder builder = new AlertDialog.Builder(SummaryScreen.this);
                builder.setTitle("Exam Summary")
                        .setView(scrollView)
                        .setPositiveButton("Close", null)
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
