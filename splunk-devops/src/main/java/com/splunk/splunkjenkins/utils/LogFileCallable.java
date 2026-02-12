package com.splunk.splunkjenkins.utils;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.model.EventType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import hudson.util.ByteArrayOutputStream2;
import jenkins.util.JenkinsJVM;
import org.jenkinsci.remoting.RoleChecker;

import java.io.*;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.splunk.splunkjenkins.Constants.MIN_BUFFER_SIZE;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Callable for sending log files from remote agents to Splunk.
 */
public class LogFileCallable implements FilePath.FileCallable<Integer> {
    /**
     * Serialization version UID for this class.
     */
    private static final long serialVersionUID = 5303809353063980298L;
    /**
     * Logger instance for this class.
     */
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(LogFileCallable.class.getName());
    /**
     * System property name for configuring the timeout duration.
     */
    private static final String TIMEOUT_NAME = LogFileCallable.class.getName() + ".timeout";
    /**
     * Maximum wait time in minutes for sending files to Splunk.
     * Defaults to 5 minutes, configurable via system property.
     */
    private final int WAIT_MINUTES = Integer.getInteger(TIMEOUT_NAME, 5);
    /**
     * Base name/path for the log file.
     */
    private final String baseName;
    /**
     * URL of the Jenkins build associated with this log file.
     */
    private final String buildUrl;
    /**
     * Event collector properties for sending data to Splunk.
     */
    private final Map eventCollectorProperty;
    /**
     * Whether to send log files directly from the slave/agent node.
     */
    private final boolean sendFromSlave;
    /**
     * Maximum allowed file size for log file transmission.
     */
    private final long maxFileSize;
    /**
     * Whether Splunk configuration has been initialized on remote agents.
     */
    private boolean enabledSplunkConfig = false;

    /**
     * Creates a LogFileCallable instance for sending log files to Splunk
     *
     * @param baseName the base name/path for the file
     * @param buildUrl the build URL
     * @param eventCollectorProperty event collector properties
     * @param sendFromSlave whether to send from slave node
     * @param maxFileSize maximum file size allowed
     */
    public LogFileCallable(String baseName, String buildUrl,
                           Map eventCollectorProperty, boolean sendFromSlave, long maxFileSize) {
        this.baseName = baseName;
        this.eventCollectorProperty = eventCollectorProperty;
        this.buildUrl = buildUrl;
        this.sendFromSlave = sendFromSlave;
        this.maxFileSize = maxFileSize;
    }

    /**
     * Sends files to Splunk
     *
     * @param paths array of FilePath objects to send
     * @return number of events sent
     */
    public int sendFiles(FilePath[] paths) {
        int eventCount = 0;
        for (FilePath path : paths) {
            try {
                if (path.isDirectory()) {
                    continue;
                }
                if (sendFromSlave) {
                    LOG.log(Level.INFO, "uploading from agent:" + path.getName());
                    eventCount += path.act(this);
                    LOG.log(Level.FINE, "sent in " + eventCount + " batches");
                } else {
                    InputStream in = path.read();
                    try {
                        LOG.log(Level.FINE, "uploading from built-in node:" + path.getName());
                        eventCount += send(path.getRemote(), in);
                    } finally {
                        in.close();
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "archive file failed", e);
            }
        }
        return eventCount;
    }

    /**
     * Sends a file to Splunk via HTTP event collector
     *
     * @param fileName the name of the file being sent
     * @param input the InputStream containing the file content
     * @return number of events sent
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public Integer send(String fileName, InputStream input) throws IOException, InterruptedException {
        long throttleSize = SplunkJenkinsInstallation.get().getMaxEventsBatchSize();
        if (!SplunkJenkinsInstallation.get().isRawEventEnabled()) {
            //if raw event is not supported, we need split the content line by line and append metadata to each line
            throttleSize = throttleSize / 2;
        }
        //always use unix style path because windows slave maybe launched by ssh
        String sourceName = fileName.replace("\\", "/");
        String ws_posix_path = baseName.replace("\\", "/");
        if (sourceName.startsWith(ws_posix_path)) {
            sourceName = sourceName.substring(ws_posix_path.length() + 1);
        }
        sourceName = buildUrl + sourceName;
        boolean jsonFile = fileName.endsWith(".json");
        EventType eventType = EventType.FILE;
        if (jsonFile && SplunkJenkinsInstallation.get().isRawEventEnabled()) {
            throttleSize = maxFileSize;
            eventType = EventType.JSON_FILE;
        }
        ByteArrayOutputStream2 logText = new ByteArrayOutputStream2(MIN_BUFFER_SIZE);
        long totalSize = 0;
        Integer count = 0;
        int n;
        byte[] buffer = new byte[MIN_BUFFER_SIZE];
        while ((n = input.read(buffer)) >= 0) {
            totalSize += n;
            for (int i = 0; i < n; i++) {
                logText.write(buffer[i]);
                if (buffer[i] == '\n' && logText.size() > throttleSize) {
                    // file is too big to send in one request, use EventType.FILE
                    eventType = EventType.FILE;
                    flushLog(sourceName, logText, eventType);
                    count++;
                }
            }
            if (maxFileSize != 0 && totalSize > maxFileSize) {
                logText.write(("file truncated to size:" + totalSize).getBytes(UTF_8));
                SplunkLogService.getInstance().send(sourceName + " too large", "large_file");
                break;
            }
        }
        if (logText.size() > 0) {
            flushLog(sourceName, logText, eventType);
            count++;
        }
        return count;
    }

    /**
     * Initializes Splunk configuration on remote agents if not already initialized
     */
    private void initSplunkins() {
        if (enabledSplunkConfig) {
            return;
        }
        RemoteUtils.initSplunkConfigOnAgent(eventCollectorProperty);
        enabledSplunkConfig = true;
    }

    /**
     * Flushes log data to Splunk
     *
     * @param source the source name for the log data
     * @param out the ByteArrayOutputStream containing the log data
     * @param eventType the type of event
     */
    private void flushLog(String source, ByteArrayOutputStream out, EventType eventType) {
        try {
            String text = out.toString("UTF-8");
            SoftReference<String> textRef = new SoftReference<>(text);
            SplunkLogService.getInstance().send(textRef, eventType, source);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        out.reset();
    }

    /** {@inheritDoc} */
    @Override
    public Integer invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        if (!JenkinsJVM.isJenkinsJVM()) {
            //running on slave node, need init config
            initSplunkins();
        }
        InputStream input = new FileInputStream(f);
        try {
            long expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(WAIT_MINUTES);
            int count = send(f.getAbsolutePath(), input);
            while (SplunkLogService.getInstance().getQueueSize() > 0 && System.currentTimeMillis() < expireTime) {
                Thread.sleep(500);
            }
            if (System.currentTimeMillis() > expireTime) {
                LOG.log(Level.SEVERE, "sending file timeout in " + WAIT_MINUTES + " minutes," +
                        " please adjust the value by passing -D" + TIMEOUT_NAME + "=minutes to slave jvm parameter");
            }
            SplunkLogService.getInstance().stopWorker();
            SplunkLogService.getInstance().releaseConnection();
            return count;
        } finally {
            input.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
