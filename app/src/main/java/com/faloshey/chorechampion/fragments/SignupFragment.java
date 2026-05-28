package com.faloshey.chorechampion.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignupFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private TextInputEditText pinInput;

    public SignupFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = view.findViewById(R.id.signup_email);
        passwordInput = view.findViewById(R.id.signup_password);
        confirmInput = view.findViewById(R.id.signup_confirm);
        pinInput = view.findViewById(R.id.create_pin);
        MaterialButton createBtn = view.findViewById(R.id.create_btn);

        createBtn.setOnClickListener(v -> handleSignup());

    }


    private void handleSignup() {

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirm = confirmInput.getText() != null ? confirmInput.getText().toString().trim() : "";
        String pin = pinInput.getText() != null ? pinInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pin.length() != 4) {
            Toast.makeText(getContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(), "Signup Failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

                    // TODO: Add in displayName for parent profile and add to this and xml
                    Map<String, Object> parentData = new HashMap<>();
                    parentData.put("email", email);
                    parentData.put("pin", pin);
                    parentData.put("parentId", parentId);
                    parentData.put("createdAt", FieldValue.serverTimestamp());
                    parentData.put("children", new ArrayList<>());

                    db.collection("Users")
                            .document(parentId)
                            .set(parentData)
                            .addOnSuccessListener(aVoid ->{

                                Toast.makeText(getContext(), "Account Created!", Toast.LENGTH_SHORT).show();

                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).enterParentMode();
                                }
                            })
                            .addOnFailureListener(e->
                                Toast.makeText(getContext(), "Error saving profile.", Toast.LENGTH_SHORT).show()
                            );

                });


    }


}
