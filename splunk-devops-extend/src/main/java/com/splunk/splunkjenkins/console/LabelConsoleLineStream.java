package com.splunk.splunkjenkins.console;

import com.splunk.splunkjenkins.model.EventRecord;
import hudson.util.ByteArrayOutputStream2;
import org.apache.commons.lang.StringUtils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.splunk.splunkjenkins.Constants.CONSOLE_TEXT_SINGLE_LINE_MAX_LENGTH;
import static com.splunk.splunkjenkins.model.EventType.CONSOLE_LOG;

public class LabelConsoleLineStream extends FilterOutputStream {
    private static final int RECEIVE_BUFFER_SIZE = 512;
    private static final Logger LOGGER = Logger.getLogger(LabelConsoleLineStream.class.getName());
    public static final Pattern ANSI_COLOR_ESCAPE = Pattern.compile("\u001B\\[[\\d;]+m");
    private ByteArrayOutputStream2 branch = new ByteArrayOutputStream2(RECEIVE_BUFFER_SIZE);
    PipelineConsoleDecoder decoder;
    String source;

    public LabelConsoleLineStream(OutputStream out, String source, PipelineConsoleDecoder decoder) {
        super(out);
        this.decoder = decoder;
        this.source = source;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        if (b == '\n') {
            eol();
        } else {
            //do not need write \n
            branch.write(b);
        }
        if (branch.size() > CONSOLE_TEXT_SINGLE_LINE_MAX_LENGTH) {
            eol();
        }
    }

    protected void eol() {
        String line = decoder.decodeLine(branch.getBuffer(), branch.size());
        if (line == null) {
            // actually line can not be null, always ends with \n, add null check in case decode error
            return;
        }
        // reuse the buffer under normal circumstances
        branch.reset();
        line = ANSI_COLOR_ESCAPE.matcher(line).replaceAll("");
        if (StringUtils.isNotBlank(line)) {
            EventRecord record = new EventRecord(line, CONSOLE_LOG);
            record.setSource(source);
            ConsoleRecordCacheUtils.enqueue(record);
        }
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        ConsoleRecordCacheUtils.flushLog();
        LOGGER.log(Level.FINE, "flush splunk log for " + source);
    }
}
