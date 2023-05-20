package me.khajiitos.servercountryflags.util;

import me.khajiitos.servercountryflags.ServerCountryFlags;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class NetworkChangeDetector {
    private static final HashMap<NetworkInterface, List<InterfaceAddress>> previousInterfaces = new HashMap<>();

    public static boolean check() {
        boolean changed = false;
        try {
            List<NetworkInterface> currentInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : currentInterfaces) {
                if (!previousInterfaces.containsKey(networkInterface)) {
                    previousInterfaces.put(networkInterface, new ArrayList<>());
                    changed = true;
                }
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if (!previousInterfaces.get(networkInterface).contains(address)) {
                        previousInterfaces.get(networkInterface).add(address);
                        changed = true;
                    }
                }
            }
            for (NetworkInterface networkInterface : previousInterfaces.keySet()) {
                if (!currentInterfaces.contains(networkInterface)) {
                    previousInterfaces.remove(networkInterface);
                    changed = true;
                } else {
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                        if (!previousInterfaces.get(networkInterface).contains(address)) {
                            previousInterfaces.get(networkInterface).remove(address);
                            changed = true;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            ServerCountryFlags.LOGGER.warn("SocketException while checking network interfaces");
        }
        return changed;
    }
}
