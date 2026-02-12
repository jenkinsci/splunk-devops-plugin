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

/**
 * Factory that adds Splunk links to buildable Jenkins items.
 */
@SuppressWarnings("unused")
@Extension
public class BuildableItemActionFactory extends TransientActionFactory<BuildableItem> {
    /** {@inheritDoc} */
    @Override
    public Class<BuildableItem> type() {
        return BuildableItem.class;
    }

    /**
     * {@inheritDoc}
     *
     * Creates Splunk link actions for a buildable Jenkins item.
     * <p>
     * Adds a generic Splunk build link to buildable items (jobs, projects, etc.).
     */
    @NonNull
    @Override
    public Collection<? extends Action> createFor(@NonNull BuildableItem target) {
        return Collections.singleton(new LinkSplunkAction("build", "", "Splunk"));
    }
}
