package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FeesFragment extends Fragment {

    private TextView tvFeeStatus, tvFeeStatusIcon, tvDueDate, tvTotalFee;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fees, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvFeeStatus = view.findViewById(R.id.tvFeeStatus);
        tvFeeStatusIcon = view.findViewById(R.id.tvFeeStatusIcon);
        tvDueDate = view.findViewById(R.id.tvDueDate);
        tvTotalFee = view.findViewById(R.id.tvTotalFee);

        view.findViewById(R.id.tvFeesBack).setOnClickListener(v -> requireActivity().onBackPressed());
        view.findViewById(R.id.btnPayFees).setOnClickListener(v -> showPaymentDialog());

        // Set fee row labels
        setFeeRowLabel(view, R.id.rowRoomFee, "Room Rent");
        setFeeRowLabel(view, R.id.rowMessFee, "Mess Charges");
        setFeeRowLabel(view, R.id.rowMaintenance, "Maintenance");
        setFeeRowLabel(view, R.id.rowOther, "Other Charges");

        loadFees(view);
    }

    private void showPaymentDialog() {
        if (getContext() == null)
            return;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setTitle("Secure Payment")
                .setMessage("Initializing secure payment gateway...\nPlease wait.")
                .setCancelable(false)
                .show();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                Toast.makeText(getContext(), "Payment Successful! (Mock)", Toast.LENGTH_LONG).show();
            }
        }, 2000);
    }

    private void setFeeRowLabel(View root, int rowId, String label) {
        View row = root.findViewById(rowId);
        if (row != null) {
            TextView lbl = row.findViewById(R.id.tvFeeLabel);
            if (lbl != null)
                lbl.setText(label);
        }
    }

    private void setFeeRowAmount(View root, int rowId, String amount) {
        View row = root.findViewById(rowId);
        if (row != null) {
            TextView amt = row.findViewById(R.id.tvFeeAmount);
            if (amt != null)
                amt.setText(amount);
        }
    }

    private void loadFees(View view) {
        if (auth.getCurrentUser() == null)
            return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("fees").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status") != null ? doc.getString("status") : "Pending";
                        String dueDate = doc.getString("dueDate") != null ? doc.getString("dueDate") : "";
                        long roomFee = doc.getLong("roomFee") != null ? doc.getLong("roomFee") : 0;
                        long messFee = doc.getLong("messFee") != null ? doc.getLong("messFee") : 0;
                        long maintenance = doc.getLong("maintenance") != null ? doc.getLong("maintenance") : 0;
                        long other = doc.getLong("other") != null ? doc.getLong("other") : 0;
                        long total = roomFee + messFee + maintenance + other;

                        tvFeeStatus.setText(status);
                        tvDueDate.setText("Due: " + dueDate);
                        tvTotalFee.setText("₹" + total);

                        setFeeRowAmount(view, R.id.rowRoomFee, "₹" + roomFee);
                        setFeeRowAmount(view, R.id.rowMessFee, "₹" + messFee);
                        setFeeRowAmount(view, R.id.rowMaintenance, "₹" + maintenance);
                        setFeeRowAmount(view, R.id.rowOther, "₹" + other);

                        if ("Paid".equals(status)) {
                            tvFeeStatusIcon.setText("✅");
                            tvFeeStatus.setTextColor(0xFF4CAF50);
                        } else if ("Overdue".equals(status)) {
                            tvFeeStatusIcon.setText("🚨");
                            tvFeeStatus.setTextColor(0xFFE53935);
                        } else {
                            tvFeeStatusIcon.setText("⏳");
                            tvFeeStatus.setTextColor(0xFFFFB300);
                        }
                    } else {
                        // No Firestore fee doc yet — show sample values
                        showSampleFees(view);
                    }
                })
                .addOnFailureListener(e -> showSampleFees(view));
    }

    private void showSampleFees(View view) {
        tvFeeStatus.setText("Pending");
        tvFeeStatusIcon.setText("⏳");
        tvFeeStatus.setTextColor(0xFFFFB300);
        tvDueDate.setText("Due: 28 Feb 2026");
        tvTotalFee.setText("₹15,000");
        setFeeRowAmount(view, R.id.rowRoomFee, "₹8,000");
        setFeeRowAmount(view, R.id.rowMessFee, "₹5,000");
        setFeeRowAmount(view, R.id.rowMaintenance, "₹1,500");
        setFeeRowAmount(view, R.id.rowOther, "₹500");
    }
}
