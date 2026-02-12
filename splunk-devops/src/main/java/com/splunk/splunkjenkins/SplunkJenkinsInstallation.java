package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.model.EventType;
import com.splunk.splunkjenkins.model.MetaDataConfigItem;
import com.splunk.splunkjenkins.utils.SplunkLogService;
import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.GroovyCodeSource;
import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.splunk.splunkjenkins.Constants.*;
import static com.splunk.splunkjenkins.utils.LogEventHelper.getDefaultDslScript;
import static com.splunk.splunkjenkins.utils.LogEventHelper.nonEmpty;
import static com.splunk.splunkjenkins.utils.LogEventHelper.validateGroovyScript;
import static com.splunk.splunkjenkins.utils.LogEventHelper.verifyHttpInput;
import static groovy.lang.GroovyShell.DEFAULT_CODE_BASE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


/**
 * Global configuration for the Splunk Jenkins plugin.
 * Manages connection settings, metadata configuration, and plugin behavior.
 */
@Extension
public class SplunkJenkinsInstallation extends GlobalConfiguration {
    private static transient boolean logHandlerRegistered = false;
    private transient static final Logger LOG = Logger.getLogger(SplunkJenkinsInstallation.class.getName());
    private transient volatile static SplunkJenkinsInstallation cachedConfig;
    private transient static final Pattern uuidPattern = Pattern.compile("[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}", CASE_INSENSITIVE);
    // Defaults plugin global config values
    private boolean enabled = false;
    private String host;
    private Secret token;
    private boolean useSSL = true;
    private Integer port = 8088;
    //for console log default cache size for 256KB
    private long maxEventsBatchSize = 1 << 18;
    private long retriesOnError = 3;
    private boolean rawEventEnabled = true;
    //groovy script path
    private String scriptPath;
    private String metaDataConfig;
    //groovy content if file path not set
    private String scriptContent;
    //the app-jenkins link
    private String splunkAppUrl;
    private String metadataHost;
    private String metadataSource;
    private String ignoredJobs;
    private Boolean globalPipelineFilter;

    //below are all transient properties
    /**
     * Metadata properties configuration (transient, not persisted)
     */
    public transient Properties metaDataProperties = new Properties();
    //cached values, will not be saved to disk!
    private transient String jsonUrl;
    private transient String rawUrl;
    private transient File scriptFile;
    private transient long scriptTimestamp;
    private transient String postActionScript;
    private transient Set<MetaDataConfigItem> metadataItemSet = new HashSet<>();
    private transient String defaultMetaData;
    private transient Pattern ignoredJobPattern;

    /**
     * Constructs SplunkJenkinsInstallation with option to load from configuration file
     *
     * @param useConfigFile whether to load configuration from disk
     */
    public SplunkJenkinsInstallation(boolean useConfigFile) {
        if (useConfigFile) {
            load();
        }
    }

    /**
     * {@inheritDoc}
     * Loads configuration from disk and initializes default metadata properties.
     */
    @Override
    public synchronized final void load() {
        super.load();
        migrate();
        //load default metadata
        try (InputStream metaInput = this.getClass().getClassLoader().getResourceAsStream("metadata.properties")) {
            defaultMetaData = IOUtils.toString(metaInput);
        } catch (IOException e) {
            //ignore
        }
    }

    /**
     * Constructs SplunkJenkinsInstallation with default configuration loading
     */
    public SplunkJenkinsInstallation() {
        this(true);
    }

    /**
     * Gets the singleton instance of SplunkJenkinsInstallation
     *
     * @return the SplunkJenkinsInstallation instance
     */
    public static SplunkJenkinsInstallation get() {
        if (cachedConfig == null) {
            if (Jenkins.getInstanceOrNull() == null) {
                // Jenkins is not ready yet
                return buildTempInstance();
            }
            synchronized (SplunkJenkinsInstallation.class) {
                if (cachedConfig == null) {
                    cachedConfig = (SplunkJenkinsInstallation) Jenkins.getInstance().getDescriptor(SplunkJenkinsInstallation.class);
                    if (cachedConfig == null) {
                        return buildTempInstance();
                    }
                }
            }
        }
        return cachedConfig;
    }

    // a temp instance with disabled flag
    private static SplunkJenkinsInstallation buildTempInstance() {
        SplunkJenkinsInstallation temp = new SplunkJenkinsInstallation(false);
        temp.enabled = false;
        return temp;
    }

