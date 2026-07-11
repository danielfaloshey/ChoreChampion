package com.faloshey.chorechampion.fragments.child;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.service.AudioManager;
import com.faloshey.chorechampion.viewmodels.AppSessionViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PinValidationFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private AppSessionViewModel sessionViewModel;

    private EditText pinBox1, pinBox2, pinBox3, pinBox4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionViewModel = new ViewModelProvider(requireActivity()).get(AppSessionViewModel.class);

        pinBox1 = view.findViewById(R.id.pin_box1);
        pinBox2 = view.findViewById(R.id.pin_box2);
        pinBox3 = view.findViewById(R.id.pin_box3);
        pinBox4 = view.findViewById(R.id.pin_box4);
        MaterialButton enterPinBtn = view.findViewById(R.id.enter_pin_btn);
        MaterialButton backBtn = view.findViewById(R.id.pin_back_btn);

        setupPinBoxNavigation();

        backBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            Navigation.findNavController(view).navigateUp();
        });

        enterPinBtn.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            processPinValidation();
        });

        pinBox4.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null &&
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                processPinValidation();
                return true;
            }
            return false;
        });
    }

    private void setupPinBoxNavigation() {

        pinBox1.addTextChangedListener(new GenericTextWatcher(pinBox1, pinBox2));
        pinBox2.addTextChangedListener(new GenericTextWatcher(pinBox2, pinBox3));
        pinBox3.addTextChangedListener(new GenericTextWatcher(pinBox3, pinBox4));
        pinBox4.addTextChangedListener(new GenericTextWatcher(pinBox4, null));

        pinBox2.setOnKeyListener(new GenericDelKeyListener(pinBox2, pinBox1));
        pinBox3.setOnKeyListener(new GenericDelKeyListener(pinBox3, pinBox2));
        pinBox4.setOnKeyListener(new GenericDelKeyListener(pinBox4, pinBox3));
    }

    private void processPinValidation() {
        String inputPin = pinBox1.getText().toString().trim() +
                pinBox2.getText().toString().trim() +
                pinBox3.getText().toString().trim() +
                pinBox4.getText().toString().trim();

        if (inputPin.length() < 4) {
            Toast.makeText(getContext(), "Please input a complete 4-digit PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) return;
        String parentId = auth.getCurrentUser().getUid();

        hideKeyboard();

        db.collection("Users").document(parentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("parentData.pin")) {
                        String storedPin = documentSnapshot.getString("parentData.pin");

                        if (inputPin.equals(storedPin)) {
                            executeExitToParentDashboard();
                        } else {
                            Toast.makeText(getContext(), "Incorrect Security PIN. The Vault stays locked!", Toast.LENGTH_SHORT).show();
                            clearPinInputs();
                        }
                    } else {
                        Toast.makeText(getContext(), "Parent configuration profile missing. Please contact setup.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Network verification failed.", Toast.LENGTH_SHORT).show());
    }

    private void executeExitToParentDashboard() {

        sessionViewModel.clearActiveChild();

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            mainActivity.enterParentMode();
        }
    }

    private void clearPinInputs() {
        pinBox1.setText("");
        pinBox2.setText("");
        pinBox3.setText("");
        pinBox4.setText("");
        pinBox1.requestFocus();
    }

    private void hideKeyboard() {
        if (getView() != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }

    private static class GenericTextWatcher implements TextWatcher {
        private final View currentView;
        private final View nextView;

        public GenericTextWatcher(View currentView, View nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }

    private static class GenericDelKeyListener implements View.OnKeyListener {
        private final EditText currentBox;
        private final EditText previousBox;

        public GenericDelKeyListener(EditText currentBox, EditText previousBox) {
            this.currentBox = currentBox;
            this.previousBox = previousBox;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (currentBox.getText().toString().isEmpty() && previousBox != null) {
                    previousBox.setText("");
                    previousBox.requestFocus();
                    return true;
                }
            }
            return false;
        }
    }








}
