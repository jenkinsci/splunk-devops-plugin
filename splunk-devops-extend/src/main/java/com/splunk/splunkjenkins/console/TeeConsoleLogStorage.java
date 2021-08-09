package com.splunk.splunkjenkins.console;

import hudson.console.AnnotatedLargeText;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.log.LogStorage;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TeeConsoleLogStorage implements LogStorage {
    private static final Logger LOGGER = Logger.getLogger(TeeConsoleLogStorage.class.getName());

    private static final Map<LogStorage, TeeConsoleLogStorage> openStorages = Collections.synchronizedMap(new HashMap<>());

    final LogStorage logStorage;
    String source;

    public static synchronized TeeConsoleLogStorage forFile(LogStorage fileLogStorage) {
        return openStorages.computeIfAbsent(fileLogStorage, TeeConsoleLogStorage::new);
    }

    public TeeConsoleLogStorage(LogStorage logstorage) {
        this.logStorage = logstorage;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    // TODO
    @Override
    public BuildListener overallListener() throws IOException, InterruptedException {
        return new TeeBuildListener(logStorage.overallListener(), source);
    }

    // TODO
    @Override
    public TaskListener nodeListener(FlowNode node) throws IOException, InterruptedException {
        return new TeeBuildListener(logStorage.nodeListener(node), source);
    }

    @Override
    public AnnotatedLargeText<FlowExecutionOwner.Executable> overallLog(FlowExecutionOwner.Executable build, boolean complete) {
        return logStorage.overallLog(build, complete);
    }

    @Override
    public AnnotatedLargeText<FlowNode> stepLog(FlowNode node, boolean complete) {
        return logStorage.stepLog(node, complete);
    }


}

