package com.faloshey.chorechampion.fragments.parent;

import android.app.AlertDialog;
import android.os.Bundle;
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
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ChildGridAdapter;
import com.faloshey.chorechampion.models.ChildModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParentHomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView welcomeUsername;
    private RecyclerView childRecyclerView;
    private ChildGridAdapter adapter;
    private List<ChildModel> childList;

    public ParentHomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parenthome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        welcomeUsername = view.findViewById(R.id.user_name);
        childRecyclerView = view.findViewById(R.id.child_account_grid);
        MaterialButton addChildBtn = view.findViewById(R.id.add_child_btn);
        MaterialButton editChildBtn = view.findViewById(R.id.edit_child_btn);

        // Config grid
        childRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        childList = new ArrayList<>();
        adapter = new ChildGridAdapter(childList, child -> {

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchProfileToChild(child);
            }
        });
        childRecyclerView.setAdapter(adapter);

        // Fetch parent name and child list
        if (auth.getCurrentUser() != null) {
            String parentId = auth.getCurrentUser().getUid();
            loadParentProfileData(parentId);
            listenToChildrenProfiles(parentId);
        }

        // Child Navigation
        addChildBtn.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_parentHomeFragment_to_createProfileFragment)
        );

        // Edit/delete pop-up
        editChildBtn.setOnClickListener(v -> showManageChildrenDialog());
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

        // Convert child options into string array
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
                .setTitle("Manage" + child.getUsername())
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





}
