package com.splunk.splunkjenkins.utils;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import hudson.util.Secret;
import org.apache.commons.beanutils.BeanUtils;

import java.util.Map;

public class RemoteUtils {

    public static void initSplunkConfigOnAgent(Map eventCollectorProperty) {
        // Init SplunkJenkins global config in slave, can not reference Jenkins.getInstance(), Xtream
        SplunkJenkinsInstallation config = new SplunkJenkinsInstallation(false);
        try {
            String tokenValue = (String) eventCollectorProperty.remove("token");
            BeanUtils.populate(config, eventCollectorProperty);
            config.setToken(Secret.fromString(tokenValue));
            config.setEnabled(true);
            initSplunkConfigOnAgent(config);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void initSplunkConfigOnAgent(SplunkJenkinsInstallation instance) {
        SplunkJenkinsInstallation.initOnAgent(instance);
        // only use one thread on agent
        SplunkLogService.getInstance().MAX_WORKER_COUNT = 1;
    }
}
