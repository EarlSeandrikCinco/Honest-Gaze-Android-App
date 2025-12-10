package com.example.honestgaze;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfessorRoomActivity extends AppCompatActivity {

    private TextView roomTitle;
    private Button btnEndSession;
    private LinearLayout studentsContainer;

    private String roomId;
    private DatabaseReference roomRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_room);

        roomId = getIntent().getStringExtra("ROOM_ID");
        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);

        roomTitle = findViewById(R.id.roomTitle);
        btnEndSession = findViewById(R.id.btnEndSession);
        studentsContainer = findViewById(R.id.studentsContainer);

        roomTitle.setText("Room: " + roomId);

        listenToStudents();
        setupEndSessionButton();
    }

    private void listenToStudents() {
        roomRef.child("students").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentsContainer.removeAllViews();

                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String name = studentSnap.child("name").getValue(String.class);
                    Long warnings = studentSnap.child("totalWarnings").getValue(Long.class);
                    String lastEvent = studentSnap.child("lastEvent").getValue(String.class);

                    if (warnings == null) warnings = 0L;
                    if (lastEvent == null) lastEvent = "No events";

                    TextView tv = new TextView(ProfessorRoomActivity.this);
                    tv.setText(name + "\nWarnings: " + warnings + "\nLast event: " + lastEvent);
                    tv.setPadding(15, 15, 15, 25);
                    studentsContainer.addView(tv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupEndSessionButton() {
        btnEndSession.setOnClickListener(v -> {
            // Disable button immediately to prevent double clicks
            btnEndSession.setEnabled(false);

            // Mark room as ended and inactive
            roomRef.child("status").setValue("ended");
            roomRef.child("active").setValue(false)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Session ended successfully", Toast.LENGTH_SHORT).show();

                        // Optionally clear students list in room (forces disconnect)
                        roomRef.child("students").removeValue();

                        // Close professor activity after ending
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to end session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnEndSession.setEnabled(true); // Re-enable if failed
                    });
        });
    }

}

