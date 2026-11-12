package com.splunk.splunkjenkins;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the stage information published for completed pipeline runs.
 */
public class PipelineRunSupportStagesTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void interruptedWithFailure() throws Exception {
        Map<String, Map<String, Object>> stages = interrupt(Result.FAILURE);
        assertEquals("FAILURE", stages.get("blocked").get("status"));
        assertEquals("org.jenkinsci.plugins.workflow.steps.FlowInterruptedException", stages.get("blocked").get("error_type"));
    }

    @Test
    public void interruptedWithNotBuilt() throws Exception {
        Map<String, Map<String, Object>> stages = interrupt(Result.NOT_BUILT);
        assertEquals("NOT_BUILT", stages.get("blocked").get("status"));
        assertFalse(stages.get("blocked").containsKey("error_type"));
    }

    @Test
    public void interruptedWithAborted() throws Exception {
        Map<String, Map<String, Object>> stages = interrupt(Result.ABORTED);
        assertEquals("ABORTED", stages.get("blocked").get("status"));
        assertEquals("org.jenkinsci.plugins.workflow.steps.FlowInterruptedException", stages.get("blocked").get("error_type"));
    }

    /**
     * Interrupts a build in the middle of a stage, the way a user abort or a milestone does.
     */
    private Map<String, Map<String, Object>> interrupt(Result result) throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("stage('first') { echo 'ok' }\n" +
                "stage('blocked') { echo 'started'; sleep 60 }\n", true));
        WorkflowRun b = p.scheduleBuild2(0).waitForStart();
        r.waitForMessage("started", b);
        b.getExecutor().interrupt(result);
        r.assertBuildStatus(result, r.waitForCompletion(b));
        Map<String, Map<String, Object>> stages = stagesByName(b);
        assertEquals("SUCCESS", stages.get("first").get("status"));
        List<Map<String, Object>> children = children(stages.get("blocked"));
        assertEquals(2, children.size());
        assertEquals("SUCCESS", children.get(0).get("status"));
        // the interrupted sleep step reports the same status as its stage
        assertEquals(stages.get("blocked").get("status"), children.get(1).get("status"));
        return stages;
    }

    @Test
    public void stagesAndSteps() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("stage('one') { echo 'a'; sleep time: 200, unit: 'MILLISECONDS' }\n" +
                "stage('two') { catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') { error 'boom' } }\n" +
                "stage('three') { try { error 'caught' } catch (e) { echo 'handled' } }\n" +
                "stage('outer') { echo 'before'; stage('inner') { unstable 'meh' }; echo 'after' }\n" +
                "stage('p') { parallel a: { stage('pa') { echo 'a' } }, b: { echo 'b' } }\n", true));
        WorkflowRun b = r.assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0));
        List<Map<String, Object>> list = stages(b);
        List<String> names = new ArrayList<>();
        for (Map<String, Object> stage : list) {
            names.add((String) stage.get("name"));
        }
        assertEquals(List.of("one", "two", "three", "outer", "inner", "p", "pa"), names);
        Map<String, Map<String, Object>> stages = stagesByName(b);

        Map<String, Object> one = stages.get("one");
        assertEquals("SUCCESS", one.get("status"));
        assertTrue("duration was " + one.get("duration"), (Float) one.get("duration") >= 0.2f);
        assertEquals(0f, one.get("pause_duration"));
        assertNull(one.get("exec_node"));
        assertEquals(List.of("Print Message", "Sleep"), childNames(one));

        assertEquals("UNSTABLE", stages.get("two").get("status"));
        assertEquals("SUCCESS", stages.get("three").get("status"));
        assertEquals(List.of("FAILURE", "SUCCESS"), childStatuses(stages.get("three")));
        assertEquals("caught", children(stages.get("three")).get(0).get("error"));
        assertEquals("hudson.AbortException", children(stages.get("three")).get(0).get("error_type"));

        // a stage reflects the stages nested in it, its steps after the first nested stage are not reported
        assertEquals("UNSTABLE", stages.get("outer").get("status"));
        assertTrue((Float) stages.get("outer").get("duration") >= (Float) stages.get("inner").get("duration"));
        assertEquals(List.of("before"), childArguments(stages.get("outer")));
        assertEquals("UNSTABLE", stages.get("inner").get("status"));

        assertEquals("SUCCESS", stages.get("p").get("status"));
        assertEquals("SUCCESS", stages.get("pa").get("status"));
    }

    private static List<Map<String, Object>> stages(WorkflowRun b) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) new PipelineRunSupport().extract(b, true).get("stages");
        return stages;
    }

    private static Map<String, Map<String, Object>> stagesByName(WorkflowRun b) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> stage : stages(b)) {
            result.put((String) stage.get("name"), stage);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> children(Map<String, Object> stage) {
        return (List<Map<String, Object>>) stage.get("children");
    }

    private static List<Object> childValues(Map<String, Object> stage, String key) {
        List<Object> result = new ArrayList<>();
        for (Map<String, Object> child : children(stage)) {
            result.add(child.get(key));
        }
        return result;
    }

    private static List<Object> childNames(Map<String, Object> stage) {
        return childValues(stage, "name");
    }

    private static List<Object> childStatuses(Map<String, Object> stage) {
        return childValues(stage, "status");
    }

    private static List<Object> childArguments(Map<String, Object> stage) {
        return childValues(stage, "arguments");
    }
}
