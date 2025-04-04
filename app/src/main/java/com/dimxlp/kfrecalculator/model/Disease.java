package com.dimxlp.kfrecalculator.model;

import java.util.HashMap;
import java.util.Map;

public class Disease {
    private String diseaseId;
    private String patientId;
    private String name;
    private boolean hasDisease;
    private String details;

    public Disease() {
        // Required for Firestore
    }

    public Disease(String diseaseId, String patientId, String name, boolean hasDisease, String details) {
        this.diseaseId = diseaseId;
        this.patientId = patientId;
        this.name = name;
        this.hasDisease = hasDisease;
        this.details = details;
    }

    // Getters and setters
    public String getDiseaseId() {
        return diseaseId;
    }

    public void setDiseaseId(String diseaseId) {
        this.diseaseId = diseaseId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHasDisease() {
        return hasDisease;
    }

    public void setHasDisease(boolean hasDisease) {
        this.hasDisease = hasDisease;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("diseaseId", diseaseId);
        map.put("patientId", patientId);
        map.put("name", name);
        map.put("hasDisease", hasDisease);
        map.put("details", details);
        return map;
    }

}
