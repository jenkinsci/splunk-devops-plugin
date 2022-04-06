package com.splunk.splunkjenkins;

import hudson.Extension;
import hudson.logging.LogRecorder;
import hudson.logging.LogRecorderManager;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Map;

public class CustomLoggerItem implements Describable<CustomLoggerItem> {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(CustomLoggerItem.class.getName());

    private static transient final LogRecorderManager logRecorderManager = Jenkins.getInstanceOrNull().getLog();

    String customLoggerName;
    LogRecorder logRecorder;

    @DataBoundConstructor
    public CustomLoggerItem(String customLoggerName) {
        this.customLoggerName = customLoggerName;
        logRecorder = logRecorderManager.getLogRecorder(customLoggerName);
        if (logRecorder == null) {
            LOG.warning("CustomLoggerItem created for non-existent custom logger with name: " + customLoggerName);
        }
    }

    public String getCustomLoggerName() {
        return customLoggerName;
    }

    public void setCustomLoggerName(String customLoggerName) {
        this.customLoggerName = customLoggerName;
    }

    public LogRecorder getLogRecorder() {
        return logRecorder;
    }

    public void setLogRecorder(LogRecorder logRecorder) {
        this.logRecorder = logRecorder;
    }

    public String toString() {
        return "CustomLoggerName: " + customLoggerName;
    }

    public Descriptor<CustomLoggerItem> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }


    @Extension
    public static final class DescriptorImpl extends Descriptor<CustomLoggerItem> {
        @Override
        public String getDisplayName() {
            return "CustomLoggerItem";
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            if (value == null || value.isEmpty()) {
                return FormValidation.error("Name cannot be empty");
            } else {
                return FormValidation.ok();
            }
        }

        public static ListBoxModel doFillCustomLoggerNameItems() {
            ListBoxModel items = new ListBoxModel();

            for (Map.Entry<String, LogRecorder> e : logRecorderManager.logRecorders.entrySet()) {
                items.add(e.getKey(), e.getValue().getDisplayName());
            }
            return items;
        }
    }
}
