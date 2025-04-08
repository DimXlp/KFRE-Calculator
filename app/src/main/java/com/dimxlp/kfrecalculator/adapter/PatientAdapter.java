package com.dimxlp.kfrecalculator.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.model.Patient;
import com.dimxlp.kfrecalculator.enumeration.Risk;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private final List<Patient> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    public PatientAdapter(List<Patient> patients) {
        this.patients = patients;
    }

    public void setOnPatientClickListener(OnPatientClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Patient patient = patients.get(position);
        Context context = holder.itemView.getContext();

        holder.txtName.setText(patient.getFullName());

        // Calculate age
        String birthDateStr = patient.getBirthDate();
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date birthDate = sdf.parse(birthDateStr);

                Calendar dob = Calendar.getInstance();
                dob.setTime(birthDate);

                Calendar today = Calendar.getInstance();

                int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    age--;
                }

                holder.txtAge.setText(String.format(Locale.getDefault(), "Age: %d", age));
            } catch (ParseException e) {
                Log.e("RAFI|PatientAdapter", "Invalid birthDate format: " + birthDateStr, e);
                holder.txtAge.setText("Age: Unknown");
            }
        } else {
            holder.txtAge.setText("Age: Unknown");
        }

        // Risk level color
        Risk risk = patient.getRisk();
        int riskColor, riskImage;
        switch (risk) {
            case HIGH:
                riskColor = R.color.colorHighRiskStat;
                riskImage = R.drawable.ic_risk;
                break;
            case MEDIUM:
                riskColor = R.color.colorMediumRiskStat;
                riskImage = R.drawable.ic_medium;
                break;
            case LOW:
                riskColor = R.color.colorLowRiskStat;
                riskImage = R.drawable.ic_tick;
                break;
            default:
                riskColor =R.color.colorRecentLast;
                riskImage = R.drawable.ic_question;
        }
        holder.imgRiskLevel.setBackgroundColor(ContextCompat.getColor(context, riskColor));
        holder.imgRiskLevel.setImageResource(riskImage);

        // Format last assessment timestamp
        long lastUpdated = patient.getLastUpdated();
        if (lastUpdated > 0) {
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String formattedDate = outputFormat.format(new Date(lastUpdated));
            holder.txtLastAssessment.setText("Last Assessment: " + formattedDate);
        } else {
            holder.txtLastAssessment.setText("Last Assessment: N/A");
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPatientClick(patient);
            }
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtAge, txtLastAssessment;
        ImageView imgRiskLevel;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtPatientName);
            txtAge = itemView.findViewById(R.id.txtPatientAge);
            txtLastAssessment = itemView.findViewById(R.id.txtPatientLastAssessment);
            imgRiskLevel = itemView.findViewById(R.id.imgRiskLevel);
        }
    }
}
