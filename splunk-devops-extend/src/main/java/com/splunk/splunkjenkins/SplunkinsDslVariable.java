package com.splunk.splunkjenkins;


import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.utils.LogEventHelper.getBuildVariables;

@Extension(optional = true)
public class SplunkinsDslVariable extends GlobalVariable {
    @NonNull
    @Override
    public String getName() {
        return "splunkins";
    }

    @NonNull
    @Override
    public Object getValue(@NonNull CpsScript script) throws Exception {
        Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("cannot find associated build");
        }
        // try to access WorkflowRun.listener
        TaskListener listener = TaskListener.NULL;
        try {
            Field field = WorkflowRun.class.getDeclaredField("listener");
            field.setAccessible(true);
            listener = (TaskListener) field.get(build);
        } catch (Exception e) {
            listener = new LogTaskListener(Logger.getLogger(SplunkinsDslVariable.class.getName()), Level.INFO);
        }
        Map buildParameters = getBuildVariables(build);
        RunDelegate delegate = new RunDelegate(build, buildParameters, listener);
        return delegate;
    }
}
