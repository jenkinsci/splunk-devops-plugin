package com.splunk.splunkjenkins;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Constants {
    public static final String TESTCASE = "testcase";
    public static final String TESTSUITE = "testsuite";
    public static final String BUILD_ID = "build_url";
    public static final String TAG = "event_tag";
    public static final String JOB_RESULT = "job_result";
    public static final String JSON_ENDPOINT = "/services/collector/event";
    public static final String RAW_ENDPOINT = "/services/collector/raw";
    public static final String LOG_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public final static String SLAVE_TAG_NAME = "slave";
    public final static String QUEUE_TAG_NAME = "queue";
    public final static String ENQUEUE_TAG_NAME = "enqueue";
    public final static String DEQUEUE_TAG_NAME = "dequeue";
    public final static String BUILDABLE_PHASE_NAME = "buildable";
    public final static String BLOCKED_PHASE_NAME = "blocked";
    public final static String WAITING_PHASE_NAME = "waiting";
    public final static String QUEUE_WAITING_ITEM_NAME = "queue_item";
    public static final String JOB_EVENT_TAG_NAME = "job_event";
    public static final String JOB_EVENT_MONITOR = "job_monitor";
    // https://www.jenkins.io/doc/upgrade-guide/2.319/#built-in-node-name-and-label-migration
    public static final String BUILT_IN_NODE = "(built-in)";
    public static final String BUILD_REPORT_ENV_TAG = "metadata";
    public static final String JENKINS_CONFIG_PREFIX = "jenkins://";
    public static final String JENKINS_SOURCE_SEP = "/";
    public static final String AUDIT_SOURCE = "audit_trail";
    public static final String USER_NAME_KEY = "user";
    public static final String EVENT_CAUSED_BY = "event_src";
    public static final String EVENT_SOURCE_TYPE = "sourcetype";
    public static final String NODE_NAME = "node_name";
    public static final String ERROR_MESSAGE_NA = "(none)";
    public static final String MASK_PASSWORD = "***";
    public static final String NO_TEST_REPORT_FOUND = "No TestResult";
    public static final String TEST_REPORT_NOT_CONFIGURED = "Junit or xUnit report not configured";
    public static final ImmutableList<String> SCRIPT_TEXT_MD5_HASH = ImmutableList.of("729ac3b82ecf2e0afc0cb00d73c22892",
            "f43916477139eb890e72c1602e0851b4", "aac4abe92db9bf90e3b27a4e41728526");
    // min buffer size for raw data (usually log file and console)
    public static final int MIN_BUFFER_SIZE = Integer.getInteger("splunkins.buffer", 4096);
    public static final int JDK_FINE_LOG_BATCH = Integer.getInteger("splunkins.debugLogBatchSize", 128);
    // max buffer size for raw data (usually log file and console)
    public static final int MAX_BATCH_SIZE = 1 << 23;
    // use gzip for http posting
    public static final int GZIP_THRESHOLD = 1024; //1kb
    // 16 KB for slave log
    public static final int SLAVE_LOG_BUFFER_SIZE = MIN_BUFFER_SIZE * 4;
    public static final String COVERAGE_OVERALL_NAME = "project";
    // maximum line length (very long lines are, however, often a sign of garbage data)
    // if it is increased, please also increase the TRUNCATE config in splunk props.conf
    // ref: http://docs.splunk.com/Documentation/Splunk/7.2.1/Admin/Propsconf
    public static final int CONSOLE_TEXT_SINGLE_LINE_MAX_LENGTH = Integer.getInteger("splunkins.lineTruncate", 100000);
    //junit stdio limit, in case keepLongStdio is turned on in junit publisher and large chunk data attached
    //the value should large than junit's trimmed size 100KB, here use 2MiB as default
    public static final int MAX_JUNIT_STDIO_SIZE = Integer.parseInt(System.getProperty("splunkins.junitStdioLimit", "2097152"));
    public static boolean DECODE_PIPELINE_CONSOLE = Boolean.parseBoolean(System.getProperty("splunkins.decodePipelineConsole", "true"));
    public static final boolean ENABLE_POST_LOGGER = Boolean.parseBoolean(System.getProperty("splunkins.auditPostRequest", "true"));
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
