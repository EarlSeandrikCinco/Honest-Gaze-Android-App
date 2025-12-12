package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignUp;

    FirebaseAuth mAuth;

    private void showErrorToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_error, null);

        TextView tvMessage = layout.findViewById(R.id.tvToastError);
        tvMessage.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);

        String autoEmail = getIntent().getStringExtra("AUTO_EMAIL");
        if (autoEmail != null) {
            etEmail.setText(autoEmail);
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Sign in with Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if(user != null) {
                                String emailStr = user.getEmail();
                                if(emailStr != null) {
                                    String name;

                                    if(emailStr.startsWith("prof_")) {
                                        // Extract first name for professor
                                        name = emailStr.substring(5, emailStr.indexOf('@')); // after "prof_"
                                        Intent intent = new Intent(LoginActivity.this, ProctorMenu.class);
                                        intent.putExtra("USERNAME", name);
                                        startActivity(intent);
                                        finish();
                                    } else if(emailStr.startsWith("student_")) {
                                        // Extract first name for student
                                        name = emailStr.substring(8, emailStr.indexOf('@')); // after "student_"
                                        Intent intent = new Intent(LoginActivity.this, StudentMenuActivity.class);
                                        intent.putExtra("USERNAME", name);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else {
                                        showErrorToast("Invalid account type");
                                    }
                                }
                            }
                        } else {
                            // Login failed
                            showErrorToast("No account exists or invalid credentials");
                        }
                    });
        });

        String signUpText = "Don't have an account? Sign up";
        SpannableString spannable = new SpannableString(signUpText);
        int signUpStart = signUpText.indexOf("Sign up");
        if (signUpStart >= 0) {
            int signUpEnd = signUpStart + "Sign up".length();
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#77CCFF")), signUpStart, signUpEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), signUpStart, signUpEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvSignUp.setText(spannable);
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SelectRoleActivity.class);
            startActivity(intent);
        });

    }
}
