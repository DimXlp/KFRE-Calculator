package com.dimxlp.kfrecalculator.adapter;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
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

        String name = patient.getFullName();
        String birthDateStr = patient.getBirthDate();
        SpannableStringBuilder styledName = new SpannableStringBuilder();

        styledName.append(name);

        // Calculate age
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

                String ageText = String.format(Locale.getDefault(), " (%d)", age);
                styledName.append(ageText);

                int start = name.length();
                int end = styledName.length();
                int secondaryColor = ContextCompat.getColor(context, R.color.textSecondary);

                styledName.setSpan(new ForegroundColorSpan(secondaryColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                styledName.setSpan(new RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            } catch (ParseException e) {
                Log.e("RAFI|PatientAdapter", "Invalid birthDate format: " + birthDateStr, e);
                // If parsing fails, the spannable already has just the name, so nothing more to do.
            }
        }
        holder.txtName.setText(styledName);

        // Risk level color
        Risk risk = patient.getRisk();
        int riskColor;
        int riskImage;

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
                riskColor = R.color.colorRecentLast;
                riskImage = R.drawable.ic_question;
        }
        holder.imgRiskLevel.setBackgroundColor(ContextCompat.getColor(context, riskColor));
        holder.imgRiskLevel.setImageResource(riskImage);

        // Format last assessment timestamp
        Date lastAssessmentDate = patient.getLastAssessment();

        if (lastAssessmentDate != null) {
            // Format the date to a readable string like "22 Mar 2025"
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.txtLastAssessment.setText("Last Assessment: " + sdf.format(lastAssessmentDate));
        } else {
            // If there's no assessment, show a default message
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
        CardView patientCard;
        TextView txtName, txtLastAssessment;
        ImageView imgRiskLevel;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            patientCard = itemView.findViewById(R.id.patientCard);
            txtName = itemView.findViewById(R.id.txtPatientName);
            txtLastAssessment = itemView.findViewById(R.id.txtPatientLastAssessment);
            imgRiskLevel = itemView.findViewById(R.id.imgRiskLevel);
        }
    }
}
