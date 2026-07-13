package com.faloshey.chorechampion.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;


public class LoginFragment extends Fragment {

    private FirebaseAuth auth;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    private CircularProgressIndicator progressIndicator;
    private MaterialButton loginBtn;
    private MaterialButton googleLogInBtn;
    private MaterialButton forgotPasswordBtn;


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
        loginBtn = view.findViewById(R.id.login_btn);
        forgotPasswordBtn = view.findViewById(R.id.forgot_password_btn);
        googleLogInBtn = view.findViewById(R.id.google_login);
        progressIndicator = view.findViewById(R.id.login_progress);
        ImageButton backButton = view.findViewById(R.id.login_btn_back);
        TextInputLayout passwordLayout = view.findViewById(R.id.password_login_layout);
        TextInputLayout emailLayout = view.findViewById(R.id.email_login_layout);

        passwordLayout.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.macondo));
        emailLayout.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.macondo));

        loginBtn.setOnClickListener(v ->{
            AudioManager.getInstance().playSound("cork_pop");
            handleLogin();
        });
        forgotPasswordBtn.setOnClickListener(v ->{
            AudioManager.getInstance().playSound("cork_pop");
            handleForgotPassword();
        });
        googleLogInBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            logInWithGoogle();
        });

        backButton.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            Navigation.findNavController(view).navigateUp();
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (progressIndicator != null) {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        loginBtn.setEnabled(!isLoading);
        googleLogInBtn.setEnabled(!isLoading);
        forgotPasswordBtn.setEnabled(!isLoading);
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

        setLoadingState(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {

                        setLoadingState(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    navigateToParentMode();
                });

    }

    private void logInWithGoogle() {

        setLoadingState(true);

        androidx.credentials.CredentialManager credentialManager = androidx.credentials.CredentialManager.create(requireContext());

        com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption googleIdOption =
                new com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id))
                        .build();

        androidx.credentials.GetCredentialRequest request = new androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        credentialManager.getCredentialAsync(
                requireContext(),
                request,
                null,
                mainHandler::post,
                new androidx.credentials.CredentialManagerCallback<>() {

                    @Override
                    public void onResult(androidx.credentials.GetCredentialResponse result) {
                        androidx.credentials.Credential credential = result.getCredential();

                        if (credential instanceof androidx.credentials.CustomCredential &&
                                credential.getType().equals(com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

                            try {
                                com.google.android.libraries.identity.googleid.GoogleIdTokenCredential googleIdTokenCredential =
                                        com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.getData());

                                String idToken = googleIdTokenCredential.getIdToken();

                                com.google.firebase.auth.AuthCredential firebaseAuthCredential =
                                        com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null);

                                authenticateLoginWithGoogle(firebaseAuthCredential);

                            } catch (IllegalArgumentException e) {
                                setLoadingState(false);
                                android.util.Log.e("GOOGLE_AUTH", "Token decryption parsing failure", e);
                                Toast.makeText(getContext(), "Google Token Parsing Error", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            setLoadingState(false);
                        }
                    }

                    @Override
                    public void onError(@NonNull androidx.credentials.exceptions.GetCredentialException e) {
                        setLoadingState(false);

                        String errorString = e.getMessage() != null ? e.getMessage() : "";
                        android.util.Log.e("GOOGLE_AUTH", "Credential system error message: " + errorString, e);

                        if (errorString.contains("GetCredentialCancellationException") ||
                                errorString.contains("cancel") ||
                                e instanceof androidx.credentials.exceptions.GetCredentialCancellationException) {

                            Toast.makeText(getContext(), "Login canceled.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Google login unavailable right now.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void authenticateLoginWithGoogle(com.google.firebase.auth.AuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {

                        setLoadingState(false);
                        String err = task.getException() != null ? task.getException().getMessage() : "Firebase handshake failed";
                        Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
                        return;
                    }

                    com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        String welcomeName = user.getDisplayName() != null ? user.getDisplayName() : "Parent";
                        Toast.makeText(getContext(), "Welcome back, " + welcomeName + "!", Toast.LENGTH_SHORT).show();
                    }

                    navigateToParentMode();
                });
    }

    private void handleForgotPassword() {

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Enter your email in the email field first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToParentMode() {
        if (getActivity() instanceof MainActivity) {

            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("ChoreChampionPrefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putString("last_active_role", "PARENT").apply();
            ((MainActivity) getActivity()).enterParentMode();
        }
    }

}
