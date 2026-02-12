package com.splunk.splunkjenkins;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Constants used throughout the Splunk Jenkins plugin.
 * Contains configuration keys, event type constants, and buffer size settings.
 */
public class Constants {
    /**
     * Field name for test case data in events
     */
    public static final String TESTCASE = "testcase";
    /**
     * Field name for test suite data in events
     */
    public static final String TESTSUITE = "testsuite";
    /**
     * Field name for build URL identifier
     */
    public static final String BUILD_ID = "build_url";
    /**
     * Field name for event tagging
     */
    public static final String TAG = "event_tag";
    /**
     * Field name for job result status
     */
    public static final String JOB_RESULT = "job_result";
    /**
     * Splunk HEC endpoint for JSON events
     */
    public static final String JSON_ENDPOINT = "/services/collector/event";
    /**
     * Splunk HEC endpoint for raw events
     */
    public static final String RAW_ENDPOINT = "/services/collector/raw";
    /**
     * Log timestamp format string
     */
    public static final String LOG_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    /**
     * Tag name for agent/slave events
     */
    public final static String SLAVE_TAG_NAME = "slave";
    /**
     * Tag name for queue events
     */
    public final static String QUEUE_TAG_NAME = "queue";
    /**
     * Tag name for queue enqueue operations
     */
    public final static String ENQUEUE_TAG_NAME = "enqueue";
    /**
     * Tag name for queue dequeue operations
     */
    public final static String DEQUEUE_TAG_NAME = "dequeue";
    /**
     * Phase name for buildable queue items
     */
    public final static String BUILDABLE_PHASE_NAME = "buildable";
    /**
     * Phase name for blocked queue items
     */
    public final static String BLOCKED_PHASE_NAME = "blocked";
    /**
     * Phase name for waiting queue items
     */
    public final static String WAITING_PHASE_NAME = "waiting";
    /**
     * Item name for queue items
     */
    public final static String QUEUE_WAITING_ITEM_NAME = "queue_item";
    /**
     * Tag name for job events
     */
    public static final String JOB_EVENT_TAG_NAME = "job_event";
    /**
     * Monitor name for job monitoring
     */
    public static final String JOB_EVENT_MONITOR = "job_monitor";

    /**
     * Name for built-in Jenkins node
     * https://www.jenkins.io/doc/upgrade-guide/2.319/#built-in-node-name-and-label-migration
     */
    public static final String BUILT_IN_NODE = "(built-in)";
    /**
     * Tag for build report environment metadata
     */
    public static final String BUILD_REPORT_ENV_TAG = "metadata";
    /**
     * Prefix for Jenkins configuration source
     */
    public static final String JENKINS_CONFIG_PREFIX = "jenkins://";
    /**
     * Separator for Jenkins source paths
     */
    public static final String JENKINS_SOURCE_SEP = "/";
    /**
     * Source name for audit trail events
     */
    public static final String AUDIT_SOURCE = "audit_trail";
    /**
     * Field key for user information
     */
    public static final String USER_NAME_KEY = "user";
    /**
     * Field name for event source tracking
     */
    public static final String EVENT_CAUSED_BY = "event_src";
    /**
     * Field name for Splunk sourcetype
     */
    public static final String EVENT_SOURCE_TYPE = "sourcetype";
    /**
     * Field name for node name
     */
    public static final String NODE_NAME = "node_name";
    /**
     * Placeholder for no error message
     */
    public static final String ERROR_MESSAGE_NA = "(none)";
    /**
     * Masked password placeholder
     */
    public static final String MASK_PASSWORD = "***";
    /**
     * Message when no test report is found
     */
    public static final String NO_TEST_REPORT_FOUND = "No TestResult";
    /**
     * Message when JUnit/xUnit is not configured
     */
    public static final String TEST_REPORT_NOT_CONFIGURED = "Junit or xUnit report not configured";
    /**
     * Known script content hashes for security validation
     */
    public static final ImmutableList<String> SCRIPT_TEXT_MD5_HASH = ImmutableList.of("729ac3b82ecf2e0afc0cb00d73c22892",
            "f43916477139eb890e72c1602e0851b4", "aac4abe92db9bf90e3b27a4e41728526");
    // min buffer size for raw data (usually log file and console)
    /**
     * Minimum buffer size for raw data processing
     */
    public static final int MIN_BUFFER_SIZE = Integer.getInteger("splunkins.buffer", 4096);
    /**
     * Batch size for JDK fine logging
     */
    public static final int JDK_FINE_LOG_BATCH = Integer.getInteger("splunkins.debugLogBatchSize", 128);
    // max buffer size for raw data (usually log file and console)
    /**
     * Maximum batch size for data processing
     */
    public static final int MAX_BATCH_SIZE = 1 << 23;
    /**
     * Threshold for enabling gzip compression (1KB)
     */
    // use gzip for http posting
    public static final int GZIP_THRESHOLD = 1024; //1kb
    /**
     * Buffer size for slave/agent logs
     */
    // 16 KB for slave log
    public static final int SLAVE_LOG_BUFFER_SIZE = MIN_BUFFER_SIZE * 4;
    /**
     * Name for overall coverage metrics
     */
    public static final String COVERAGE_OVERALL_NAME = "project";
    /** Maximum line length (very long lines are, however, often a sign of garbage data)
    * if it is increased, please also increase the TRUNCATE config in splunk props.conf
    * ref: http://docs.splunk.com/Documentation/Splunk/7.2.1/Admin/Propsconf
    */
    public static final int CONSOLE_TEXT_SINGLE_LINE_MAX_LENGTH = Integer.getInteger("splunkins.lineTruncate", 100000);
    /** Maximum size for JUnit stdio content, in case keepLongStdio is turned on in junit publisher and large chunk data attached
    * the value should larger than junit's trimmed size 100KB, here use 2MiB as default
     */
    public static final int MAX_JUNIT_STDIO_SIZE = Integer.parseInt(System.getProperty("splunkins.junitStdioLimit", "2097152"));
    /**
     * Flag to enable pipeline console decoding
     */
    public static boolean DECODE_PIPELINE_CONSOLE = Boolean.parseBoolean(System.getProperty("splunkins.decodePipelineConsole", "true"));
    /**
     * Flag to enable POST request logging for audit
     */
    public static final boolean ENABLE_POST_LOGGER = Boolean.parseBoolean(System.getProperty("splunkins.auditPostRequest", "true"));
    /**
     * Pattern for whitelisting console log content
     */
    public static final Pattern JOB_CONSOLE_FILTER_WHITELIST_PATTERN;

    static {
        Pattern filterPattern = null;
        String filterStr = System.getProperty("splunkins.allowConsoleLogPattern", "");
        if (StringUtils.isNotBlank(filterStr)) {
            try {
                filterPattern = Pattern.compile(filterStr);

            } catch (PatternSyntaxException ex) {
                Logger.getLogger("com.splunk.splunkjenkins.SplunkJenkinsInstallation").log(Level.SEVERE,
                        "failed to parse allowConsoleLogPattern=" + filterStr, ex);
            }
        }
        JOB_CONSOLE_FILTER_WHITELIST_PATTERN = filterPattern;
    }
}
