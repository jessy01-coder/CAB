package com.sanj.cabme.models;

public class RideOrderModel {

    private String name, phone, startLocation, pickupTime, destinationLocation, orderId;

    public RideOrderModel() {
    }

    public RideOrderModel(String name, String phone, String startLocation, String pickupTime, String destinationLocation, String orderId) {
        this.name = name;
        this.phone = phone;
        this.startLocation = startLocation;
        this.pickupTime = pickupTime;
        this.destinationLocation = destinationLocation;
        this.orderId = orderId;
    }

    public String getPickupTime() {
        return pickupTime;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public String getOrderId() {
        return orderId;
    }
}
