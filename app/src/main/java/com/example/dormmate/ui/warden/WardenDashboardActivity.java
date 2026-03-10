package com.example.dormmate.ui.warden;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dormmate.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class WardenDashboardActivity extends AppCompatActivity {

        private FirebaseFirestore db;
        private FirebaseAuth auth;
        private PieChart pieChartOccupancy;
        private BarChart barChartComplaints;
        private TextView tvWardenName, tvOccupancy, tvPendingLeaves;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_warden_dashboard);

                db = FirebaseFirestore.getInstance();
                auth = FirebaseAuth.getInstance();

                tvWardenName = findViewById(R.id.tvWardenName);
                tvOccupancy = findViewById(R.id.tvOccupancy);
                tvPendingLeaves = findViewById(R.id.tvPendingLeaves);
                pieChartOccupancy = findViewById(R.id.pieChartOccupancy);
                barChartComplaints = findViewById(R.id.barChartComplaints);

                // Chart clicks
                barChartComplaints.setOnClickListener(
                                v -> startActivity(new Intent(this, WardenComplaintsActivity.class)));
                pieChartOccupancy.setOnClickListener(v -> startActivity(new Intent(this, StudentListActivity.class)));

                // Logout
                findViewById(R.id.tvLogout).setOnClickListener(v -> {
                        auth.signOut();
                        android.content.Intent intent = new android.content.Intent(this,
                                        com.example.dormmate.MainActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                });

                // Tile navigation
                setupTiles();

                // Load data
                loadWardenName();
                loadDashboardStats();
        }

        private void setupTiles() {
                // Quick Actions
                findViewById(R.id.tileStudents)
                                .setOnClickListener(v -> startActivity(new Intent(this, StudentListActivity.class)));
                findViewById(R.id.tileLeave)
                                .setOnClickListener(v -> startActivity(new Intent(this, WardenLeaveActivity.class)));
                findViewById(R.id.tileComplaints).setOnClickListener(
                                v -> startActivity(new Intent(this, WardenComplaintsActivity.class)));
                findViewById(R.id.tileVisitor)
                                .setOnClickListener(v -> startActivity(new Intent(this, VisitorLogActivity.class)));
                findViewById(R.id.tileBroadcast)
                                .setOnClickListener(v -> startActivity(new Intent(this, BroadcastActivity.class)));
                findViewById(R.id.tileRules)
                                .setOnClickListener(v -> startActivity(new Intent(this, WardenRulesActivity.class)));
                findViewById(R.id.tileFees).setOnClickListener(v -> Toast.makeText(this,
                                "Fees Management — assign fees per student in Firestore", Toast.LENGTH_LONG).show());
                findViewById(R.id.tileMessMenu)
                                .setOnClickListener(v -> startActivity(new Intent(this, WardenMessMenuActivity.class)));
                findViewById(R.id.tileChatbot)
                                .setOnClickListener(v -> startActivity(
                                                new Intent(this, com.example.dormmate.ui.ChatbotActivity.class)));
                findViewById(R.id.tileMessPredict)
                                .setOnClickListener(v -> showMessPredictionDialog());
                findViewById(R.id.tileMessages)
                                .setOnClickListener(v -> startActivity(
                                                new Intent(this, com.example.dormmate.ui.ChatListActivity.class)));

                // Card navigation (Stats cards)
                findViewById(R.id.cardOccupancy)
                                .setOnClickListener(v -> startActivity(new Intent(this, StudentListActivity.class)));
                findViewById(R.id.cardLeaves)
                                .setOnClickListener(v -> startActivity(new Intent(this, WardenLeaveActivity.class)));
        }

        private void showMessPredictionDialog() {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Mess Demand Prediction (ML)");
                builder.setMessage("This is mock data from our predictive model.\n\n" +
                                "Total Students: 200\n" +
                                "On Leave: 30\n" +
                                "Predicted Dinner: 170\n\n" +
                                "(Backend ML integration pending)");
                builder.setPositiveButton("OK", null);
                builder.show();
        }

        private void loadWardenName() {
                if (auth.getCurrentUser() == null)
                        return;
                db.collection("users").document(auth.getCurrentUser().getUid()).get()
                                .addOnSuccessListener(doc -> {
                                        String name = doc.getString("name");
                                        if (name != null)
                                                tvWardenName.setText("Warden " + name);
                                })
                                .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Profile load failed: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                });
        }

        private void loadDashboardStats() {
                // Occupancy: count users with role=Student
                db.collection("users").whereEqualTo("role", "Student").get()
                                .addOnSuccessListener(snap -> {
                                        int occupied = snap.size();
                                        int total = 100; // Configure based on hostel capacity
                                        int empty = Math.max(0, total - occupied);
                                        tvOccupancy.setText(occupied + "/" + total);
                                        setupPieChart(occupied, empty);
                                })
                                .addOnFailureListener(e -> {
                                        tvOccupancy.setText("Error");
                                        setupPieChart(0, 100);
                                        Toast.makeText(this, "Users query failed: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                });

                // Pending leaves
                db.collection("leave_requests").whereEqualTo("status", "Pending").get()
                                .addOnSuccessListener(snap -> {
                                        tvPendingLeaves.setText(String.valueOf(snap.size()));
                                })
                                .addOnFailureListener(e -> {
                                        tvPendingLeaves.setText("!");
                                        Toast.makeText(this, "Leaves query failed", Toast.LENGTH_SHORT).show();
                                });

                // Complaint counts for bar chart
                db.collection("complaints").whereEqualTo("status", "Open").get()
                                .addOnSuccessListener(openSnap -> {
                                        int open = openSnap.size();
                                        db.collection("complaints").whereEqualTo("status", "Resolved").get()
                                                        .addOnSuccessListener(resolvedSnap -> setupBarChart(open,
                                                                        resolvedSnap.size()))
                                                        .addOnFailureListener(e -> setupBarChart(open, 0));
                                })
                                .addOnFailureListener(e -> {
                                        setupBarChart(0, 0);
                                        Toast.makeText(this, "Complaints query failed", Toast.LENGTH_SHORT).show();
                                });
        }

        private void setupPieChart(int occupied, int empty) {
                List<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(occupied, "Occupied"));
                entries.add(new PieEntry(empty, "Empty"));

                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(0xFF7C4DFF, 0xFF2D2B55);
                dataSet.setValueTextColor(Color.WHITE);
                dataSet.setValueTextSize(12f);
                dataSet.setSliceSpace(3f);

                PieData data = new PieData(dataSet);
                pieChartOccupancy.setData(data);
                pieChartOccupancy.setHoleColor(Color.TRANSPARENT);
                pieChartOccupancy.setHoleRadius(40f);
                pieChartOccupancy.setTransparentCircleRadius(45f);
                pieChartOccupancy.getDescription().setEnabled(false);
                pieChartOccupancy.getLegend().setTextColor(Color.WHITE);
                pieChartOccupancy.setEntryLabelColor(Color.WHITE);
                pieChartOccupancy.animateY(800);
                pieChartOccupancy.invalidate();
        }

        private void setupBarChart(int open, int resolved) {
                List<BarEntry> entries = new ArrayList<>();
                entries.add(new BarEntry(0f, open));
                entries.add(new BarEntry(1f, resolved));

                BarDataSet dataSet = new BarDataSet(entries, "Complaints");
                dataSet.setColors(0xFFEF5350, 0xFF4CAF50);
                dataSet.setValueTextColor(Color.WHITE);
                dataSet.setValueTextSize(12f);

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.5f);

                XAxis xAxis = barChartComplaints.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[] { "Open", "Resolved" }));
                xAxis.setGranularity(1f);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setTextColor(Color.WHITE);
                xAxis.setDrawGridLines(false);

                barChartComplaints.getAxisLeft().setTextColor(Color.WHITE);
                barChartComplaints.getAxisRight().setEnabled(false);
                barChartComplaints.getDescription().setEnabled(false);
                barChartComplaints.getLegend().setEnabled(false);
                barChartComplaints.setFitBars(true);
                barChartComplaints.setData(data);
                barChartComplaints.setBackgroundColor(Color.TRANSPARENT);
                barChartComplaints.animateY(800);
                barChartComplaints.invalidate();
        }
}
