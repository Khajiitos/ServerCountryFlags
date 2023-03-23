package me.khajiitos.servercountryflags;

import com.google.gson.JsonObject;

public class ServerLocationInfo {
    public boolean success = false;
    public String countryCode = null;
    public String countryName = null;
    public String cityName = null;
    public ServerLocationInfo(JsonObject apiObject) {
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
        } else {
            ServerCountryFlags.LOGGER.error("API Object is incomplete");
            ServerCountryFlags.LOGGER.error(apiObject.toString());
            success = false;
        }
    }
}