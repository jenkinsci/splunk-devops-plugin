package com.splunk.splunkjenkins;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Extension
public class CustomLoggersConfig extends GlobalConfiguration {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(CustomLoggersConfig.class.getName());

    List<CustomLoggerItem> customLoggers;

    public static CustomLoggersConfig get() {
        return (CustomLoggersConfig) Jenkins.getInstance().getDescriptor(CustomLoggersConfig.class);
    }

    public CustomLoggersConfig() {
        super.load();
        if (customLoggers == null) {
            this.customLoggers = new ArrayList<>();
        }
    }

    @DataBoundConstructor
    public CustomLoggersConfig(List<CustomLoggerItem> customLoggers) {
        if (customLoggers == null) {
            this.customLoggers = Collections.emptyList();
        } else {
            this.customLoggers = customLoggers;
        }
        this.save();
    }

    public List<CustomLoggerItem> getCustomLoggers() {
        return customLoggers;
    }

    @DataBoundSetter
    public void setCustomLoggers(List<CustomLoggerItem> customLoggers) {
        this.customLoggers = customLoggers;
    }

    public void addCustomLogger(CustomLoggerItem customLoggerItem) {
        this.customLoggers.add(customLoggerItem);
        this.save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        if (!json.has("customLoggers")) {
            json.put("customLoggers", new JSONArray());
        }
        req.bindJSON(this, json);
        save();
        return true;
    }
}
