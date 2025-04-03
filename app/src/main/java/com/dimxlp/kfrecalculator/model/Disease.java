package com.dimxlp.kfrecalculator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Disease {
    private boolean hasDisease;
    private String name;
    private String details;
    private List<MedicationAssignment> medications;

    public Disease() {
        this.hasDisease = false;
        this.name = name;
        this.details = "";
        this.medications = new ArrayList<>();
    }

    public Disease(String name) {
        this.hasDisease = true;
        this.name = name;
        this.details = "";
        this.medications = new ArrayList<>();
    }

    public boolean isHasDisease() {
        return hasDisease;
    }

    public void setHasDisease(boolean hasDisease) {
        this.hasDisease = hasDisease;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void addMedication(MedicationAssignment medication) {
        medications.add(medication);
    }

    public List<MedicationAssignment> getMedications() {
        return medications;
    }

    public void setMedications(List<MedicationAssignment> medications) {
        this.medications = medications;
    }

    public List<Map<String, String>> getMedicationsAsMap() {
        List<Map<String, String>> medList = new ArrayList<>();
        for (MedicationAssignment med : medications) {
            Map<String, String> map = new HashMap<>();
            map.put("name", med.getMedicationName());
            map.put("frequency", med.getFrequency());
            medList.add(map);
        }
        return medList;
    }
}
