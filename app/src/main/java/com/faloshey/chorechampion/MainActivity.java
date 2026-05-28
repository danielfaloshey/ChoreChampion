package com.faloshey.chorechampion;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private FrameLayout bottomNavContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavContainer = findViewById(R.id.bottom_nav_container);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        navController = Objects.requireNonNull(navHostFragment).getNavController();

        navController.setGraph(R.navigation.auth_nav_graph);

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

        if (inventory != null) {
            inventory.setOnClickListener(v ->
                    navController.navigate(R.id.parentInventoryFragment));
        }

        if (quests != null) {
            quests.setOnClickListener(v ->
                    navController.navigate(R.id.parentQuestsFragment));
        }

        if (home != null) {
            home.setOnClickListener(v ->
                    navController.navigate(R.id.parentHomeFragment));
        }

        if (shop != null) {
            shop.setOnClickListener(v ->
                    navController.navigate(R.id.parentShopFragment));
        }

        if (settings != null) {
            settings.setOnClickListener(v ->
                    navController.navigate(R.id.parentSettingsFragment));
        }
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

        if (quests != null) {
            quests.setOnClickListener(v ->
                    navController.navigate(R.id.childQuestsFragment));
        }

        if (home != null) {
            home.setOnClickListener(v ->
                    navController.navigate(R.id.childHomeFragment));
        }

        if (shop != null) {
            shop.setOnClickListener(v ->
                    navController.navigate(R.id.childShopFragment));
        }

    }




}