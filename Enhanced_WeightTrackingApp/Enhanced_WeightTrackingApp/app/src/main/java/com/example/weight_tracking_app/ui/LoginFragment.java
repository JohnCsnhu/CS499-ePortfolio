package com.example.weight_tracking_app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.weight_tracking_app.R;
import com.example.weight_tracking_app.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private WeightViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        binding.buttonLogin.setOnClickListener(v -> attemptLogin());
        binding.buttonGoRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_loginFragment_to_registerFragment));
    }

    private void attemptLogin() {
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            binding.textError.setText("Enter your username and password.");
            return;
        }
        binding.textError.setText("");
        binding.buttonLogin.setEnabled(false);
        viewModel.login(username, password, success -> requireActivity().runOnUiThread(() -> {
            binding.buttonLogin.setEnabled(true);
            if (success) {
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true).build();
                NavHostFragment.findNavController(this).navigate(R.id.dashboardFragment, null, options);
            } else {
                binding.textError.setText("Incorrect username or password.");
            }
        }));
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
