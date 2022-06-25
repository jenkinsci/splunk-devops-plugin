package com.splunk.splunkjenkins;

import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.StageChunkFinder;
import org.jenkinsci.plugins.workflow.support.steps.StageStep;
import org.jenkinsci.plugins.workflow.cps.steps.ParallelStep;


import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class SplunkStageChunkFinder extends StageChunkFinder {

    @Override
    public boolean isChunkStart(@Nonnull FlowNode current, @CheckForNull FlowNode previous) {

        if ((current instanceof StepAtomNode || current instanceof StepStartNode)
                && !(((StepNode) current).getDescriptor() instanceof StageStep.DescriptorImpl || ((StepNode) current).getDescriptor() instanceof ParallelStep.DescriptorImpl)) {
            // Faster than looking at actions
            return false;
        } else if (current instanceof BlockEndNode) {
            return false;
        } else if (current instanceof StepStartNode) {
            StepStartNode startNode = (StepStartNode)current;
            if (!((startNode.getDescriptor() instanceof StageStep.DescriptorImpl) || (startNode.getDescriptor() instanceof ParallelStep.DescriptorImpl) )) {
                return false;
            }
            return startNode.getAction(LabelAction.class) != null;
        }
        return current.getAction(StageAction.class) != null;
    }

    /** End is where you have a label marker before it... or  */
    @Override
    public boolean isChunkEnd(@Nonnull FlowNode current, @CheckForNull FlowNode previous) {

        // First a block-scoped stage
        if (current instanceof StepEndNode && ((((StepEndNode) current).getDescriptor() instanceof StageStep.DescriptorImpl))
                || (current instanceof StepEndNode && (((StepEndNode) current).getDescriptor() instanceof ParallelStep.DescriptorImpl))) {
            // We have to look for the labelaction because block-scoped stage creates two nested blocks
            return ((StepEndNode) current).getStartNode().getAction(LabelAction.class) != null;
        }
        // Then a marker-scoped stage
        if (previous != null) {
            return isChunkStart(previous, null);
        }
        return false;
    }
}
