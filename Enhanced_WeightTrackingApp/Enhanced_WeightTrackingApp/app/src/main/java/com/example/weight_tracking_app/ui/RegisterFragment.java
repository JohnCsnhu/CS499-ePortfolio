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
import com.example.weight_tracking_app.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private WeightViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        binding.buttonCreate.setOnClickListener(v -> attemptRegister());
        binding.buttonBackToLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    private void attemptRegister() {
        String username = binding.editUsername.getText().toString().trim();
        String password = binding.editPassword.getText().toString();
        String confirm = binding.editConfirm.getText().toString();
        String phone = binding.editPhone.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            binding.textError.setText("Username and password are required.");
            return;
        }
        if (password.length() < 6) {
            binding.textError.setText("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            binding.textError.setText("Passwords do not match.");
            return;
        }
        binding.textError.setText("");
        binding.buttonCreate.setEnabled(false);
        viewModel.register(username, password, phone, success -> requireActivity().runOnUiThread(() -> {
            binding.buttonCreate.setEnabled(true);
            if (success) {
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true).build();
                NavHostFragment.findNavController(this).navigate(R.id.dashboardFragment, null, options);
            } else {
                binding.textError.setText("That username is already taken.");
            }
        }));
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
