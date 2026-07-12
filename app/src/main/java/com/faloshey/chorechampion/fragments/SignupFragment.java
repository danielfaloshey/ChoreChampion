package com.faloshey.chorechampion.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignupFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputEditText emailInput;
    private TextInputEditText nameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private TextInputEditText pinInput;

    private CircularProgressIndicator progressIndicator;
    private MaterialButton createBtn;
    private MaterialButton googleSignup;

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
        nameInput = view.findViewById(R.id.signup_display_name);
        passwordInput = view.findViewById(R.id.signup_password);
        confirmInput = view.findViewById(R.id.signup_confirm);
        pinInput = view.findViewById(R.id.create_pin);
        googleSignup = view.findViewById(R.id.google_signup);
        createBtn = view.findViewById(R.id.create_btn);
        ImageButton backButton = view.findViewById(R.id.signup_btn_back);
        progressIndicator = view.findViewById(R.id.signup_progress);

        googleSignup.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            signUpWithGoogle();
        });

        createBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            handleSignup();
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
        createBtn.setEnabled(!isLoading);
        googleSignup.setEnabled(!isLoading);
        nameInput.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        confirmInput.setEnabled(!isLoading);
        pinInput.setEnabled(!isLoading);
    }


    private void handleSignup() {

        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirm = confirmInput.getText() != null ? confirmInput.getText().toString().trim() : "";
        String pin = pinInput.getText() != null ? pinInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Parent username is required", Toast.LENGTH_SHORT).show();
            return;
        }
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
            return;
        }
        if (!pin.matches("\\d{4}")) {
            Toast.makeText(getContext(), "PIN must contain numbers only", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoadingState(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Signup Failed";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String parentId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

                    Map<String, Object> parentDetails = new HashMap<>();
                    parentDetails.put("displayName", name);
                    parentDetails.put("email", email);
                    parentDetails.put("pin", pin);
                    parentDetails.put("createdAt", FieldValue.serverTimestamp());

                    Map<String, Object> rootDocument = new HashMap<>();
                    rootDocument.put("parentData", parentDetails);

                    db.collection("Users")
                            .document(parentId)
                            .set(rootDocument)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Account Created!", Toast.LENGTH_SHORT).show();
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).enterParentMode();
                                }
                            })
                            .addOnFailureListener(e -> {
                                setLoadingState(false);
                                Toast.makeText(getContext(), "Auth succeeded, but profile save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                });


    }

    private void signUpWithGoogle() {
        String pin = pinInput.getText() != null ? pinInput.getText().toString().trim() : "";
        if (pin.length() != 4 || !pin.matches("\\d{4}")) {
            Toast.makeText(getContext(), "Please create a 4-digit numeric PIN first before signing up with Google!", Toast.LENGTH_LONG).show();
            return;
        }

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
                                String googleDisplayName = googleIdTokenCredential.getDisplayName();

                                com.google.firebase.auth.AuthCredential firebaseAuthCredential =
                                        com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null);

                                authenticateFirebaseWithGoogle(firebaseAuthCredential, googleDisplayName, pin);

                            } catch (IllegalArgumentException e) {
                                setLoadingState(false);
                                android.util.Log.e("GOOGLE_AUTH", "Token decryption parsing failure", e);
                                Toast.makeText(getContext(), "Google Token Parsing Error", Toast.LENGTH_SHORT).show();
                            }
                        } else {
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

                            Toast.makeText(getContext(), "Sign up canceled.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Google signup unavailable right now.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void authenticateFirebaseWithGoogle(com.google.firebase.auth.AuthCredential credential, String fallBackName, String securePin) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoadingState(false);
                        String err = task.getException() != null ? task.getException().getMessage() : "Firebase handshake failed";
                        Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
                        return;
                    }

                    com.google.firebase.auth.FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        setLoadingState(false);
                        return;
                    }

                    String parentId = user.getUid();
                    String email = user.getEmail() != null ? user.getEmail() : "";

                    String typedName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                    String finalDisplayName = !android.text.TextUtils.isEmpty(typedName) ? typedName :
                            (!android.text.TextUtils.isEmpty(fallBackName) ? fallBackName : "Chore Parent");

                    Map<String, Object> parentDetails = new HashMap<>();
                    parentDetails.put("displayName", finalDisplayName);
                    parentDetails.put("email", email);
                    parentDetails.put("pin", securePin);
                    parentDetails.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    Map<String, Object> rootDocument = new HashMap<>();
                    rootDocument.put("parentData", parentDetails);

                    db.collection("Users")
                            .document(parentId)
                            .set(rootDocument)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Welcome aboard, " + finalDisplayName + "!", Toast.LENGTH_SHORT).show();
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).enterParentMode();
                                }
                            })
                            .addOnFailureListener(e -> {
                                setLoadingState(false);
                                Toast.makeText(getContext(), "Auth complete, but database setup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }
}
