package com.example.d424capstone.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.d424capstone.R;
import com.example.d424capstone.database.Repository;
import com.example.d424capstone.entities.User;
import com.example.d424capstone.utilities.UserRoles;

public class UserProfileScreen extends BaseActivity {
    private Repository repository;
    private SharedPreferences sharedPreferences;
    private EditText emailEditText, firstNameEditText, lastNameEditText, passwordEditText, phoneNumberEditText;
    private TextView userBanner;
    private Button saveButton, cancelButton, loginButton;

    private boolean isSignUpMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile_screen);

        repository = new Repository(getApplication());
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        initViews();
        initializeDrawer();

        // Determine if it's sign-up mode or profile mode
        isSignUpMode = getIntent().getBooleanExtra("isSignUpMode", false);
        setupUI();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        emailEditText = findViewById(R.id.email);
        firstNameEditText = findViewById(R.id.firstName);
        lastNameEditText = findViewById(R.id.lastName);
        passwordEditText = findViewById(R.id.password);
        phoneNumberEditText = findViewById(R.id.phone_number);
        saveButton = findViewById(R.id.save_user);
        cancelButton = findViewById(R.id.cancel_user);
        loginButton = findViewById(R.id.login_button);
        setupPasswordVisibilityToggle();
    }

    private void setupUI() {
        if (isSignUpMode) {
            setupSignUpMode();
        } else {
            setupProfileMode();
        }
    }

    private void setupSignUpMode() {
        saveButton.setText("Sign Up");
        loginButton.setVisibility(View.VISIBLE);
        loginButton.setOnClickListener(v -> startActivity(new Intent(UserProfileScreen.this, UserLoginScreen.class)));
        saveButton.setOnClickListener(this::handleSignUp);
    }

    private void setupProfileMode() {
        saveButton.setText("Save");
        loginButton.setVisibility(View.GONE);
        loadUserProfile();
        saveButton.setOnClickListener(this::handleProfileUpdate);
    }

    private void setupPasswordVisibilityToggle() {
        ImageButton togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        togglePasswordVisibility.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.baseline_visibility_24);
            } else {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePasswordVisibility.setImageResource(R.drawable.baseline_visibility_off_24);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });
    }

    private void handleSignUp(View view) {
        String email = emailEditText.getText().toString();
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String phone = phoneNumberEditText.getText().toString();

        if (!validateInput(email, firstName, lastName, password)) {
            return;
        }

        new Thread(() -> {
            User existingUser = repository.getUserByEmail(email);
            runOnUiThread(() -> {
                if (existingUser != null) {
                    showAlert("Registration Error", "Email already exists.");
                } else {
                    User user = new User(0, firstName, lastName, email, phone, password, UserRoles.REGULAR);
                    repository.insertUser(user);
                    showToast("Sign up successful");
                    startActivity(new Intent(UserProfileScreen.this, HomeScreen.class));
                    finish();
                }
            });
        }).start();
    }

    private void handleProfileUpdate(View view) {
        int userID = sharedPreferences.getInt("LoggedInUserID", -1);
        if (userID == -1) {
            showToast("Invalid user ID");
            return;
        }

        String email = emailEditText.getText().toString();
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String phone = phoneNumberEditText.getText().toString();

        if (!validateInput(email, firstName, lastName, password)) {
            return;
        }

        new Thread(() -> {
            User user = new User(userID, firstName, lastName, email, phone, password, UserRoles.REGULAR);
            repository.updateUser(user);
            runOnUiThread(() -> {
                showToast("Profile updated successfully");
                finish();
            });
        }).start();
    }

    private void loadUserProfile() {
        int userID = sharedPreferences.getInt("LoggedInUserID", -1);
        if (userID == -1) {
            showToast("Invalid user ID");
            finish();
            return;
        }

        new Thread(() -> {
            User user = repository.getUserByID(userID);
            if (user != null) {
                runOnUiThread(() -> {
                    firstNameEditText.setText(user.getFirstName());
                    lastNameEditText.setText(user.getLastName());
                    emailEditText.setText(user.getEmail());
                    phoneNumberEditText.setText(user.getPhone());
                    passwordEditText.setText(user.getPassword());
                });
            } else {
                runOnUiThread(() -> {
                    showToast("User not found");
                    finish();
                });
            }
        }).start();
    }

    private boolean validateInput(String email, String firstName, String lastName, String password) {
        if (email.isEmpty() || !isValidEmail(email)) {
            showAlert("Email Input Error", "Please enter a valid email.\nEnsure format is test@test.test");
            return false;
        }
        if (firstName.isEmpty() || !isAlphabetic(firstName)) {
            showAlert("First Name Input Error", "Please enter a valid first name containing only alphabetic characters.");
            return false;
        }
        if (lastName.isEmpty() || !isAlphabetic(lastName)) {
            showAlert("Last Name Input Error", "Please enter a valid last name containing only alphabetic characters.");
            return false;
        }
        if (password.isEmpty()) {
            showAlert("Password Input Error", "Please enter a password.");
            return false;
        }
        if (!isPasswordValid(password)) {
            showAlert("Password Format Error", "Password must be at least 8 characters, contain at least one digit, one upper case letter, one lower case letter, and one special character.");
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isAlphabetic(String text) {
        return text.matches("[a-zA-Z]+");
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 8 &&
                password.length() <= 12 &&
                password.matches(".*\\d.*") &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[!@#$%^&+=?-].*");
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(UserProfileScreen.this, message, Toast.LENGTH_LONG).show());
    }
}