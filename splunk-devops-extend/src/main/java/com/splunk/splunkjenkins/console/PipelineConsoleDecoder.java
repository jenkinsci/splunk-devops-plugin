package com.splunk.splunkjenkins.console;

import com.splunk.splunkjenkins.Constants;
import com.splunk.splunkjenkins.utils.PlainTextConsoleUtils;
import hudson.console.ConsoleNote;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.utils.PlainTextConsoleUtils.arrayIndexOf;
import static java.util.logging.Level.WARNING;

public class PipelineConsoleDecoder implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PipelineConsoleDecoder.class.getName());
    private transient WorkflowRun run;
    private transient LabelMarkupText markupText = new LabelMarkupText();
    private boolean parseLabelFlag = Constants.DECODE_PIPELINE_CONSOLE;

    public PipelineConsoleDecoder(WorkflowRun run) {
        this.run = run;
        if (run == null) {
            parseLabelFlag = false;
        }
    }

    @CheckForNull
    public String decodeLine(byte[] in, int length) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            if (parseLabelFlag) {
                decodeConsoleObjectStream(in, length, bout);
            } else {
                PlainTextConsoleUtils.decodeConsole(in, length, bout);
            }
            return bout.toString("UTF-8");
        } catch (IOException ex) {
            LOG.log(WARNING, "failed to decode log" + ex);
            return null;
        }
    }

    private void decodeConsoleObjectStream(byte[] in, int length, ByteArrayOutputStream out) throws IOException {
        int next = arrayIndexOf(in, 0, length, ConsoleNote.PREAMBLE);
        // perform byte[]->char[] while figuring out the char positions of the BLOBs
        int written = 0;
        while (next >= 0) {
            if (next > written) {
                out.write(in, written, next - written);
                written = next;
            }
            int rest = length - next;
            ByteArrayInputStream b = new ByteArrayInputStream(in, next, rest);
            try {
                ConsoleNote consoleNote = ConsoleNote.readFrom(new DataInputStream(b));
                consoleNote.annotate(run, markupText, 0);
                markupText.write(out);
            } catch (IOException | ClassNotFoundException ex) {
                LOG.log(WARNING, "failed to decode console note", ex);
            }
            int bytesUsed = rest - b.available(); // bytes consumed by annotations
            written += bytesUsed;
            next = arrayIndexOf(in, written, length, ConsoleNote.PREAMBLE);
        }
        if (length - written > 0) {
            markupText.writePreviousLabel(out);
            // finish the remaining bytes->chars conversion
            out.write(in, written, length - written);
        }
    }

}
