package com.example.honestgaze;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RoomMonitorActivity extends AppCompatActivity {

    private TextView roomLabel;
    private Button btnEndSession;
    private RecyclerView recyclerView;
    private StudentMonitorAdapter adapter;
    private List<StudentMonitorModel> studentList;

    private String roomId;
    private DatabaseReference roomRef;
    private DatabaseReference roomRootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_monitor);

        roomLabel = findViewById(R.id.roomLabel);
        btnEndSession = findViewById(R.id.btnEndSession);
        recyclerView = findViewById(R.id.recyclerViewStudents);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentList = new ArrayList<>();
        adapter = new StudentMonitorAdapter(studentList);
        recyclerView.setAdapter(adapter);

        roomId = getIntent().getStringExtra("ROOM_ID");
        if (roomId == null || roomId.isEmpty()) finish();

        roomLabel.setText("Room ID: " + roomId);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/");
        roomRootRef = database.getReference("rooms").child(roomId);
        roomRef = roomRootRef.child("students");

        // Live updates
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot studentSnap : snapshot.getChildren()) {
                    String name = studentSnap.child("name").getValue(String.class);
                    Long totalWarnings = studentSnap.child("totalWarnings").getValue(Long.class);
                    int eventsCount = (int) (studentSnap.child("events").getChildrenCount());

                    studentList.add(new StudentMonitorModel(
                            name != null ? name : "Unknown",
                            totalWarnings != null ? totalWarnings.intValue() : 0,
                            eventsCount
                    ));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        btnEndSession.setOnClickListener(v -> endSession());
    }

    private void endSession() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://honest-gaze-default-rtdb.asia-southeast1.firebasedatabase.app/");
        
        // First, get the quizKey from the room
        roomRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String quizKey = snapshot.child("quizKey").getValue(String.class);
                long endTime = System.currentTimeMillis();
                
                // Update room status
                roomRootRef.child("status").setValue("ended");
                roomRootRef.child("active").setValue(false);
                
                // Update quiz node: set isActive=false and endTime
                if (quizKey != null) {
                    database.getReference("quizzes").child(quizKey).child("isActive").setValue(false);
                    database.getReference("quizzes").child(quizKey).child("endTime").setValue(endTime);
                }
                
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Still try to end the room session even if quiz update fails
                roomRootRef.child("status").setValue("ended");
                roomRootRef.child("active").setValue(false);
                finish();
            }
        });
    }
}
