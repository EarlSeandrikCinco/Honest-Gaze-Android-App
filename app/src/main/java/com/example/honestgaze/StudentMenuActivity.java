package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentMenuActivity extends AppCompatActivity {

    private EditText etQuizLink;
    private Button btnEnterQuiz, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_menu);

        etQuizLink = findViewById(R.id.etQuizLink);
        btnEnterQuiz = findViewById(R.id.btnEnterQuiz);
        btnHistory = findViewById(R.id.btnHistory);

        btnEnterQuiz.setOnClickListener(v -> {
            String roomLink = etQuizLink.getText().toString().trim();

            if (roomLink.isEmpty()) {
                Toast.makeText(this, "Please enter a room link", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the room exists in Firebase
            DatabaseReference roomRef = FirebaseDatabase.getInstance(
                    "https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/"
            ).getReference("rooms").child(roomLink);

            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String status = snapshot.child("status").getValue(String.class);
                        if (status != null && status.equals("ended")) {
                            Toast.makeText(StudentMenuActivity.this, "This session has ended", Toast.LENGTH_SHORT).show();
                            return; // Don't enter
                        }

                        // Room exists and is active
                        Intent intent = new Intent(StudentMenuActivity.this, OngoingExamActivity.class);
                        intent.putExtra("ROOM_ID", roomLink);
                        startActivity(intent);
                    } else {
                        Toast.makeText(StudentMenuActivity.this, "Invalid room link", Toast.LENGTH_SHORT).show();
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(StudentMenuActivity.this, "Error checking room: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(StudentMenuActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}
