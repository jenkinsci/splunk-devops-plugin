package com.splunk.splunkjenkins.console;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.CloseProofOutputStream;
import hudson.model.BuildListener;
import hudson.remoting.RemoteOutputStream;
import org.jenkinsci.remoting.SerializableOnlyOverRemoting;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/*
Code copied over from
https://github.com/jenkinsci/workflow-api-plugin/blob/master/src/main/java/org/jenkinsci/plugins/workflow/log/BufferedBuildListener.java
 */

final class BufferedBuildListener implements BuildListener, Closeable, SerializableOnlyOverRemoting {
    private final OutputStream out;
    @SuppressFBWarnings(
            value = {"SE_BAD_FIELD"},
            justification = "using Replacement anyway, fields here are irrelevant"
    )
    private final PrintStream ps;

    BufferedBuildListener(OutputStream out) throws IOException {
        this.out = out;
        this.ps = new PrintStream(out, false, "UTF-8");
    }

    public PrintStream getLogger() {
        return this.ps;
    }

    public void close() throws IOException {
        this.ps.close();
    }

    private Object writeReplace() {
        return new BufferedBuildListener.Replacement(this);
    }

    private static final class Replacement implements SerializableOnlyOverRemoting {
        private static final long serialVersionUID = 1L;
        private final RemoteOutputStream ros;
        private final DelayBufferedOutputStream.Tuning tuning;

        Replacement(BufferedBuildListener cbl) {
            this.tuning = DelayBufferedOutputStream.Tuning.DEFAULT;
            this.ros = new RemoteOutputStream(new CloseProofOutputStream(cbl.out));
        }

        private Object readResolve() throws IOException {
            return new BufferedBuildListener(new GCFlushedOutputStream(new DelayBufferedOutputStream(this.ros, this.tuning)));
        }
    }
}
