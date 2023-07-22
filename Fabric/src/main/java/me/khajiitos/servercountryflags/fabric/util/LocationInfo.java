package me.khajiitos.servercountryflags.fabric.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import me.khajiitos.servercountryflags.fabric.ServerCountryFlags;

import java.util.List;

public class LocationInfo {
    private static final double MILE_KM_RATIO = 1.609344;

    public boolean success = false;
    public String countryCode = null;
    public String countryName = null;
    public String cityName = null;
    public String districtName = null;
    public String ispName = null;
    public double latitude = -1.0;
    public double longitude = -1.0;
    private double distanceFromLocal = -1.0; // in miles

    private boolean objectContainsAll(JsonObject object, List<String> keys) {
        for (String key : keys) {
            if (!object.has(key)) {
                return false;
            }
        }
        return true;
    }

    public LocationInfo(JsonObject apiObject) {
        if (apiObject.has("status")) {
            success = apiObject.get("status").getAsString().equals("success");
            if (!success) {
                ServerCountryFlags.LOGGER.severe("API result isn't successful");
                ServerCountryFlags.LOGGER.severe(apiObject.toString());
                return;
            }
        } else {
            ServerCountryFlags.LOGGER.severe("API Object doesn't include the field 'status'");
            ServerCountryFlags.LOGGER.severe(apiObject.toString());
            return;
        }
        if (objectContainsAll(apiObject, ImmutableList.of("country", "countryCode", "city", "lon", "lat", "district", "isp"))) {
            this.countryName = apiObject.get("country").getAsString();
            this.countryCode = apiObject.get("countryCode").getAsString().toLowerCase();
            this.cityName = apiObject.get("city").getAsString();
            this.districtName = apiObject.get("district").getAsString();
            this.ispName = apiObject.get("isp").getAsString();

            this.longitude = apiObject.get("lon").getAsDouble();
            this.latitude = apiObject.get("lat").getAsDouble();
            this.distanceFromLocal = calculateDistanceFromLocal();
        } else {
            ServerCountryFlags.LOGGER.severe("API Object is incomplete");
            ServerCountryFlags.LOGGER.severe(apiObject.toString());
            success = false;
        }
    }

    private double calculateDistanceFromLocal() {
        LocationInfo local = ServerCountryFlags.localLocation;

        if (local == null)
            return -1.0;

        double theta = local.longitude - this.longitude;
        return Math.toDegrees(Math.acos(
                Math.sin(Math.toRadians(local.latitude)) * Math.sin(Math.toRadians(this.latitude))
                + Math.cos(Math.toRadians(local.latitude)) * Math.cos(Math.toRadians(this.latitude))
                * Math.cos(Math.toRadians(theta))
        )) * 69.09;
    }

    public double getDistanceFromLocal(boolean inKm) {
        if (this.distanceFromLocal == -1.0)
            return -1.0;
        return inKm ? this.distanceFromLocal * MILE_KM_RATIO : this.distanceFromLocal;
    }

    public void updateDistanceFromLocal() {
        this.distanceFromLocal = calculateDistanceFromLocal();
    }
}