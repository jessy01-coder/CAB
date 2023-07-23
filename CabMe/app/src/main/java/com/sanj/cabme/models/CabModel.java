package com.sanj.cabme.models;

public class CabModel {
    private String name, phone, location, routeFrom, routeTo, carPlate, carModel, driverNID,price;

    public CabModel() {
    }

    public CabModel(String name, String phone, String location, String routeFrom, String routeTo, String carPlate, String carModel, String driverNID, String price) {
        this.name = name;
        this.phone = phone;
        this.location = location;
        this.routeFrom = routeFrom;
        this.routeTo = routeTo;
        this.carPlate = carPlate;
        this.carModel = carModel;
        this.driverNID = driverNID;
        this.price = price;
    }

    public String getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getLocation() {
        return location;
    }

    public String getRouteFrom() {
        return routeFrom;
    }

    public String getRouteTo() {
        return routeTo;
    }

    public String getCarPlate() {
        return carPlate;
    }

    public String getCarModel() {
        return carModel;
    }

    public String getDriverNID() {
        return driverNID;
    }
}
