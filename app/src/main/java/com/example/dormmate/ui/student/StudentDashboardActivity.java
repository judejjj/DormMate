package com.example.dormmate.ui.student;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private RecyclerView rvBentoGrid;
    private FloatingActionButton fabSOS;
    private TextView tvStudentName;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Tile model
    static class BentoTile {
        String icon, label, subLabel;

        BentoTile(String icon, String label, String subLabel) {
            this.icon = icon;
            this.label = label;
            this.subLabel = subLabel;
        }
    }

    private final List<BentoTile> tiles = Arrays.asList(
            new BentoTile("🛏️", "My Room", "View room & report issues"),
            new BentoTile("📄", "Gate Pass", "Apply for leave"),
            new BentoTile("🍽️", "Mess Menu", "Weekly menu & ratings"),
            new BentoTile("💰", "Fees", "View & pay fees"),
            new BentoTile("📋", "Complaints", "Submit a complaint"),
            new BentoTile("🔔", "Notifications", "View announcements"),
            new BentoTile("📜", "Rules", "Hostel rules"),
            new BentoTile("🔍", "Room Finder", "Find a student's room"),
            new BentoTile("🤖", "Chatbot", "AI Assistant"),
            new BentoTile("📷", "Gate Entry/Exit", "Face scan ML"),
            new BentoTile("💬", "Messages", "Chat with others"),
            new BentoTile("👤", "Profile", "My profile"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvBentoGrid = findViewById(R.id.rvBentoGrid);
        fabSOS = findViewById(R.id.fabSOS);
        tvStudentName = findViewById(R.id.tvStudentName);

        // Load student name from Firestore
        if (auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        String email = doc.getString("email") != null ? doc.getString("email")
                                : auth.getCurrentUser().getEmail();
                        tvStudentName.setText(email != null ? email.split("@")[0] : "Student");
                    });
        }

        // Setup Bento Grid
        BentoAdapter adapter = new BentoAdapter(tiles, this::onTileClick);
        rvBentoGrid.setLayoutManager(new GridLayoutManager(this, 2));
        rvBentoGrid.setAdapter(adapter);

        // SOS FAB
        fabSOS.setOnClickListener(v -> showSOSDialog());

        // Logout
        findViewById(R.id.tvLogout).setOnClickListener(v -> {
            auth.signOut();
            android.content.Intent intent = new android.content.Intent(this, com.example.dormmate.MainActivity.class);
            intent.setFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void onTileClick(int position) {
        BentoTile tile = tiles.get(position);
        switch (tile.label) {
            case "My Room":
                openFragment(new RoomDetailsFragment());
                break;
            case "Gate Pass":
                openFragment(new LeaveRequestFragment());
                break;
            case "Mess Menu":
                openFragment(new MessMenuFragment());
                break;
            case "Complaints":
                openFragment(new ComplaintFragment());
                break;
            case "Notifications":
                openFragment(new NotificationsFragment());
                break;
            case "Rules":
                openFragment(new RulesFragment());
                break;
            case "Fees":
                openFragment(new FeesFragment());
                break;
            case "Room Finder":
                startActivity(new android.content.Intent(this, AiRoomFinderActivity.class));
                break;
            case "Chatbot":
                startActivity(new android.content.Intent(this, com.example.dormmate.ui.ChatbotActivity.class));
                break;
            case "Gate Entry/Exit":
                startActivity(new android.content.Intent(this, FaceScanPlaceholderActivity.class));
                break;
            case "Messages":
                startActivity(new android.content.Intent(this, com.example.dormmate.ui.ChatListActivity.class));
                break;
            case "Profile":
                openFragment(new ProfileFragment());
                break;
        }
    }

    private void openFragment(androidx.fragment.app.Fragment fragment) {
        View container = findViewById(R.id.studentContainer);
        container.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.studentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                findViewById(R.id.studentContainer).setVisibility(View.GONE);
            }
        } else {
            super.onBackPressed();
        }
    }

    private void showSOSDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🚨 Emergency SOS")
                .setMessage(
                        "Are you sure you want to send an Emergency Alert?\nThe warden will be notified immediately.")
                .setPositiveButton("SEND SOS", (dialog, which) -> triggerSOS())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void triggerSOS() {
        if (auth.getCurrentUser() == null)
            return;
        Map<String, Object> sosData = new HashMap<>();
        sosData.put("studentUid", auth.getCurrentUser().getUid());
        sosData.put("email", auth.getCurrentUser().getEmail());
        sosData.put("timestamp", com.google.firebase.Timestamp.now());
        sosData.put("status", "active");

        db.collection("emergency").document("global")
                .set(sosData)
                .addOnSuccessListener(
                        unused -> Toast.makeText(this, "🚨 SOS Sent! Help is on the way.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Failed to send SOS: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Bento RecyclerView Adapter
    interface TileClickListener {
        void onTileClick(int position);
    }

    static class BentoAdapter extends RecyclerView.Adapter<BentoAdapter.ViewHolder> {
        private final List<BentoTile> tiles;
        private final TileClickListener listener;

        BentoAdapter(List<BentoTile> tiles, TileClickListener listener) {
            this.tiles = tiles;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater(parent).inflate(R.layout.item_bento_tile, parent, false);
            return new ViewHolder(view);
        }

        private static android.view.LayoutInflater getLayoutInflater(ViewGroup parent) {
            return android.view.LayoutInflater.from(parent.getContext());
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BentoTile tile = tiles.get(position);
            holder.tvIcon.setText(tile.icon);
            holder.tvLabel.setText(tile.label);
            holder.tvSubLabel.setText(tile.subLabel);
            holder.itemView.setOnClickListener(v -> listener.onTileClick(position));
        }

        @Override
        public int getItemCount() {
            return tiles.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvLabel, tvSubLabel;

            ViewHolder(@NonNull View view) {
                super(view);
                tvIcon = view.findViewById(R.id.tvTileIcon);
                tvLabel = view.findViewById(R.id.tvTileLabel);
                tvSubLabel = view.findViewById(R.id.tvTileSubLabel);
            }
        }
    }
}