    /**
     * <p>isLogHandlerRegistered.</p>
     *
     * @return true if the plugin had been setup by Jenkins (constructor had been called)
     */
    public static boolean isLogHandlerRegistered() {
        return logHandlerRegistered && Jenkins.getInstance() != null;
    }

    /**
     * mark this plugin as initiated
     *
     * @param completed mark the init as initiate completed
     */
    public static void markComplete(boolean completed) {
        logHandlerRegistered = completed;
    }

    /**
     * Note: this method is meant to be called on agent only!
     *
     * @param config the SplunkJenkinsInstallation to be used on Agent
     */
    public static void initOnAgent(SplunkJenkinsInstallation config) {
        SplunkJenkinsInstallation.cachedConfig = config;
        config.updateCache();
    }

    /**
     * {@inheritDoc}
     * Handles the global configuration form submission, binding form data,
     * updating the cache, and managing the log service lifecycle.
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        this.metadataItemSet = null; // otherwise bindJSON will never clear it once set
        boolean previousState = this.enabled;
        req.bindJSON(this, formData);
        if (this.metadataItemSet == null) {
            this.metaDataConfig = "";
        }
        //handle choice
        if ("file".equals(formData.get("commandsOrFileInSplunkins"))) {
            this.scriptContent = null;
        } else {
            this.scriptPath = null;
        }
        updateCache();
        save();
        if (previousState && !this.enabled) {
            //switch from enable to disable
            SplunkLogService.getInstance().stopWorker();
            SplunkLogService.getInstance().releaseConnection();
        }
        return true;
    }

    /**
     * Validates the Splunk hostname configuration
     *
     * @param hostName the hostname to validate
     * @return a {@link hudson.util.FormValidation} object.
     */
    @RequirePOST
    public FormValidation doCheckHost(@QueryParameter("value") String hostName) {
        if (StringUtils.isBlank(hostName)) {
            return FormValidation.warning(Messages.PleaseProvideHost());
        } else if (hostName.startsWith("http://") || hostName.startsWith("https://")) {
            try {
                URI uri = new URI(hostName);
                String domain = uri.getHost();
                return FormValidation.warning(Messages.HostNameSchemaWarning(domain));
            } catch (URISyntaxException e) {
                return FormValidation.warning(Messages.HostNameInvalid());
            }
        } else if ((hostName.endsWith("cloud.splunk.com") || hostName.endsWith("splunkcloud.com")
                || hostName.endsWith("splunktrial.com")) &&
                !(hostName.startsWith("input-") || hostName.startsWith("http-inputs-"))) {
            return FormValidation.warning(Messages.CloudHostPrefix(hostName));
        } else {
            return FormValidation.ok();
        }
    }


    /**
     * Validates the Splunk token configuration
     *
     * @param token the token to validate
     * @return a {@link hudson.util.FormValidation} object.
     */
    @RequirePOST
    public FormValidation doCheckToken(@QueryParameter("value") Secret token) {
        String value = Secret.toString(token);
        //check GUID format such as 18654C68-B28B-4450-9CF0-6E7645CA60CA
        if (StringUtils.isBlank(value) || !uuidPattern.matcher(value).find()) {
            return FormValidation.warning(Messages.InvalidToken());
        }

        return FormValidation.ok();
    }


