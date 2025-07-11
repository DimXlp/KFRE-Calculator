package com.dimxlp.kfrecalculator.model;

import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.enumeration.SortDirection;

public class FilterOptionsPatientList {

    private SortDirection dateSort = SortDirection.NONE;
    private SortDirection ageSort = SortDirection.NONE;
    private Boolean statusIsActive = null;
    private Risk riskCategory = null;
    private String gender = null;
    private int minAge = 0;
    private int maxAge = 120;

    public FilterOptionsPatientList() {}

    public SortDirection getDateSort() {
        return dateSort;
    }

    public void setDateSort(SortDirection dateSort) {
        this.dateSort = dateSort;
    }

    public SortDirection getAgeSort() {
        return ageSort;
    }

    public void setAgeSort(SortDirection ageSort) {
        this.ageSort = ageSort;
    }

    public Boolean getStatusIsActive() {
        return statusIsActive;
    }

    public void setStatusIsActive(Boolean statusIsActive) {
        this.statusIsActive = statusIsActive;
    }

    public Risk getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(Risk riskCategory) {
        this.riskCategory = riskCategory;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Checks if any filter is currently active.
     * @return true if at least one filter option is set, false otherwise.
     */
    public boolean isAnyFilterActive() {
        return statusIsActive != null ||
                riskCategory != null ||
                gender != null ||
                minAge > 0 ||
                maxAge < 120;
    }
}