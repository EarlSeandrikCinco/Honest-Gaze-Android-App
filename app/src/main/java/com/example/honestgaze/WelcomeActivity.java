package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class    WelcomeActivity extends AppCompatActivity {

    private Button btnSignUp, btnLogin;

    // TODO Comment out if not testing detection feature
    private static final boolean DEBUG_SKIP_LOGIN = true;

    @Override
    protected void onStart() {
        super.onStart();

        if (DEBUG_SKIP_LOGIN) {
            startActivity(new Intent(this, OngoingExamActivity.class));
            finish();
        }
    }
    // ------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);

        btnSignUp.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, SelectRoleActivity.class)));

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class)));
    }
}
