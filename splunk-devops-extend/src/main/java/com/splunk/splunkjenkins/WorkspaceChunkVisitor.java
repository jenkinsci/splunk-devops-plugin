package com.splunk.splunkjenkins;

import com.cloudbees.workflow.rest.external.ChunkVisitor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Record workspace node during visitation
 * Not thread safe
 */
public class WorkspaceChunkVisitor extends ChunkVisitor {
    private static final Logger LOG = Logger.getLogger(WorkspaceChunkVisitor.class.getName());
    // key is node id, value is jenkins worker node name
    Map<String, String> workspaceNodes = new HashMap<>();
    String execNodeName = null;
    String execNodeStartId = null;

    public WorkspaceChunkVisitor(@NonNull WorkflowRun run) {
        super(run);
    }

    @Override
    public void atomNode(@CheckForNull FlowNode before, @NonNull FlowNode atomNode, @CheckForNull FlowNode after, @NonNull ForkScanner scan) {
        //reverse-order, traverse from end node to start node
        try {
            recordExecNode(atomNode);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "failed to extract pipeline info", ex);
        }
        super.atomNode(before, atomNode, after, scan);
    }

    /**
     * store the jenkins node name where pipeline ran
     *
     * @param atomNode flow  node
     */
    private void recordExecNode(FlowNode atomNode) {
        if (execNodeName == null) {
            StepStartNode nodeStep = getPipelineBlockBoundaryStartNode(atomNode, "node");
            if (nodeStep != null) {
                //WorkspaceAction is recorded in node start
                WorkspaceAction workspaceAction = nodeStep.getAction(WorkspaceAction.class);
                if (workspaceAction != null) {
                    //store which jenkins node it is built on
                    execNodeName = workspaceAction.getNode();
                    execNodeStartId = nodeStep.getId();
                    if (StringUtils.isEmpty(execNodeName)) {
                        execNodeName = Constants.BUILT_IN_NODE;
                    }
                    LOG.log(Level.FINE, "found workspace node id={0}, name={1}", new String[]{execNodeStartId, execNodeName});
                }
            }
        } else if (atomNode instanceof StepStartNode && atomNode.getId().equals(execNodeStartId)) {
            execNodeName = null;
        }
        if (execNodeName != null) {
            workspaceNodes.put(atomNode.getId(), execNodeName);
        }
    }

    /**
     * Check whether it is an enclosed functional node (with BodyInvocationAction)
     *
     * @param atomNode
     * @param functionName
     * @return
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private StepStartNode getPipelineBlockBoundaryStartNode(FlowNode atomNode, String functionName) {
        StepStartNode startNode = null;
        // it should have BodyInvocationAction
        if (atomNode instanceof StepEndNode
                && ((StepEndNode) atomNode).getStartNode() != null
                && ((StepEndNode) atomNode).getStartNode().getDescriptor() != null
                && ((StepEndNode) atomNode).getStartNode().getDescriptor().getFunctionName().equals(functionName)) {
            StepStartNode blockStart = ((StepEndNode) atomNode).getStartNode();
            if (blockStart.getParents().size() < 1) {
                return null;
            }
            FlowNode boundaryNode = blockStart.getParents().get(0);
            if (boundaryNode instanceof StepStartNode) {
                startNode = (StepStartNode) boundaryNode;
            } else if (boundaryNode instanceof FlowStartNode) {
                //special handling for stage node
                startNode = blockStart;
            }
        }
        return startNode;
    }

    public Map<String, String> getWorkspaceNodes() {
        return workspaceNodes;
    }
}
