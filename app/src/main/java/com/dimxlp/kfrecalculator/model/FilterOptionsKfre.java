package com.dimxlp.kfrecalculator.model;

import com.dimxlp.kfrecalculator.enumeration.Risk;
import com.dimxlp.kfrecalculator.enumeration.SortDirection;

import java.util.Date;

public class FilterOptionsKfre {

    private SortDirection dateSort = SortDirection.NONE;
    private SortDirection risk2YrSort = SortDirection.NONE;
    private SortDirection risk5YrSort = SortDirection.NONE;
    private boolean hasNoteOnly = false;
    private Risk riskCategory = null;
    private Date startDate = null;
    private Date endDate = null;

    public SortDirection getDateSort() {
        return dateSort;
    }

    public void setDateSort(SortDirection dateSort) {
        this.dateSort = dateSort;
    }

    public SortDirection getRisk2YrSort() {
        return risk2YrSort;
    }

    public void setRisk2YrSort(SortDirection risk2YrSort) {
        this.risk2YrSort = risk2YrSort;
    }

    public SortDirection getRisk5YrSort() {
        return risk5YrSort;
    }

    public void setRisk5YrSort(SortDirection risk5YrSort) {
        this.risk5YrSort = risk5YrSort;
    }

    public boolean isHasNoteOnly() {
        return hasNoteOnly;
    }

    public void setHasNoteOnly(boolean hasNoteOnly) {
        this.hasNoteOnly = hasNoteOnly;
    }

    public Risk getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(Risk riskCategory) {
        this.riskCategory = riskCategory;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
