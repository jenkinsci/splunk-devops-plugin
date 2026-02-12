package com.splunk.splunkjenkins.links;

import com.splunk.splunkjenkins.Messages;
import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import hudson.Extension;
import hudson.model.ManagementLink;

/**
 * Management link for Splunk Jenkins health dashboard.
 * Adds a link to the Jenkins management page for accessing Splunk health reports.
 */
@SuppressWarnings("unused")
@Extension
public class HealthLinkAction extends ManagementLink {
    /** {@inheritDoc} */
    @Override
    public String getIconFileName() {
        return Messages.SplunkIconName();
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "Jenkins Health";
    }

    /** {@inheritDoc} */
    @Override
    public String getUrlName() {
        SplunkJenkinsInstallation instance = SplunkJenkinsInstallation.get();
        return instance.getAppUrlOrHelp() + "jenkins_health?form.hostname=" + instance.getMetadataHost();
    }
}
