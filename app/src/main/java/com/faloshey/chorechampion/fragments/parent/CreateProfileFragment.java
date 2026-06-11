package com.faloshey.chorechampion.fragments.parent;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.models.ChildModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputEditText usernameInput;

    public CreateProfileFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_createprofile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameInput = view.findViewById(R.id.child_username_input);
        MaterialButton createBtn = view.findViewById(R.id.create_profile_btn);

        createBtn.setOnClickListener(v -> createChildProfile(view));
    }

    private void createChildProfile(View view) {
        String childName = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(childName)) {
            Toast.makeText(getContext(), "A champion needs a name!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String parentId = auth.getCurrentUser().getUid();

        DocumentReference childDocRef = db.collection("Users")
                .document(parentId)
                .collection("children")
                .document();

        String childId = childDocRef.getId();
        ChildModel newChild = new ChildModel(childId, childName);

        childDocRef.set(newChild)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), childName + "has joined the realm!", Toast.LENGTH_LONG).show();

                    NavController navController = Navigation.findNavController(view);
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    String error = e.getMessage() != null ? e.getMessage() : "Database write rejected";
                    Toast.makeText(getContext(), "Error forging profile: " + error, Toast.LENGTH_LONG).show();
                });
    }
}
