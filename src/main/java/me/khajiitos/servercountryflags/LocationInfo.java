package me.khajiitos.servercountryflags;

import com.google.gson.JsonObject;

public class LocationInfo {
    private static final double MILE_KM_RATIO = 1.609344;

    public boolean success = false;
    public String countryCode = null;
    public String countryName = null;
    public String cityName = null;
    public double latitude = -1.0;
    public double longitude = -1.0;
    private double distanceFromLocal = -1.0; // in miles

    public LocationInfo(JsonObject apiObject) {
        if (apiObject.has("status")) {
            success = apiObject.get("status").getAsString().equals("success");
            if (!success) {
                ServerCountryFlags.LOGGER.error("API result isn't successful");
                ServerCountryFlags.LOGGER.error(apiObject.toString());
                return;
            }
        } else {
            ServerCountryFlags.LOGGER.error("API Object doesn't include the field 'status'");
            ServerCountryFlags.LOGGER.error(apiObject.toString());
            return;
        }
        if (apiObject.has("country") && apiObject.has("countryCode") && apiObject.has("city")) {
            this.countryName = apiObject.get("country").getAsString();
            this.countryCode = apiObject.get("countryCode").getAsString().toLowerCase();
            this.cityName = apiObject.get("city").getAsString();

            if (apiObject.has("lon") && apiObject.has("lat")) {
                this.longitude = apiObject.get("lon").getAsDouble();
                this.latitude = apiObject.get("lat").getAsDouble();
                this.distanceFromLocal = calculateDistanceFromLocal();
            }
        } else {
            ServerCountryFlags.LOGGER.error("API Object is incomplete");
            ServerCountryFlags.LOGGER.error(apiObject.toString());
            success = false;
        }
    }

    private double calculateDistanceFromLocal() {
        LocationInfo local = ServerCountryFlags.localLocation;

        if (local == null)
            return -1.0;

        double theta = local.longitude - this.longitude;
        double distance =
            Math.toDegrees(Math.acos(
                    Math.sin(Math.toRadians(local.latitude)) * Math.sin(Math.toRadians(this.latitude))
                    + Math.cos(Math.toRadians(local.latitude)) * Math.cos(Math.toRadians(this.latitude))
                    * Math.cos(Math.toRadians(theta))
            )) * 69.09; // nice
        return distance;
    }

    public boolean hasLonlat() {
        return this.latitude != -1.0 && this.longitude != -1.0;
    }

    public double getDistanceFromLocal(boolean inKm) {
        if (this.distanceFromLocal == -1.0)
            return -1.0;
        return inKm ? this.distanceFromLocal * MILE_KM_RATIO : this.distanceFromLocal;
    }
}