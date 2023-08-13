package me.khajiitos.servercountryflags.common.util;

import me.khajiitos.servercountryflags.common.ServerCountryFlags;

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
            List<NetworkInterface> interfacesToRemove = new ArrayList<>();
            for (NetworkInterface networkInterface : previousInterfaces.keySet()) {
                if (!currentInterfaces.contains(networkInterface)) {
                    interfacesToRemove.add(networkInterface);
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
            interfacesToRemove.forEach(previousInterfaces::remove);
        } catch (SocketException e) {
            ServerCountryFlags.LOGGER.warn("SocketException while checking network interfaces");
        }

        return changed;
    }
}
