package com.faloshey.chorechampion.fragments.child;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ChildShopAdapter;
import com.faloshey.chorechampion.models.NotificationModel;
import com.faloshey.chorechampion.models.ShopItemModel;
import com.faloshey.chorechampion.service.AudioManager;
import com.faloshey.chorechampion.viewmodels.AppSessionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class ChildShopFragment extends Fragment implements ChildShopAdapter.OnItemClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AppSessionViewModel sessionViewModel;

    private TextView currentGoldLabel;
    private RecyclerView recyclerView;
    private ChildShopAdapter adapter;
    private MaterialButton buyBtn;
    private MaterialButton childShopInfoBtn;

    private final List<ShopItemModel> masterItemList = new ArrayList<>();
    private ListenerRegistration shopListener;
    private ListenerRegistration childProfileListener;

    private String currentParentId = "";
    private String currentChildId = "";
    private ShopItemModel selectedItem = null;
    private long currentChildGold = 0;

    public ChildShopFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_childshop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionViewModel = new ViewModelProvider(requireActivity()).get(AppSessionViewModel.class);

        currentGoldLabel = view.findViewById(R.id.current_gold_label);
        recyclerView = view.findViewById(R.id.child_shop_grid);
        buyBtn = view.findViewById(R.id.buy_btn);
        childShopInfoBtn = view.findViewById(R.id.child_shop_info);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChildShopAdapter(masterItemList, this);
        recyclerView.setAdapter(adapter);

        resetBuyButtonState();

        buyBtn.setOnClickListener(v -> {
            if (selectedItem != null) {
                AudioManager.getInstance().playSound("purchase_complete");
                purchaseItemFromShop(selectedItem);
            }
        });

        childShopInfoBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            showChildShopInfo();
        });

        sessionViewModel.getActiveChild().observe(getViewLifecycleOwner(), child -> {
            if (child != null && auth.getCurrentUser() != null) {
                currentParentId = auth.getCurrentUser().getUid();
                currentChildId = child.getChildId();
                startShopInventoryListener();
                startChildProfileListener();
            }
        });
    }

    private void startShopInventoryListener() {
        if (shopListener != null) shopListener.remove();

        shopListener = db.collection("Users").document(currentParentId)
                .collection("items")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        masterItemList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ShopItemModel item = doc.toObject(ShopItemModel.class);
                            if (item != null) {
                                masterItemList.add(item);
                            }
                        }
                        adapter.updateList(masterItemList);
                        onItemCleared();
                    }
                });
    }

    private void startChildProfileListener() {
        if (childProfileListener != null) childProfileListener.remove();

        childProfileListener = db.collection("Users").document(currentParentId)
                .collection("children").document(currentChildId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    Long gold = snapshot.getLong("gold");
                    currentChildGold = (gold != null) ? gold : 0;
                    updateGoldDisplay();
                });
    }

    @SuppressLint("SetTextI18n")
    private void updateGoldDisplay() {
        currentGoldLabel.setText("Current Gold: " + currentChildGold + " 🪙");
    }

    @SuppressWarnings("DataFlowIssue")
    private void purchaseItemFromShop(ShopItemModel item) {
        if (currentChildGold < item.getCost()) {
            Toast.makeText(getContext(), "You don't have enough gold for this reward!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference childRef = db.collection("Users").document(currentParentId)
                .collection("children").document(currentChildId);

        DocumentReference itemStoreRef = db.collection("Users").document(currentParentId)
                .collection("items").document(item.getItemId());

        java.util.Map<String, Object> serializedItemMap = new java.util.HashMap<>();
        serializedItemMap.put("itemId", item.getItemId());
        serializedItemMap.put("title", item.getTitle());
        serializedItemMap.put("description", item.getDescription());
        serializedItemMap.put("cost", item.getCost());

        db.runTransaction(transaction -> {
            DocumentSnapshot childSnap = transaction.get(childRef);
            if (!childSnap.exists()) return null;

            String verifiedChildName = childSnap.getString("username");

            long liveGold = childSnap.getLong("gold") != null ? childSnap.getLong("gold") : 0;

            if (liveGold >= item.getCost()) {
                long updatedGold = liveGold - item.getCost();
                transaction.update(childRef, "gold", updatedGold);
                transaction.update(childRef, "rewards", FieldValue.arrayUnion(serializedItemMap));
                transaction.delete(itemStoreRef);

            } else {
                throw new RuntimeException("Insufficient gold balance inside database verification layer.");
            }

            return verifiedChildName;
        }).addOnSuccessListener(childNameResult -> {

            String finalName = (childNameResult != null) ? childNameResult : "A child";

            createNotification(
                    finalName,
                    "Purchased: " + item.getTitle()
            );
            showPurchaseCompleteDialog();
            onItemCleared();
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Transaction rolled back: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showPurchaseCompleteDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.shop_dialog, null);

        ImageView treasureGif = dialogView.findViewById(R.id.treasureIcon);
        com.bumptech.glide.Glide.with(requireContext())
                .asGif()
                .load(R.drawable.treasure)
                .placeholder(R.drawable.treasure)
                .into(treasureGif);

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

        java.util.List<Integer> shopColors = java.util.Arrays.asList(0xFFFFD700, 0xFF00E676, 0xFFFF4081, 0xFF7C4DFF);

        java.util.List<nl.dionsegijn.konfetti.core.models.Shape> circleShapes =
                java.util.Collections.singletonList(nl.dionsegijn.konfetti.core.models.Shape.Circle.INSTANCE);

        nl.dionsegijn.konfetti.core.emitter.EmitterConfig burstEmitter =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(100, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);

        nl.dionsegijn.konfetti.core.Party leftCannon = new nl.dionsegijn.konfetti.core.PartyFactory(burstEmitter)
                .colors(shopColors)
                .shapes(circleShapes)
                .angle(315)
                .spread(45)
                .setSpeedBetween(15f, 30f)
                .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.0, 1.0))
                .build();

        nl.dionsegijn.konfetti.core.Party rightCannon = new nl.dionsegijn.konfetti.core.PartyFactory(burstEmitter)
                .colors(shopColors)
                .shapes(circleShapes)
                .angle(225)
                .spread(45)
                .setSpeedBetween(15f, 30f)
                .position(new nl.dionsegijn.konfetti.core.Position.Relative(1.0, 1.0))
                .build();

        konfettiView.start(java.util.Arrays.asList(leftCannon, rightCannon));

        dialogView.findViewById(R.id.close_shop_dialog).setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            alertDialog.dismiss();}
        );
    }

    private void showChildShopInfo() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_child_shop_info, null);

        MaterialButton closeBtn = dialogView.findViewById(R.id.info_close_btn);

        AlertDialog infoDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (infoDialog.getWindow() != null) {
            infoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            infoDialog.dismiss();
        });

        infoDialog.show();
    }

    private void resetBuyButtonState() {
        selectedItem = null;
        buyBtn.setEnabled(false);
        buyBtn.setAlpha(0.5f);
    }

    @Override
    public void onItemSelected(ShopItemModel item) {
        selectedItem = item;
        buyBtn.setEnabled(true);
        buyBtn.setAlpha(1.0f);
    }

    @Override
    public void onItemCleared() {
        resetBuyButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shopListener != null) shopListener.remove();
        if (childProfileListener != null) childProfileListener.remove();
    }

    private void createNotification(String username, String actionText) {

        DocumentReference newNotificationRef = db.collection("Users").document(currentParentId)
                .collection("notifications").document();

        String notificationId = newNotificationRef.getId();
        long currentTimestamp = System.currentTimeMillis();

        NotificationModel newNotification = new NotificationModel(
                notificationId,
                username,
                actionText,
                "purchase_complete",
                currentTimestamp
        );

        newNotificationRef.set(newNotification)
                .addOnFailureListener(e -> Log.e("NotificationError", "Failed to compile log track", e));
    }

}
