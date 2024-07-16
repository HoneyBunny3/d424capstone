package com.example.d424capstone.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.d424capstone.MyApplication;
import com.example.d424capstone.R;
import com.example.d424capstone.adapters.CatAdapter;
import com.example.d424capstone.database.Repository;
import com.example.d424capstone.entities.Cat;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class CatProfileScreen extends BaseActivity {
    private static final int PICK_IMAGE_REQUEST = 1; // Request code for image picker
    private Repository repository;
    private TextInputEditText editName, editAge, editBio;
    private ImageView catImageView;
    private Uri catImageUri;
    private Button saveButton, cancelButton;
    private RecyclerView catRecyclerView;
    private CatAdapter catAdapter;
    private int catID = -1; // Default value for new cat profiles
    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cat_profile_screen);

        repository = MyApplication.getInstance().getRepository(); // Initialize repository instance

        initializeDrawer(); // Initialize the DrawerLayout and ActionBarDrawerToggle
        initViews(); // Initialize UI components
        initializeButtons(); // Set up button click listeners
        initializeRecyclerView(); // Initialize RecyclerView for displaying cat profiles

        // Get catID and userID from intent extras
        catID = getIntent().getIntExtra("catID", -1);
        userID = getIntent().getIntExtra("userID", -1);

        if (userID == -1) {
            showToast("Invalid user ID");
            finish();
            return;
        }

        if (catID != -1) {
            loadCatDetails(); // Load cat details if catID is valid
        }

        loadCatsForUser(); // Load cats associated with the user

        // Set window insets for EdgeToEdge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Initialize UI components
    private void initViews() {
        editName = findViewById(R.id.cat_name);
        editAge = findViewById(R.id.cat_age);
        editBio = findViewById(R.id.cat_bio);
        catImageView = findViewById(R.id.cat_image);
        catImageView.setImageResource(R.drawable.baseline_image_search_24); // Set default image
        saveButton = findViewById(R.id.save_cat);
        cancelButton = findViewById(R.id.cancel_cat);
        catRecyclerView = findViewById(R.id.cat_recycler_view);
    }

    // Set up button click listeners
    private void initializeButtons() {
        saveButton.setOnClickListener(view -> saveCatProfile());
        cancelButton.setOnClickListener(view -> finish());
        catImageView.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
    }

    // Initialize RecyclerView for displaying cat profiles
    private void initializeRecyclerView() {
        catRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        catAdapter = new CatAdapter(this, null);
        catRecyclerView.setAdapter(catAdapter);
    }

    // Load cats associated with the user from the repository
    private void loadCatsForUser() {
        new Thread(() -> {
            List<Cat> cats = repository.getCatsForUser(userID);
            runOnUiThread(() -> {
                if (cats != null && !cats.isEmpty()) {
                    catAdapter.setCats(cats);
                } else {
                    showToast("No cats found for this user.");
                }
            });
        }).start();
    }

    // Load cat details from the repository
    private void loadCatDetails() {
        new Thread(() -> {
            Cat cat = repository.getCatByID(catID);
            runOnUiThread(() -> {
                if (cat != null) {
                    editName.setText(cat.getCatName());
                    editAge.setText(String.valueOf(cat.getCatAge()));
                    editBio.setText(cat.getCatBio());
                    if (cat.getCatImage() != null && !cat.getCatImage().isEmpty()) {
                        catImageUri = Uri.parse(cat.getCatImage());
                        catImageView.setImageURI(catImageUri);
                    }
                } else {
                    showToast("Error loading cat details");
                }
            });
        }).start();
    }

    // Save cat profile details to the repository
    private void saveCatProfile() {
        String catName = editName.getText().toString();
        String catAgeStr = editAge.getText().toString();
        String catBio = editBio.getText().toString();

        // Validate input fields
        if (catName.isEmpty()) {
            showToast("Please enter your cat's name");
            return;
        }

        if (catAgeStr.isEmpty()) {
            showToast("Please enter your cat's age");
            return;
        }

        int catAge;
        try {
            catAge = Integer.parseInt(catAgeStr);
            if (catAge < 0) {
                showToast("Please enter a valid age for your cat");
                return;
            }
        } catch (NumberFormatException e) {
            showToast("Please enter a valid age for your cat");
            return;
        }

        if (catBio.isEmpty()) {
            showToast("Please enter a bio for your cat");
            return;
        }

        String imageUriString = (catImageUri != null) ? catImageUri.toString() : "" + getPackageName() + "/" + R.drawable.baseline_image_search_24;

        if (catID == -1) {
            catID = 0;
        }

        Cat cat = new Cat(catID, catName, catAge, imageUriString, catBio, userID);

        new Thread(() -> {
            if (catID == 0) {
                long newCatID = repository.insertCat(cat);
                if (newCatID != -1) {
                    catID = (int) newCatID;
                    runOnUiThread(() -> {
                        showToast("New cat profile created successfully");
                        loadCatsForUser(); // Refresh the cat list
                        clearInputs(); // Clear the input fields
                    });
                } else {
                    runOnUiThread(() -> showToast("Error saving cat profile"));
                }
            } else {
                repository.updateCat(cat);
                runOnUiThread(() -> {
                    showToast("Cat profile updated successfully");
                    loadCatsForUser(); // Refresh the cat list
                });
            }
        }).start();
    }

    // Clear input fields
    private void clearInputs() {
        editName.setText("");
        editAge.setText("");
        editBio.setText("");
        catImageView.setImageResource(R.drawable.baseline_image_search_24);
        catImageUri = null;
    }

    // Show a toast message
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(CatProfileScreen.this, message, Toast.LENGTH_LONG).show());
    }

    // Handle the result from the image picker intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            catImageUri = data.getData();
            catImageView.setImageURI(catImageUri);
        }
    }

    @Override
    protected boolean shouldShowSearch() {
        return false; // Disable the search feature on this activity
    }
}