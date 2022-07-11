package com.splunk.splunkjenkins.listeners;

import com.splunk.splunkjenkins.BaseTest;
import com.splunk.splunkjenkins.Constants;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.splunk.splunkjenkins.SplunkConfigUtil.verifySplunkSearchResult;
import static org.junit.Assert.assertEquals;

public class LoggingQueueListenerTest extends BaseTest {
    private int waitInQueueTime = 5;
    private String jobName = "validate-queue-foo";

    @Test
    public void testQueueMessage() throws ExecutionException, InterruptedException, IOException {
        FreeStyleProject project = j.createFreeStyleProject(jobName);
        long startTime = System.currentTimeMillis();
        // set a quiet period
        QueueTaskFuture future = project.scheduleBuild2(waitInQueueTime);
        Queue.Item[] items = j.jenkins.getQueue().getItems();
        assertEquals(1, items.length);
        Long queueId = items[0].getId();
        future.waitForStart();
        // check wait message
        String splQuery = "event_tag=queue queue_id=" + queueId + " type=dequeue_waiting "
                + "waiting_time>" + waitInQueueTime;
        verifySplunkSearchResult(splQuery, startTime, 1);
        splQuery = "event_tag=queue queue_id=" + queueId + " type=" + Constants.DEQUEUE_TAG_NAME;
        verifySplunkSearchResult(splQuery, startTime, 1);
        // set a label
        String nodeLabel = "remotes-no-such-node";
        project.setAssignedLabel(Label.get(nodeLabel));
        project.scheduleBuild2(0);
        items = j.jenkins.getQueue().getItems();
        assertEquals(1, items.length);
        queueId = items[0].getId();
        // job waits for start since there is no node have the label
        Thread.sleep(TimeUnit.SECONDS.toMillis(15));
        j.jenkins.getQueue().clear();
        splQuery = "event_tag=queue queue_id=" + queueId + " type=dequeue_buildable "
                + "buildable_time>3";
        verifySplunkSearchResult(splQuery, startTime, 1);
        splQuery = "event_tag=queue queue_id=" + queueId + " type=" + Constants.DEQUEUE_TAG_NAME;
        verifySplunkSearchResult(splQuery, startTime, 1);
    }
}