package com.example.honestgaze;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private TextView txtRole;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;

    private void markError(EditText field) {
        field.setBackgroundResource(R.drawable.edittext_error_border);
    }

    private void clearError(EditText field) {
        field.setBackgroundResource(R.drawable.edittext_normal);
    }

    private void showWarningToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_error, null);

        TextView tvMessage = layout.findViewById(R.id.tvToastError);
        tvMessage.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void clearAllErrors() {
        clearError(etName);
        clearError(etEmail);
        clearError(etPassword);
        clearError(etConfirmPassword);
    }

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

        String role = getIntent().getStringExtra("role");
        txtRole.setText("Registering as "+role+": " );

        btnRegister.setOnClickListener(v -> {

            clearAllErrors();

            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validation: Empty name
            if (name.isEmpty()) {
                markError(etName);
                showWarningToast("Name cannot be empty");
                return;
            }

            // Empty email
            if (email.isEmpty()) {
                markError(etEmail);
                showWarningToast("Email cannot be empty");
                return;
            }

            // Invalid email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                markError(etEmail);
                showWarningToast("Please enter a valid email");
                return;
            }

            // Empty password
            if (password.isEmpty()) {
                markError(etPassword);
                showWarningToast("Please enter a password");
                return;
            }

            // Password not long enough
            if (password.length() < 6) {
                markError(etPassword);
                showWarningToast("Password must be at least 6 characters");
                return;
            }

            // Empty confirm password
            if (confirmPassword.isEmpty()) {
                markError(etConfirmPassword);
                showWarningToast("Please confirm your password");
                return;
            }

            // Password mismatch
            if (!password.equals(confirmPassword)) {
                markError(etConfirmPassword);
                showWarningToast("Passwords do not match");
                return;
            }

            // Success
            showWarningToast("Registering...");

            // TODO: Add Firebase or database logic here
        });
    }
}