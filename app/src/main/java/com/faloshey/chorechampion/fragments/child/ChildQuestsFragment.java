package com.faloshey.chorechampion.fragments.child;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ChildQuestAdapter;
import com.faloshey.chorechampion.models.QuestModel;
import com.faloshey.chorechampion.viewmodels.AppSessionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class ChildQuestsFragment extends Fragment implements ChildQuestAdapter.OnQuestClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AppSessionViewModel sessionViewModel;
    private ListenerRegistration questsListener;

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private ChildQuestAdapter adapter;
    private MaterialButton actionBtn;
    private TextView hintText;

    private final List<QuestModel> masterQuestList = new ArrayList<>();
    private final List<QuestModel> filteredQuestList = new ArrayList<>();

    private String currentParentId = "";
    private String currentChildId = "";
    private String currentChildName = "";

    private QuestModel selectedQuest = null;

    public ChildQuestsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_childquests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sessionViewModel = new ViewModelProvider(requireActivity()).get(AppSessionViewModel.class);

        tabLayout = view.findViewById(R.id.child_quest_tabs);
        recyclerView = view.findViewById(R.id.child_quest_grid);
        actionBtn = view.findViewById(R.id.select_quests_btn);
        hintText = view.findViewById(R.id.child_quest_hint_msg);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChildQuestAdapter(filteredQuestList, this);
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateUiForCurrentTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        actionBtn.setOnClickListener(v -> {
            if (selectedQuest == null) return;

            if (tabLayout.getSelectedTabPosition() == 0) {
                completeQuest(selectedQuest);
            } else {
                claimQuest(selectedQuest);
            }
        });

        sessionViewModel.getActiveChild().observe(getViewLifecycleOwner(), child -> {
            if (child != null && auth.getCurrentUser() != null) {
                currentParentId = auth.getCurrentUser().getUid();
                currentChildId = child.getChildId();
                currentChildName = child.getUsername();

                listenToQuestsPipeline();
            }
        });

    }

    private void listenToQuestsPipeline() {
        if (questsListener != null) {
            questsListener.remove();
        }

        resetActionButtonState();

        questsListener = db.collection("Users").document(currentParentId)
                .collection("quests")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error syncing quest logs.", Toast.LENGTH_SHORT).show();
                        return;
                    }
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
    }

    @SuppressLint("SetTextI18n")
    private void updateUiForCurrentTab() {
        filteredQuestList.clear();
        int activeTab = tabLayout.getSelectedTabPosition();
        boolean isQuestBoard = (activeTab == 1);

        for (QuestModel quest : masterQuestList) {
            if (isQuestBoard) {
                boolean isUnassigned = quest.getAssignedChildId() == null ||
                        quest.getAssignedChildId().trim().isEmpty() ||
                        "Unassigned".equalsIgnoreCase(quest.getAssignedChildName());

                if (isUnassigned && !quest.isCompleted()) {
                    filteredQuestList.add(quest);
                }
            } else {
                if (currentChildId.equals(quest.getAssignedChildId()) && !quest.isCompleted()) {
                    filteredQuestList.add(quest);
                }
            }
        }

        adapter.updateData(filteredQuestList, isQuestBoard);
        resetActionButtonState();

        if (isQuestBoard) {
            hintText.setText("Claim a quest from the quest board!");
            actionBtn.setText("Accept Quest!");
            actionBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.new_blue));
        }
        else {
            hintText.setText("Turn in completed Quests!");
            actionBtn.setText("Turn In Quest!");
            actionBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.forest_green));
        }
    }

    private void claimQuest(QuestModel quest) {
        quest.setAssignedChildId(currentChildId);
        quest.setAssignedChildName(currentChildName);

        db.collection("Users").document(currentParentId)
                .collection("quests").document(quest.getQuestId())
                .set(quest)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Quest accepted! Added to your log.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Quest collection error.", Toast.LENGTH_SHORT).show());
    }

    private void completeQuest(QuestModel quest) {
        quest.setCompleted(true);

        db.collection("Users").document(currentParentId)
                .collection("quests").document(quest.getQuestId())
                .set(quest)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Quest submitted for parent approval!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Completion report dropped.", Toast.LENGTH_SHORT).show());
    }

    private void resetActionButtonState() {
        selectedQuest = null;
        actionBtn.setEnabled(false);
        actionBtn.setAlpha(0.5f);
    }

    @Override
    public void onQuestClick(QuestModel quest) {
        selectedQuest = quest;
        actionBtn.setEnabled(true);
        actionBtn.setAlpha(1.0f);
    }

    @Override
    public void onSelectedCleared() {
        resetActionButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (questsListener != null) {
            questsListener.remove();
        }
    }


}
