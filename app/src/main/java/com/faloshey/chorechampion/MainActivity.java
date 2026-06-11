package com.faloshey.chorechampion;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.viewmodels.AppSessionViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private FrameLayout bottomNavContainer;
    private FirebaseAuth auth;
    private AppSessionViewModel sessionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sessionViewModel = new ViewModelProvider(this).get(AppSessionViewModel.class);

        auth = FirebaseAuth.getInstance();
        bottomNavContainer = findViewById(R.id.bottom_nav_container);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = Objects.requireNonNull(navHostFragment).getNavController();

        if (auth.getCurrentUser() != null) {
            enterParentMode();
        }
        else {
            navController.setGraph(R.navigation.auth_nav_graph);
        }

    }

    // Enter Parent Mode
    public void enterParentMode() {
        navController.setGraph(R.navigation.parent_nav_graph);

        bottomNavContainer.removeAllViews();
        getLayoutInflater().inflate(R.layout.custom_bottom_nav, bottomNavContainer);

        setUpParentNavClicks();
    }

    private void setUpParentNavClicks() {

        View inventory = findViewById(R.id.nav_inventory_container);
        View quests = findViewById(R.id.nav_quests_container);
        View home = findViewById(R.id.nav_home_container);
        View shop = findViewById(R.id.nav_shop_container);
        View settings = findViewById(R.id.nav_settings_container);

        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                .build();

        if (inventory != null) {
            inventory.setOnClickListener(v -> navController.navigate(R.id.parentInventoryFragment, null, navOptions));
        }
        if (quests != null) {
            quests.setOnClickListener(v -> navController.navigate(R.id.parentQuestsFragment, null, navOptions));
        }
        if (home != null) {
            home.setOnClickListener(v -> navController.navigate(R.id.parentHomeFragment, null, navOptions));
        }
        if (shop != null) {
            shop.setOnClickListener(v -> navController.navigate(R.id.parentShopFragment, null, navOptions));
        }
        if (settings != null) {
            settings.setOnClickListener(v -> navController.navigate(R.id.parentSettingsFragment, null, navOptions));
        }
    }

    public void switchProfileToChild(ChildModel child) {
        sessionViewModel.setActiveChild(child);

        enterChildMode();
    }

    // Enter Child Mode
    public void enterChildMode() {
        navController.setGraph(R.navigation.child_nav_graph);

        bottomNavContainer.removeAllViews();
        getLayoutInflater().inflate(R.layout.custom_nav_child, bottomNavContainer);

        setUpChildNavClicks();
    }

    private void setUpChildNavClicks() {
        View quests = findViewById(R.id.child_nav_quests_container);
        View home = findViewById(R.id.child_nav_home_container);
        View shop = findViewById(R.id.child_nav_shop_container);

        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                .build();

        if (quests != null) {
            quests.setOnClickListener(v -> navController.navigate(R.id.childQuestsFragment, null, navOptions));
        }
        if (home != null) {
            home.setOnClickListener(v -> navController.navigate(R.id.childHomeFragment, null, navOptions));
        }
        if (shop != null) {
            shop.setOnClickListener(v -> navController.navigate(R.id.childShopFragment, null, navOptions));
        }
    }

}