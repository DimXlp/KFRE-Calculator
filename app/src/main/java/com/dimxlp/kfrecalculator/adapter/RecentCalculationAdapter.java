package com.dimxlp.kfrecalculator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.model.KfreCalculation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentCalculationAdapter extends RecyclerView.Adapter<RecentCalculationAdapter.ViewHolder> {

    private final List<KfreCalculation> kfreCalculationList;

    public RecentCalculationAdapter(List<KfreCalculation> kfreCalculationList) {
        this.kfreCalculationList = kfreCalculationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_calculation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KfreCalculation calc = kfreCalculationList.get(position);

        holder.date.setText(formatDate(calc.getCreatedAt()));
        holder.egfr.setText("eGFR: " + formatEgfr(calc.getEgfr()));
        holder.acr.setText("ACR: " + formatAcr(calc.getAcr()));
        holder.kfre.setText(formatKfreRisk(calc.getRisk2Yr()));
    }

    @Override
    public int getItemCount() {
        return kfreCalculationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date, egfr, acr, kfre;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.recentCalcDate);
            egfr = itemView.findViewById(R.id.recentEgfr);
            acr = itemView.findViewById(R.id.recentAcr);
            kfre = itemView.findViewById(R.id.recentKFRE);
        }
    }

    private String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private String formatEgfr(double egfr) {
        return String.format(Locale.getDefault(), "%.0f mL/min/1.73m²", egfr);
    }

    private String formatAcr(double acr) {
        return String.format(Locale.getDefault(), "%.0f mg/g", acr);
    }

    private String formatKfreRisk(double kfre2Year) {
        String riskLevel;
        if (kfre2Year < 5) {
            riskLevel = "Low";
        } else if (kfre2Year < 15) {
            riskLevel = "Medium";
        } else {
            riskLevel = "High";
        }
        return String.format(Locale.getDefault(), "2-Year Risk: %.0f%% (%s)", kfre2Year, riskLevel);
    }
}
