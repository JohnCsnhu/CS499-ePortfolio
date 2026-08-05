package com.example.weight_tracking_app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weight_tracking_app.databinding.ItemEntryBinding;
import com.example.weight_tracking_app.model.WeightEntry;
import com.example.weight_tracking_app.util.HealthUtils;

import java.util.ArrayList;
import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {
    public interface Listener { void onEdit(WeightEntry entry); void onDelete(WeightEntry entry); }

    private final Listener listener;
    private final List<WeightEntry> items = new ArrayList<>();
    private String displayUnit = "lb";

    public EntryAdapter(Listener listener) { this.listener = listener; }

    /** Updates the list using DiffUtil so only changed rows are rebound. */
    public void submitList(List<WeightEntry> data, String unit) {
        List<WeightEntry> incoming = data == null ? new ArrayList<>() : new ArrayList<>(data);
        boolean unitChanged = !this.displayUnit.equals(unit);
        this.displayUnit = unit;
        if (unitChanged) {
            // A unit change affects every row's rendered text; rebind all.
            items.clear();
            items.addAll(incoming);
            notifyDataSetChanged();
            return;
        }
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new EntryDiff(new ArrayList<>(items), incoming));
        items.clear();
        items.addAll(incoming);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EntryViewHolder(ItemEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        WeightEntry item = items.get(position);
        holder.binding.textDate.setText(item.entryDate);
        holder.binding.textWeight.setText(HealthUtils.formatWeight(HealthUtils.fromKg(item.weightKg, displayUnit), displayUnit));
        holder.binding.textNotes.setText(item.notes == null || item.notes.isEmpty() ? "No notes" : item.notes);
        holder.binding.buttonEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.binding.buttonDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        final ItemEntryBinding binding;
        EntryViewHolder(ItemEntryBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }

    /** DiffUtil callback: identity by row id, content by the fields we display. */
    static class EntryDiff extends DiffUtil.Callback {
        private final List<WeightEntry> oldList;
        private final List<WeightEntry> newList;
        EntryDiff(List<WeightEntry> oldList, List<WeightEntry> newList) {
            this.oldList = oldList; this.newList = newList;
        }
        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        @Override public boolean areItemsTheSame(int o, int n) { return oldList.get(o).id == newList.get(n).id; }
        @Override public boolean areContentsTheSame(int o, int n) {
            WeightEntry a = oldList.get(o), b = newList.get(n);
            return a.entryDate.equals(b.entryDate)
                    && Double.compare(a.weightKg, b.weightKg) == 0
                    && a.notes.equals(b.notes);
        }
    }
}
