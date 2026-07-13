package com.faloshey.chorechampion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.faloshey.chorechampion.models.ChildModel;
import com.faloshey.chorechampion.service.AudioManager;
import com.faloshey.chorechampion.viewmodels.AppSessionViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private FrameLayout bottomNavContainer;
    private FirebaseAuth auth;
    private AppSessionViewModel sessionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager.getInstance().init(this);
        SharedPreferences prefs = getSharedPreferences("ChoreChampionPrefs", Context.MODE_PRIVATE);
        int savedVolume = prefs.getInt("music_volume", 40);
        AudioManager.getInstance().setMusicVolume(savedVolume);
        AudioManager.getInstance().startTheme();

        sessionViewModel = new ViewModelProvider(this).get(AppSessionViewModel.class);

        auth = FirebaseAuth.getInstance();
        bottomNavContainer = findViewById(R.id.bottom_nav_container);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer, (v, windowInsets) -> {

            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom
            );

            return windowInsets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            throw new IllegalStateException("NavHostFragment not found! Check your activity_main.xml layout tag.");
        }

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

        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.parent_nav_graph, false)
                .build();

        if (inventory != null) {
            inventory.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.parentInventoryFragment, null, navOptions);
            });
        }
        if (quests != null) {
            quests.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.parentQuestsFragment, null, navOptions);
            });
        }
        if (home != null) {
            home.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.parentHomeFragment, null, navOptions);
            });
        }
        if (shop != null) {
            shop.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.parentShopFragment, null, navOptions);
            });
        }
        if (settings != null) {
            settings.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.parentSettingsFragment, null, navOptions);
            });
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

    public void navigateToLoginGate() {
        navController.setGraph(R.navigation.auth_nav_graph);
        bottomNavContainer.removeAllViews();
    }

    private void setUpChildNavClicks() {
        View quests = findViewById(R.id.child_nav_quests_container);
        View home = findViewById(R.id.child_nav_home_container);
        View shop = findViewById(R.id.child_nav_shop_container);

        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.child_nav_graph, false)
                .build();

        if (quests != null) {
            quests.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.childQuestsFragment, null, navOptions);
            });
        }
        if (home != null) {
            home.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.childHomeFragment, null, navOptions);
            });
        }
        if (shop != null) {
            shop.setOnClickListener(v -> {
                AudioManager.getInstance().playSound("cork_pop");
                navController.navigate(R.id.childShopFragment, null, navOptions);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AudioManager.getInstance().startTheme();
    }

    @Override
    public void onPause() {
        super.onPause();
        AudioManager.getInstance().pauseTheme();
    }
}