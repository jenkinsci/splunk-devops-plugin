package com.splunk.splunkjenkins.console;

import com.splunk.splunkjenkins.model.EventRecord;
import com.splunk.splunkjenkins.model.EventType;
import com.splunk.splunkjenkins.utils.SplunkLogService;
import jenkins.util.JenkinsJVM;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleRecordCacheUtils {
    private static final int CACHED_LINES_LIMIT;
    private transient static final Logger LOGGER = Logger.getLogger(ConsoleRecordCacheUtils.class.getName());
    private transient static final ConcurrentLinkedQueue<EventRecord> consoleQueue = new ConcurrentLinkedQueue<>();

    static {
        if (JenkinsJVM.isJenkinsJVM()) {
            CACHED_LINES_LIMIT = 200;
        } else {
            CACHED_LINES_LIMIT = 10;
        }
    }

    public static void enqueue(EventRecord record) {
        boolean added = consoleQueue.add(record);
        if (!added) {
            LOGGER.warning("failed to add log " + record.getMessageString());
        } else if (consoleQueue.size() > CACHED_LINES_LIMIT) {
            flushLog();
        }
    }

    public static void flushLog() {
        EventRecord record;
        List<EventRecord> pendingRecords = new ArrayList<>();
        try {
            while ((record = consoleQueue.poll()) != null) {
                pendingRecords.add(record);
            }
            SplunkLogService.getInstance().sendBatch(pendingRecords, EventType.CONSOLE_LOG);
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "flush log error", ex);
        }
    }
}
