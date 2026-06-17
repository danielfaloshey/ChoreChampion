package com.faloshey.chorechampion.fragments.child;

import android.os.Bundle;
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
import androidx.navigation.Navigation;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.viewmodels.AppSessionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ChildHomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AppSessionViewModel sessionViewModel;
    private ListenerRegistration childProfileListener;

    private TextView usernameText;
    private TextView goldText;
    private TextView xpText;
    private ImageView avatarImage;

    public ChildHomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_childhome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sessionViewModel = new ViewModelProvider(requireActivity()).get(AppSessionViewModel.class);

        usernameText = view.findViewById(R.id.username_label);
        goldText = view.findViewById(R.id.gold_label);
        xpText = view.findViewById(R.id.xp_label);
        avatarImage = view.findViewById(R.id.child_home_avatar);

        MaterialButton editAvatarBtn = view.findViewById(R.id.edit_avatar_btn);
        MaterialButton exitBtn = view.findViewById(R.id.exit_btn);

        sessionViewModel.getActiveChild().observe(getViewLifecycleOwner(), child -> {
            if (child != null && auth.getCurrentUser() != null) {
                String parentId = auth.getCurrentUser().getUid();
                String childId = child.getChildId();

                listenToChildUpdates(parentId, childId);
            }
        });

        editAvatarBtn.setOnClickListener(v -> {
            ChildModel currentChild = sessionViewModel.getActiveChild().getValue();
            if (currentChild != null) {
                Bundle args = new Bundle();
                args.putString("childId", currentChild.getChildId());
                Navigation.findNavController(view).navigate(R.id.action_childHomeFragment_to_avatarSelectionFragment, args);
            }
        });

        exitBtn.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_childHomeFragment_to_pinValidationFragment)
        );

    }

    private void listenToChildUpdates(String parentId, String childId) {
        if (childProfileListener != null) {
            childProfileListener.remove();
        }

        childProfileListener = db.collection("Users")
                .document(parentId)
                .collection("children")
                .document(childId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error syncing profile data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        ChildModel updatedChild = documentSnapshot.toObject(ChildModel.class);

                        if (updatedChild != null) {
                            usernameText.setText(updatedChild.getUsername());
                            goldText.setText("Gold: " + updatedChild.getGold());
                            xpText.setText("XP: " + updatedChild.getXp());

                            String savedAvatarName = updatedChild.getAvatarName();

                            if (savedAvatarName != null && !savedAvatarName.trim().isEmpty()) {
                                int resId = requireContext().getResources().getIdentifier(
                                        savedAvatarName,
                                        "drawable",
                                        requireContext().getPackageName()
                                );

                                if (resId != 0) {
                                    avatarImage.setImageResource(resId);
                                } else {
                                    avatarImage.setImageResource(R.drawable.ic_placeholder_user);
                                }
                            } else {
                                avatarImage.setImageResource(R.drawable.ic_placeholder_user);
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (childProfileListener != null) {
            childProfileListener.remove();
        }
    }

}
