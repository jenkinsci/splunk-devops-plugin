package com.splunk.splunkjenkins;

import java.lang.Exception;
import java.io.IOException;

import org.apache.http.HttpStatus;

import shaded.splk.org.apache.http.HttpResponse;
import shaded.splk.org.apache.http.StatusLine;

import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.instanceOf;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import com.splunk.splunkjenkins.utils.LogConsumerHandler;
import com.splunk.splunkjenkins.utils.SplunkServiceError;


public class LogConsumerHandlerTest {
    private static final Logger LOG = Logger.getLogger(LogConsumerHandlerTest.class.getName());

    @Test
    public void test502Error() throws IOException, InterruptedException {
        LOG.info("Running test502Error in LogConsumerHandlerTest");

        // Configure mocked objects
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        StatusLine mockStatusLine = mock(StatusLine.class);

        // Configure mocked responses
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_GATEWAY);

        // Analyze 502 response
        long errorCount = 0;
        AtomicLong counter = new AtomicLong();
        LogConsumerHandler handler = new LogConsumerHandler(errorCount, counter);

        try {
            handler.handleResponse(mockHttpResponse);
        } catch(Exception ex) {
            assertThat(ex, instanceOf(SplunkServiceError.class));
        }
    }
}
