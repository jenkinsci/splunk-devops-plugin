package com.splunk.splunkjenkins;

import java.io.IOException;

import shaded.splk.org.apache.http.HttpResponse;
import shaded.splk.org.apache.http.StatusLine;
import shaded.splk.org.apache.http.HttpStatus;
import shaded.splk.org.apache.http.client.HttpClient;
import shaded.splk.org.apache.http.client.methods.HttpPost;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.splunk.splunkjenkins.utils.LogConsumer;
import com.splunk.splunkjenkins.model.EventType;
import com.splunk.splunkjenkins.model.EventRecord;
import com.splunk.splunkjenkins.utils.SplunkQueue;
import com.splunk.splunkjenkins.utils.DefaultSplunkQueue;

import static com.splunk.splunkjenkins.utils.LogEventHelper.buildPost;

public class LogConsumerTest {
    private static final Logger LOG = Logger.getLogger(LogConsumerTest.class.getName());

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        // Configure dummy Splunk installation
        SplunkJenkinsInstallation splunkJenkinsInstallation = new SplunkJenkinsInstallation();
        splunkJenkinsInstallation.setEnabled(true);
        SplunkJenkinsInstallation.initOnSlave(splunkJenkinsInstallation);
    }

    @Test
    public void test502Retry() throws Exception {
        // Configure mocked objects
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpPost mockHttpPost = mock(HttpPost.class);
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);

        // Configure mocked responses
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_GATEWAY);

        // Verify mocked 502 response set-up
        HttpPost testPost = buildPost(
            new EventRecord("ping from test502Retry", EventType.LOG),
            SplunkJenkinsInstallation.get()
        );
        final HttpResponse testResponse = mockHttpClient.execute(testPost);
        assertEquals(502, testResponse.getStatusLine().getStatusCode());

        // Create LogConsumer thread to test
        SplunkQueue logQueue = new DefaultSplunkQueue(5);
        String line = "127.0.0.1 - admin \"GET /en-US/ HTTP/1.1\"";
        EventRecord record = new EventRecord(line, EventType.LOG);
        boolean added = logQueue.offer(record);
        assertTrue(added);
        LOG.info("Added record to test queue for initialization: " + logQueue.toString());

        // Start LogConsumer thread
        LogConsumer workerThread = new LogConsumer(mockHttpClient, logQueue, new AtomicLong());
        LogConsumer spyLogConsumer = spy(workerThread);
        spyLogConsumer.start();
        LOG.info("Started LogConsumer: " + logQueue.toString());
        LOG.info(spyLogConsumer.toString());

        // As LogConsumer runs, take EventRecord off of queue
        // wait for this
        TimeUnit.SECONDS.sleep(5);
        LOG.info("Paused for LogConsumer: " + logQueue.toString());
        LOG.info(spyLogConsumer.toString());

        // Assert that handleRetry was called
        verify(spyLogConsumer).handleRetry(any(IOException.class), any(EventRecord.class));

        // Tear down
        spyLogConsumer.stopTask();
    }
}
