package com.splunk.splunkjenkins.console;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.log.BrokenLogStorage;
import org.jenkinsci.plugins.workflow.log.FileLogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorage;
import org.jenkinsci.plugins.workflow.log.LogStorageFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class TeeConsoleLogStorageFactory implements LogStorageFactory {
    private static final Logger LOGGER = Logger.getLogger(TeeConsoleLogStorageFactory.class.getName());

    @CheckForNull
    @Override
    public LogStorage forBuild(@Nonnull FlowExecutionOwner owner) {
        try {
            Queue.Executable exec = owner.getExecutable();
            if (exec instanceof Run) {
                TeeConsoleLogStorage tcls = TeeConsoleLogStorage.forFile(FileLogStorage.forFile(new File(owner.getRootDir(), "log")));
                tcls.setSource(owner.getUrl() + "console");
                return tcls;
            } else {
                return null;
            }
        } catch (IOException x) {
            return new BrokenLogStorage(x);
        }
    }
}
