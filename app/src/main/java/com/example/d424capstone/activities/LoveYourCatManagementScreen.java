package com.example.d424capstone.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.d424capstone.MyApplication;
import com.example.d424capstone.R;
import com.example.d424capstone.database.Repository;
import com.example.d424capstone.entities.Tip;
import com.example.d424capstone.models.TipsAdapter;

import java.util.List;

public class LoveYourCatManagementScreen extends BaseActivity implements TipsAdapter.OnTipInteractionListener {
    private Repository repository;
    private RecyclerView recyclerView;
    private TipsAdapter tipsAdapter;
    private List<Tip> tipList;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_love_your_cat_management_screen);

        repository = MyApplication.getInstance().getRepository(); // Initialize repository instance
        userRole = repository.getCurrentUser().getRole();

        initializeDrawer(); // Initialize the DrawerLayout and ActionBarDrawerToggle
        initializeButtons(); // Initialize buttons and set their click listeners

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.love_your_cat_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load tips
        loadTips();
    }

    private void initializeButtons() {
        Button addTipButton = findViewById(R.id.add_tip_button);
        addTipButton.setOnClickListener(v -> showAddTipDialog());
    }

    private void loadTips() {
        tipList = repository.getAllTips();
        tipsAdapter = new TipsAdapter(tipList, this, true); // Show buttons
        recyclerView.setAdapter(tipsAdapter);
    }

    private void showAddTipDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.item_add_tip, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Add New Tip");

        EditText tipTitleEditText = dialogView.findViewById(R.id.tip_title);
        EditText tipContentEditText = dialogView.findViewById(R.id.tip_content);
        EditText tipSourceEditText = dialogView.findViewById(R.id.tip_source);

        AlertDialog dialog = builder.create();

        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        saveButton.setOnClickListener(v -> {
            String title = tipTitleEditText.getText().toString();
            String content = tipContentEditText.getText().toString();
            String source = tipSourceEditText.getText().toString();

            if (title.isEmpty() || content.isEmpty() || source.isEmpty()) {
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
                return;
            }

            Tip newTip = new Tip(0, title, content, source);
            long tipID = repository.insertTip(newTip); // Get the generated ID
            newTip.setTipID((int) tipID); // Set the ID on the Tip object
            tipList.add(newTip);
            tipsAdapter.notifyItemInserted(tipList.size() - 1);
            Toast.makeText(this, "Tip added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onEditTip(int position) {
        Tip tip = tipList.get(position);
        showEditTipDialog(tip, position);
    }

    @Override
    public void onDeleteTip(int position) {
        Tip tip = tipList.get(position);
        repository.deleteTip(tip);
        tipList.remove(tip);
        tipsAdapter.notifyItemRemoved(position);
        Toast.makeText(this, "Tip deleted", Toast.LENGTH_SHORT).show();
    }

    private void showEditTipDialog(Tip tip, int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.item_add_tip, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Edit Tip");

        EditText tipTitleEditText = dialogView.findViewById(R.id.tip_title);
        EditText tipContentEditText = dialogView.findViewById(R.id.tip_content);
        EditText tipSourceEditText = dialogView.findViewById(R.id.tip_source);

        tipTitleEditText.setText(tip.getTitle());
        tipContentEditText.setText(tip.getContent());
        tipSourceEditText.setText(tip.getSource());

        AlertDialog dialog = builder.create();

        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        saveButton.setOnClickListener(v -> {
            String title = tipTitleEditText.getText().toString();
            String content = tipContentEditText.getText().toString();
            String source = tipSourceEditText.getText().toString();

            if (title.isEmpty() || content.isEmpty() || source.isEmpty()) {
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
                return;
            }

            tip.setTitle(title);
            tip.setContent(content);
            tip.setSource(source);
            repository.updateTip(tip);
            tipsAdapter.notifyItemChanged(position);
            Toast.makeText(this, "Tip updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}