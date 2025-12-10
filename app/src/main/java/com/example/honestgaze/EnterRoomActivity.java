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

public class EnterRoomActivity extends AppCompatActivity {

    private EditText roomInput, nameInput;
    private Button btnJoin;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_room);

        roomInput = findViewById(R.id.roomInput);
        nameInput = findViewById(R.id.nameInput);
        btnJoin = findViewById(R.id.btnJoin);

        btnJoin.setOnClickListener(v -> {
            String roomId = roomInput.getText().toString().trim();
            String studentName = nameInput.getText().toString().trim();
            if (roomId.isEmpty()) {
                Toast.makeText(this, "Enter a room ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (studentName.isEmpty()) studentName = "Student";

            // Validate room exists
            DatabaseReference roomRef = FirebaseDatabase.getInstance()
                    .getReference("rooms").child(roomId);

            String finalStudentName = studentName;
            roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Valid room, start exam activity
                        Intent intent = new Intent(EnterRoomActivity.this, OngoingExamActivity.class);
                        intent.putExtra("ROOM_ID", roomId);
                        intent.putExtra("STUDENT_NAME", finalStudentName);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(EnterRoomActivity.this, "Room ID invalid. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
    }
}
