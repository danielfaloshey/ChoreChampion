package com.faloshey.chorechampion.fragments.parent;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ParentInventoryAdapter;
import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.models.ShopItemModel;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class ParentInventoryFragment extends Fragment implements ParentInventoryAdapter.OnRewardLongClickListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private Spinner childSelectorSpinner;
    private RecyclerView recyclerView;
    private ParentInventoryAdapter adapter;
    private MaterialButton inventoryInfoBtn;

    private List<ChildModel> childrenList = new ArrayList<>();
    private final List<ShopItemModel> selectedChildInventory = new ArrayList<>();

    private ListenerRegistration childrenListener;
    private ListenerRegistration inventoryListener;

    private String parentId = "";
    private String selectedChildId = "";

    public ParentInventoryFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        childSelectorSpinner = view.findViewById(R.id.child_selector_spinner);
        recyclerView = view.findViewById(R.id.inventory_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        inventoryInfoBtn = view.findViewById(R.id.parent_inventory_info);

        adapter = new ParentInventoryAdapter(selectedChildInventory, this);
        recyclerView.setAdapter(adapter);
        inventoryInfoBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            showInventoryInfo();
        });

        if (auth.getCurrentUser() != null) {
            parentId = auth.getCurrentUser().getUid();
            listenToChildrenList();
        }

        childSelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position >= 0 && position < childrenList.size()) {
                    selectedChildId = childrenList.get(position).getChildId();
                    listenToChildInventory();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });
    }

    private void listenToChildrenList() {
        childrenListener = db.collection("Users").document(parentId)
                .collection("children")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    childrenList.clear();
                    List<String> names = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChildModel child = doc.toObject(ChildModel.class);
                        if (child != null) {
                            childrenList.add(child);
                            names.add(child.getUsername() + "'s Inventory");
                        }
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            requireContext(),
                            R.layout.spinner_item_selected,
                            names
                    );
                    spinnerAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
                    childSelectorSpinner.setAdapter(spinnerAdapter);

                    if (!childrenList.isEmpty() && selectedChildId.isEmpty()) {
                        childSelectorSpinner.setSelection(0);
                        selectedChildId = childrenList.get(0).getChildId();
                        listenToChildInventory();
                    }
                });
    }

    private void listenToChildInventory() {
        if (inventoryListener != null) inventoryListener.remove();
        if (selectedChildId.isEmpty()) return;

        android.util.Log.d("DEBUG_INVENTORY", "Initializing Snapshot Listener for Child ID: " + selectedChildId);

        inventoryListener = db.collection("Users").document(parentId)
                .collection("children").document(selectedChildId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        android.util.Log.e("DEBUG_INVENTORY", "Firestore Error: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        android.util.Log.w("DEBUG_INVENTORY", "Snapshot snapshot does not exist or is null.");
                        return;
                    }

                    selectedChildInventory.clear();

                    List<Map<String, Object>> rawRewardsList = (List<Map<String, Object>>) snapshot.get("rewards");

                    if (rawRewardsList != null) {

                        android.util.Log.d("DEBUG_INVENTORY", "Found " + rawRewardsList.size() + " total items in raw Firestore array.");

                        for (Map<String, Object> itemMap : rawRewardsList) {
                            try {
                                ShopItemModel item = new ShopItemModel();

                                if (itemMap.containsKey("itemId")) item.setItemId((String) itemMap.get("itemId"));
                                if (itemMap.containsKey("title")) item.setTitle((String) itemMap.get("title"));
                                if (itemMap.containsKey("description")) item.setDescription((String) itemMap.get("description"));

                                if (itemMap.containsKey("cost")) {
                                    Object costObj = itemMap.get("cost");
                                    if (costObj instanceof Long) {
                                        item.setCost(((Long) costObj).intValue());
                                    } else if (costObj instanceof Integer) {
                                        item.setCost((Integer) costObj);
                                    }
                                }

                                selectedChildInventory.add(item);

                                android.util.Log.d("DEBUG_INVENTORY", "Successfully parsed item: " + item.getTitle());

                            } catch (Exception e) {
                                android.util.Log.e("DEBUG_INVENTORY", "Error parsing item map data model: ", e);
                            }
                        }
                    } else {
                        android.util.Log.w("DEBUG_INVENTORY", "The 'rewards' field array is completely null/missing on this child's document.");
                    }
                    android.util.Log.d("DEBUG_INVENTORY", "Pushing " + selectedChildInventory.size() + " items to Adapter updateList()");
                    adapter.updateList(selectedChildInventory);
                });
    }

    @Override
    public void onRewardLongClicked(ShopItemModel item, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Fulfill Reward Document")
                .setMessage("Are you sure you want to fulfill and clear \"" + item.getTitle() + "\" from this inventory log?")
                .setPositiveButton("Fulfill & Delete", (dialog, which) -> deleteRewardFromDatabase(item))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @SuppressLint("SetTextI18n")
    private void showInventoryInfo() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_info_popup, null);

        TextView title = dialogView.findViewById(R.id.info_title);
        TextView message = dialogView.findViewById(R.id.info_message);
        MaterialButton closeBtn = dialogView.findViewById(R.id.info_close_btn);

        title.setText("Inventory Management");
        message.setText("• After child buys their reward, it is now time to give it to them in the real world. \n\n" +
                "• Once that is complete, select which child inventory from the drop down menu. \n\n " +
                "• Select the reward you gave them and long-press it to delete.");

        AlertDialog infoDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (infoDialog.getWindow() != null) {
            infoDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        closeBtn.setOnClickListener(v -> infoDialog.dismiss());
        infoDialog.show();
    }

    private void deleteRewardFromDatabase(ShopItemModel item) {
        DocumentReference childRef = db.collection("Users").document(parentId)
                .collection("children").document(selectedChildId);

        Map<String, Object> targetItemMap = new HashMap<>();
        targetItemMap.put("itemId", item.getItemId());
        targetItemMap.put("title", item.getTitle());
        targetItemMap.put("description", item.getDescription());
        targetItemMap.put("cost", item.getCost());

        childRef.update("rewards", FieldValue.arrayRemove(targetItemMap))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Reward cleared from active inventory.", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to modify array: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (childrenListener != null) childrenListener.remove();
        if (inventoryListener != null) inventoryListener.remove();
    }
}
