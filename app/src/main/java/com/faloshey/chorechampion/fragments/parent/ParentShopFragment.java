package com.faloshey.chorechampion.fragments.parent;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ParentShopAdapter;
import com.faloshey.chorechampion.models.ShopItemModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
public class ParentShopFragment extends Fragment implements ParentShopAdapter.OnItemClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private RecyclerView shopRecyclerView;
    private ParentShopAdapter shopAdapter;
    private MaterialButton addBtn;
    private MaterialButton editBtn;

    private final List<ShopItemModel> shopItemList = new ArrayList<>();
    private ListenerRegistration shopListener;
    private ShopItemModel selectedShopItem = null;

    public ParentShopFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parentshop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        shopRecyclerView = view.findViewById(R.id.parent_shop_grid);
        addBtn = view.findViewById(R.id.shop_add_btn);
        editBtn = view.findViewById(R.id.shop_edit_btn);

        shopRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        shopAdapter = new ParentShopAdapter(shopItemList, this);
        shopRecyclerView.setAdapter(shopAdapter);

        addBtn.setOnClickListener(v -> showShopDialog(null));
        editBtn.setOnClickListener(v -> {
            if (selectedShopItem != null) {
                showShopDialog(selectedShopItem);
            }
        });

        updateButtonStates();

        if (auth.getCurrentUser() != null) {
            startShopListener(auth.getCurrentUser().getUid());
        }
    }

    private void startShopListener(String parentId) {
        shopListener = db.collection("Users").document(parentId)
                .collection("items")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        shopItemList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ShopItemModel item = doc.toObject(ShopItemModel.class);
                            if (item != null) {
                                shopItemList.add(item);
                            }
                        }
                        shopAdapter.updateList(shopItemList);
                        onItemCleared();
                    }
                });
    }

    private void showShopDialog(@Nullable ShopItemModel itemToEdit) {
        boolean isEditMode = (itemToEdit != null);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText titleInput = new EditText(getContext());
        titleInput.setHint("Item Title (ex. Extra Hour Screen Time)");
        if (isEditMode) titleInput.setText(itemToEdit.getTitle());
        layout.addView(titleInput);

        EditText descInput = new EditText(getContext());
        descInput.setHint("Description");
        if (isEditMode) descInput.setText(itemToEdit.getDescription());
        layout.addView(descInput);

        EditText costInput = new EditText(getContext());
        costInput.setHint("Gold Cost");
        costInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (isEditMode) costInput.setText(String.valueOf(itemToEdit.getCost()));
        layout.addView(costInput);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(layout)
                .setTitle(isEditMode ? "Modify Shop Item" : "New Shop Item")
                .setPositiveButton(isEditMode ? "Save Changes" : "Stock Item", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        if (isEditMode) {
            builder.setNeutralButton("Delete", (dialog, which) -> deleteItemFromVault(itemToEdit.getItemId()));
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String desc = descInput.getText().toString().trim();
            String costStr = costInput.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc) || TextUtils.isEmpty(costStr)) {
                Toast.makeText(getContext(), "All parameters are mandatory!", Toast.LENGTH_SHORT).show();
                return;
            }

            int cost = Integer.parseInt(costStr);
            String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
            String itemId = isEditMode ? itemToEdit.getItemId() : db.collection("Users").document(parentId).collection("items").document().getId();

            ShopItemModel updatedItem = new ShopItemModel(itemId, title, desc, cost);

            db.collection("Users").document(parentId)
                    .collection("items").document(itemId)
                    .set(updatedItem)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), isEditMode ? "Item modified!" : "Item stocked!", Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

    }

    private void deleteItemFromVault(String itemId) {
        String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("Users").document(parentId)
                .collection("items").document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Item removed from inventory.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to clear item.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onItemSelected(ShopItemModel item) {
        selectedShopItem = item;
        updateButtonStates();
    }

    @Override
    public void onItemCleared() {
        selectedShopItem = null;
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (selectedShopItem != null) {
            editBtn.setEnabled(true);
            editBtn.setAlpha(1.0f);
        } else {
            editBtn.setEnabled(false);
            editBtn.setAlpha(0.5f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shopListener != null) shopListener.remove();
    }
 }
