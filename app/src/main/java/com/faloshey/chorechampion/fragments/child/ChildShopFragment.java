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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ChildShopAdapter;
import com.faloshey.chorechampion.models.ShopItemModel;
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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChildShopAdapter(masterItemList, this);
        recyclerView.setAdapter(adapter);

        resetBuyButtonState();

        buyBtn.setOnClickListener(v -> {
            if (selectedItem != null) {
                purchaseItemFromShop(selectedItem);
            }
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

            long liveGold = childSnap.getLong("gold") != null ? childSnap.getLong("gold") : 0;

            if (liveGold >= item.getCost()) {
                long updatedGold = liveGold - item.getCost();

                transaction.update(childRef, "gold", updatedGold);

                transaction.update(childRef, "rewards", FieldValue.arrayUnion(serializedItemMap));

                transaction.delete(itemStoreRef);

            } else {
                throw new RuntimeException("Insufficient gold balance inside database verification layer.");
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Purchase complete! 🎉", Toast.LENGTH_SHORT).show();
            onItemCleared();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Transaction rolled back: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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

}
