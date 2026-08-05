package com.example.weight_tracking_app;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.weight_tracking_app.databinding.ActivityMainBinding;
import com.example.weight_tracking_app.notifications.NotificationHelper;
import com.example.weight_tracking_app.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.createChannel(this);
        requestRuntimePermissions();

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) throw new IllegalStateException("NavHostFragment missing");
        NavController navController = navHostFragment.getNavController();

        // Choose the start destination based on whether a user is already logged in.
        SessionManager session = new SessionManager(this);
        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        graph.setStartDestination(session.isLoggedIn() ? R.id.dashboardFragment : R.id.loginFragment);
        navController.setGraph(graph);

        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        // Hide the bottom navigation on the authentication screens.
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean authScreen = destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.registerFragment;
            binding.bottomNav.setVisibility(authScreen ? View.GONE : View.VISIBLE);
        });
    }

    private void requestRuntimePermissions() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        needed.add(Manifest.permission.SEND_SMS);
        if (!needed.isEmpty()) permissionLauncher.launch(needed.toArray(new String[0]));
    }
}
