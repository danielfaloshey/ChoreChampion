package com.faloshey.chorechampion.fragments.parent;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ChildGridAdapter;
import com.faloshey.chorechampion.adapters.NotificationsAdapter;
import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.models.NotificationModel;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class ParentHomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView welcomeUsername;
    private RecyclerView childRecyclerView;
    private ChildGridAdapter adapter;
    private List<ChildModel> childList;
    private MaterialButton notifsBtn;
    private com.google.android.material.badge.BadgeDrawable notificationBadge;
    private ListenerRegistration liveDialogNotificationListener;

    public ParentHomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parenthome, container, false);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @com.google.android.material.badge.ExperimentalBadgeUtils
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        welcomeUsername = view.findViewById(R.id.user_name);
        childRecyclerView = view.findViewById(R.id.child_account_grid);
        MaterialButton addChildBtn = view.findViewById(R.id.add_child_btn);
        MaterialButton editChildBtn = view.findViewById(R.id.edit_child_btn);
        notifsBtn = view.findViewById(R.id.notifications_button);

        childRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        childList = new ArrayList<>();
        adapter = new ChildGridAdapter(childList, child -> {

            AudioManager.getInstance().playSound("cork_pop");
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchProfileToChild(child);
            }
        });
        childRecyclerView.setAdapter(adapter);

        if (auth.getCurrentUser() != null) {
            String parentId = auth.getCurrentUser().getUid();
            loadParentProfileData(parentId);
            listenToChildrenProfiles(parentId);

            startNotificationCountListener(parentId);
        }

        notifsBtn.post(() -> {
            notificationBadge = com.google.android.material.badge.BadgeDrawable.create(requireContext());
            com.google.android.material.badge.BadgeUtils.attachBadgeDrawable(
                    notificationBadge,
                    notifsBtn,
                    null
            );

            notificationBadge.setBackgroundColor(android.graphics.Color.parseColor("#E53935"));
            notificationBadge.setBadgeTextColor(android.graphics.Color.WHITE);
            notificationBadge.setVisible(false);
        });

        notifsBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            if (auth.getCurrentUser() != null) {
                showNotificationsDialog(auth.getCurrentUser().getUid());
            }
        });

        addChildBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            Navigation.findNavController(view).navigate(R.id.action_parentHomeFragment_to_createProfileFragment);
        });

        editChildBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            showManageChildrenDialog();
        });
    }

    private void loadParentProfileData(String parentId) {
        db.collection("Users").document(parentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("parentData")) {
                        String name = documentSnapshot.getString("parentData.displayName");
                        if (name != null) welcomeUsername.setText(name);
                    }
                });
    }

    private void listenToChildrenProfiles(String parentId) {
        db.collection("Users").document(parentId).collection("children")
                .orderBy("createdAt")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    childList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ChildModel model = doc.toObject(ChildModel.class);
                        if (model != null) childList.add(model);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showManageChildrenDialog() {
        if (childList.isEmpty()) {
            Toast.makeText(getContext(), "No profiles available to modify.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] usernames = new String[childList.size()];
        for (int i = 0; i < childList.size(); i++) {
            usernames[i] = childList.get(i).getUsername();
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Select a profile to modify")
                .setItems(usernames, (dialog, which) -> {
                    ChildModel selectedChild = childList.get(which);
                    openEditOrDeleteOptions(selectedChild);
                })
                .show();
    }

    private void openEditOrDeleteOptions(ChildModel child) {
        String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        new AlertDialog.Builder(getContext())
                .setTitle("Manage " + child.getUsername())
                .setPositiveButton("Rename", (dialog, which) -> {

                    EditText inputField = new EditText(getContext());
                    inputField.setText(child.getUsername());

                    new AlertDialog.Builder(getContext())
                            .setTitle("Rename Profile")
                            .setView(inputField)
                            .setPositiveButton("Save", (d, w) -> {
                                String newName = inputField.getText().toString().trim();
                                if (!newName.isEmpty()) {
                                    db.collection("Users").document(parentId)
                                            .collection("children").document(child.getChildId())
                                            .update("username", newName);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Delete Profile", (dialog, which) -> new AlertDialog.Builder(getContext())
                        .setTitle("Banish " + child.getUsername() + "?")
                        .setMessage("Are you certain you want to banish this profile? All progression progress will be lost forever.")
                        .setPositiveButton("Banish", (d, w) -> db.collection("Users").document(parentId)
                                .collection("children").document(child.getChildId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile cleared.", Toast.LENGTH_SHORT).show()))
                        .setNegativeButton("Keep Profile", null)
                        .show())
                .setNeutralButton("Cancel", null)
                .show();

    }

    private void startNotificationCountListener(String parentUid) {
        FirebaseFirestore.getInstance().collection("Users").document(parentUid)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    int pendingAlertsCount = snapshots.size();

                    if (pendingAlertsCount > 0) {
                        notificationBadge.setNumber(pendingAlertsCount);
                        notificationBadge.setVisible(true);
                    } else {

                        notificationBadge.setVisible(false);
                    }
                });
    }

    private void showNotificationsDialog(String parentUid) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.notification_dialog, null);
        RecyclerView dialogRecycler = dialogView.findViewById(R.id.notifications_recycler);
        TextView txtEmptyState = dialogView.findViewById(R.id.empty_notifications_text);
        MaterialButton btnClearAll = dialogView.findViewById(R.id.btn_clear_all);
        MaterialButton btnClose = dialogView.findViewById(R.id.btn_close_notifs);

        List<NotificationModel> dialogNotificationsList = new ArrayList<>();
        NotificationsAdapter dialogAdapter = new NotificationsAdapter(getContext(), dialogNotificationsList);
        dialogRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        dialogRecycler.setAdapter(dialogAdapter);

        AlertDialog alertLayout = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (alertLayout.getWindow() != null) {
            alertLayout.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        liveDialogNotificationListener = db.collection("Users").document(parentUid)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("NotificationDebug", "FIRESTORE ERROR CODE: " + error.getCode(), error);
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        Log.d("NotificationDebug", "Raw database snapshots found: " + snapshots.size());

                        dialogNotificationsList.clear();

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {

                            NotificationModel notification = doc.toObject(NotificationModel.class);

                            if (notification == null) {
                                Log.e("NotificationDebug", "Failed to deserialize document ID: " + doc.getId());
                            } else {
                                dialogNotificationsList.add(notification);
                            }
                        }
                        dialogAdapter.updateList(new ArrayList<>(dialogNotificationsList));

                        if (dialogNotificationsList.isEmpty()) {
                            dialogRecycler.setVisibility(View.GONE);
                            txtEmptyState.setVisibility(View.VISIBLE);
                            btnClearAll.setEnabled(false);
                            btnClearAll.setAlpha(0.4f);
                        } else {
                            dialogRecycler.setVisibility(View.VISIBLE);
                            txtEmptyState.setVisibility(View.GONE);
                            btnClearAll.setEnabled(true);
                            btnClearAll.setAlpha(1.0f);
                        }
                    }
                });

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int targetPosition = viewHolder.getBindingAdapterPosition();
                NotificationModel selectedItem = dialogNotificationsList.get(targetPosition);

                AudioManager.getInstance().playSound("cork_pop");

                db.collection("Users").document(parentUid)
                        .collection("notifications").document(selectedItem.getId())
                        .delete()
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to clear item", Toast.LENGTH_SHORT).show());
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(dialogRecycler);

        btnClearAll.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");

            if (dialogNotificationsList.isEmpty()) return;

            WriteBatch processingBatch = db.batch();
            for (NotificationModel notification : dialogNotificationsList) {
                processingBatch.delete(db.collection("Users").document(parentUid)
                        .collection("notifications").document(notification.getId()));
            }

            processingBatch.commit()
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "All updates cleared.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to clear collection.", Toast.LENGTH_SHORT).show());
        });

        btnClose.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            alertLayout.dismiss();
        });

        alertLayout.setOnDismissListener(dialog -> {
            if (liveDialogNotificationListener != null) {
                liveDialogNotificationListener.remove();
                liveDialogNotificationListener = null;
            }
        });

        alertLayout.show();

    }

}
