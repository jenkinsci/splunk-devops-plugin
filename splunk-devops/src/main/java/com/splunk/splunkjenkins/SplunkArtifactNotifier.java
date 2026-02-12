package com.splunk.splunkjenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.utils.LogEventHelper.parseFileSize;
import static com.splunk.splunkjenkins.utils.LogEventHelper.sendFiles;

/**
 * Jenkins build step that sends build artifacts to Splunk.
 * Allows configurable file patterns, size limits, and publishing options.
 */
@SuppressWarnings("unused")
public class SplunkArtifactNotifier extends Notifier implements SimpleBuildStep {
    /**
     * {@link org.apache.tools.ant.types.FileSet} "includes" string, like "foo/bar/*.xml"
     */
    private final String includeFiles;
    private final String excludeFiles;
    private final boolean publishFromSlave;
    private final boolean skipGlobalSplunkArchive;
    private final String sizeLimit;

    /**
     * Constructs a SplunkArtifactNotifier with file patterns and configuration options
     *
     * @param includeFiles the file pattern for files to include (Ant-style glob)
     * @param excludeFiles the file pattern for files to exclude (Ant-style glob)
     * @param publishFromSlave whether to publish files from slave nodes
     * @param skipGlobalSplunkArchive whether to skip global Splunk archive settings
     * @param sizeLimit the maximum file size limit (e.g., "10MB", "5GB")
     */
    @DataBoundConstructor
    public SplunkArtifactNotifier(String includeFiles, String excludeFiles, boolean publishFromSlave,
                                  boolean skipGlobalSplunkArchive, String sizeLimit) {
        this.includeFiles = includeFiles;
        this.excludeFiles = excludeFiles;
        this.publishFromSlave = publishFromSlave;
        this.skipGlobalSplunkArchive = skipGlobalSplunkArchive;
        this.sizeLimit=sizeLimit;
    }

    /** {@inheritDoc} */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * {@inheritDoc}
     *
     * Main execution method that sends build artifacts to Splunk based on configured
     * file patterns and size limits. The method resolves file patterns using Ant-style
     * glob syntax, respects include/exclude filters, and sends matching files to Splunk
     * via the HTTP Event Collector.
     */
    @Override
    public void perform(@NonNull Run<?, ?> build, @NonNull FilePath workspace,
                           @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        Map<String, String> envVars = new HashMap<>();
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception ex) {
            listener.getLogger().println("failed to get env");
        }
        long maxFileSize=parseFileSize(sizeLimit);
        listener.getLogger().println("sending files at job level, includes:" + includeFiles + " excludes:" + excludeFiles);
        int eventCount = sendFiles(build, workspace, envVars, listener, includeFiles, excludeFiles, publishFromSlave, maxFileSize);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE,"sent "+eventCount+" events with file size limit "+maxFileSize);
    }

    /**
     * Descriptor for SplunkArtifactNotifier that provides UI labels and applicability
     */
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return Messages.SplunArtifactArchive();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "SplunkArtifactNotifier{" +
                "includeFiles='" + includeFiles + '\'' +
                ", excludeFiles='" + excludeFiles + '\'' +
                ", publishFromSlave=" + publishFromSlave +
                ", skipGlobalSplunkArchive=" + skipGlobalSplunkArchive +
                ", sizeLimit='" + sizeLimit + '\'' +
                '}';
    }

    /**
     * Gets the file pattern for files to include
     *
     * @return the include files pattern
     */
    public String getIncludeFiles() {
        return includeFiles;
    }

    /**
     * Gets the file pattern for files to exclude
     *
     * @return the exclude files pattern
     */
    public String getExcludeFiles() {
        return excludeFiles;
    }

    /**
     * Checks whether to publish files from slave nodes
     *
     * @return true if publishing from slave nodes
     */
    public boolean isPublishFromSlave() {
        return publishFromSlave;
    }

    /**
     * Checks whether to skip global Splunk archive settings
     *
     * @return true if skipping global archive settings
     */
    public boolean isSkipGlobalSplunkArchive() {
        return skipGlobalSplunkArchive;
    }

    /**
     * Gets the maximum file size limit
     *
     * @return the size limit (e.g., "10MB", "5GB")
     */
    public String getSizeLimit() {
        return sizeLimit;
    }
}
