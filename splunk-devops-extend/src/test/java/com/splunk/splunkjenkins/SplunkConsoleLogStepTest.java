package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.console.PipelineConsoleDecoder;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDurabilityHint;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.job.properties.DurabilityHintJobProperty;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static com.splunk.splunkjenkins.SplunkConfigUtil.checkTokenAvailable;
import static com.splunk.splunkjenkins.SplunkConfigUtil.verifySplunkSearchResult;
import static com.splunk.splunkjenkins.console.LabelConsoleLineStream.ANSI_COLOR_ESCAPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SplunkConsoleLogStepTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule r = new JenkinsRule();
    String id = UUID.randomUUID().toString();

    private String jobScript = "sendSplunkConsoleLog {" +
            "node{\n" +
            "  sh \"echo testjob\";\n" +
            "  sh \"echo " + id + "\";\n" +
            " }" +
            "}";

    @Before
    public void setUp() throws Exception {
        org.junit.Assume.assumeTrue(checkTokenAvailable());
    }

    @Test
    public void testSendConsoleLog() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(jobScript, true));
        WorkflowRun b1 = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertFalse(b1.isBuilding());
        r.assertLogContains("testjob", b1);
        assertTrue(b1.getDuration() > 0);
        //check log
        verifySplunkSearchResult("source=" + b1.getUrl() + "console " + id, startTime, 1);
    }

    @Test
    public void testDecodeLine() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.addProperty(new DurabilityHintJobProperty(FlowDurabilityHint.SURVIVABLE_NONATOMIC));
        p.setDefinition(new CpsFlowDefinition("parallel first: {echo 'hello'}, second: {echo 'in-second'};", true));
        WorkflowRun b = r.buildAndAssertSuccess(p);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.getLogText().writeRawLogTo(0, out);
        PipelineConsoleDecoder decoder = new PipelineConsoleDecoder(b);
        byte[] logs = out.toByteArray();
        assertTrue("end with \n", logs[logs.length - 1] == '\n');
        StringBuffer lines = new StringBuffer();
        // decode line by line
        ByteArrayOutputStream branch = new ByteArrayOutputStream();
        int lineCount = 0;
        for (int i = 0; i < logs.length; i++) {
            branch.write(logs[i]);
            if (logs[i] == '\n') {
                lines.append(decoder.decodeLine(branch.toByteArray(), branch.size()));
                branch.reset();
                lineCount++;
            }
        }
        String text = lines.toString();
        assertTrue(text.contains("parallel_label=\"first\" [Pipeline] echo"));
        assertTrue(text.contains("parallel_label=\"first\" hello"));
        assertTrue(text.contains("parallel_label=\"second\" [Pipeline] echo"));
        assertTrue(text.contains("parallel_label=\"second\" in-second"));
        assertEquals(lineCount, StringUtils.countMatches(text, "\n"));
        assertEquals(2, StringUtils.countMatches(text, "label=\"second\" "));
    }

    @Test
    public void testAnsilColor() {
        String lineWithAnsi = "\u001B[0;31mline in red\nend\u001B[0m";
        String line = ANSI_COLOR_ESCAPE.matcher(lineWithAnsi).replaceAll("");
        assertEquals("line in red\nend", line);
    }
}