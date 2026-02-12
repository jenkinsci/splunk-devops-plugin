package com.splunk.splunkjenkins.links;


import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import hudson.model.Action;

import com.splunk.splunkjenkins.Messages;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;

/**
 * Action that provides Splunk deep links for Jenkins objects.
 */
public class LinkSplunkAction implements Action {
    String query;
    String page;
    String displayName;

    /**
     * Creates a new LinkSplunkAction for deep linking to Splunk
     *
     * @param tab the Splunk page/tab
     * @param query the query string
     * @param displayName the display name for the action
     */
    public LinkSplunkAction(String tab, String query, String displayName) {
        this.query = query;
        this.page = tab;
        this.displayName = displayName;
    }

    /** {@inheritDoc} */
    @Override
    public String getIconFileName() {
        if (Jenkins.get().hasPermission(ReportAction.SPLUNK_LINK)) {
            return Messages.SplunkIconName();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        if (Jenkins.get().hasPermission(ReportAction.SPLUNK_LINK)) {
            return displayName;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUrlName() {
        SplunkJenkinsInstallation instance = SplunkJenkinsInstallation.get();
        if (StringUtils.isNotEmpty(query)) {
            return instance.getAppUrlOrHelp() + page + "?" + query;
        } else {
            return instance.getAppUrlOrHelp() + page;
        }
    }
}
