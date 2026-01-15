package com.splunk.splunkjenkins.links;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.utils.LogEventHelper;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Extension
public class RunActionFactory extends TransientActionFactory<Run> {
    @Override
    public Class<Run> type() {
        return Run.class;
    }

    @Override
    public Class<? extends Action> actionType() {
        return LinkSplunkAction.class;
    }

    @NonNull
    @Override
    public Collection<? extends Action> createFor(@NonNull Run target) {
        Job job = target.getParent();
        LogEventHelper.UrlQueryBuilder builder = new LogEventHelper.UrlQueryBuilder()
                .putIfAbsent("job", job.getFullName())
                .putIfAbsent("build", target.getNumber() + "");
        if (job.getClass().getName().startsWith("hudson.maven.") || hasTestAction(target)) {
            // test page is using master query param instead of host
            builder.putIfAbsent("master", SplunkJenkinsInstallation.get().getMetadataHost());
            String query = builder.build();
            return Collections.singleton(new LinkSplunkAction("testAnalysis", query, "Splunk"));
        }
        String query = builder.putIfAbsent("type", "build")
                .putIfAbsent("host", SplunkJenkinsInstallation.get().getMetadataHost()).build();
        return Collections.singleton(new LinkSplunkAction("build", query, "Splunk"));
    }

    private boolean hasTestAction(Run run) {
        // can not use run.getAllActions which triggers createFor and causes infinite loop.
        // has to use Deprecated method run.getActions
        List<? extends Action> actionList = run.getActions();
        for (Action action : actionList) {
            String actionName = action.getClass().getName();
            // can be junit AggregatedTestResultAction or TestResultAction or TestNGTestResultBuildAction
            if ("hudson.tasks.test.AggregatedTestResultAction".equals(actionName) || "hudson.tasks.junit.TestResultAction".equals(actionName)
                    || "hudson.plugins.testng.TestNGTestResultBuildAction".equals(actionName)) {
                return true;
            }
        }
        return false;
    }
}
