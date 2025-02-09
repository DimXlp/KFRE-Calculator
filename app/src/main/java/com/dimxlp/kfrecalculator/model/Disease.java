package com.dimxlp.kfrecalculator.model;

import java.util.List;
import java.util.Map;

public class Disease {
    private boolean hasDisease;
    private List<String> medications;
    private Map<String, String> medicationFrequencies; // Maps medicationId to frequency
    private String details;

    public Disease() {}

    public Disease(boolean hasDisease, List<String> medications, Map<String, String> medicationFrequencies, String details) {
        this.hasDisease = hasDisease;
        this.medications = medications;
        this.medicationFrequencies = medicationFrequencies;
        this.details = details;
    }

    public boolean isHasDisease() {
        return hasDisease;
    }

    public void setHasDisease(boolean hasDisease) {
        this.hasDisease = hasDisease;
    }

    public List<String> getMedications() {
        return medications;
    }

    public void setMedications(List<String> medications) {
        this.medications = medications;
    }

    public Map<String, String> getMedicationFrequencies() {
        return medicationFrequencies;
    }

    public void setMedicationFrequencies(Map<String, String> medicationFrequencies) {
        this.medicationFrequencies = medicationFrequencies;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
