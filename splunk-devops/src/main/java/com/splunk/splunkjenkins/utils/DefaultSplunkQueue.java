package com.splunk.splunkjenkins.utils;

import hudson.Extension;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.model.EventRecord;
import com.splunk.splunkjenkins.model.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

@Extension
public class DefaultSplunkQueue extends LinkedBlockingQueue<EventRecord> implements SplunkQueue {
    public static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(DefaultSplunkQueue.class.getName());

    private Lock maintenanceLock = new ReentrantLock(true); // favor access to longest-waiting thread
    private final static int QUEUE_SIZE = 1 << 17;

    public DefaultSplunkQueue(){
        this(QUEUE_SIZE);
    }

    public DefaultSplunkQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean enqueue(EventRecord record) {
        if (SplunkJenkinsInstallation.get().isEventDisabled(record.getEventType())) {
            LOG.log(Level.FINE, "config invalid or eventType {0} is disabled, can not send {1}", new String[]{record.getEventType().toString(), record.getShortDescription()});
            return false;
        }

        // Insert record into queue, analyze capacity restrictions
        boolean added = false;
        try {
            // Try to add, wait for space to become available
            added = super.offer(record, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Try to add immediately
            added = super.offer(record);
        }
        if (!added) {
            LOG.log(Level.SEVERE, "failed to send message due to queue is full");
            if (maintenanceLock.tryLock()) {
                try {
                    // Event in queue may have format issues causing congestion, remove non-critical events
                    List<EventRecord> stuckRecords = new ArrayList<>(super.size());
                    super.drainTo(stuckRecords);
                    LOG.log(Level.SEVERE, "jenkins is too busy or has too few workers, clearing up queue");
                    for (EventRecord queuedRecord : stuckRecords) {
                        if (!queuedRecord.getEventType().equals(EventType.BUILD_REPORT)) {
                            continue;
                        }
                        boolean enqueued = super.offer(queuedRecord);
                        if (!enqueued) {
                            LOG.log(Level.SEVERE, "failed to add {0}", record.getShortDescription());
                            break;
                        }
                    }
                } finally {
                    maintenanceLock.unlock();
                }
            }
            return false;
        }

        return true;
    }
}
