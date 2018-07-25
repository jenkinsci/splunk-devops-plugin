package com.splunk.splunkjenkins.utils;

import java.io.IOException;
import java.util.logging.Logger;

public class SplunkServiceError extends IOException {
    private static final Logger LOG = Logger.getLogger(SplunkServiceError.class.getName());

    public SplunkServiceError(String message, int status) {
        super(message);
    }
}
