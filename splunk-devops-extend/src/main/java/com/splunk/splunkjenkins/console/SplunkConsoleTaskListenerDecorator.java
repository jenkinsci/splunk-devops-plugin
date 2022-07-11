package com.splunk.splunkjenkins.console;

import com.splunk.splunkjenkins.utils.RemoteUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jenkins.util.JenkinsJVM;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class SplunkConsoleTaskListenerDecorator extends TaskListenerDecorator {
    private static final long serialVersionUID = 1L;
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    transient PipelineConsoleDecoder decoder;
    // it is optional but not use Optional<Map> since Optional is not serializable
    Map remoteSplunkinsConfig = null;
    String source;

    public SplunkConsoleTaskListenerDecorator(WorkflowRun run) {
        this.decoder = new PipelineConsoleDecoder(run);
        this.source = run.getUrl() + "console";
    }

    @NonNull
    @Override
    public OutputStream decorate(@NonNull OutputStream outputStream) throws IOException {
        if (!JenkinsJVM.isJenkinsJVM()) {
            if (remoteSplunkinsConfig != null) {
                RemoteUtils.initSplunkConfigOnAgent(remoteSplunkinsConfig);
            } else {
                // no-op
                return outputStream;
            }
        }
        if (decoder == null) {
            // resume from restart
            decoder = new PipelineConsoleDecoder(null);
        }
        //called for every step 
        return new LabelConsoleLineStream(outputStream, source, decoder);
    }

    protected void setRemoteSplunkinsConfig(Map remoteSplunkinsConfig) {
        this.remoteSplunkinsConfig = remoteSplunkinsConfig;
    }
}
