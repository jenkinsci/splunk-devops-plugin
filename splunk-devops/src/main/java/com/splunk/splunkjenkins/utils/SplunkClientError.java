package com.splunk.splunkjenkins.utils;

import java.io.IOException;
import java.util.logging.Logger;

public class SplunkClientError extends IOException {
    private static final Logger LOG = Logger.getLogger(SplunkClientError.class.getName());
    private int status;

    public SplunkClientError(String message, int status) {
        super(message + ", status code:" + status);
        this.status = status;
    }

    public int getStatus(){
        return status;
    }
}
