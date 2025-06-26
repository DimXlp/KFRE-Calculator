package com.dimxlp.kfrecalculator.model;

import com.dimxlp.kfrecalculator.enumeration.SortDirection;

import java.util.Date;

public class FilterOptionsCkdEpi {

    private SortDirection dateSort = SortDirection.NONE;
    private SortDirection egfrSort = SortDirection.NONE;
    private boolean hasNoteOnly = false;
    private Date startDate = null;
    private Date endDate = null;

    public SortDirection getDateSort() {
        return dateSort;
    }

    public void setDateSort(SortDirection dateSort) {
        this.dateSort = dateSort;
    }

    public SortDirection getEgfrSort() {
        return egfrSort;
    }

    public void setEgfrSort(SortDirection egfrSort) {
        this.egfrSort = egfrSort;
    }

    public boolean isHasNoteOnly() {
        return hasNoteOnly;
    }

    public void setHasNoteOnly(boolean hasNoteOnly) {
        this.hasNoteOnly = hasNoteOnly;
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