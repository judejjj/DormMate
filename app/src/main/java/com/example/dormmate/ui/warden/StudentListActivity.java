package com.example.dormmate.ui.warden;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dormmate.R;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView rvStudents;
    private StudentAdapter adapter;
    private final List<DocumentSnapshot> allStudents = new ArrayList<>();
    private final List<DocumentSnapshot> filteredStudents = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration studentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        db = FirebaseFirestore.getInstance();
        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tvStudentListBack).setOnClickListener(v -> finish());

        adapter = new StudentAdapter();
        rvStudents.setAdapter(adapter);

        // Real-time search
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnAddStudent).setOnClickListener(v -> showAddStudentDialog());

        listenToStudents();
    }

    private void showAddStudentDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);
        final EditText etName = dialogView.findViewById(R.id.etNewStudentName);
        final EditText etEmail = dialogView.findViewById(R.id.etNewStudentEmail);
        final EditText etPass = dialogView.findViewById(R.id.etNewStudentPassword);
        final TextView tvStatusMessage = dialogView.findViewById(R.id.tvStatusMessage);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("ENROLL", null) // Set to null to prevent auto-dismissal
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialogInterface) {
                Button button = ((androidx.appcompat.app.AlertDialog) dialog)
                        .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String name = etName.getText().toString().trim();
                        String email = etEmail.getText().toString().trim();
                        String password = etPass.getText().toString().trim();

                        boolean isValid = true;

                        if (name.isEmpty()) {
                            etName.setError("Name is required");
                            isValid = false;
                        }

                        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            etEmail.setError("Valid email is required");
                            isValid = false;
                        }

                        if (!isValid) {
                            return;
                        }

                        createStudentAccount(name, email, password, etName, etEmail, etPass, tvStatusMessage);
                    }
                });
            }
        });

        dialog.show();
    }

    private void createStudentAccount(String name, String email, String password, EditText etName, EditText etEmail,
            EditText etPass, TextView tvStatusMessage) {
        Map<String, Object> newStudent = new HashMap<>();
        newStudent.put("name", name);
        newStudent.put("email", email);
        newStudent.put("password", password);
        newStudent.put("role", "Student");
        newStudent.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("pre_approved_students").document(email).set(newStudent)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(StudentListActivity.this, "Student added to pre-approval list",
                                Toast.LENGTH_LONG).show();
                        // Clear fields and reset
                        etName.setText("");
                        etEmail.setText("");
                        etPass.setText("123123");
                        tvStatusMessage.setText("User acc created. Student needs to login via this new cred.");
                        tvStatusMessage.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(StudentListActivity.this, "Failed to add student: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private final Map<String, DocumentSnapshot> activatedMap = new HashMap<>();
    private final Map<String, DocumentSnapshot> invitationMap = new HashMap<>();

    private void listenToStudents() {
        // 1. Listen to Activated Students
        db.collection("users")
                .whereEqualTo("role", "Student")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@androidx.annotation.Nullable QuerySnapshot snapshots,
                            @androidx.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(StudentListActivity.this, "Profile Query Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (snapshots != null) {
                            activatedMap.clear();
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String email = doc.getString("email");
                                if (email != null)
                                    activatedMap.put(email, doc);
                            }
                            mergeAndDisplay();
                        }
                    }
                });

        // 2. Listen to Pending Invitations
        db.collection("pre_approved_students")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@androidx.annotation.Nullable QuerySnapshot snapshots,
                            @androidx.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(StudentListActivity.this, "Invitation Query Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (snapshots != null) {
                            invitationMap.clear();
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String email = doc.getString("email");
                                if (email != null)
                                    invitationMap.put(email, doc);
                            }
                            mergeAndDisplay();
                        }
                    }
                });
    }

    private void mergeAndDisplay() {
        allStudents.clear();

        // Add all activated first
        allStudents.addAll(activatedMap.values());

        // Add invitations that aren't activated yet
        for (String email : invitationMap.keySet()) {
            if (!activatedMap.containsKey(email)) {
                allStudents.add(invitationMap.get(email));
            }
        }

        filterStudents(
                findViewById(R.id.etSearch) != null ? ((EditText) findViewById(R.id.etSearch)).getText().toString()
                        : "");
    }

    private void filterStudents(String query) {
        filteredStudents.clear();
        if (query.isEmpty()) {
            filteredStudents.addAll(allStudents);
        } else {
            String q = query.toLowerCase();
            for (DocumentSnapshot doc : allStudents) {
                String name = doc.getString("name") != null ? doc.getString("name").toLowerCase() : "";
                String email = doc.getString("email") != null ? doc.getString("email").toLowerCase() : "";

                if (name.contains(q) || email.contains(q)) {
                    filteredStudents.add(doc);
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showStudentOptionsDialog(DocumentSnapshot doc) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_student_options, null);
        TextView tvStudentNameDialogMsg = dialogView.findViewById(R.id.tvStudentNameDialogMsg);
        Button btnAllocateRoom = dialogView.findViewById(R.id.btnOptionAllocateRoom);
        Button btnUpdateDetails = dialogView.findViewById(R.id.btnOptionUpdateDetails);

        tvStudentNameDialogMsg.setText("Actions for " + doc.getString("name"));

        AlertDialog optionsDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (optionsDialog.getWindow() != null) {
            optionsDialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnAllocateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsDialog.dismiss();
                showAllocateRoomDialog(doc);
            }
        });

        btnUpdateDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                optionsDialog.dismiss();
                Toast.makeText(StudentListActivity.this, "Update Details functionality pending", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        optionsDialog.show();
    }

    private void showAllocateRoomDialog(DocumentSnapshot doc) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_allocate_room, null);
        EditText etAllocateRoom = dialogView.findViewById(R.id.etAllocateRoom);
        Button btnCancelAllocation = dialogView.findViewById(R.id.btnCancelAllocation);
        Button btnSaveAllocation = dialogView.findViewById(R.id.btnSaveAllocation);

        // Pre-fill if exists
        etAllocateRoom.setText(doc.getString("room") != null ? doc.getString("room") : "");

        AlertDialog allocateDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (allocateDialog.getWindow() != null) {
            allocateDialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCancelAllocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allocateDialog.dismiss();
            }
        });

        btnSaveAllocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String roomStr = etAllocateRoom.getText().toString().trim();
                String floor = "";
                String wing = "";

                try {
                    int roomNum = Integer.parseInt(roomStr);
                    floor = String.valueOf(roomNum / 100);
                    wing = String.valueOf(roomNum % 100);
                } catch (NumberFormatException e) {
                    Toast.makeText(StudentListActivity.this, "Room must be a number for auto-allocation",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean isInvitation = doc.getReference().getPath().contains("pre_approved_students");

                if (isInvitation) {
                    Toast.makeText(StudentListActivity.this,
                            "Cannot allocate rooms to pending invitations. Wait for student to activate account first.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("room", roomStr);
                updates.put("floor", floor);
                updates.put("wing", wing);

                doc.getReference().update(updates)
                        .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                allocateDialog.dismiss();
                                Toast.makeText(StudentListActivity.this, "Room Details Updated", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        })
                        .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(StudentListActivity.this, "Failed to update room: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        allocateDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentListener != null)
            studentListener.remove();
    }

    class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DocumentSnapshot doc = filteredStudents.get(position);
            String name = doc.getString("name") != null ? doc.getString("name") : "Unknown";
            String email = doc.getString("email") != null ? doc.getString("email") : "";

            // Check if it's an invitation or activated
            boolean isInvitation = doc.getReference().getPath().contains("pre_approved_students");

            holder.tvName.setText(name + (isInvitation ? " (Invitation)" : ""));
            holder.tvEmail.setText(email);
            holder.tvAvatar.setText(!name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "?");

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showStudentOptionsDialog(doc);
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredStudents.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvAvatar;

            VH(@NonNull View view) {
                super(view);
                tvName = view.findViewById(R.id.tvStudentName);
                tvEmail = view.findViewById(R.id.tvStudentEmail);
                tvAvatar = view.findViewById(R.id.tvStudentAvatar);
            }
        }
    }
}
