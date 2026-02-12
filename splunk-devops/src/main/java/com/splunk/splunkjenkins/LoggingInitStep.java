package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.utils.LogEventHelper;
import hudson.init.Initializer;
import hudson.util.PluginServletFilter;
import jenkins.util.Timer;

import javax.servlet.ServletException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.init.InitMilestone.JOB_LOADED;

/**
 * Initializes the Splunk Jenkins plugin during Jenkins startup.
 * Registers the custom log handler and web access logger.
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings("LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE")
public class LoggingInitStep {
    private final static String rootLoggerName = "";

    /**
     * Scheduled initialization method that registers the Splunk log handler
     * 30 seconds after Jenkins job loading completes. This delay ensures
     * all plugins are properly initialized before setting up Splunk integration.
     *
     * This method is called automatically by Jenkins through the @Initializer
     * annotation and should not be invoked directly.
     */
    @Initializer(after = JOB_LOADED)
    public static void setupSplunkJenkins() {
        Timer.get().schedule(new Runnable() {
            @Override
            public void run() {
                registerHandler();
            }
        }, 30, TimeUnit.SECONDS);
    }

    /**
     * Registers the JDK Splunk log handler with the root logger, initializes
     * the Splunk plugin configuration, and sets up web access logging if enabled.
     * This method checks for duplicate handlers and configures appropriate
     * log levels for the health monitor.
     */
    protected static void registerHandler() {
        Handler[] handlers = Logger.getLogger(rootLoggerName).getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof JdkSplunkLogHandler) {
                // already registered
                return;
            }
        }
        //only log warning message for HealthMonitor which runs every 20s
        Logger.getLogger(HealthMonitor.class.getName()).setLevel(Level.WARNING);
        Logger.getLogger(rootLoggerName).addHandler(JdkSplunkLogHandler.LogHolder.LOG_HANDLER);
        //init plugin
        SplunkJenkinsInstallation.get().updateCache();
        SplunkJenkinsInstallation.markComplete(true);
        Logger.getLogger(LoggingInitStep.class.getName()).info("plugin splunk-devops version " + LogEventHelper.getBuildVersion() + " loaded");
        // check filter
        if (Constants.ENABLE_POST_LOGGER) {
            try {
                PluginServletFilter.addFilter(new WebPostAccessLogger());
            } catch (ServletException e) {
                Logger.getLogger(LoggingInitStep.class.getName()).warning("failed to register splunk web access logger");
            }
        }
    }

}
