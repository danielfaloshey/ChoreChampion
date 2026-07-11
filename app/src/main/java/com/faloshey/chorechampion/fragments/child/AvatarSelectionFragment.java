package com.faloshey.chorechampion.fragments.child;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.AvatarGridAdapter;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class AvatarSelectionFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private Spinner classSelectionSpinner;
    private RecyclerView avatarRecycler;
    private AvatarGridAdapter avatarAdapter;
    private MaterialButton doneBtn;

    private String childDocumentId;

    private final int[] royaltyAvatars = {
            R.drawable.king1, R.drawable.queen1, R.drawable.prince1, R.drawable.prince2,
            R.drawable.princess1, R.drawable.princess2
    };

    private final int[] knightAvatars = {
            R.drawable.knight1, R.drawable.knight2, R.drawable.knight3, R.drawable.knight4,
            R.drawable.knight5, R.drawable.knight6
    };

    private final int[] wizardAvatars = {
            R.drawable.wizard1, R.drawable.wizard2, R.drawable.wizard3
    };

    private final int[] otherAvatars = {
            R.drawable.dragon1, R.drawable.dragon2, R.drawable.swords, R.drawable.crown, R.drawable.horse
    };

    private Map<String, int[]> classAvatarMap;
    private final List<String> spinnerCategories = new ArrayList<>();


    public AvatarSelectionFragment() { }

    private void initializeAvatarData() {
        classAvatarMap = new HashMap<>();
        classAvatarMap.put("Royalty", royaltyAvatars);
        classAvatarMap.put("Knights", knightAvatars);
        classAvatarMap.put("Wizards", wizardAvatars);
        classAvatarMap.put("Other", otherAvatars);

        spinnerCategories.add("Royalty");
        spinnerCategories.add("Knights");
        spinnerCategories.add("Wizards");
        spinnerCategories.add("Other");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatarselection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeAvatarData();

        if (getArguments() != null) {
            childDocumentId = getArguments().getString("childId");
        }

        classSelectionSpinner = view.findViewById(R.id.class_selector_spinner);
        avatarRecycler = view.findViewById(R.id.avatar_icon_recycler);
        doneBtn = view.findViewById(R.id.avatar_done_btn);

        avatarRecycler.setLayoutManager(new GridLayoutManager(getContext(), 3));
        avatarAdapter = new AvatarGridAdapter(requireContext(), royaltyAvatars);
        avatarRecycler.setAdapter(avatarAdapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                spinnerCategories
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classSelectionSpinner.setAdapter(spinnerAdapter);

        classSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = spinnerCategories.get(position);
                int[] selectedArray = classAvatarMap.get(selectedCategory);
                if (selectedArray != null) {
                    avatarAdapter.updateDataPool(selectedArray);
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        doneBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            saveSelectedAvatarToFirestore();
        });
    }

    private void saveSelectedAvatarToFirestore() {
        int selectedResId = avatarAdapter.getSelectedResourceId();
        if (selectedResId == -1) {
            Toast.makeText(getContext(), "Please choose an avatar for your champion!", Toast.LENGTH_SHORT).show();
            return;
        }

        String avatarResourceName = getResources().getResourceEntryName(selectedResId);

        if (auth.getCurrentUser() == null || childDocumentId == null) {
            Toast.makeText(getContext(), "Session tracking error. Profile could not be saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentUid = auth.getCurrentUser().getUid();

        db.collection("Users").document(parentUid)
                .collection("children").document(childDocumentId)
                .update("avatarName", avatarResourceName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Avatar customized successfully!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Database sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
