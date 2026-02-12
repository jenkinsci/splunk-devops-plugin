package com.splunk.splunkjenkins.model;

/**
 * Enumeration of event types that can be sent to Splunk by the Jenkins plugin.
 * Each event type represents a different category of Jenkins data and has a flag
 * indicating whether the data needs to be split by line breaks before sending.
 */
public enum EventType {
    /**
     * Build report data
     */
    BUILD_REPORT(false),
    /**
     * Build events
     */
    BUILD_EVENT(false),
    /**
     * Queue information
     */
    QUEUE_INFO(false),
    /**
     * Jenkins configuration data
     */
    JENKINS_CONFIG(false),
    /**
     * Console log output (requires line splitting)
     */
    CONSOLE_LOG(true),
    /**
     * File data (requires line splitting)
     */
    FILE(true),
    /**
     * Slave/agent information
     */
    SLAVE_INFO(false),
    /**
     * Log data
     */
    LOG(false),
    /**
     * Batch JSON data
     */
    BATCH_JSON(false),
    /**
     * JSON file data (requires line splitting)
     */
    JSON_FILE(true);

    /**
     * whether the data need to be split by line breaker before send
     */
    private boolean needSplit;

    EventType(boolean needSplit) {
        this.needSplit = needSplit;
    }

    /**
     * Need spit the content line by line if raw event not supported
     * Only applied for non-structural data, such as file and console text.
     * It doesn't applied for json data or xml data
     *
     * @return <code>true</code> if need spit the contents line by line if raw event not supported;
     * <code>false</code> otherwise.
     */
    public boolean needSplit() {
        return needSplit;
    }

    /**
     * <p>getKey.</p>
     *
     * @param suffix the config metadata, can be either index, source or sourcetype
     * @return return name.suffix
     */
    public String getKey(String suffix) {
        return this.name().toLowerCase() + "." + suffix;
    }
}
