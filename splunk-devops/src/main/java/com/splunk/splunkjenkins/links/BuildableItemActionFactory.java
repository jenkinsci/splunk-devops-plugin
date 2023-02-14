package com.splunk.splunkjenkins.links;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.utils.LogEventHelper;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildableItem;
import jenkins.model.TransientActionFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings("unused")
@Extension
public class BuildableItemActionFactory extends TransientActionFactory<BuildableItem> {
    @Override
    public Class<BuildableItem> type() {
        return BuildableItem.class;
    }

    @NonNull
    @Override
    public Collection<? extends Action> createFor(@NonNull BuildableItem target) {
        return Collections.singleton(new LinkSplunkAction("build", "", "Splunk"));
    }
}
