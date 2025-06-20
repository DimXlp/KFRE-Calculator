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
import com.dimxlp.kfrecalculator.model.KfreCalculation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KfreAssessmentAdapter extends RecyclerView.Adapter<KfreAssessmentAdapter.KfreViewHolder> {

    public interface AssessmentClickListener {
        void onAssessmentClick(KfreCalculation calculation);
        void onAssessmentDelete(KfreCalculation calculation);
    }

    private final List<KfreCalculation> items;
    private final AssessmentClickListener listener;
    private final Context context;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private int lastSelectedPosition = RecyclerView.NO_POSITION;


    public KfreAssessmentAdapter(Context context, List<KfreCalculation> items, AssessmentClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KfreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient_kfre_assessment, parent, false);
        return new KfreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KfreViewHolder holder, int position) {
        KfreCalculation calc = items.get(position);

        String risk2 = String.format(Locale.getDefault(), "%.2f", calc.getRisk2Yr());
        String risk5 = String.format(Locale.getDefault(), "%.2f", calc.getRisk5Yr());
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(calc.getCreatedAt()));

        holder.txtRiskSummary.setText("2-Yr Risk: " + risk2 + "% • 5-Yr Risk: " + risk5 + "%");
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

    public void updateData(List<KfreCalculation> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    static class KfreViewHolder extends RecyclerView.ViewHolder {
        TextView txtRiskSummary, txtDate, txtNote;
        ImageButton btnDelete;

        public KfreViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRiskSummary = itemView.findViewById(R.id.txtRiskSummary);
            txtDate = itemView.findViewById(R.id.txtAssessmentDate);
            txtNote = itemView.findViewById(R.id.txtAssessmentNote);
            btnDelete = itemView.findViewById(R.id.btnDeleteAssessment);
        }
    }
}
