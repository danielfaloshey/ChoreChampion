package com.faloshey.chorechampion.fragments.parent;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ParentQuestAdapter;
import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.models.QuestModel;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class ParentQuestsFragment extends Fragment implements ParentQuestAdapter.OnQuestClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TabLayout tabLayout;
    private RecyclerView questRecyclerView;
    private ParentQuestAdapter questAdapter;
    private MaterialButton addBtn;
    private MaterialButton editBtn;
    private MaterialButton parentQuestInfoBtn;
    private TextView hintMsg;

    private final List<QuestModel> masterQuestList = new ArrayList<>();
    private final List<QuestModel> filteredQuestList = new ArrayList<>();
    private List<ChildModel> childrenList = new ArrayList<>();

    private ListenerRegistration questsListener;
    private ListenerRegistration childrenListener;
    private QuestModel activeSelectedQuest = null;

    public ParentQuestsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parentquests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tabLayout = view.findViewById(R.id.parent_quest_tabs);
        questRecyclerView = view.findViewById(R.id.parent_quest_grid);
        addBtn = view.findViewById(R.id.quest_add_btn);
        editBtn = view.findViewById(R.id.quest_edit_btn);
        parentQuestInfoBtn = view.findViewById(R.id.parent_quest_info);
        hintMsg = view.findViewById(R.id.parent_hint_msg);

        questRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        questAdapter = new ParentQuestAdapter(filteredQuestList, this);
        questRecyclerView.setAdapter(questAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                AudioManager.getInstance().playSound("cork_pop");
                updateUiForCurrentTab();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                AudioManager.getInstance().playSound("cork_pop");
            }
        });

        addBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            showQuestDialog(null);
        });
        parentQuestInfoBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            showQuestInfoDialog();
        });
        editBtn.setOnClickListener(v -> {
            if (activeSelectedQuest == null) return;

            if (tabLayout.getSelectedTabPosition() == 0) {
                AudioManager.getInstance().playSound("cork_pop");
                showQuestDialog(activeSelectedQuest);
            } else {
                AudioManager.getInstance().playSound("cork_pop");
                approveQuestAndRewardChild(activeSelectedQuest);
            }
        });

        if (auth.getCurrentUser() != null) {
            String parentId = auth.getCurrentUser().getUid();
            listenToDataSets(parentId);
        }
    }

    private void listenToDataSets(String parentId) {
        questsListener = db.collection("Users").document(parentId)
                .collection("quests")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        masterQuestList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            QuestModel quest = doc.toObject(QuestModel.class);
                            if (quest != null) {
                                masterQuestList.add(quest);
                            }
                        }
                        updateUiForCurrentTab();
                    }
                });

        childrenListener = db.collection("Users").document(parentId)
                .collection("children")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        List<ChildModel> updatedChildren = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChildModel child = doc.toObject(ChildModel.class);
                            if (child != null) {
                                updatedChildren.add(child);
                            }
                        }
                        childrenList = updatedChildren;
                    }
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateUiForCurrentTab() {
        filteredQuestList.clear();
        int activeTab = tabLayout.getSelectedTabPosition();
        boolean isApprovalTab = (activeTab == 1);

        for (QuestModel quest : masterQuestList) {
            if (isApprovalTab) {

                if (quest.isCompleted() && !quest.isApproved()) {
                    filteredQuestList.add(quest);
                }
            } else {

                if (!quest.isCompleted()) {
                    filteredQuestList.add(quest);
                }
            }
        }

        questAdapter.updateList(filteredQuestList);
        onQuestCleared();

        if (isApprovalTab) {
            addBtn.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.0f);
            params.setMargins(0, 0, 0, 0);
            editBtn.setLayoutParams(params);

            editBtn.setText("Approve Rewards!");
            editBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.forest_green));
            hintMsg.setText("Select a completed quest to award payouts!");
        } else {
            addBtn.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            editParams.setMargins(8, 0, 0, 0);
            editBtn.setLayoutParams(editParams);

            editBtn.setText(R.string.edit_button);
            editBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
            hintMsg.setText(R.string.quests_msg);
        }
    }

    private void approveQuestAndRewardChild(QuestModel quest) {
        String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        String childId = quest.getAssignedChildId();

        if (childId == null || childId.trim().isEmpty()) {
            Toast.makeText(getContext(), "Error: No child associated with this quest.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference childRef = db.collection("Users").document(parentId).collection("children").document(childId);
        DocumentReference questRef = db.collection("Users").document(parentId).collection("quests").document(quest.getQuestId());

        db.runTransaction(transaction -> {
            DocumentSnapshot childSnap = transaction.get(childRef);

            if (childSnap.exists()) {
                long currentGold = childSnap.getLong("gold") != null ? childSnap.getLong("gold") : 0;
                long currentXp = childSnap.getLong("xp") != null ? childSnap.getLong("xp") : 0;

                long newGold = currentGold + quest.getGoldReward();
                long newXp = currentXp + quest.getXpReward();

                transaction.update(childRef, "gold", newGold);
                transaction.update(childRef, "xp", newXp);
            }

            transaction.delete(questRef);

            return null;
        }).addOnSuccessListener(aVoid ->
                Toast.makeText(getContext(), "Rewards disbursed! Quest finalized.", Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Transaction failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void showQuestDialog(@Nullable QuestModel questToEdit) {
        boolean isEditMode = (questToEdit != null);

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quest_form, null);
        EditText titleInput = dialogView.findViewById(R.id.dialog_quest_title);
        EditText descInput = dialogView.findViewById(R.id.dialog_quest_desc);
        EditText goldInput = dialogView.findViewById(R.id.dialog_quest_gold);
        Spinner assignmentSpinner = dialogView.findViewById(R.id.dialog_quest_assignment_spinner);

        List<String> spinnerOptions = new ArrayList<>();
        spinnerOptions.add("Unassigned");

        Map<String, String> nameToIdMap = new HashMap<>();
        int selectionIndex = 0;

        for (int i = 0; i < childrenList.size(); i++) {
            ChildModel child = childrenList.get(i);
            spinnerOptions.add(child.getUsername());
            nameToIdMap.put(child.getUsername(), child.getChildId());

            if (isEditMode && child.getChildId().equals(questToEdit.getAssignedChildId())) {
                selectionIndex = i + 1;
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, spinnerOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assignmentSpinner.setAdapter(spinnerAdapter);
        assignmentSpinner.setSelection(selectionIndex);

        if (isEditMode) {
            titleInput.setText(questToEdit.getTitle());
            descInput.setText(questToEdit.getDescription());
            goldInput.setText(String.valueOf(questToEdit.getGoldReward()));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setTitle(isEditMode ? "Modify Quest" : "Forge New Quest Document");

        builder.setPositiveButton(isEditMode ? "Save Changes" : "Deploy Quest", null);
        builder.setNegativeButton("Cancel", ((dialog, which) -> dialog.dismiss()));

        if (isEditMode) {
            builder.setNeutralButton("Delete", (dialog, which) -> deleteQuestFromFirestore(questToEdit.getQuestId()));
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();
            String goldStr = goldInput.getText().toString().trim();
            String selectedName = assignmentSpinner.getSelectedItem().toString();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc) || TextUtils.isEmpty(goldStr)) {
                Toast.makeText(getContext(), "All quest structural parameters are mandatory!", Toast.LENGTH_SHORT).show();
                return;
            }

            int goldReward = Integer.parseInt(goldStr);
            int xpReward = goldReward / 2;
            String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

            String questId = isEditMode ? questToEdit.getQuestId() : db.collection("Users").document(parentId).collection("quests").document().getId();

            QuestModel savedQuest = new QuestModel();
            savedQuest.setQuestId(questId);
            savedQuest.setTitle(title);
            savedQuest.setDescription(desc);
            savedQuest.setGoldReward(goldReward);
            savedQuest.setXpReward(xpReward);
            savedQuest.setCompleted(isEditMode && questToEdit.isCompleted());
            savedQuest.setApproved(isEditMode && questToEdit.isApproved());

            if (selectedName.equals("Unassigned")) {
                savedQuest.setAssignedChildId("");
                savedQuest.setAssignedChildName("Unassigned");
            } else {
                savedQuest.setAssignedChildId(nameToIdMap.get(selectedName));
                savedQuest.setAssignedChildName(selectedName);
            }

            db.collection("Users").document(parentId)
                    .collection("quests").document(questId)
                    .set(savedQuest)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), isEditMode ? "Quest updated!" : "Quest dispatched!", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Transaction dropped: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void deleteQuestFromFirestore(String questId) {
        String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("Users").document(parentId)
                .collection("quests").document(questId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Quest discarded successfully.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to discard quest.", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetTextI18n")
    private void showQuestInfoDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_info_popup, null);

        TextView title = dialogView.findViewById(R.id.info_title);
        TextView message = dialogView.findViewById(R.id.info_message);
        MaterialButton closeBtn = dialogView.findViewById(R.id.info_close_btn);

        title.setText("Quest Management");
        message.setText("• Add Quest: Click Add, enter quest information, assign to child or leave unassigned. \n\n" +
                "• Edit/Delete child quest: Select quest and press Edit. \n\n " +
                "• After child completes quest: Click Need Approved Tab, select quest and click Approve Rewards.");

        AlertDialog infoDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (infoDialog.getWindow() != null) {
            infoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> infoDialog.dismiss());
        infoDialog.show();
    }

    @Override
    public void onQuestSelected(QuestModel quest) {
        activeSelectedQuest = quest;
        updateButtonStates();
    }

    @Override
    public void onQuestCleared() {
        activeSelectedQuest = null;
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (activeSelectedQuest != null) {
            editBtn.setEnabled(true);
            editBtn.setAlpha(1.0f);
        }
        else {
            editBtn.setEnabled(false);
            editBtn.setAlpha(0.5f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (questsListener != null) questsListener.remove();
        if (childrenListener != null) childrenListener.remove();
    }
}
