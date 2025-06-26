package com.dimxlp.kfrecalculator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.model.CkdEpiCalculation; // Changed model

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CkdEpiAssessmentAdapter extends RecyclerView.Adapter<CkdEpiAssessmentAdapter.CkdEpiViewHolder> {

    public interface AssessmentClickListener {
        void onAssessmentClick(CkdEpiCalculation calculation); // Changed model
        void onAssessmentDelete(CkdEpiCalculation calculation); // Changed model
    }

    private final List<CkdEpiCalculation> items; // Changed model
    private final AssessmentClickListener listener;
    private final Context context;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private int lastSelectedPosition = RecyclerView.NO_POSITION;


    public CkdEpiAssessmentAdapter(Context context, List<CkdEpiCalculation> items, AssessmentClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CkdEpiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient_ckd_epi_assessment, parent, false);
        return new CkdEpiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CkdEpiViewHolder holder, int position) {
        CkdEpiCalculation calc = items.get(position); // Changed model

        // Changed data binding logic for CKD-EPI result
        String egfr = String.format(Locale.getDefault(), "%.2f", calc.getResult());
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(calc.getCreatedAt()));

        holder.txtCkdEpiResult.setText("eGFR: " + egfr + " mL/min/1.73m²");
        holder.txtDate.setText(date);

        String notes = calc.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            holder.txtNote.setText(notes);
            holder.txtNote.setVisibility(View.VISIBLE);
        } else {
            holder.txtNote.setVisibility(View.GONE);
        }

        holder.btnDelete.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onAssessmentClick(calc));

        holder.itemView.setOnLongClickListener(v -> {
            lastSelectedPosition = selectedPosition;
            selectedPosition = (selectedPosition == position) ? RecyclerView.NO_POSITION : position;

            if (lastSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(lastSelectedPosition);
            }
            notifyItemChanged(selectedPosition);
            return true;
        });

        holder.btnDelete.setOnClickListener(v -> listener.onAssessmentDelete(calc));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<CkdEpiCalculation> newItems) { // Changed model
        items.clear();
        items.addAll(newItems);
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    // Renamed ViewHolder
    static class CkdEpiViewHolder extends RecyclerView.ViewHolder {
        TextView txtCkdEpiResult, txtDate, txtNote; // Renamed TextView
        ImageButton btnDelete;

        public CkdEpiViewHolder(@NonNull View itemView) {
            super(itemView);
            // Updated view IDs to match the new layout
            txtCkdEpiResult = itemView.findViewById(R.id.txtCkdEpiResult);
            txtDate = itemView.findViewById(R.id.txtAssessmentDate);
            txtNote = itemView.findViewById(R.id.txtAssessmentNote);
            btnDelete = itemView.findViewById(R.id.btnDeleteAssessment);
        }
    }
}