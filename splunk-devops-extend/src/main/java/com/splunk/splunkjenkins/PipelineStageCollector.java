package com.splunk.splunkjenkins;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Result;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.NotExecutedNodeAction;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.support.actions.PauseAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the stages of a pipeline run, and the steps directly inside each stage, as maps sent to Splunk.
 * <p>
 * A stage is a {@link BlockStartNode} carrying a {@link LabelAction} that is not a {@link ThreadNameAction}
 * (parallel branches are not reported). Stages are reported flat, including nested ones, sorted by node id.
 * The children of a stage are the steps whose nearest enclosing stage it is, up to the first nested stage.
 */
final class PipelineStageCollector {
    /**
     * Declarative Pipeline tags the stage node with this tag; values starting with {@code SKIPPED_} mean the
     * stage did not run (a {@code when} condition, an earlier failure or unstable result, a restart).
     */
    static final String STAGE_STATUS_TAG = "STAGE_STATUS";
    static final String SKIPPED_PREFIX = "SKIPPED_";
    /** Limit on the child steps reported for each stage. */
    static final int MAX_CHILD_NODES = 100;
    static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    static final Comparator<FlowNode> NODE_ID_ORDER =
            Comparator.comparingInt((FlowNode node) -> node.getId().length()).thenComparing(FlowNode::getId);

    private PipelineStageCollector() {
    }

    /**
     * @param execution the flow execution of a run
     * @return one map per stage, sorted by node id
     */
    @NonNull
    static List<Map<String, Object>> collect(@NonNull FlowExecution execution) {
        Map<String, BlockStartNode> stages = new HashMap<>();
        Map<String, BlockEndNode<?>> ends = new HashMap<>();
        // first node following each node, used for the end time of steps
        Map<String, FlowNode> successors = new HashMap<>();
        Map<String, Result> worstWarnings = new HashMap<>();
        Map<String, Long> pauses = new HashMap<>();
        Map<String, List<FlowNode>> children = new HashMap<>();
        // id of the first stage started directly inside each stage
        Map<String, FlowNode> firstNestedStages = new HashMap<>();

        for (FlowNode node : new DepthFirstScanner().allNodes(execution)) {
            for (FlowNode parent : node.getParents()) {
                successors.merge(parent.getId(), node, PipelineStageCollector::earliest);
            }
            if (isStage(node)) {
                stages.put(node.getId(), (BlockStartNode) node);
            } else if (node instanceof BlockEndNode) {
                BlockEndNode<?> end = (BlockEndNode<?>) node;
                if (isStage(end.getStartNode())) {
                    ends.put(end.getStartNode().getId(), end);
                }
            }
            WarningAction warning = node.getPersistentAction(WarningAction.class);
            long pause = PauseAction.getPauseDuration(node);
            BlockStartNode nearestStage = null;
            for (BlockStartNode enclosing : node.getEnclosingBlocks()) {
                if (!isStage(enclosing)) {
                    continue;
                }
                if (nearestStage == null) {
                    nearestStage = enclosing;
                }
                if (warning != null) {
                    worstWarnings.merge(enclosing.getId(), warning.getResult(), PipelineStageCollector::worst);
                }
                if (pause > 0) {
                    pauses.merge(enclosing.getId(), pause, Long::sum);
                }
            }
            if (nearestStage != null) {
                if (isStage(node)) {
                    firstNestedStages.merge(nearestStage.getId(), node, PipelineStageCollector::earliest);
                } else if (node instanceof AtomNode) {
                    children.computeIfAbsent(nearestStage.getId(), k -> new ArrayList<>()).add(node);
                }
            }
        }

        List<BlockStartNode> sortedStages = new ArrayList<>(stages.values());
        sortedStages.sort(NODE_ID_ORDER);
        List<Map<String, Object>> result = new ArrayList<>(sortedStages.size());
        for (BlockStartNode stage : sortedStages) {
            String id = stage.getId();
            BlockEndNode<?> end = ends.get(id);
            FlowNode afterEnd = end == null ? null : successors.getOrDefault(end.getId(), end);
            Map<String, Object> stageInfo = stageToMap(stage, end, afterEnd, worstWarnings.get(id), pauses.getOrDefault(id, 0L));
            List<FlowNode> stageChildren = children.getOrDefault(id, new ArrayList<>());
            FlowNode firstNestedStage = firstNestedStages.get(id);
            if (firstNestedStage != null) {
                // steps after a nested stage starts are not reported
                stageChildren.removeIf(child -> NODE_ID_ORDER.compare(child, firstNestedStage) > 0);
            }
            stageChildren.sort(NODE_ID_ORDER);
            List<Map<String, Object>> childInfos = new ArrayList<>();
            for (FlowNode child : stageChildren) {
                if (childInfos.size() >= MAX_CHILD_NODES) {
                    break;
                }
                childInfos.add(stepToMap(child, successors.get(child.getId())));
            }
            if (!childInfos.isEmpty()) {
                stageInfo.put("children", childInfos);
            }
            result.add(stageInfo);
        }
        return result;
    }

