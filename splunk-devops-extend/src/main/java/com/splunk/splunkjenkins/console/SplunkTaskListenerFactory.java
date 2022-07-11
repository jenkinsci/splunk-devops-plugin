package com.splunk.splunkjenkins.console;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import hudson.Extension;
import hudson.model.Queue;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.model.EventType.CONSOLE_LOG;

@Extension(optional = true)
public class SplunkTaskListenerFactory implements TaskListenerDecorator.Factory {
    private static final Logger LOGGER = Logger.getLogger(SplunkConsoleTaskListenerDecorator.class.getName());
    private static final boolean ENABLE_REMOTE_DECORATOR = Boolean.parseBoolean(System.getProperty("splunkins.enableRemoteTaskListenerDecorator", "true"));
    private static final transient LoadingCache<WorkflowRun, SplunkConsoleTaskListenerDecorator> cachedDecorator = CacheBuilder.newBuilder()
            .weakKeys()
            .maximumSize(1024)
            .build(new CacheLoader<WorkflowRun, SplunkConsoleTaskListenerDecorator>() {
                @Override
                public SplunkConsoleTaskListenerDecorator load(WorkflowRun key) {
                    SplunkConsoleTaskListenerDecorator decorator = new SplunkConsoleTaskListenerDecorator(key);
                    if (ENABLE_REMOTE_DECORATOR) {
                        decorator.setRemoteSplunkinsConfig(SplunkJenkinsInstallation.get().toMap());
                    }
                    return decorator;
                }
            });

    @Override
    /*
       data stream is passed to splunk decorator first (it sees data in the last due to decorator behavior)
     */
    public boolean isAppliedBeforeMainDecorator() {
        return true;
    }

    @CheckForNull
    @Override
    public TaskListenerDecorator of(@NonNull FlowExecutionOwner flowExecutionOwner) {
        if (!SplunkJenkinsInstallation.get().isPipelineFilterEnabled()) {
            return null;
        }
        if (SplunkJenkinsInstallation.get().isEventDisabled(CONSOLE_LOG)) {
            return null;
        }
        try {
            Queue.Executable executable = flowExecutionOwner.getExecutable();
            if (executable instanceof WorkflowRun) {
                WorkflowRun run = (WorkflowRun) executable;
                if (SplunkJenkinsInstallation.get().isJobIgnored(run.getUrl())) {
                    return null;
                }
                return cachedDecorator.get(run);
            }
        } catch (IOException x) {
            LOGGER.log(Level.WARNING, null, x);
        } catch (ExecutionException e) {
            LOGGER.finer("failed to load cached decorator");
        }
        return null;
    }

    public static void removeCache(WorkflowRun run) {
        cachedDecorator.invalidate(run);
    }
}
