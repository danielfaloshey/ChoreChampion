package com.faloshey.chorechampion.fragments.child;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private MaterialButton childQuestInfoBtn;
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
        childQuestInfoBtn = view.findViewById(R.id.child_quest_info);
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
        childQuestInfoBtn.setOnClickListener(v -> showChildQuestInfo());

        sessionViewModel.getActiveChild().observe(getViewLifecycleOwner(), child -> {
            if (child != null && auth.getCurrentUser() != null) {
                currentParentId = auth.getCurrentUser().getUid();
                currentChildId = child.getChildId();
                currentChildName = child.getUsername();

                listenToQuestsPipeline();
            }
        });

    }

    private void showChildQuestInfo() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_child_quest_info, null);

        MaterialButton closeBtn = dialogView.findViewById(R.id.info_close_btn);

        AlertDialog infoDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (infoDialog.getWindow() != null) {
            infoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> infoDialog.dismiss());
        infoDialog.show();
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
                .addOnSuccessListener(aVoid -> showCelebrationDialog(quest.getGoldReward()))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Completion report dropped.", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetTextI18n")
    private void showCelebrationDialog(int goldEarned) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.reward_dialog, null);

        TextView goldTxt = dialogView.findViewById(R.id.reward_dialog_gold);
        goldTxt.setText("+" + goldEarned + " 🪙");

        ImageView rewardGif = dialogView.findViewById(R.id.rewardIcon);
        com.bumptech.glide.Glide.with(requireContext())
                .asGif()
                .load(R.drawable.crossed_swords)
                .placeholder(R.drawable.crossed_swords)
                .into(rewardGif);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext());
        builder.setView(dialogView);
        builder.setCancelable(false);

        androidx.appcompat.app.AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        alertDialog.show();

        nl.dionsegijn.konfetti.xml.KonfettiView konfettiView = dialogView.findViewById(R.id.konfetti_view);
        java.util.List<Integer> celebrationColors = java.util.Arrays.asList(0xFFFDD835, 0xFFE53935, 0xFF1E88E5, 0xFF43A047);

        nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(5, java.util.concurrent.TimeUnit.SECONDS).perSecond(100);

        nl.dionsegijn.konfetti.core.Party party = new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                .colors(celebrationColors)
                .angle(90)
                .spread(360)
                .setSpeedBetween(1f, 15f)
                .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.0, 0.0).between(new nl.dionsegijn.konfetti.core.Position.Relative(1.0, 0.0))) // Rain down from the entire horizontal ceiling
                .build();

        konfettiView.start(party);

        dialogView.findViewById(R.id.close_reward_dialog).setOnClickListener(v -> alertDialog.dismiss());
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
