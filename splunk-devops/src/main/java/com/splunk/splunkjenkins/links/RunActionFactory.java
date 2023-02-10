package com.splunk.splunkjenkins.links;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.utils.LogEventHelper;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings("unused")
@Extension
public class RunActionFactory extends TransientActionFactory<Run> {
    @Override
    public Class<Run> type() {
        return Run.class;
    }

    @NonNull
    @Override
    public Collection<? extends Action> createFor(@NonNull Run target) {
        Job job = target.getParent();
        LogEventHelper.UrlQueryBuilder builder = new LogEventHelper.UrlQueryBuilder()
                .putIfAbsent("job", job.getFullName())
                .putIfAbsent("build", target.getNumber() + "");
        File junitFile = new File(target.getRootDir(), "junitResult.xml");
        if (junitFile.exists() || job.getClass().getName().startsWith("hudson.maven.")) {
            // test page is using master query param instead of host
            builder.putIfAbsent("master", SplunkJenkinsInstallation.get().getMetadataHost());
            String query = builder.build();
            return Collections.singleton(new LinkSplunkAction("testAnalysis", query, "Splunk"));
        }
        String query = builder.putIfAbsent("type", "build")
                .putIfAbsent("host", SplunkJenkinsInstallation.get().getMetadataHost()).build();
        return Collections.singleton(new LinkSplunkAction("build", query, "Splunk"));
    }
}
