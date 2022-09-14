package com.sepulkary.ftrack;

import java.util.Date;

public class HourResult {
    private Date HourStartDate;

    private float UserCaloriesBurned;
    private int UserMetersTravelled;

    private int CrowdAverageCaloriesBurned;
    private int CrowdAverageMetersTravelled;

    HourResult() {
        HourStartDate = new Date(0);
    }

    public boolean setHourStartDate(Date date) {
        HourStartDate = date;
        return true;
    }

    public boolean setUserCaloriesBurned(float calories) {
        UserCaloriesBurned = calories;
        return true;
    }

    public boolean setUserMetersTravelled(int meters) {
        UserMetersTravelled = meters;
        return true;
    }

    public boolean setCrowdAverageCaloriesBurned(int calories) {
        CrowdAverageCaloriesBurned = calories;
        return true;
    }

    public boolean setCrowdAverageMetersTravelled(int meters) {
        CrowdAverageMetersTravelled = meters;
        return true;
    }

    public Date getHourStartDate() {
        return HourStartDate;
    }

    public float getUserCaloriesBurned() {
        return UserCaloriesBurned;
    }

    public int getUserMetersTravelled() {
        return UserMetersTravelled;
    }

    public int getCrowdAverageCaloriesBurned() {
        return CrowdAverageCaloriesBurned;
    }

    public int getCrowdAverageMetersTravelled() {
        return CrowdAverageMetersTravelled;
    }
}