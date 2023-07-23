package com.sanj.cabme.models;

public class RideHistoryModel {
    private String date, description, departureTime, arrivalTime, passengerPhone, orderId,fare;

    public RideHistoryModel() {
    }

    public RideHistoryModel(String date, String description, String departureTime, String arrivalTime, String passengerPhone, String orderId, String fare) {
        this.date = date;
        this.description = description;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.passengerPhone = passengerPhone;
        this.orderId = orderId;
        this.fare = fare;
    }

    public String getFare() {
        return fare;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getPassengerPhone() {
        return passengerPhone;
    }

    public String getOrderId() {
        return orderId;
    }
}
