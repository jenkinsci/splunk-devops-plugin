package com.splunk.splunkjenkins;

import com.google.common.collect.ImmutableSet;
import com.splunk.splunkjenkins.console.ConsoleRecordCacheUtils;
import com.splunk.splunkjenkins.console.SplunkConsoleTaskListenerDecorator;
import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SplunkConsoleLogStep extends Step {
    private static final Logger LOG = Logger.getLogger(SplunkConsoleLogStep.class.getName());

    @DataBoundConstructor
    public SplunkConsoleLogStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ConsoleLogExecutionImpl(context);
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFunctionName() {
            return "sendSplunkConsoleLog";
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return "Send console log Splunk";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }


    public static class ConsoleLogExecutionImpl extends StepExecution {
        public ConsoleLogExecutionImpl(StepContext context) {
            super(context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean start() throws Exception {
            //refer to WithContextStep implementation
            StepContext context = getContext();
            Run run = context.get(Run.class);
            BodyInvoker invoker = context.newBodyInvoker().withCallback(new BodyExecutionCallbackConsole());
            if (!SplunkJenkinsInstallation.get().isPipelineFilterEnabled()) {
                invoker.withContext(TaskListenerDecorator.merge(context.get(TaskListenerDecorator.class), new SplunkConsoleTaskListenerDecorator((WorkflowRun) run)));
            } else {
                String jobName = run.getParent().getFullName();
                LOG.log(Level.INFO, "ignored sendSplunkConsoleLog since global filter is enabled, job-name=" + jobName);
            }
            invoker.start();
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void stop(@NonNull Throwable cause) throws Exception {
            getContext().onFailure(cause);
        }
    }

    public static class BodyExecutionCallbackConsole extends BodyExecutionCallback.TailCall {
        private static final long serialVersionUID = 1L;

        @Override
        protected void finished(StepContext stepContext) throws Exception {
            ConsoleRecordCacheUtils.flushLog();
        }
    }
}
