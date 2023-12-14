package me.khajiitos.servercountryflags.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.khajiitos.servercountryflags.common.ServerCountryFlags;

import java.util.List;

public class LocationInfo {
    private static final double MILE_KM_RATIO = 1.609344;

    public final String countryCode;
    public final String countryName;
    public final String cityName;
    public final String districtName;
    public final String ispName;
    public final double latitude;
    public final double longitude;
    private double distanceFromLocal; // in miles

    public LocationInfo(JsonElement apiJson) throws InvalidAPIResponseException {
        if (apiJson == null) {
            throw new InvalidAPIResponseException("Received something that's not JSON");
        }

        if (!(apiJson instanceof JsonObject apiObject)) {
            throw new InvalidAPIResponseException("Received JSON element, but it's not an object");
        }

        if (!apiObject.has("status") || !apiObject.get("status").getAsString().equals("success")) {
            throw new InvalidAPIResponseException("API result isn't successful");
        }

        if (apiObject.keySet().containsAll(List.of("country", "countryCode", "city", "lon", "lat", "district", "isp"))) {
            this.countryName = apiObject.get("country").getAsString();
            this.countryCode = apiObject.get("countryCode").getAsString().toLowerCase();
            this.cityName = apiObject.get("city").getAsString();
            this.districtName = apiObject.get("district").getAsString();
            this.ispName = apiObject.get("isp").getAsString();

            this.longitude = apiObject.get("lon").getAsDouble();
            this.latitude = apiObject.get("lat").getAsDouble();
            this.distanceFromLocal = calculateDistanceFromLocal();
        } else {
            throw new InvalidAPIResponseException("API Object is incomplete");
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