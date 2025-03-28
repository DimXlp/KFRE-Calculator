package com.dimxlp.kfrecalculator.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.model.Patient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentPatientAdapter extends RecyclerView.Adapter<RecentPatientAdapter.ViewHolder> {

    private final List<Patient> patientList;

    public RecentPatientAdapter(List<Patient> patientList) {
        this.patientList = patientList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patientList.get(position);

        holder.name.setText(patient.getFullName());
        holder.age.setText("Age: " + calculateAge(patient.getBirthDate()));
        holder.lastAssessment.setText("Last Assessment: " + formatDate(patient.getLastUpdated()));
        holder.risk.setText("Risk: " + patient.getRisk().name());
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, age, lastAssessment, risk;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.recentPatientName);
            age = itemView.findViewById(R.id.recentPatientAge);
            lastAssessment = itemView.findViewById(R.id.recentPatientLast);
            risk = itemView.findViewById(R.id.recentPatientRisk);
        }
    }

    // Utility to convert "yyyy-MM-dd" birth date to age
    private int calculateAge(String birthDateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthDate = sdf.parse(birthDateString);
            if (birthDate == null) return -1;

            Calendar birth = Calendar.getInstance();
            birth.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            return -1;
        }
    }

    // Utility to format timestamp into readable date
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
