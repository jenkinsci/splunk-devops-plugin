package com.splunk.splunkjenkins.utils;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.model.EventRecord;
import shaded.splk.org.apache.http.HttpResponse;
import shaded.splk.org.apache.http.client.HttpClient;
import shaded.splk.org.apache.http.client.methods.HttpPost;
import shaded.splk.org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.utils.LogEventHelper.buildPost;

public class LogConsumer extends Thread {
    private static final Logger LOG = Logger.getLogger(LogConsumer.class.getName());
    private static final int retryInterval = Integer.parseInt(System.getProperty("splunk-retryinterval", "15"));
    private final HttpClient client;
    private final SplunkQueue queue;
    private boolean acceptingTask = true;
    private AtomicLong outgoingCounter;
    private long errorCount;
    private boolean sending = false;
    private final int RETRY_SLEEP_THRESHOLD = 1 << 10;
    private List<Class<? extends IOException>> giveUpExceptions = Arrays.asList(
            UnknownHostException.class,
            SSLException.class,
            SplunkClientError.class);
    private final LogConsumerHandler responseHandler;

    public LogConsumer(HttpClient client, SplunkQueue queue, AtomicLong counter) {
        this.client = client;
        this.queue = queue;
        this.errorCount = 0;
        this.outgoingCounter = counter;
        this.responseHandler = new LogConsumerHandler(this.errorCount, this.outgoingCounter);
    }

    @Override
    public void run() {
        while (acceptingTask) {
            try {
                EventRecord record = queue.take();
                if (!record.isDiscarded()) {
                    HttpPost post = null;
                    try {
                        sending = true;
                        post = buildPost(record, SplunkJenkinsInstallation.get());
                        HttpResponse httpResponse = client.execute(post);
                        responseHandler.handleResponse(httpResponse);
                    } catch (IOException ex) {
                        boolean isDiscarded = false;
                        for (Class<? extends IOException> giveUpException : giveUpExceptions) {
                            if (giveUpException.isInstance(ex)) {
                                isDiscarded = true;
                                LOG.log(Level.SEVERE, "message not delivered:" + record.getShortDescription(), ex);
                                break;
                            }
                        }
                        if (!isDiscarded) {
                            handleRetry(ex, record);
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "failed construct post message" + record.getShortDescription(), e);
                    } finally {
                        sending = false;
                        if (post != null) {
                            post.releaseConnection();
                        }
                    }
                } else {
                    //message discarded
                    LOG.log(Level.SEVERE, "failed to send " + record.getShortDescription());
                }
            } catch (InterruptedException e) {
                errorCount++;
                //thread interrupted, just ignore
            }
        }
    }

    public void handleRetry(IOException ex, EventRecord record) throws InterruptedException {
        if (ex instanceof SplunkServiceError) {
            int sleepTime = 2 * retryInterval;
            LOG.log(Level.WARNING, "{0}, will wait {1} seconds and retry", new Object[]{ex.getMessage(), sleepTime});
            retry(record, sleepTime);
        } else if (ex instanceof ConnectException) {
            // splunk is restarting or network broke
            LOG.log(Level.WARNING, "{0} connect error, will wait {1} seconds and retry", new Object[]{this.getName(), retryInterval});
            retry(record, retryInterval);
        } else {
            //other errors
            LOG.log(Level.WARNING, "will resend the message:{0}", record.getShortDescription());
            retry(record, 1);
        }
    }

    public void stopTask() {
        this.acceptingTask = false;
        for (int i = 0; i < 5; i++) {
            if (sending) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        if (this.isAlive()) {
            //queue.take() may block the thread
            this.interrupt();
        }
    }

    /**
     * @param record
     * @throws InterruptedException
     */
    private void retry(EventRecord record, int sleepIntervalInSeconds) throws InterruptedException {
        if (acceptingTask) {
            record.increase();
            if (queue.size() < RETRY_SLEEP_THRESHOLD) {
                //We don't have much data in queue so wait a while for the service to recovery(hopefully)
                Thread.sleep(sleepIntervalInSeconds * 1000);
            }
            SplunkLogService.getInstance().enqueue(record);
        }
    }

    @Override
    public String toString() {
        return "LogConsumer{ errors=" + errorCount +
                ", name=" + this.getName() + " }";
    }
}
