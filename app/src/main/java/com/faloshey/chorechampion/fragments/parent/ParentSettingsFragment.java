package com.faloshey.chorechampion.fragments.parent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.faloshey.chorechampion.MainActivity;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.service.AudioManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class ParentSettingsFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;

    private TextInputEditText editDisplayName, editPin;
    private MaterialButton btnSaveName, btnSavePin, btnLogOut;
    private SeekBar volumeSeekBar;
    private TextView txtCoppaLink;

    private String currentParentId;

    public ParentSettingsFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = requireActivity().getSharedPreferences("ChoreChampionPrefs", Context.MODE_PRIVATE);

        editDisplayName = view.findViewById(R.id.edit_display_name);
        editPin = view.findViewById(R.id.edit_pin);
        btnSaveName = view.findViewById(R.id.save_name_change);
        btnSavePin = view.findViewById(R.id.save_pin_change);
        btnLogOut = view.findViewById(R.id.log_out_btn);
        volumeSeekBar = view.findViewById(R.id.music_volume_seekbar);
        txtCoppaLink = view.findViewById(R.id.coppa_link);

        if (auth.getCurrentUser() != null) {
            currentParentId = auth.getCurrentUser().getUid();
            loadSettingsData();
        }

        setupVolumeController();
        setupClickListeners();
    }

    private void loadSettingsData() {
        db.collection("Users").document(currentParentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        String name = documentSnapshot.getString("parentData.displayName");
                        String pin = documentSnapshot.getString("parentData.pin");

                        if (name != null) editDisplayName.setText(name);
                        if (pin != null) editPin.setText(pin);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to sync system preferences.", Toast.LENGTH_SHORT).show());
    }

    private void setupVolumeController() {
        int savedVolume = prefs.getInt("music_volume", 40);
        volumeSeekBar.setProgress(savedVolume);

        AudioManager.getInstance().setMusicVolume(savedVolume);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                AudioManager.getInstance().setMusicVolume(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                prefs.edit().putInt("music_volume", seekBar.getProgress()).apply();
            }
        });
    }

    private void setupClickListeners() {

        btnSaveName.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            String newName = Objects.requireNonNull(editDisplayName.getText()).toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Users").document(currentParentId)
                    .update("parentData.displayName", newName)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Display name updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show());
        });

        btnSavePin.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            String newPin = Objects.requireNonNull(editPin.getText()).toString().trim();

            if (newPin.length() != 4) {
                Toast.makeText(getContext(), "Security PIN must be exactly 4 digits.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Users").document(currentParentId)
                    .update("parentData.parentPin", newPin)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Parental PIN updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show());
        });

        txtCoppaLink.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");
            String url = "https://www.ftc.gov/legal-library/browse/rules/childrens-online-privacy-protection-rule-coppa";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        });

        btnLogOut.setOnClickListener(v -> {
            AudioManager.getInstance().playSound("cork_pop");

            auth.signOut();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToLoginGate();
            }
        });
    }

}
