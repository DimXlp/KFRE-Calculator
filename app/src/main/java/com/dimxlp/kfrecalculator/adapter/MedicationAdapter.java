package com.dimxlp.kfrecalculator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.model.Medication;

import java.util.ArrayList;
import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.ViewHolder> {

    private List<Medication> fullList;
    private List<Medication> filteredList;
    private int selectedPosition = -1;

    public MedicationAdapter(List<Medication> medicationList) {
        this.fullList = new ArrayList<>(medicationList);
        this.filteredList = new ArrayList<>(medicationList);
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            for (Medication med : fullList) {
                if (med.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(med);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_activated_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medication med = filteredList.get(position);
        holder.textView.setText(med.getName() + " " + med.getDosage());
        holder.itemView.setActivated(position == selectedPosition);
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public Medication getSelectedMedication() {
        return selectedPosition >= 0 ? filteredList.get(selectedPosition) : null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}