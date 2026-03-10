package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RoomDetailsFragment extends Fragment {

    private TextView tvRoomNumber, tvFloor, tvWing, tvBack;
    private EditText etAssetName, etDamageDescription;
    private Button btnSubmitDamage;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvRoomNumber = view.findViewById(R.id.tvRoomNumber);
        tvFloor = view.findViewById(R.id.tvFloor);
        tvWing = view.findViewById(R.id.tvWing);
        tvBack = view.findViewById(R.id.tvRoomBack);
        etAssetName = view.findViewById(R.id.etAssetName);
        etDamageDescription = view.findViewById(R.id.etDamageDescription);
        btnSubmitDamage = view.findViewById(R.id.btnSubmitDamage);

        tvBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnSubmitDamage.setOnClickListener(v -> submitDamageReport());

        loadRoomDetails();
    }

    private void loadRoomDetails() {
        if (auth.getCurrentUser() == null)
            return;
        db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String room = doc.getString("room");
                    String floor = doc.getString("floor");
                    String wing = doc.getString("wing");
                    tvRoomNumber.setText("Room No: " + (room != null ? room : "Not Assigned"));
                    tvFloor.setText("Floor: " + (floor != null ? floor : "—"));
                    tvWing.setText("Hostel Wing: " + (wing != null ? wing : "—"));
                })
                .addOnFailureListener(e -> Toast
                        .makeText(getContext(), "Error loading room: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void submitDamageReport() {
        String asset = etAssetName.getText().toString().trim();
        String description = etDamageDescription.getText().toString().trim();

        if (asset.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null)
            return;

        Map<String, Object> report = new HashMap<>();
        report.put("studentUid", auth.getCurrentUser().getUid());
        report.put("email", auth.getCurrentUser().getEmail());
        report.put("assetName", asset);
        report.put("description", description);
        report.put("status", "Pending");
        report.put("timestamp", Timestamp.now());

        db.collection("room_issues").add(report)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), "Damage report submitted!", Toast.LENGTH_SHORT).show();
                    etAssetName.setText("");
                    etDamageDescription.setText("");
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
