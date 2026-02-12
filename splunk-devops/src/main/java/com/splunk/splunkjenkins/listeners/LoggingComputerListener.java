package com.splunk.splunkjenkins.listeners;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.utils.SplunkLogService;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.OfflineCause;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Map;

import static com.splunk.splunkjenkins.Constants.EVENT_CAUSED_BY;
import static com.splunk.splunkjenkins.model.EventType.SLAVE_INFO;
import static com.splunk.splunkjenkins.utils.LogEventHelper.getComputerStatus;

/**
 * Listens for Jenkins agent/computer events and sends status updates to Splunk.
 * Tracks online/offline status changes and launch failures.
 */
@SuppressWarnings("unused")
@Extension
public class LoggingComputerListener extends ComputerListener {
    /** {@inheritDoc} */
    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        updateStatus(c, "Online");
        listener.getLogger().flush();
    }

    /** {@inheritDoc} */
    @Override
    public void onOffline(@NonNull Computer c, @CheckForNull OfflineCause cause) {
        updateStatus(c, "Offline");
    }

    /** {@inheritDoc} */
    @Override
    public void onTemporarilyOnline(Computer c) {
        updateStatus(c, "Temporarily Online");
    }

    /** {@inheritDoc} */
    @Override
    public void onTemporarilyOffline(Computer c, OfflineCause cause) {
        updateStatus(c, "Temporarily Offline");
    }

    /** {@inheritDoc} */
    @Override
    public void onLaunchFailure(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        updateStatus(c, "Launch Failure");
        taskListener.getLogger().flush();
    }

    private void updateStatus(Computer c, String eventSource) {
        if (SplunkJenkinsInstallation.get().isEventDisabled(SLAVE_INFO)) {
            return;
        }
        Map slaveInfo = getComputerStatus(c);
        slaveInfo.put(EVENT_CAUSED_BY, eventSource);
        SplunkLogService.getInstance().send(slaveInfo, SLAVE_INFO);
    }

}
