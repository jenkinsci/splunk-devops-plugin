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

/**
 * Pipeline step that sends console output to Splunk in real-time.
 *
 * <p>This step wraps a block of pipeline code and captures all console output (from {@code echo}
 * statements, shell commands, etc.) within that block, sending it to Splunk as log events.
 * The output is sent asynchronously to avoid blocking the pipeline execution.</p>
 *
 * <h2>Pipeline Syntax</h2>
 * <pre>{@code
 * // Declarative Pipeline
 * pipeline {
 *     agent any
 *     stages {
 *         stage('Deploy') {
 *             steps {
 *                 sendSplunkConsoleLog {
 *                     // All console output in this block is sent to Splunk
 *                     sh 'kubectl apply -f deployment.yaml'
 *                     echo 'Deployment completed'
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * // Scripted Pipeline
 * node {
 *     stage('Build') {
 *         sendSplunkConsoleLog {
 *             // Console output from all steps in this block
 *             sh 'make build'
 *             sh 'make test'
 *             echo "Build #${env.BUILD_NUMBER} completed"
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Execution Flow</h2>
 * <ol>
 *     <li>Step starts and creates a {@link TaskListenerDecorator}</li>
 *     <li>Decorator intercepts all console output within the block</li>
 *     <li>Output is cached and sent to Splunk asynchronously</li>
 *     <li>On block completion, the cache is flushed</li>
 * </ol>
 *
 * <h2>Global Filter Behavior</h2>
 * <p>If global pipeline filters are enabled ({@link SplunkJenkinsInstallation#isPipelineFilterEnabled()}),
 * this step is skipped and a log message is recorded instead. This allows administrators to
 * control log sending at a global level.</p>
 *
 * <p>When filters are enabled, the step still executes but applies a no-op decorator that passes
 * through console output without sending to Splunk.</p>
 *
 * <h2>Required Pipeline Context</h2>
 * This step requires:
 * <ul>
 *     <li>{@link Run} - Current build context</li>
 *     <li>{@link TaskListenerDecorator} - For decorator chaining</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The step uses concurrent queue structures and does not block pipeline execution.
 * The decorator is safe to use in parallel stages and with multiple concurrent pipeline runs.</p>
 *
 * <h2>Decorator Implementation</h2>
 * <p>The step uses {@link SplunkConsoleTaskListenerDecorator} which:</p>
 * <ul>
 *     <li>Intercepts all console output streams</li>
 *     <li>Decodes Jenkins console markup and annotations</li>
 *     <li>Batches output for efficient sending to Splunk</li>
 *     <li>Respects global configuration and filters</li>
 * </ul>
 *
 *  This step is designed to be lightweight and non-blocking. The actual log sending
 * to Splunk is handled asynchronously by {@link ConsoleRecordCacheUtils} on a separate thread.
 *
 * @see SplunkConsoleTaskListenerDecorator
 * @see ConsoleRecordCacheUtils
 * @see TaskListenerDecorator
 * @since 1.0.0
 */
public class SplunkConsoleLogStep extends Step {
    private static final Logger LOG = Logger.getLogger(SplunkConsoleLogStep.class.getName());

    /**
     * Creates a new console log step with default configuration.
     *
     * <p>The {@link DataBoundConstructor} annotation enables Jenkins to automatically
     * instantiate this step from pipeline scripts and configuration data.</p>
     *
     * <p>This step requires no configuration parameters as it inherits behavior from
     * global Splunk plugin settings.</p>
     */
    @DataBoundConstructor
    public SplunkConsoleLogStep() {
    }

