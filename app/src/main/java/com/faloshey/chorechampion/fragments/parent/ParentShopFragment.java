package com.faloshey.chorechampion.fragments.parent;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ParentShopAdapter;
import com.faloshey.chorechampion.models.ShopItemModel;
import com.faloshey.chorechampion.service.AudioManager;
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
    private MaterialButton parentShopInfoBtn;

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
        parentShopInfoBtn = view.findViewById(R.id.parent_shop_info);

        shopRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        shopAdapter = new ParentShopAdapter(shopItemList, this);
        shopRecyclerView.setAdapter(shopAdapter);

        addBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            showShopDialog(null);
        });
        parentShopInfoBtn.setOnClickListener(v ->{
            AudioManager.getInstance().playSound("cork_pop");
            showShopInfoDialog();
        });
        editBtn.setOnClickListener(v -> {
            if (selectedShopItem != null) {
                AudioManager.getInstance().playSound("cork_pop");
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

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_shop_form, null);

        TextView headerTitle = dialogView.findViewById(R.id.dialog_shop_header_title);
        TextView headerSubtitle = dialogView.findViewById(R.id.dialog_shop_header_subtitle);

        EditText titleInput = dialogView.findViewById(R.id.dialog_shop_title);
        EditText descInput = dialogView.findViewById(R.id.dialog_shop_desc);
        EditText costInput = dialogView.findViewById(R.id.dialog_shop_cost);

        MaterialButton btnDelete = dialogView.findViewById(R.id.btn_shop_delete);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_shop_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_shop_save);

        if (isEditMode) {
            headerTitle.setText(R.string.modify_reward_title);
            headerSubtitle.setText(R.string.modify_reward_subtitle);
            btnSave.setText(R.string.save_button);
            btnDelete.setVisibility(View.VISIBLE);

            titleInput.setText(itemToEdit.getTitle());
            descInput.setText(itemToEdit.getDescription());
            costInput.setText(String.valueOf(itemToEdit.getCost()));
        } else {
            headerTitle.setText(R.string.add_rewards_title);
            headerSubtitle.setText(R.string.add_rewards_subtitle);
            btnSave.setText(R.string.stock_btn);
            btnDelete.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(dialogView);

        AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        alertDialog.show();

        btnCancel.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            alertDialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            if (isEditMode) {
                deleteItemFromVault(itemToEdit.getItemId());
                alertDialog.dismiss();
            }
        });

        btnSave.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");

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

    @SuppressLint("SetTextI18n")
    private void showShopInfoDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_info_popup, null);

        TextView title = dialogView.findViewById(R.id.info_title);
        TextView message = dialogView.findViewById(R.id.info_message);
        MaterialButton closeBtn = dialogView.findViewById(R.id.info_close_btn);

        title.setText("Shop Management");
        message.setText("• Add New Shop Item: Click Add, enter item information. \n\n" +
                "• Edit/Delete Shop Item: Select item, click Edit \n\n " +
                "• *After child purchases items, it will show up in the Inventory screen where you can mark as complete after you give them their reward.*");

        AlertDialog infoDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (infoDialog.getWindow() != null) {
            infoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> infoDialog.dismiss());
        infoDialog.show();
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
