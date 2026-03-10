package com.example.dormmate.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notifListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.tvNotifBack).setOnClickListener(v -> requireActivity().onBackPressed());

        listenToNotifications();
    }

    private void listenToNotifications() {
        if (auth.getCurrentUser() == null)
            return;
        String uid = auth.getCurrentUser().getUid();

        // 1. Fetch user's floor property first
        db.collection("users").document(uid).get()
                .addOnSuccessListener(
                        new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
                            @Override
                            public void onSuccess(com.google.firebase.firestore.DocumentSnapshot userDoc) {
                                String floor = userDoc.getString("floor");
                                String targetFloor = floor != null && !floor.trim().isEmpty() ? "floor_" + floor.trim()
                                        : "unknown_floor";

                                // 2. Attach listener to global announcements filtering by 'all' or specific
                                // floor target
                                notifListener = db.collection("global_announcements")
                                        .whereIn("target", java.util.Arrays.asList("all", targetFloor))
                                        .addSnapshotListener(
                                                new com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot>() {
                                                    @Override
                                                    public void onEvent(
                                                            @androidx.annotation.Nullable com.google.firebase.firestore.QuerySnapshot snapshots,
                                                            @androidx.annotation.Nullable com.google.firebase.firestore.FirebaseFirestoreException e) {
                                                        if (e != null || snapshots == null)
                                                            return;

                                                        List<NotifItem> items = new ArrayList<>();
                                                        for (QueryDocumentSnapshot doc : snapshots) {
                                                            String title = doc.getString("title");
                                                            String message = doc.getString("message");
                                                            com.google.firebase.Timestamp ts = doc
                                                                    .getTimestamp("timestamp");

                                                            String timeStr = "";
                                                            if (ts != null) {
                                                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                                                        "MMM dd, HH:mm", java.util.Locale.getDefault());
                                                                timeStr = sdf.format(ts.toDate());
                                                            }

                                                            if (title != null && message != null) {
                                                                items.add(new NotifItem(title, message, timeStr));
                                                            }
                                                        }

                                                        // Sort locally since we used whereIn which prevents multi-field
                                                        // orderBy complexity
                                                        java.util.Collections.sort(items,
                                                                new java.util.Comparator<NotifItem>() {
                                                                    @Override
                                                                    public int compare(NotifItem o1, NotifItem o2) {
                                                                        return o2.time.compareTo(o1.time); // basic
                                                                                                           // descending
                                                                                                           // sort on
                                                                                                           // string
                                                                                                           // formatted
                                                                                                           // time
                                                                    }
                                                                });

                                                        rvNotifications.setAdapter(new NotifAdapter(items));
                                                    }
                                                });
                            }
                        })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        android.widget.Toast.makeText(getContext(), "Failed to verify user floor for notifications",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notifListener != null)
            notifListener.remove();
    }

    // Simple model: extracted standard fields
    static class NotifItem {
        String title, message, time;

        NotifItem(String title, String message, String time) {
            this.title = title;
            this.message = message;
            this.time = time;
        }
    }

    // Notifications Adapter
    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.ViewHolder> {
        private final List<NotifItem> items;

        NotifAdapter(List<NotifItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotifItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);
            holder.tvTime.setText(item.time);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;

            ViewHolder(@NonNull View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tvNotifTitle);
                tvMessage = view.findViewById(R.id.tvNotifMessage);
                tvTime = view.findViewById(R.id.tvNotifTime);
            }
        }
    }
}
