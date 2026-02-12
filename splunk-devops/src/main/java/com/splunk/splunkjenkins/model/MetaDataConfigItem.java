package com.splunk.splunkjenkins.model;

import com.google.common.collect.ImmutableMap;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration item for Splunk metadata settings.
 * Allows per-event-type configuration of index, sourcetype, and other metadata.
 *
 * @since 1.4
 */
public class MetaDataConfigItem implements Describable<MetaDataConfigItem> {
    private static final String DISABLED_KEY = "disabled";
    private static final Map<String, String> CONFIG_ITEM_MAP = new ImmutableMap.Builder<String, String>().put("Index", "index")
            .put("Source Type", "sourcetype").put("Disabled", DISABLED_KEY).build();
    @NonNull
    private String dataSource;
    @NonNull
    private String keyName;
    //can only be null if enabled is false
    private String value;

    /**
     * Gets the data source for this metadata configuration
     *
     * @return the data source
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Sets the data source for this metadata configuration
     *
     * @param dataSource the data source to set
     */
    public void setDataSource(@NonNull String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets the key name for this metadata configuration
     *
     * @return the key name
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Sets the key name for this metadata configuration
     *
     * @param keyName the key name to set
     */
    public void setKeyName(@NonNull String keyName) {
        this.keyName = keyName;
    }

    /**
     * Gets the value for this metadata configuration
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value for this metadata configuration
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Constructs a MetaDataConfigItem with the specified data source, key name, and value
     *
     * @param dataSource the data source
     * @param keyName the key name
     * @param value the value
     */
    @DataBoundConstructor
    public MetaDataConfigItem(String dataSource, String keyName, String value) {
        this.dataSource = dataSource;
        this.keyName = keyName;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     *
     * Returns a string representation of this metadata configuration item
     */
    @Override
    public String toString() {
        String prefix = dataSource.toLowerCase() + ".";
        if ("default".equals(dataSource)) {
            prefix = "";
        }
        if (DISABLED_KEY.equals(this.keyName)) {
            return prefix + "enabled=false";
        } else {
            return prefix + keyName + "=" + value;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MetaDataConfigItem)) {
            return false;
        }
        return this.toString().equals(obj.toString());
    }

    /** {@inheritDoc} */
    @Override
    public Descriptor<MetaDataConfigItem> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(MetaDataConfigItem.class);
    }

    /**
     * Descriptor for MetaDataConfigItem that provides UI form field options
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<MetaDataConfigItem> {

        @Override
        public String getDisplayName() {
            return "Metadata config";
        }

        /**
         * Provides available data source options for the configuration UI
         *
         * @return ListBoxModel with available event types and default option
         */
        public ListBoxModel doFillDataSourceItems() {
            ListBoxModel m = new ListBoxModel();
            m.add("Build Event", EventType.BUILD_EVENT.toString());
            m.add("Build Report", EventType.BUILD_REPORT.toString());
            m.add("Console Log", EventType.CONSOLE_LOG.toString());
            m.add("Jenkins Config", EventType.JENKINS_CONFIG.toString());
            m.add("Log File", EventType.FILE.toString());
            m.add("Queue Information", EventType.QUEUE_INFO.toString());
            m.add("Agent Information", EventType.SLAVE_INFO.toString());
            m.add("Default", "default");
            return m;
        }

        /**
         * Provides available metadata key options for the configuration UI
         *
         * @return ListBoxModel with available metadata keys (index, sourcetype, disabled)
         */
        public static ListBoxModel doFillKeyNameItems() {
            ListBoxModel m = new ListBoxModel();
            for (Map.Entry<String, String> entry : CONFIG_ITEM_MAP.entrySet()) {
                m.add(entry.getKey(), entry.getValue());
            }
            return m;
        }
    }

    /**
     * Loads metadata configuration items from a properties string
     *
     * @param properties the properties string in Java properties format
     * @return a set of metadata configuration items
     */
    public static Set<MetaDataConfigItem> loadProps(String properties) {
        Set<MetaDataConfigItem> config = new HashSet<>();
        if (properties != null) {
            Properties metaDataConfigProps = new Properties();
            try {
                metaDataConfigProps.load(new StringReader(properties));
                for (EventType eventType : EventType.values()) {
                    //backward compatible, xx.enabled=false
                    if ("false".equals(metaDataConfigProps.getProperty(eventType.getKey("enabled")))) {
                        config.add(new MetaDataConfigItem(eventType.toString(), DISABLED_KEY, ""));
                    } else {
                        for (String suffix : CONFIG_ITEM_MAP.values()) {
                            String lookupKey = eventType.getKey(suffix);
                            if (metaDataConfigProps.containsKey(lookupKey)) {
                                config.add(new MetaDataConfigItem(eventType.toString(), suffix,
                                        metaDataConfigProps.getProperty(lookupKey)));
                            }
                        }
                    }
                }
                //add default
                for (String keyName : CONFIG_ITEM_MAP.values()) {
                    if (metaDataConfigProps.containsKey(keyName)) {
                        config.add(new MetaDataConfigItem("default", keyName,
                                metaDataConfigProps.getProperty(keyName)));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return config;
    }

    /**
     * Convert config set to plain text string, just to keep backward compatibility
     *
     * @param configs config items
     * @return java property file content
     */
    public static String toString(Set<MetaDataConfigItem> configs) {
        StringBuffer sbf = new StringBuffer();
        if (configs == null || configs.isEmpty()) {
            return "";
        }
        for (MetaDataConfigItem config : configs) {
            sbf.append(config.toString()).append("\n");
        }
        return sbf.toString();
    }

    /**
     * Gets the CSS display style for this metadata configuration item
     * Used to hide the value field when the key is "disabled"
     *
     * @return the CSS display style
     */
    public String getCssDisplay() {
        if (DISABLED_KEY.equals(this.keyName)) {
            return "display:none";
        } else {
            return "display:''";
        }
    }
}
