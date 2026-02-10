package com.splunk.splunkjenkins;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;

import static com.splunk.splunkjenkins.utils.LogEventHelper.parseFileSize;
import static com.splunk.splunkjenkins.utils.LogEventHelper.sendFiles;

/**
 * Pipeline step that sends build artifacts and log files to Splunk.
 *
 * <p>This step searches for files in the workspace that match specified patterns and sends them
 * to Splunk as document events. Files can be filtered by inclusion/exclusion patterns,
 * size limits, and can optionally be published from remote agent/slave nodes.</p>
 *
 * <h2>Pipeline Syntax</h2>
 * <pre>{@code
 * // Declarative Pipeline
 * pipeline {
 *     agent any
 *     stages {
 *         stage('Build') {
 *             steps {
 *                 sh 'mvn clean package > build.log'
 *             }
 *         }
 *         stage('Upload Logs') {
 *             steps {
 *                 sendSplunkFile(
 *                     includes: '*@/*.log',
 *                     excludes: '*@/test-*.log',
 *                     sizeLimit: '10MB',
 *                     publishFromSlave: false
 *                 )
 *             }
 *         }
 *     }
 * }
 *
 * // Scripted Pipeline
 * node {
 *     stage('Test') {
 *         sh './run-tests.sh > test-results.log'
 *         sendSplunkFile(
 *             includes: 'test-results.log',
 *             sizeLimit: '5MB'
 *         )
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration Parameters</h2>
 * <ul>
 *     <li><b>includes</b> (required) - Ant-style file pattern to match files to send (e.g., "**@/*.log")</li>
 *     <li><b>excludes</b> (optional) - Ant-style pattern to exclude specific files</li>
 *     <li><b>sizeLimit</b> (optional) - Maximum file size to send (e.g., "10MB", "1GB")</li>
 *     <li><b>publishFromSlave</b> (optional) - Whether to allow sending from remote agents (default: false)</li>
 * </ul>
 *
 * <h2>File Search Behavior</h2>
 * <p>The step searches the workspace recursively for files matching the {@code includes}
 * pattern. Files matching the {@code excludes} pattern are filtered out. Each file is sent
 * as a separate event to Splunk with metadata about the build. Files larger than
 * {@code sizeLimit} are skipped to prevent overwhelming Splunk.</p>
 *
 * <h2>Slave/Agent Publishing</h2>
 * <p>When {@code publishFromSlave} is false (default), files are only sent from the Jenkins
 * controller. When set to true, files can be sent directly from remote agents. Enabling this
 * requires:</p>
 * <ul>
 *     <li>Agent's workspace must be network-accessible</li>
 *     <li>Agent must have valid Splunk credentials</li>
 *     <li>Additional security considerations</li>
 * </ul>
 *
 * <h2>Size Limit Format</h2>
 * <p>The {@code sizeLimit} parameter accepts formats like:</p>
 * <ul>
 *     <li>"10KiB" or "10KB" - kilobytes</li>
 *     <li>"5MB"  - megabytes</li>
 *     <li>"1GB"  - gigabytes</li>
 *     <li>"1024" - bytes</li>
 * </ul>
 * If not specified or if parsing fails, all files are sent regardless of size.
 *
 * <h2>Required Pipeline Context</h2>
 * <p>This step requires:</p>
 * <ul>
 *     <li>{@link Run} - Current build</li>
 *     <li>{@link TaskListener} - For logging</li>
 *     <li>{@link FilePath} - Workspace to search</li>
 *     <li>{@link EnvVars} - Environment variables</li>
 * </ul>
 *
 * <h3>File Transmission</h3>
 * <p>Files are sent via {@link com.splunk.splunkjenkins.utils.LogEventHelper#sendFiles} which handles:</p>
 * <ul>
 *     <li>Reading file contents</li>
 *     <li>Adding build metadata</li>
 *     <li>Batching for network efficiency</li>
 *     <li>Error handling and retry logic</li>
 * </ul>
 *
 * The step ignores Splunk when globally disabled ({@link SplunkJenkinsInstallation#isEnabled()}).
 * This allows pipelines to include the step without conditional logic, making it safe to
 * commit these steps to version control even in environments where Splunk is not configured.
 *
 * @see com.splunk.splunkjenkins.utils.LogEventHelper#sendFiles
 * @see com.splunk.splunkjenkins.utils.LogEventHelper#parseFileSize
 * @since 1.0.0
 */
public class SplunkLogFileStep extends Step {

    /**
     * Ant-style pattern to match files to send (e.g., "**@/*.log").
     * This is the required parameter that defines which files to upload.
     */
    //required fields
    String includes;

    /**
     * Maximum file size limit (e.g., "10MB", "1GB").
     * Files larger than this limit are skipped.
     */
    @DataBoundSetter
    String sizeLimit;

    /**
     * Ant-style pattern to exclude specific files from upload.
     * Applied after includes pattern matching.
     */
    @DataBoundSetter
    String excludes;

    /**
     * Whether to enable sending files from remote Jenkins agents/slaves.
     * When false, only files from controller workspace are sent.
     */
    @DataBoundSetter
    boolean publishFromSlave;

    @DataBoundConstructor
    public SplunkLogFileStep(@NonNull String includes) {
        this.includes = includes;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SplunkLogFileStepExecution(context, this);
    }

    public String getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    public boolean isPublishFromSlave() {
        return publishFromSlave;
    }

    public void setPublishFromSlave(boolean publishFromSlave) {
        this.publishFromSlave = publishFromSlave;
    }

    public String getSizeLimit() {
        return sizeLimit;
    }

    public void setSizeLimit(String sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class, FilePath.class, EnvVars.class);
        }

        @Override
        public String getFunctionName() {
            return "sendSplunkFile";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Send files to Splunk";
        }
    }

    public static class SplunkLogFileStepExecution extends SynchronousNonBlockingStepExecution<Void> {
        protected SplunkLogFileStepExecution(StepContext context, SplunkLogFileStep step) throws Exception {
            super(context);
            this.step = step;
        }

        private static final long serialVersionUID = 1152009261375345133L;
        private transient SplunkLogFileStep step;

        @Override
        protected Void run() throws Exception {
            if (!SplunkJenkinsInstallation.get().isEnabled()) {
                return null;
            }
            TaskListener listener = getContext().get(TaskListener.class);
            FilePath workspace = getContext().get(FilePath.class);
            Run build =  getContext().get(Run.class);
            EnvVars envVars =  getContext().get(EnvVars.class);
            sendFiles(build, workspace, envVars, listener,
                    step.includes, step.excludes, step.publishFromSlave, parseFileSize(step.sizeLimit));
            return null;
        }
    }
}
