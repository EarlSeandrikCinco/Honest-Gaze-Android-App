package com.example.honestgaze;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout quizContainer;
    private DatabaseReference studentHistoryRef;
    private Map<String, LinearLayout> quizCardMap = new HashMap<>();
    private Map<String, ValueEventListener> listenerMap = new HashMap<>();
    private String persistentStudentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Get persistent student ID
        SharedPreferences prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE);
        persistentStudentId = prefs.getString("studentId", null);

        if (persistentStudentId == null) {
            // No student ID found, show empty state
            Toast.makeText(this, "No quiz history found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/");
        studentHistoryRef = database.getReference("studentHistory").child(persistentStudentId);

        // Find container inside ScrollView
        ScrollView scrollView = findViewById(R.id.scrollViewHistory);
        if (scrollView != null && scrollView.getChildCount() > 0) {
            quizContainer = (LinearLayout) scrollView.getChildAt(0);
        } else {
            // Fallback: create container if it doesn't exist
            quizContainer = new LinearLayout(this);
            quizContainer.setOrientation(LinearLayout.VERTICAL);
            quizContainer.setPadding(16, 16, 16, 16);
            if (scrollView != null) {
                scrollView.addView(quizContainer);
            }
        }

        loadStudentHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all listeners
        for (Map.Entry<String, ValueEventListener> entry : listenerMap.entrySet()) {
            FirebaseDatabase.getInstance("https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("quizzes").child(entry.getKey()).child("isActive")
                    .removeEventListener(entry.getValue());
        }
        listenerMap.clear();
    }

    private void loadStudentHistory() {
        studentHistoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                quizContainer.removeAllViews();
                quizCardMap.clear();
                
                // Remove old listeners
                for (Map.Entry<String, ValueEventListener> entry : listenerMap.entrySet()) {
                    FirebaseDatabase.getInstance("https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("quizzes").child(entry.getKey()).child("isActive")
                            .removeEventListener(entry.getValue());
                }
                listenerMap.clear();

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    TextView emptyText = new TextView(HistoryActivity.this);
                    emptyText.setText("No quiz history found");
                    emptyText.setTextSize(16);
                    emptyText.setPadding(20, 20, 20, 20);
                    quizContainer.addView(emptyText);
                    return;
                }

                FirebaseDatabase database = FirebaseDatabase.getInstance(
                        "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/");

                for (DataSnapshot historySnap : snapshot.getChildren()) {
                    String quizKey = historySnap.getKey();
                    String quizName = historySnap.child("quizName").getValue(String.class);
                    String roomId = historySnap.child("roomId").getValue(String.class);
                    String date = historySnap.child("date").getValue(String.class);

                    if (quizKey == null || quizName == null) continue;

                    // Create card for this quiz
                    LinearLayout card = createQuizCard(quizName, date != null ? date : "No date");
                    quizCardMap.put(quizKey, card);
                    quizContainer.addView(card);

                    // Listen for isActive changes
                    ValueEventListener isActiveListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Boolean isActive = snapshot.getValue(Boolean.class);
                            if (isActive == null) isActive = true;
                            updateCardState(card, isActive, roomId, quizKey);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    };

                    database.getReference("quizzes").child(quizKey).child("isActive")
                            .addValueEventListener(isActiveListener);
                    listenerMap.put(quizKey, isActiveListener);

                    // Set initial state
                    database.getReference("quizzes").child(quizKey).child("isActive")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Boolean isActive = snapshot.getValue(Boolean.class);
                                    if (isActive == null) isActive = true;
                                    updateCardState(card, isActive, roomId, quizKey);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private LinearLayout createQuizCard(String quizName, String date) {
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
        quizLabel.setText(quizName + "\n" + date);
        quizLabel.setTextSize(18);
        quizLabel.setTextColor(0xFF000000);
        card.addView(quizLabel);

        return card;
    }

    private void updateCardState(LinearLayout card, boolean isActive, String roomId, String quizKey) {
        TextView quizLabel = (TextView) card.getChildAt(0);
        String currentText = quizLabel.getText().toString();
        
        if (isActive) {
            // Session is active - disable button
            card.setAlpha(0.5f);
            card.setEnabled(false);
            card.setClickable(false);
            
            // Update text to show status
            if (!currentText.contains("Exam in Progress")) {
                String baseText = currentText.replace("\n(Exam in Progress)", "");
                quizLabel.setText(baseText + "\n(Exam in Progress)");
            }
        } else {
            // Session ended - enable button
            card.setAlpha(1.0f);
            card.setEnabled(true);
            card.setClickable(true);
            
            // Update text
            currentText = currentText.replace("\n(Exam in Progress)", "");
            quizLabel.setText(currentText);
            
            // Set click listener to navigate to ExamSummaryActivity
            card.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, ExamSummaryActivity.class);
                intent.putExtra("ROOM_ID", roomId);
                intent.putExtra("QUIZ_KEY", quizKey);
                startActivity(intent);
            });
        }
    }
}