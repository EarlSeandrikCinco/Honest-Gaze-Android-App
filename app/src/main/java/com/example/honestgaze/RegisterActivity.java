package com.example.honestgaze;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextView txtRole;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtRole = findViewById(R.id.txtRole);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get role from previous activity
        role = getIntent().getStringExtra("role");
        if (role == null) role = "Student"; // default role
        txtRole.setText("Registering as " + role);

        btnRegister.setOnClickListener(v -> registerUser(role));
    }

    private void registerUser(String role) {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation
        if (name.isEmpty()) { showToast("Name cannot be empty"); return; }
        if (email.isEmpty()) { showToast("Email cannot be empty"); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { showToast("Enter valid email"); return; }
        if (password.isEmpty()) { showToast("Enter password"); return; }
        if (password.length() < 6) { showToast("Password must be at least 6 chars"); return; }
        if (!password.equals(confirmPassword)) { showToast("Passwords do not match"); return; }

        // Prepend prefix based on role
        String fullEmail = (role.equalsIgnoreCase("Student") ? "student_" : "prof_") + email;

        // Firebase Auth registration
        mAuth.createUserWithEmailAndPassword(fullEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // Save extra info to Firestore
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("role", role);
                            userMap.put("email", fullEmail);

                            db.collection("users").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> showSuccessDialog(fullEmail))
                                    .addOnFailureListener(e -> showToast("Error saving user info: " + e.getMessage()));
                        }
                    } else {
                        showToast("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private void showSuccessDialog(String fullEmail) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registration Successful!");
        builder.setMessage("Your login email is:\n" + fullEmail + "\n\nPlease use this email to log in.");

        builder.setPositiveButton("OK", (dialog, which) -> {
            // Redirect to LoginActivity with pre-filled email
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("AUTO_EMAIL", fullEmail);
            startActivity(intent);
            finish();
        });

        builder.setCancelable(false); // prevent closing without pressing OK
        builder.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
