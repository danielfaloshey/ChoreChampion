package com.faloshey.chorechampion.fragments.parent;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ParentQuestAdapter;
import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.models.QuestModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParentQuestsFragment extends Fragment implements ParentQuestAdapter.OnQuestClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private RecyclerView questRecyclerView;
    private ParentQuestAdapter questAdapter;
    private MaterialButton addBtn;
    private MaterialButton editBtn;

    private List<QuestModel> questList = new ArrayList<>();
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

        questRecyclerView = view.findViewById(R.id.parent_quest_grid);
        addBtn = view.findViewById(R.id.quest_add_btn);
        editBtn = view.findViewById(R.id.quest_edit_btn);

        questRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        questAdapter = new ParentQuestAdapter(questList, this);
        questRecyclerView.setAdapter(questAdapter);

        updateButtonStates();

        addBtn.setOnClickListener(v -> showQuestDialog(null));
        editBtn.setOnClickListener(v -> {
            if (activeSelectedQuest != null) {
                showQuestDialog(activeSelectedQuest);
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
                .orderBy("createdAt")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        List<QuestModel> updatedQuests = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            QuestModel quest = doc.toObject(QuestModel.class);
                            if (quest != null) {
                                updatedQuests.add(quest);
                            }
                        }
                        questList = updatedQuests;
                        questAdapter.updateList(questList);
                        onQuestCleared();
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
                .setTitle(isEditMode ? "Modify Quest Blueprint" : "Forge New Quest Document");

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
