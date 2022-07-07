package com.splunk.splunkjenkins;

import hudson.Extension;
import jenkins.model.OptionalJobProperty;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.CheckForNull;

@SuppressWarnings("rawtypes")
public class SplunkPipelineJobProperty extends OptionalJobProperty<WorkflowJob> {
    Boolean enableDiagram;

    @DataBoundConstructor
    public SplunkPipelineJobProperty() {
    }

    @CheckForNull
    public Boolean getEnableDiagram() {
        return enableDiagram;
    }

    @DataBoundSetter
    public void setEnableDiagram(Boolean enableDiagram) {
        this.enableDiagram = enableDiagram;
    }

    public boolean isDiagramEnabled() {
        return Boolean.TRUE.equals(enableDiagram);
    }

    @Override
    public String toString() {
        return String.format("SplunkPipelineJobProperty{enableDiagram=%s}", enableDiagram);
    }

    @Extension
    @Symbol("splunkinsJobOption")
    public static class DescriptorImpl extends OptionalJobPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return "Opt in data sent to Splunk";
        }
    }
}
