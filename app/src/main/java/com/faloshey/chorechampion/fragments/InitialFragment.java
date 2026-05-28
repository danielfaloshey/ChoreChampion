package com.faloshey.chorechampion.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.faloshey.chorechampion.R;
import com.google.android.material.button.MaterialButton;

public class InitialFragment extends Fragment {

    public InitialFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_initial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        MaterialButton loginBtn = view.findViewById(R.id.login_btn_initial);
        MaterialButton signupBtn = view.findViewById(R.id.signup_btn_initial);

        NavController navController = Navigation.findNavController(view);

        loginBtn.setOnClickListener(v ->
                navController.navigate(R.id.action_initialFragment_to_loginFragment)
        );

        signupBtn.setOnClickListener(v ->
                navController.navigate(R.id.action_initialFragment_to_signupFragment)
        );
    }


}
