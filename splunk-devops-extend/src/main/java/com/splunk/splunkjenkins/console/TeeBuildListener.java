package com.splunk.splunkjenkins.console;

import com.splunk.splunkjenkins.TeeConsoleLogFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.CloseProofOutputStream;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.remoting.RemoteOutputStream;
import org.jenkinsci.remoting.SerializableOnlyOverRemoting;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/*
Helper class that delegates BuildListener functionality to underlying TaskListener
 */
public class TeeBuildListener implements BuildListener, Closeable, SerializableOnlyOverRemoting {
    private final transient OutputStream out;
    @SuppressFBWarnings(
            value = {"SE_BAD_FIELD"},
            justification = "using Replacement anyway, fields here are irrelevant"
    )
    private final PrintStream ps;
    public String source;

    TeeBuildListener(OutputStream out, String source) throws IOException {
        this.out = new TeeConsoleLogFilter.TeeOutputStream(out, source);
        this.ps = new PrintStream(out, false, "UTF-8");
        this.source = source;
    }

    TeeBuildListener(@Nonnull TaskListener actualListener, String source) throws IOException {
        this.out = new TeeConsoleLogFilter.TeeOutputStream(actualListener.getLogger(), source);
        this.ps = new PrintStream(out, false, "UTF-8");
        this.source = source;
    }

    public PrintStream getLogger() {
        return this.ps;
    }

    public void close() throws IOException {
        this.ps.close();
    }

    private Object writeReplace() {
        return new TeeBuildListener.Replacement(this);
    }

    private static final class Replacement implements SerializableOnlyOverRemoting {
        private static final long serialVersionUID = 1L;
        private final RemoteOutputStream ros;
        private final String source;

        Replacement(TeeBuildListener tbl) {
            this.ros = new RemoteOutputStream(new CloseProofOutputStream(tbl.out));
            this.source = tbl.source;
        }

        private Object readResolve() throws IOException {
            return new TeeBuildListener(new FilterOutputStream(this.ros), this.source);
        }
    }
}
