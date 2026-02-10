package com.splunk.splunkjenkins;

import hudson.Extension;
import jenkins.model.OptionalJobProperty;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Job property that enables pipeline diagram visualization for Splunk integration.
 *
 * <p>This property attaches to {@link WorkflowJob} instances and allows users to opt-in
 * to sending pipeline stage diagrams to Splunk. When enabled, the plugin generates
 * GraphViz diagrams that visualize the pipeline structure and execution flow.</p>
 *
 * <h2>Configuration</h2>
 * <p>This property is configured via the job configuration page under:</p>
 * <pre>General → Opt in data sent to Splunk → "Enable diagram"</pre>
 * Users can enable/disable diagram generation on a per-job basis.
 *
 * <h2>Usage in Pipeline</h2>
 * <pre>{@code
 * options {
 *     splunkinsJobOption(enableDiagram: true)
 * }
 * }</pre>
 *
 * <h2>Diagram Generation</h2>
 * <p>When enabled, this property triggers diagram generation in:</p>
 * <ul>
 *     <li>{@link PipelineGraphVizSupport} - Creates GraphViz .dot files</li>
 *     <li>Diagrams are sent to Splunk during pipeline completion</li>
 * </ul>
 *
 * @see OptionalJobProperty
 * @see WorkflowJob
 * @since 1.11.0
 */
@SuppressWarnings("rawtypes")
public class SplunkPipelineJobProperty extends OptionalJobProperty<WorkflowJob> {

    /**
     * Whether diagram visualization is enabled for this job.
     * Null values are treated as false (disabled).
     */
    @CheckForNull
    private Boolean enableDiagram;

    /**
     * Creates a new job property with diagram support disabled by default.
     *
     * <p>The {@link DataBoundConstructor} annotation enables Jenkins to automatically
     * bind configuration data from job configuration forms to this object.</p>
     */
    @DataBoundConstructor
    public SplunkPipelineJobProperty() {
    }

    /**
     * Gets whether diagram visualization is enabled for this job.
     *
     * @return {@link Boolean#TRUE} if diagram is enabled, {@link Boolean#FALSE} if explicitly
     *         disabled, or {@code null} if not configured (treated as disabled)
     * @see #isDiagramEnabled()
     */
    @CheckForNull
    public Boolean getEnableDiagram() {
        return enableDiagram;
    }

    /**
     * Sets whether diagram visualization is enabled for this job.
     *
     * @param enableDiagram {@link Boolean#TRUE} to enable diagram generation,
     *                      {@link Boolean#FALSE} to explicitly disable,
     *                      or {@code null} to reset to default (disabled)
     * @see #isDiagramEnabled()
     */
    @DataBoundSetter
    public void setEnableDiagram(Boolean enableDiagram) {
        this.enableDiagram = enableDiagram;
    }

    /**
     * Checks whether diagram visualization is enabled for this job.
     *
     * <p>This method safely handles null values by returning {@code false} when
     * {@code enableDiagram} is null or explicitly set to {@link Boolean#FALSE}.</p>
     *
     * @return {@code true} if diagram is enabled, {@code false} otherwise
     */
    public boolean isDiagramEnabled() {
        return Boolean.TRUE.equals(enableDiagram);
    }

    /**
     * Returns a string representation of this job property.
     *
     * <p>Primarily used for debugging and logging purposes. The string includes the
     * current value of the {@code enableDiagram} field.</p>
     *
     * @return a formatted string representation
     */
    @Override
    public String toString() {
        return String.format("SplunkPipelineJobProperty{enableDiagram=%s}", enableDiagram);
    }

    /**
     * Descriptor for {@link SplunkPipelineJobProperty} that defines its metadata.
     *
     * <p>This descriptor is required by Jenkins' extension system to provide:</p>
     * <ul>
     *     <li>Display name shown in the job configuration UI</li>
     *     <li>Configuration page handling via Stapler</li>
     *     <li>Data binding between form fields and property</li>
     * </ul>
     *
     * <h2>Symbol Usage</h2>
     * <p>The {@code @Symbol("splunkinsJobOption")} annotation allows this property to be
     * referenced in pipeline scripts using the symbol name:</p>
     * <pre>{@code
     * options {
     *     splunkinsJobOption(enableDiagram: true)
     * }
     * }</pre>
     *
     * @see OptionalJobPropertyDescriptor
     * @see Symbol
     */
    @Extension
    @Symbol("splunkinsJobOption")
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {

        /**
         * Returns the display name shown in the Jenkins job configuration UI.
         *
         * @return "Opt in data sent to Splunk"
         */
        @Override
        public String getDisplayName() {
            return "Opt in data sent to Splunk";
        }
    }
}
