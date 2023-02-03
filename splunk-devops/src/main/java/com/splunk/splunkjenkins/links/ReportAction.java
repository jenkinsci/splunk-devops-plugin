package com.splunk.splunkjenkins.links;

import com.splunk.splunkjenkins.Messages;
import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import jenkins.model.Jenkins;

@SuppressWarnings("unused")
@Extension
public class ReportAction implements RootAction {

    /**
     * Permission group for Splunk Link related permissions.
     */
    public static final PermissionGroup PERMISSIONS =
            new PermissionGroup(ReportAction.class, Messages._PermissionGroup());
    /**
     * Permission to get the Splunk link displayed.
     */
    public static final Permission SPLUNK_LINK = new Permission(PERMISSIONS,
            "SplunkLink", Messages._PluginViewPermission_Description(), Jenkins.ADMINISTER, PermissionScope.JENKINS);


    @Override
    public String getIconFileName() {
        if (Jenkins.getInstance().hasPermission(ReportAction.SPLUNK_LINK)){
            return Messages.SplunkIconName();
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        if (Jenkins.getInstance().hasPermission(ReportAction.SPLUNK_LINK)){
            return "Splunk";
        }
        return null;
    }

    @Override
    public String getUrlName() {
        if (Jenkins.getInstance().hasPermission(ReportAction.SPLUNK_LINK)){
            SplunkJenkinsInstallation instance = SplunkJenkinsInstallation.get();
            return instance.getAppUrlOrHelp() + "overview?overview_jenkinsmaster=" + instance.getMetadataHost();
        }
        return null;
    }
}