    /**
     * Tests the Splunk HTTP Event Collector connection
     *
     * @param host           the Splunk host
     * @param port           the Splunk port
     * @param token          the authentication token
     * @param useSSL         whether to use SSL
     * @param metaDataConfig metadata configuration
     * @return a {@link hudson.util.FormValidation} object.
     */
    @RequirePOST
    public FormValidation doTestHttpInput(@QueryParameter String host, @QueryParameter int port,
                                          @QueryParameter Secret token, @QueryParameter boolean useSSL,
                                          @QueryParameter String metaDataConfig) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        //create new instance to avoid pollution global config
        SplunkJenkinsInstallation config = new SplunkJenkinsInstallation(false);
        config.host = host;
        config.port = port;
        config.token = token;
        config.useSSL = useSSL;
        config.metaDataConfig = metaDataConfig;
        config.enabled = true;
        config.updateCache();
        if (!config.isValid()) {
            return FormValidation.error(Messages.InvalidHostOrToken());
        }
        return verifyHttpInput(config);
    }


    /**
     * Validates the Groovy script content
     *
     * @param value the script content to validate
     * @return a {@link hudson.util.FormValidation} object.
     */
    @RequirePOST
    public FormValidation doCheckScriptContent(@QueryParameter String value) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        if (StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        return validateGroovyScript(value);
    }


    /**
     * Validates the maximum events batch size configuration
     *
     * @param value the batch size value to validate
     * @return a {@link hudson.util.FormValidation} object.
     */
    @RequirePOST
    public FormValidation doCheckMaxEventsBatchSize(@QueryParameter int value) {
        if (value < MIN_BUFFER_SIZE || value > MAX_BATCH_SIZE) {
            return FormValidation.error(String.format("please consider a value between %d and %d", MIN_BUFFER_SIZE, MAX_BATCH_SIZE));
        }
        return FormValidation.ok();
    }


    /**
     * Validates the ignored jobs regex pattern
     *
     * @param value the regex pattern to validate
     * @return a {@link hudson.util.FormValidation} object.
     */
    @RequirePOST
    public FormValidation doCheckIgnoredJobs(@QueryParameter String value) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        try {
            Pattern.compile(value);
        } catch (PatternSyntaxException ex) {
            return FormValidation.errorWithMarkup(Messages.InvalidPattern());
        }
        return FormValidation.ok();
    }

    ////////END OF FORM VALIDATION/////////
    /**
     * Updates the cached configuration values including URLs, script content,
     * metadata properties, and ignored job patterns.
     */
    protected void updateCache() {
        if (!this.enabled) {
            //nothing to do if not enabled
            return;
        }
        if (scriptPath != null) {
            scriptFile = new File(scriptPath);
            //load the text content into postActionScript
            refreshScriptText();
        } else if (nonEmpty(scriptContent)) {
            postActionScript = scriptContent;
            scriptTimestamp = System.currentTimeMillis();
            checkApprove(postActionScript);
        } else {
            postActionScript = null;
            scriptTimestamp = 0;
        }
        if (StringUtils.isEmpty(ignoredJobs)) {
            ignoredJobPattern = null;
        } else {
            try {
                ignoredJobPattern = Pattern.compile(ignoredJobs);
            } catch (PatternSyntaxException ex) {
                LOG.log(Level.SEVERE, "invalid ignore job pattern {0}, error: {1}", new Object[]{
                        ignoredJobs, ex.getDescription()});
            }
        }
        try {
            String scheme = useSSL ? "https://" : "http://";
            jsonUrl = scheme + host + ":" + port + JSON_ENDPOINT;
            rawUrl = scheme + host + ":" + port + RAW_ENDPOINT;
            //discard previous metadata cache and load new one
            metaDataProperties = new Properties();
            String combinedMetaData = Util.fixNull(defaultMetaData) + "\n" + Util.fixNull(metaDataConfig);
            if (!isEmpty(combinedMetaData)) {
                metaDataProperties.load(new StringReader(combinedMetaData));
            }
            if (isNotEmpty(metadataSource)) {
                metaDataProperties.put("source", metadataSource);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "update cache failed, splunk host:" + host, e);
        }
    }

    private void checkApprove(String scriptText) {
        if (scriptText == null) {
            return;
        }
        // During startup, hudson.model.User.current() calls User.load which will load other plugins, will throw error:
        // Tried proxy for com.splunk.splunkjenkins.SplunkJenkinsInstallation to support a circular dependency, but it is not an interface.
        // Use Jenkins.getAuthentication() will bypass the issue
        Authentication auth = Jenkins.getAuthentication();
        String userName = auth.getName();

        ApprovalContext context = ApprovalContext.create().withUser(userName).withKey(this.getClass().getName());
        //check approval and save pending for admin approval
        ScriptApproval.get().configuring(scriptText, GroovyLanguage.get(), context);
    }

    /**
     * Reload script content from file if modified
     */
    private void refreshScriptText() {
        if (scriptFile == null) {
            return;
        }
        try {
            if (!scriptFile.canRead()) {
                postActionScript = null;
            } else {
                scriptTimestamp = scriptFile.lastModified();
                postActionScript = IOUtils.toString(scriptFile.toURI());
                checkApprove(postActionScript);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "can not read file " + scriptFile, e);
            //file was removed from jenkins, just ignore
        }
    }

    /**
     * check if configured correctly
     *
     * @return true setup is completed
     */
    public boolean isValid() {
        return enabled && host != null && token != null && isNotEmpty(token.getPlainText())
                && jsonUrl != null && rawUrl != null;
    }

    /**
     * get cached script contents
     *
     * @return script content
     */
    public String getScript() {
        if (scriptPath != null && scriptFile.lastModified() > scriptTimestamp) {
            refreshScriptText();
        }
        return this.postActionScript;
    }

    /**
     * Creates a GroovyCodeSource from the current script content.
     *
     * @return the compiled GroovyCodeSource for the user script
     */
    public GroovyCodeSource getCode() {
        String script = getScript();
        GroovyCodeSource codeSource = new GroovyCodeSource(script, "SplunkinUserScript" + scriptTimestamp, DEFAULT_CODE_BASE);
        return codeSource;
    }

    /**
     * Checks whether raw event sending is enabled.
     *
     * @return true if raw event sending is enabled
     */
    public boolean isRawEventEnabled() {
        return rawEventEnabled;
    }

    /**
     * Check whether we can optimize sending process, e.g. if we need to send 1000 lines for one job console log,
     * and we can specify host,source,sourcetype,index only once in query parameter if raw event is supported,
     * instead of sending 1000 times in request body
     *
     * @param eventType does this type of text need to be logged to splunk line by line
     * @return true if HEC supports specify metadata in url query parameter
     */
    public boolean canPostRaw(EventType eventType) {
        return rawEventEnabled && eventType.needSplit();
    }

    /**
     * Gets the Splunk HEC authentication token.
     *
     * @return the token as a Secret
     */
    public Secret getToken() {
        return token;
    }

    /**
     * Gets the plain text value of the Splunk HEC token.
     *
     * @return the token value as a string, never null
     */
    @NonNull
    public String getTokenValue() {
        return Secret.toString(token);
    }
    /**
     * Gets the maximum number of retries on error.
     *
     * @return the maximum number of retries
     */
    public long getMaxRetries() {
        return retriesOnError;
    }

    /**
     * <p>getMetaData.</p>
     *
     * @param keyName such as host,source,index
     * @return the configured metadata
     */
    public String getMetaData(String keyName) {
        return metaDataProperties.getProperty(keyName);
    }

    /**
     * Gets the Splunk HEC JSON event endpoint URL.
     *
     * @return the JSON endpoint URL
     */
    public String getJsonUrl() {
        return jsonUrl;
    }

    /**
     * Gets the Splunk HEC raw event endpoint URL.
     *
     * @return the raw endpoint URL
     */
    public String getRawUrl() {
        return rawUrl;
    }

    /**
     * Checks whether the plugin is enabled.
     *
     * @return true if the plugin is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * <p>isEventDisabled.</p>
     *
     * @param eventType a {@link com.splunk.splunkjenkins.model.EventType} object.
     * @return a boolean.
     */
    public boolean isEventDisabled(EventType eventType) {
        return !isValid() || metaDataProperties == null || "false".equals(metaDataProperties.getProperty(eventType.getKey("enabled")));
    }

    /**
     * <p>isJobIgnored.</p>
     *
     * @param jobUrl a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isJobIgnored(String jobUrl) {
        boolean ignored = false;
        if (JOB_CONSOLE_FILTER_WHITELIST_PATTERN != null) {
            // white list via system properties
            if (!JOB_CONSOLE_FILTER_WHITELIST_PATTERN.matcher(jobUrl).find()) {
                LOG.log(Level.FINE, "{0} is not in whitelist set by splunkins.allowConsoleLogPattern", jobUrl);
                ignored = true;
            }
        }
        if (!ignored && ignoredJobPattern != null) {
            // black list
            ignored = ignoredJobPattern.matcher(jobUrl).find();
        }
        return ignored;
    }

    /**
     * <p>Setter for the field <code>enabled</code>.</p>
     *
     * @param enabled a boolean.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * <p>Getter for the field <code>host</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>Setter for the field <code>host</code>.</p>
     *
     * @param host a {@link java.lang.String} object.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * <p>Setter for the field <code>token</code>.</p>
     *
     * @param token a {@link hudson.util.Secret} object.
     */
    public void setToken(Secret token) {
        this.token = token;
    }

    /**
     * <p>isUseSSL.</p>
     *
     * @return a boolean.
     */
    public boolean isUseSSL() {
        return useSSL;
    }

    /**
     * <p>Setter for the field <code>useSSL</code>.</p>
     *
     * @param useSSL a boolean.
     */
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    /**
     * <p>Getter for the field <code>maxEventsBatchSize</code>.</p>
     *
     * @return a long.
     */
    public long getMaxEventsBatchSize() {
        return maxEventsBatchSize;
    }

    /**
     * <p>Setter for the field <code>maxEventsBatchSize</code>.</p>
     *
     * @param maxEventsBatchSize a long.
     */
    public void setMaxEventsBatchSize(long maxEventsBatchSize) {
        if (maxEventsBatchSize > MIN_BUFFER_SIZE) {
            this.maxEventsBatchSize = maxEventsBatchSize;
        } else {
            this.maxEventsBatchSize = MIN_BUFFER_SIZE;
        }
    }

    /**
     * <p>Setter for the field <code>rawEventEnabled</code>.</p>
     *
     * @param rawEventEnabled a boolean.
     */
    public void setRawEventEnabled(boolean rawEventEnabled) {
        this.rawEventEnabled = rawEventEnabled;
    }

    /**
     * <p>Getter for the field <code>metaDataConfig</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetaDataConfig() {
        return metaDataConfig;
    }

    /**
     * <p>Setter for the field <code>metaDataConfig</code>.</p>
     *
     * @param metaDataConfig a {@link java.lang.String} object.
     */
    public void setMetaDataConfig(String metaDataConfig) {
        this.metaDataConfig = metaDataConfig;
    }

    /**
     * <p>Getter for the field <code>port</code>.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * <p>Setter for the field <code>port</code>.</p>
     *
     * @param port a {@link java.lang.Integer} object.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * <p>Getter for the field <code>retriesOnError</code>.</p>
     *
     * @return a long.
     */
    public long getRetriesOnError() {
        return retriesOnError;
    }

    /**
     * <p>Setter for the field <code>retriesOnError</code>.</p>
     *
     * @param retriesOnError a long.
     */
    public void setRetriesOnError(long retriesOnError) {
        this.retriesOnError = retriesOnError;
    }

    /**
     * <p>Getter for the field <code>scriptPath</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getScriptPath() {
        return scriptPath;
    }

    /**
     * <p>Setter for the field <code>scriptPath</code>.</p>
     *
     * @param scriptPath a {@link java.lang.String} object.
     */
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    /**
     * Gets the Groovy script content
     *
     * @return the script content
     */
    public String getScriptContent() {
        return scriptContent;
    }

    /**
     * Sets the Groovy script content
     *
     * @param scriptContent the script content to set
     */
    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    /**
     * Converts the configuration to a Map for agent communication
     *
     * @return map representation of the configuration
     */
    public Map toMap() {
        HashMap map = new HashMap();
        map.put("token", this.getTokenValue());
        map.put("rawEventEnabled", this.rawEventEnabled);
        map.put("maxEventsBatchSize", this.maxEventsBatchSize);
        map.put("host", host);
        map.put("port", port);
        map.put("useSSL", useSSL);
        map.put("metaDataConfig", Util.fixNull(defaultMetaData) + Util.fixNull(metaDataConfig));
        map.put("retriesOnError", retriesOnError);
        map.put("metadataHost", metadataHost);
        map.put("metadataSource", metadataSource);
        return map;
    }

    /**
     * Gets the script content or returns the default DSL script
     *
     * @return script content or default DSL script
     */
    public String getScriptOrDefault() {
        if (scriptContent == null && scriptPath == null) {
            //when user clear the text on UI, it will be set to empty string
            //so use null check will not overwrite user settings
            return getDefaultDslScript();
        } else {
            return scriptContent;
        }
    }

    /**
     * Gets the Splunk app URL
     *
     * @return the Splunk app URL, or a default based on the host
     */
    public String getSplunkAppUrl() {
        if (isEmpty(splunkAppUrl) && isNotEmpty(host)) {
            return "http://" + host + ":8000/en-US/app/splunk_app_jenkins/";
        }
        return splunkAppUrl;
    }

    /**
     * Gets the Splunk app URL or help page if not configured
     *
     * @return the Splunk app URL or help page URL
     */
    public String getAppUrlOrHelp() {
        String url = getSplunkAppUrl();
        if (isEmpty(url)) {
            return "/plugin/splunk-devops/help-splunkAppUrl.html?";
        }
        return url;
    }

    /**
     * Sets the Splunk app URL
     *
     * @param splunkAppUrl the Splunk app URL to set
     */
    public void setSplunkAppUrl(String splunkAppUrl) {
        if (!isEmpty(splunkAppUrl) && !splunkAppUrl.endsWith("/")) {
            splunkAppUrl += "/";
        }
        this.splunkAppUrl = splunkAppUrl;
    }

    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "jenkins";
        }
    }

    /**
     * <p>Getter for the field <code>metadataItemSet</code>.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<MetaDataConfigItem> getMetadataItemSet() {
        return metadataItemSet;
    }

    /**
     * <p>Getter for the field <code>metadataHost</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataHost() {
        if (metadataHost != null) {
            return metadataHost;
        } else {
            //backwards compatible
            if (metaDataProperties != null && metaDataProperties.containsKey("host")) {
                return metaDataProperties.getProperty("host");
            } else {
                String url = null;
                JenkinsLocationConfiguration jenkinsLocation = JenkinsLocationConfiguration.get();
                if (jenkinsLocation != null) {
                    url = jenkinsLocation.getUrl();
                }
                if (url != null && !url.startsWith("http://localhost")) {
                    try {
                        return (new URL(url)).getHost();
                    } catch (MalformedURLException e) {
                        //do not care,just ignore
                    }
                }
                return getLocalHostName();
            }
        }
    }

    /**
     * <p>Setter for the field <code>metadataHost</code>.</p>
     *
     * @param metadataHost a {@link java.lang.String} object.
     */
    public void setMetadataHost(String metadataHost) {
        this.metadataHost = metadataHost;
    }

    /**
     * <p>Setter for the field <code>metadataItemSet</code>.</p>
     *
     * @param metadataItemSet a {@link java.util.Set} object.
     */
    public void setMetadataItemSet(Set<MetaDataConfigItem> metadataItemSet) {
        this.metadataItemSet = metadataItemSet;
        this.metaDataConfig = MetaDataConfigItem.toString(metadataItemSet);
    }

    /**
     * <p>Getter for the field <code>metadataSource</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataSource() {
        if (metadataSource != null) {
            return metadataSource;
        } else if (metaDataProperties != null && metaDataProperties.containsKey("source")) {
            return metaDataProperties.getProperty("source");
        } else {
            return "";
        }
    }

    /**
     * <p>Getter for the field <code>metadataSource</code>.</p>
     *
     * @param suffix a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataSource(String suffix) {
        return getMetadataSource() + JENKINS_SOURCE_SEP + suffix;
    }

    /**
     * <p>Setter for the field <code>metadataSource</code>.</p>
     *
     * @param metadataSource a {@link java.lang.String} object.
     */
    public void setMetadataSource(String metadataSource) {
        this.metadataSource = metadataSource;
    }

    private void migrate() {
        if (this.scriptContent != null) {
            String hash = DigestUtils.md5Hex(this.scriptContent);
            if (SCRIPT_TEXT_MD5_HASH.contains(hash)) { //previous versions' script hash, update to use new version
                this.scriptContent = getDefaultDslScript();
            }
        }
        this.metadataItemSet = MetaDataConfigItem.loadProps(this.metaDataConfig);
        //migrate settings prior to 1.9.0
        if (this.globalPipelineFilter == null) {
            this.globalPipelineFilter = true;
        }
    }

    /**
     * <p>Getter for the field <code>ignoredJobs</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIgnoredJobs() {
        return ignoredJobs;
    }

    /**
     * <p>Setter for the field <code>ignoredJobs</code>.</p>
     *
     * @param ignoredJobs a {@link java.lang.String} object.
     */
    public void setIgnoredJobs(String ignoredJobs) {
        this.ignoredJobs = ignoredJobs;
    }

    /**
     * <p>Getter for the field <code>globalPipelineFilter</code>.</p>
     *
     * @return a {@link java.lang.Boolean} object.
     */
    public Boolean getGlobalPipelineFilter() {
        return globalPipelineFilter;
    }

    /**
     * <p>Setter for the field <code>globalPipelineFilter</code>.</p>
     *
     * @param globalPipelineFilter a {@link java.lang.Boolean} object.
     */
    public void setGlobalPipelineFilter(Boolean globalPipelineFilter) {
        this.globalPipelineFilter = globalPipelineFilter;
    }

    /**
     * <p>isPipelineFilterEnabled.</p>
     *
     * @return a boolean.
     */
    public boolean isPipelineFilterEnabled() {
        return Boolean.TRUE.equals(globalPipelineFilter);
    }
}
