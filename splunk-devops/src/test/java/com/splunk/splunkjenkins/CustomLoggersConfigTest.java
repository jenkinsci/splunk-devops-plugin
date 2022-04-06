package com.splunk.splunkjenkins;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CustomLoggersConfigTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void roundTripTest() throws Exception {
        CustomLoggerItem cli1 = new CustomLoggerItem("logger1");
        CustomLoggerItem cli2 = new CustomLoggerItem("logger2");
        List<CustomLoggerItem> loggerItems = Arrays.asList(cli1, cli2);

        CustomLoggersConfig config = CustomLoggersConfig.get();

        config.addCustomLogger(cli1);
        config.addCustomLogger(cli2);

        r.configRoundtrip();

        assertEquals(loggerItems, config.getCustomLoggers());
    }
}
