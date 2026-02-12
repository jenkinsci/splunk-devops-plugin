package com.splunk.splunkjenkins.utils;

import shaded.splk.org.apache.http.conn.DnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves multiple comma-separated hostnames to IP addresses.
 */
public class MultipleHostResolver implements DnsResolver {
    /**
     * Delimiter for separating multiple hostnames
     */
    public static final String NAME_DELIMITER = ",";

    /** {@inheritDoc} */
    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {
        if (host == null) {
            return null;
        }
        String hostname = host;
        //split by comma
        String[] hosts = hostname.split(NAME_DELIMITER);
        List<InetAddress> addressList = new ArrayList<>();
        for (String endpointHost : hosts) {
            InetAddress[] addresses = InetAddress.getAllByName(endpointHost);
            for (InetAddress address : addresses) {
                addressList.add(address);
            }
        }
        return addressList.toArray(new InetAddress[0]);
    }
}
