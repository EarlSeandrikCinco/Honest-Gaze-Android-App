package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SelectRoleActivity extends AppCompatActivity {

    Button btnStudent, btnProctor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        btnStudent = findViewById(R.id.btnStudent);
        btnProctor = findViewById(R.id.btnProctor);

        btnStudent.setOnClickListener(v -> {
            Intent intent = new Intent(SelectRoleActivity.this, RegisterActivity.class);
            intent.putExtra("role", "Student");
            startActivity(intent);
        });

        btnProctor.setOnClickListener(v -> {
            Intent intent = new Intent(SelectRoleActivity.this, RegisterActivity.class);
            intent.putExtra("role", "Proctor");
            startActivity(intent);
        });
    }
}
