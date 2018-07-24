package com.splunk.splunkjenkins.utils;

import shaded.splk.org.apache.http.HttpEntity;
import shaded.splk.org.apache.http.HttpResponse;
import shaded.splk.org.apache.http.client.ResponseHandler;
import shaded.splk.org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.utils.LogEventHelper.buildPost;

public class LogConsumerHandler implements ResponseHandler{
    private static final Logger LOG = Logger.getLogger(LogConsumerHandler.class.getName());
    private AtomicLong outgoingCounter;
    private long errorCount;

    public LogConsumerHandler(long errorCount, AtomicLong counter) {
        this.errorCount = errorCount;
        this.outgoingCounter = counter;
    }

    @Override
    public String handleResponse(final HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        LOG.info("LogConsumerHandler | STATUS: " + status);
        String reason = response.getStatusLine().getReasonPhrase();

        if (status == 200) {
            outgoingCounter.incrementAndGet();
            HttpEntity entity = response.getEntity();
            //need consume entity so underlying connection can be released to pool
            return entity != null ? EntityUtils.toString(entity) : null;
        } else {
            errorCount++;
            //see also http://docs.splunk.com/Documentation/Splunk/6.3.0/RESTREF/RESTinput#services.2Fcollector

            // SplunkServiceError
            if (status == 503) {
                throw new SplunkServiceError("Server is busy, maybe caused by blocked queue, please check " +
                        "https://wiki.splunk.com/Community:TroubleshootingBlockedQueues", status);
            }
            if (status == 502) {
                throw new SplunkServiceError("Bad gateway, target may have closed the connection", status);
            }

            // SplunkClientError
            String message;
            if (status == 403 || status == 401) {
                //Token disabled or Invalid authorization
                message = reason + ", http event collector token is invalid";
            } else if (status == 400) {
                //Invalid data format or incorrect index
                message = reason + ", incorrect index or invalid data format";
            } else {
                message = reason;
            }
            throw new SplunkClientError(message, status);
        }
    }

    public long getErrorCount(){
        return errorCount;
    }
}
