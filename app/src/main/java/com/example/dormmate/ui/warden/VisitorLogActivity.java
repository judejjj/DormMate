package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class VisitorLogActivity extends AppCompatActivity {

    private RecyclerView rvVisitors;
    private VisitorAdapter adapter;
    private final List<DocumentSnapshot> visitors = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration visitorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_log);

        db = FirebaseFirestore.getInstance();
        rvVisitors = findViewById(R.id.rvVisitors);
        rvVisitors.setLayoutManager(new LinearLayoutManager(this));
        rvVisitors.setNestedScrollingEnabled(false);

        findViewById(R.id.tvVisitorBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRegisterVisitor).setOnClickListener(v -> registerVisitor());
        findViewById(R.id.btnCaptureFace).setOnClickListener(
                v -> Toast.makeText(this, "Face capture module pending", Toast.LENGTH_SHORT).show());

        adapter = new VisitorAdapter();
        rvVisitors.setAdapter(adapter);
        listenVisitors();
    }

    private void registerVisitor() {
        String name = ((EditText) findViewById(R.id.etVisitorName)).getText().toString().trim();
        String phone = ((EditText) findViewById(R.id.etVisitorPhone)).getText().toString().trim();
        String visiting = ((EditText) findViewById(R.id.etVisitingStudent)).getText().toString().trim();
        String purpose = ((EditText) findViewById(R.id.etVisitPurpose)).getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || visiting.isEmpty()) {
            Toast.makeText(this, "Please fill Name, Phone, and Visiting Student", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> visitor = new HashMap<>();
        visitor.put("name", name);
        visitor.put("phone", phone);
        visitor.put("visitingStudent", visiting);
        visitor.put("purpose", purpose);
        visitor.put("timestamp", Timestamp.now());

        db.collection("visitor_log").add(visitor)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "✅ Visitor Registered", Toast.LENGTH_SHORT).show();
                    clearInputs();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearInputs() {
        ((EditText) findViewById(R.id.etVisitorName)).setText("");
        ((EditText) findViewById(R.id.etVisitorPhone)).setText("");
        ((EditText) findViewById(R.id.etVisitingStudent)).setText("");
        ((EditText) findViewById(R.id.etVisitPurpose)).setText("");
    }

    private void listenVisitors() {
        visitorListener = db.collection("visitor_log")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null)
                        return;
                    visitors.clear();
                    visitors.addAll(snapshots.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visitorListener != null)
            visitorListener.remove();
    }

    class VisitorAdapter extends RecyclerView.Adapter<VisitorAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_visitor, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = visitors.get(position);
            holder.tvName.setText(doc.getString("name"));
            holder.tvPhone.setText(doc.getString("phone") != null ? doc.getString("phone") : "");
            holder.tvVisiting.setText(
                    "Visiting: " + (doc.getString("visitingStudent") != null ? doc.getString("visitingStudent") : "—"));
            Timestamp ts = doc.getTimestamp("timestamp");
            if (ts != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
                holder.tvTime.setText(sdf.format(ts.toDate()));
            }

            // Mock an Overstay Alert for the first item in the list as a demo
            if (position == 0) {
                holder.tvOverstayAlert.setVisibility(View.VISIBLE);
            } else {
                holder.tvOverstayAlert.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return visitors.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvVisiting, tvTime, tvOverstayAlert;

            VH(@NonNull View view) {
                super(view);
                tvName = view.findViewById(R.id.tvVisitorName);
                tvPhone = view.findViewById(R.id.tvVisitorPhone);
                tvVisiting = view.findViewById(R.id.tvVisitingStudent);
                tvTime = view.findViewById(R.id.tvVisitTime);
                tvOverstayAlert = view.findViewById(R.id.tvOverstayAlert);
            }
        }
    }
}
