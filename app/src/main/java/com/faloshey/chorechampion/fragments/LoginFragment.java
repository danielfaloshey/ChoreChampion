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


public class LoginFragment extends Fragment {

    private FirebaseAuth auth;

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    public LoginFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        emailInput = view.findViewById(R.id.login_email);
        passwordInput = view.findViewById(R.id.login_password);
        MaterialButton loginBtn = view.findViewById(R.id.login_btn);
        MaterialButton forgotPasswordBtn = view.findViewById(R.id.forgot_password_btn);

        loginBtn.setOnClickListener(v -> handleLogin());

        forgotPasswordBtn.setOnClickListener(v-> handleForgotPassword());
    }

    private void handleLogin() {

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(), "Login failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).enterParentMode();
                    }
                });

    }

    private void handleForgotPassword() {

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid->
                        Toast.makeText(getContext(), "Reset email sent", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show()
                );
    }

}
