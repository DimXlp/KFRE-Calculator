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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.kfrecalculator.R;
import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.model.Patient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentPatientAdapter extends RecyclerView.Adapter<RecentPatientAdapter.RecentPatientViewHolder> {

    private final List<Patient> patientList;
    private RecentPatientAdapter.OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    public RecentPatientAdapter(List<Patient> patientList) {
        this.patientList = patientList;
    }

    public void setOnPatientClickListener(RecentPatientAdapter.OnPatientClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentPatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_patient, parent, false);
        return new RecentPatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentPatientViewHolder holder, int position) {
        Patient patient = patientList.get(position);
        Context context = holder.itemView.getContext();

        adjustNameAndAge(holder, patient, context);
        adjustRiskBadge(holder, patient, context);
        adjustLastAssessment(holder, patient);
        adjustPatientClickListener(holder, patient);
    }

    private void adjustPatientClickListener(@NonNull RecentPatientViewHolder holder, Patient patient) {
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPatientClick(patient);
            }
        });
    }

    private static void adjustLastAssessment(@NonNull RecentPatientViewHolder holder, Patient patient) {
        Date lastAssessmentDate = patient.getLastAssessment();
        if (lastAssessmentDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.txtLastAssessment.setText("Last Assessment: " + sdf.format(lastAssessmentDate));
        } else {
            holder.txtLastAssessment.setText("Last Assessment: N/A");
        }
    }

    private static void adjustRiskBadge(@NonNull RecentPatientViewHolder holder, Patient patient, Context context) {
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
    }

    private static void adjustNameAndAge(@NonNull RecentPatientViewHolder holder, Patient patient, Context context) {
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
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    static class RecentPatientViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtLastAssessment;
        ImageView imgRiskLevel;

        public RecentPatientViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtPatientName);
            txtLastAssessment = itemView.findViewById(R.id.txtPatientLastAssessment);
            imgRiskLevel = itemView.findViewById(R.id.imgRiskLevel);
        }
    }
}
