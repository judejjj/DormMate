package com.example.dormmate.ui;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HostelContextBuilder {

    public interface OnContextBuiltListener {
        void onContextBuilt(String context);

        void onError(Exception e);
    }

    public static void buildLiveContext(String currentUserId, OnContextBuiltListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Tasks for Global Data
        Task<DocumentSnapshot> rulesTask = db.collection("hostel_rules").document("rules").get();
        // Get today's day string (e.g. "Monday")
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.util.Date date = calendar.getTime();
        String dayOfWeek = new java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH).format(date.getTime());

        // Fetch the entire week's menu so AI can answer about any day
        Task<QuerySnapshot> messTask = db.collection("mess_menu").get();
        // Fetch recent announcements and sort locally to avoid composite index
        // requirement
        Task<QuerySnapshot> broadcastTask = db.collection("global_announcements").get();

        // Tasks for Personalized Data
        Task<DocumentSnapshot> profileTask = db.collection("users").document(currentUserId).get();
        Task<DocumentSnapshot> feesTask = db.collection("fees").document(currentUserId).get();
        Task<QuerySnapshot> leavesTask = db.collection("leave_requests")
                .whereEqualTo("userId", currentUserId)
                .limit(1)
                .get();

        // Combine all tasks
        Task<List<Object>> allTasks = Tasks.whenAllSuccess(rulesTask, messTask, broadcastTask, profileTask, feesTask,
                leavesTask);

        allTasks.addOnSuccessListener(results -> {
            try {
                StringBuilder contextBuilder = new StringBuilder();
                contextBuilder.append("--- HOSTEL OMNI-CONTEXT ---\n");
                contextBuilder.append("Today's Date: ").append(date.toString()).append("\n");
                contextBuilder.append("Current Day: ").append(dayOfWeek).append("\n\n");

                // Process Profile
                DocumentSnapshot profileDoc = (DocumentSnapshot) results.get(3);
                if (profileDoc.exists()) {
                    contextBuilder.append("[User Profile]\n");
                    contextBuilder.append("Name: ").append(profileDoc.getString("name")).append("\n");
                    contextBuilder.append("Room: ").append(profileDoc.getString("room")).append("\n");
                    contextBuilder.append("Floor: ").append(profileDoc.getString("floor")).append("\n");
                    contextBuilder.append("Wing: ").append(profileDoc.getString("wing")).append("\n\n");
                }

                // Process Fees
                DocumentSnapshot feesDoc = (DocumentSnapshot) results.get(4);
                if (feesDoc.exists()) {
                    contextBuilder.append("[Pending Fees]\n");
                    String status = feesDoc.getString("status");
                    long amount = feesDoc.getLong("roomFee") != null ? feesDoc.getLong("roomFee") : 0;
                    contextBuilder.append("Status: ").append(status).append("\n");
                    contextBuilder.append("Amount Due: ₹").append(amount).append("\n\n");
                } else {
                    contextBuilder.append("[Pending Fees]\nNo fees data available.\n\n");
                }

                // Process Leaves
                QuerySnapshot leavesSnap = (QuerySnapshot) results.get(5);
                if (!leavesSnap.isEmpty()) {
                    DocumentSnapshot leaveDoc = leavesSnap.getDocuments().get(0);
                    contextBuilder.append("[Latest Leave Request]\n");
                    contextBuilder.append("Status: ").append(leaveDoc.getString("status")).append("\n");
                    contextBuilder.append("Reason: ").append(leaveDoc.getString("reason")).append("\n\n");
                } else {
                    contextBuilder.append("[Latest Leave Request]\nNo recent leaves.\n\n");
                }

                // Process Rules
                DocumentSnapshot rulesDoc = (DocumentSnapshot) results.get(0);
                if (rulesDoc.exists() && rulesDoc.getString("content") != null) {
                    contextBuilder.append("[Hostel Rules]\n").append(rulesDoc.getString("content")).append("\n\n");
                }

                // Process Mess Menu (Show multiple days if available)
                QuerySnapshot messSnap = (QuerySnapshot) results.get(1);
                if (!messSnap.isEmpty()) {
                    contextBuilder.append("[Mess Menu Schedule]\n");
                    for (DocumentSnapshot messDoc : messSnap.getDocuments()) {
                        String dayLabel = messDoc.getString("Day");
                        if (dayLabel == null)
                            dayLabel = "Unknown Day";

                        contextBuilder.append("Day: ").append(dayLabel).append("\n");
                        contextBuilder.append(" - Breakfast: ")
                                .append(messDoc.getString("Breakfast") != null ? messDoc.getString("Breakfast")
                                        : messDoc.getString("breakfast"))
                                .append("\n");
                        contextBuilder.append(" - Lunch: ")
                                .append(messDoc.getString("Lunch") != null ? messDoc.getString("Lunch")
                                        : messDoc.getString("lunch"))
                                .append("\n");
                        contextBuilder.append(" - Dinner: ")
                                .append(messDoc.getString("Dinner") != null ? messDoc.getString("Dinner")
                                        : messDoc.getString("dinner"))
                                .append("\n\n");
                    }
                }

                // Process Broadcast (Find latest locally)
                QuerySnapshot broadcastSnap = (QuerySnapshot) results.get(2);
                if (!broadcastSnap.isEmpty()) {
                    DocumentSnapshot latestBroadcast = null;
                    for (DocumentSnapshot doc : broadcastSnap.getDocuments()) {
                        if (latestBroadcast == null) {
                            latestBroadcast = doc;
                        } else {
                            if (doc.getTimestamp("timestamp") != null
                                    && latestBroadcast.getTimestamp("timestamp") != null) {
                                if (doc.getTimestamp("timestamp")
                                        .compareTo(latestBroadcast.getTimestamp("timestamp")) > 0) {
                                    latestBroadcast = doc;
                                }
                            }
                        }
                    }
                    if (latestBroadcast != null) {
                        contextBuilder.append("[Latest Broadcast]\n");
                        contextBuilder.append(latestBroadcast.getString("message")).append("\n\n");
                    }
                }

                listener.onContextBuilt(contextBuilder.toString());

            } catch (Exception e) {
                listener.onError(e);
            }
        }).addOnFailureListener(listener::onError);
    }
}