    /**
     * Starts the execution of this step.
     *
     * <p>This method creates the {@link ConsoleLogExecutionImpl} that will handle the
     * actual execution, including decorator setup and console output interception.</p>
     *
     * @param context the step context containing the current pipeline execution state
     * @return a new {@link ConsoleLogExecutionImpl} instance
     * @throws Exception if the step cannot be started due to invalid context or configuration
     *
     * Implementation follows the {@link Step} contract by returning a concrete
     * {@link StepExecution} implementation.
     */
    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ConsoleLogExecutionImpl(context);
    }


    /**
     * Descriptor for {@link SplunkConsoleLogStep} that defines its metadata.
     *
     * <p>This descriptor is a Jenkins extension point that describes how the step integrates
     * with Jenkins:</p>
     * <ul>
     *     <li>Function name for pipeline DSL</li>
     *     <li>Display name in UI and snippet generator</li>
     *     <li>Required pipeline context types</li>
     *     <li>Whether it takes a block (body) argument</li>
     * </ul>
     *
     * <h2>Extension Loading</h2>
     * <p>The {@code @Extension(optional = true)} annotation indicates this is an optional
     * Jenkins extension. If the extension cannot be loaded (e.g., due to missing dependencies),
     * Jenkins will log a warning but continue operation.</p>
     *
     * <h2>Snipped Generator Integration</h2>
     * <p>This descriptor enables the Pipeline Snippet Generator to recognize and generate
     * syntax for this step in Jenkins' UI.</p>
     *
     * @see StepDescriptor
     * @see Extension
     */
    @Extension(optional = true)
    public static class DescriptorImpl extends StepDescriptor {

        /**
         * Returns the required pipeline context types for this step.
         *
         * <p>This step requires {@link Run} to be available in the pipeline context.
         * The run provides access to the current build, job, and execution context.</p>
         *
         * <p>If the required context is not available, the step will fail during
         * pipeline execution with a clear error message.</p>
         *
         * @return a set containing {@link Run}
         * @see StepContext#get(Class)
         */
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns the function name used in pipeline scripts to invoke this step.
         * The name {@code "sendSplunkConsoleLog"} follows the step's primary purpose.
         * Usage example:</p>
         * <pre>{@code
         * sendSplunkConsoleLog {
         *     // steps within block
         * }
         * }</pre>
         *
         * @return "sendSplunkConsoleLog"
         */
        @Override
        public String getFunctionName() {
            return "sendSplunkConsoleLog";
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns the human-readable name displayed in Jenkins UI, particularly
         * in the Pipeline Snippet Generator and step documentation. The name
         * "Send console log Splunk" clearly describes what the step does from a
         * user's perspective.</p>
         *
         * @return "Send console log Splunk"
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return "Send console log Splunk";
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns {@code true} indicating this step takes a block (body) argument.
         * The block contains the pipeline steps whose console output should be sent
         * to Splunk.</p>
         *
         * <p>Example usage with block:</p>
         * <pre>{@code
         * sendSplunkConsoleLog {
         *     sh 'echo "This goes to Splunk"'
         *     echo "This also goes to Splunk"
         * }
         * }</pre>
         *
         * @return {@code true}
         */
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }


    /**
     * Execution implementation for {@link SplunkConsoleLogStep}.
     *
     * <p>This class implements the actual execution logic when the step is invoked in a pipeline.
     * It handles the setup of the console decorator and manages the lifecycle of the step.
     * When the step starts, it applies a {@link SplunkConsoleTaskListenerDecorator} to intercept
     * console output. When the step completes or fails, it ensures the output cache is flushed.</p>
     *
     * <h2>Execution Flow</h2>
     * <ol>
     *     <li>{@link #start()} - Creates and applies the console decorator</li>
     *     <li>{@link BodyExecutionCallback} handles completion/failure</li>
     *     <li>{@link #stop(Throwable)} - Cleanup and failure handling</li>
     *     <li>{@link BodyExecutionCallbackConsole#finished(StepContext)} - Flushes cache on completion</li>
     * </ol>
     *
     * <h3>Decorator Application</h3>
     * <p>The execution checks global filter settings and conditionally applies:</p>
     * <ul>
     *     <li>If filters enabled: Logs an info message and applies a pass-through decorator</li>
     *     <li>If filters disabled: Applies {@link SplunkConsoleTaskListenerDecorator}</li>
     * </ul>
     *
     *
     * <h3>Error Handling</h3>
     * <p>The execution includes comprehensive error handling:</p>
     * <ul>
     *     <li>Decorator creation failures are handled gracefully</li>
     *     <li>Cache flush failures are logged but don't fail the step</li>
     *     <li>Stop() failures are reported to the step context</li>
     * </ul>
     *
     * The execution creates a TaskListenerDecorator that merges with any existing
     * decorators using {@link org.jenkinsci.plugins.workflow.log.TaskListenerDecorator#merge } to ensure compatibility with
     * other plugins that also decorate console output.
     *
     * @see StepExecution
     * @see SplunkConsoleTaskListenerDecorator
     * @see ConsoleRecordCacheUtils
     */
    public static class ConsoleLogExecutionImpl extends StepExecution {

        /**
         * Creates a new execution instance for the console log step.
         *
         * @param context the step context containing execution context and state
         */
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

    /**
     * Callback that flushes cached console logs when the step completes.
     *
     * <p>This callback extends {@link BodyExecutionCallback.TailCall}, which is invoked after
     * the body (block) of the step completes execution, regardless of whether it succeeded
     * or failed. The primary responsibility is to ensure all cached console output is
     * flushed to Splunk before the step terminates.</p>
     *
     * <h2>Execution Guarantees</h2>
     * <p>This callback is guaranteed to execute in the following scenarios:</p>
     * <ul>
     *     <li>Successful completion of step block</li>
     *     <li>Step block throws exception (failure)</li>
     *     <li>Pipeline is aborted or cancelled</li>
     * </ul>
     *
     * <h2>Log Flush Process</h2>
     * <p>The flush process:</p>
     * <ol>
     *     <li>Drains all cached records from the queue</li>
     *     <li>Sends them as a batch to Splunk</li>
     *     <li>Logs errors but does not fail the pipeline</li>
     * </ol>
     *
     *
     * <p><strong>Error Handling:</strong> Flush failures are logged but do not propagate
     * to the pipeline execution. This ensures that Splunk failures don't cause pipeline
     * failures.</p>
     *
     * <h3>Serialization</h3>
     * <p>This callback must be {@link java.io.Serializable} because it persists with the pipeline
     * execution state. The serialVersionUID ensures version compatibility if the class
     * structure changes.</p>
     *
     * @see BodyExecutionCallback
     * @see ConsoleRecordCacheUtils#flushLog()
     */
    public static class BodyExecutionCallbackConsole extends BodyExecutionCallback.TailCall {
        private static final long serialVersionUID = 1L;

        /**
         * Flushes cached console logs to Splunk when the step completes.
         *
         * <p>This method is automatically invoked by Jenkins when the step's body block
         * finishes execution. It delegates to {@link ConsoleRecordCacheUtils#flushLog()}
         * to drain the queue and send any pending records to Splunk.</p>
         *
         * @param stepContext the context for the completed step
         * @throws Exception if an error occurs during log flushing (logged but does not fail the step)
         */
        @Override
        protected void finished(StepContext stepContext) throws Exception {
            ConsoleRecordCacheUtils.flushLog();
        }
    }
}
