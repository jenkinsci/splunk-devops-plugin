package com.splunk.splunkjenkins.links;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.utils.LogEventHelper;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.model.TransientComputerActionFactory;

import java.util.Collections;
import java.util.Collection;

/**
 * Factory that adds Splunk links to Jenkins agent/computer pages.
 */
@Extension
public class ComputerLogActionFactory extends TransientComputerActionFactory {
    /**
     * {@inheritDoc}
     *
     * Creates Splunk link actions for a Jenkins computer/agent.
     * <p>
     * Generates a link to Splunk for agent/computer logs and monitoring.
     */
    @Override
    public Collection<? extends Action> createFor(Computer target) {
        String query = new LogEventHelper.UrlQueryBuilder()
                .putIfAbsent("master", SplunkJenkinsInstallation.get().getMetadataHost())
                .putIfAbsent("slave", target.getName())
                .build();
        return Collections.singleton(new LinkSplunkAction("jenkins_slave", query, "Splunk"));
    }
}
