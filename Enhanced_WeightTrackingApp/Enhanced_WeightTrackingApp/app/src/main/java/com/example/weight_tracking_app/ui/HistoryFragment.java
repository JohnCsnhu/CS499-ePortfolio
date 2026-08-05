package com.example.weight_tracking_app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.weight_tracking_app.R;
import com.example.weight_tracking_app.databinding.FragmentHistoryBinding;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment implements EntryAdapter.Listener {
    private FragmentHistoryBinding binding;
    private WeightViewModel viewModel;
    private EntryAdapter adapter;
    private UserProfile profile;
    private String query = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(WeightViewModel.class);
        adapter = new EntryAdapter(this);
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
        binding.fabAdd.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_historyFragment_to_entryEditorFragment));
        binding.buttonExport.setOnClickListener(v -> viewModel.exportCsv(path -> requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), path != null ? "CSV exported to: " + path : "Export failed", Toast.LENGTH_LONG).show())));
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { query = s.toString().trim().toLowerCase(Locale.US); refreshAdapter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        viewModel.getProfile().observe(getViewLifecycleOwner(), userProfile -> { profile = userProfile; refreshAdapter(); });
        viewModel.getEntries().observe(getViewLifecycleOwner(), entries -> refreshAdapter());
    }

    /** Applies the in-memory search filter over the observed list, then submits to the adapter. */
    private void refreshAdapter() {
        String unit = profile != null ? profile.preferredUnit : "lb";
        List<WeightEntry> all = viewModel.getEntries().getValue();
        List<WeightEntry> filtered = new ArrayList<>();
        if (all != null) {
            for (WeightEntry e : all) {
                if (query.isEmpty()
                        || e.entryDate.toLowerCase(Locale.US).contains(query)
                        || (e.notes != null && e.notes.toLowerCase(Locale.US).contains(query))) {
                    filtered.add(e);
                }
            }
        }
        adapter.submitList(filtered, unit);
        boolean empty = filtered.isEmpty();
        binding.textEmpty.setText(query.isEmpty()
                ? "No entries yet. Tap + to add your first entry."
                : "No entries match your search.");
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEdit(WeightEntry entry) {
        Bundle bundle = new Bundle();
        bundle.putInt("entryId", entry.id);
        NavHostFragment.findNavController(this).navigate(R.id.action_historyFragment_to_entryEditorFragment, bundle);
    }

    @Override
    public void onDelete(WeightEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete entry")
                .setMessage("Delete " + entry.entryDate + " entry?")
                .setPositiveButton("Delete", (dialog, which) ->
                        viewModel.deleteEntry(entry.id, () -> requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show())))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