    /**
     * @param afterEnd the node following the end of the stage, the stage duration runs until it starts
     */
    private static Map<String, Object> stageToMap(@NonNull BlockStartNode start, @CheckForNull BlockEndNode<?> end,
                                                  @CheckForNull FlowNode afterEnd, @CheckForNull Result worstWarning,
                                                  long pause) {
        boolean executed = NotExecutedNodeAction.isExecuted(start) && !isSkipped(start);
        String status;
        ErrorAction error = null;
        if (!executed) {
            status = Result.NOT_BUILT.toString();
        } else if (end == null) {
            status = STATUS_IN_PROGRESS;
        } else {
            error = end.getError();
            if (error != null) {
                status = statusOf(error.getError());
            } else if (worstWarning != null) {
                status = worstWarning.toString();
            } else {
                status = Result.SUCCESS.toString();
            }
        }
        long duration = executed ? durationBetween(start, afterEnd) : 0;
        long startTime = NotExecutedNodeAction.isExecuted(start) ? TimingAction.getStartTime(start) : 0;
        return toMap(start, status, duration, Math.min(pause, duration), startTime, error, null);
    }

    private static Map<String, Object> stepToMap(@NonNull FlowNode step, @CheckForNull FlowNode next) {
        boolean executed = NotExecutedNodeAction.isExecuted(step);
        String status;
        ErrorAction error = null;
        if (!executed) {
            status = Result.NOT_BUILT.toString();
        } else if (next == null) {
            status = STATUS_IN_PROGRESS;
        } else {
            error = step.getError();
            WarningAction warning = step.getPersistentAction(WarningAction.class);
            if (error != null) {
                status = statusOf(error.getError());
            } else if (warning != null) {
                status = warning.getResult().toString();
            } else {
                status = Result.SUCCESS.toString();
            }
        }
        long duration = executed ? durationBetween(step, next) : 0;
        long pause = Math.min(PauseAction.getPauseDuration(step), duration);
        return toMap(step, status, duration, pause, TimingAction.getStartTime(step), error, findExecNode(step));
    }

    @SuppressFBWarnings(value = {"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "DCN_NULLPOINTER_EXCEPTION"},
            justification = "Deserialization can leave a null Throwable, GROOVY-8936 is fixed only in Groovy 4")
    private static Map<String, Object> toMap(FlowNode node, String status, long duration, long pause, long startTime,
                                             @CheckForNull ErrorAction error, @CheckForNull String execNode) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", node.getDisplayName());
        result.put("id", node.getId());
        result.put("status", status);
        result.put("duration", duration / 1000f);
        result.put("pause_duration", pause / 1000f);
        result.put("start_time", startTime / 1000);
        // the error is not reported for NOT_BUILT, including NOT_BUILT interruptions
        if (error != null && !Result.NOT_BUILT.toString().equals(status)) {
            Throwable throwable = error.getError();
            if (throwable != null) {
                String message;
                try {
                    message = throwable.getMessage();
                } catch (NullPointerException npe) {
                    // GROOVY-8936
                    message = "No message: NullPointerException on .getMessage()";
                }
                result.put("error", message);
                result.put("error_type", throwable.getClass().getName());
            } else {
                // rare cases, for example serialization problems
                result.put("error", "No message: null Throwable on error");
                result.put("error_type", "No type: null Throwable on error");
            }
        }
        result.put("arguments", ArgumentsAction.getStepArgumentsAsString(node));
        result.put("exec_node", execNode);
        return result;
    }

    /**
     * @return the status of a failed node, a {@link FlowInterruptedException} carries its own result
     * (for example ABORTED when a user aborts the build, NOT_BUILT when a milestone cancels it)
     */
    static String statusOf(@CheckForNull Throwable error) {
        if (error instanceof FlowInterruptedException) {
            return ((FlowInterruptedException) error).getResult().toString();
        }
        return Result.FAILURE.toString();
    }

    /**
     * Duration from the start of a node to the start of the node that follows it, or to now if there is none yet.
     */
    private static long durationBetween(@NonNull FlowNode start, @CheckForNull FlowNode end) {
        long startTime = TimingAction.getStartTime(start);
        if (startTime == 0) {
            return 0;
        }
        long endTime = end == null ? System.currentTimeMillis() : TimingAction.getStartTime(end);
        return Math.max(0, endTime - startTime);
    }

    static boolean isStage(FlowNode node) {
        return node instanceof BlockStartNode
                && node.getPersistentAction(LabelAction.class) != null
                && node.getPersistentAction(ThreadNameAction.class) == null;
    }

    private static boolean isSkipped(FlowNode stage) {
        TagsAction tags = stage.getPersistentAction(TagsAction.class);
        if (tags == null) {
            return false;
        }
        String value = tags.getTagValue(STAGE_STATUS_TAG);
        return value != null && value.startsWith(SKIPPED_PREFIX);
    }

    /**
     * @return the Jenkins node name of the outermost {@code node} block enclosing the step
     */
    @CheckForNull
    private static String findExecNode(FlowNode step) {
        String execNode = null;
        for (BlockStartNode enclosing : step.getEnclosingBlocks()) {
            if (!(enclosing instanceof StepNode)) {
                continue;
            }
            StepDescriptor descriptor = ((StepNode) enclosing).getDescriptor();
            WorkspaceAction workspace = enclosing.getPersistentAction(WorkspaceAction.class);
            if (descriptor != null && "node".equals(descriptor.getFunctionName()) && workspace != null) {
                execNode = StringUtils.isEmpty(workspace.getNode()) ? Constants.BUILT_IN_NODE : workspace.getNode();
            }
        }
        return execNode;
    }

    private static FlowNode earliest(FlowNode a, FlowNode b) {
        return NODE_ID_ORDER.compare(a, b) <= 0 ? a : b;
    }

    private static Result worst(Result a, Result b) {
        return a.isWorseThan(b) ? a : b;
    }
}
