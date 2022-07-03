package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.console.ConsoleRecordCacheUtils;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.plugins.workflow.steps.*;
import hudson.console.ConsoleLogFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static com.splunk.splunkjenkins.SplunkConfigUtil.checkTokenAvailable;
import static com.splunk.splunkjenkins.SplunkConfigUtil.verifySplunkSearchResult;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SplunkConsoleTaskListenerDecoratorTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    String id = UUID.randomUUID().toString();
    @Rule
    public JenkinsRule r = new JenkinsRule();
    private static final String TEST_SECRET = "secret";
    private static final String TEST_REPLACEMENT = "xxxx";
    private String jobScript = "node{\n" +
            "  parallel first: {sh \"echo SplunkConsoleTaskListenerDecoratorTest\"},\n" +
            "          second: {sh \"echo " + id + "\"}\n" +
            "  echo '" + id + "'" +
            " }";

    @Before
    public void setUpToken() throws Exception {
        org.junit.Assume.assumeTrue(checkTokenAvailable());
        SplunkJenkinsInstallation.get().setGlobalPipelineFilter(true);
    }

    @After
    public void tearDown() {
        SplunkJenkinsInstallation.get().setGlobalPipelineFilter(false);
    }

    @Test
    public void testSendConsole() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "listener_test");
        p.setDefinition(new CpsFlowDefinition(jobScript, false));
        WorkflowRun b1 = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertFalse(b1.isBuilding());
        r.assertLogContains("SplunkConsoleTaskListenerDecoratorTest", b1);
        assertTrue(b1.getDuration() > 0);
        //manual flush
        ConsoleRecordCacheUtils.flushLog();
        //check log
        verifySplunkSearchResult("source=" + b1.getUrl() + "console " + id, startTime, 2);
        verifySplunkSearchResult("source=" + b1.getUrl() + "console parallel_label=first", startTime, 1);
    }


    @Issue("SECURITY-2128")
    @Test
    public void testDataFilter() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "mask-job");
        p.setDefinition(new CpsFlowDefinition("withTestMaskFilter {node {echo 'hello" + TEST_SECRET + "'}}", false));
        long startTime = System.currentTimeMillis();
        WorkflowRun b1 = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertFalse(b1.isBuilding());
        String testStr = "hello" + TEST_REPLACEMENT;
        r.assertLogContains(testStr, b1);
        verifySplunkSearchResult("source=" + b1.getUrl() + "console " + testStr, startTime, 1);
    }

    /**
     * simply replace secret with xxxx
     */
    public static final class FilterStep extends Step {
        @DataBoundConstructor
        public FilterStep() {
        }

        @Override
        public StepExecution start(StepContext context) throws Exception {
            return new Execution(context);
        }

        private static final class Execution extends StepExecution {
            private static final long serialVersionUID = 1L;

            Execution(StepContext context) {
                super(context);
            }

            @Override
            public boolean start() throws Exception {
                getContext().newBodyInvoker().withContext(new Filter()).withCallback(BodyExecutionCallback.wrap(getContext())).start();
                return false;
            }
        }

        private static final class Filter extends ConsoleLogFilter implements Serializable {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("rawtypes")
            @Override
            public OutputStream decorateLogger(AbstractBuild _ignore, OutputStream logger) throws IOException, InterruptedException {
                return new MaskingOutputStream(logger);
            }
        }

        public static class MaskingOutputStream extends LineTransformationOutputStream.Delegating {


            protected MaskingOutputStream(OutputStream out) {
                super(out);
            }

            @Override
            protected void eol(byte[] b, int len) throws IOException {
                if (len < TEST_SECRET.length()) {
                    out.write(b, 0, len);
                    return;
                }
                String content = new String(b, 0, len, "utf-8");
                out.write(content.replaceAll(TEST_SECRET, TEST_REPLACEMENT).getBytes());
            }
        }

        @TestExtension
        public static final class DescriptorImpl extends StepDescriptor {
            @Override
            public Set<? extends Class<?>> getRequiredContext() {
                return Collections.emptySet();
            }

            @Override
            public String getFunctionName() {
                return "withTestMaskFilter";
            }

            @Override
            public boolean takesImplicitBlockArgument() {
                return true;
            }
        }
    }

}
