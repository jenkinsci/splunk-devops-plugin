package com.splunk.splunkjenkins;


import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.splunk.splunkjenkins.utils.LogEventHelper.getBuildVariables;

/**
 * Global pipeline variable {@code splunkins} that provides access to Splunk integration features.
 *
 * <p>This global variable is automatically available in all Pipeline scripts (both declarative
 * and scripted) without requiring explicit import or configuration. It provides a convenient
 * DSL for sending custom data to Splunk and accessing build information.</p>
 *
 * <h2>Variable Name</h2>
 * <pre>{@code splunkins}</pre>
 *
 * <h2>Usage in Pipeline</h2>
 * <pre>{@code
 * pipeline {
 *     agent any
 *     stages {
 *         stage('Build') {
 *             steps {
 *                 script {
 *                     // Send a custom event to Splunk
 *                     splunkins.send([
 *                         eventType: 'custom_event',
 *                         message: 'Build started',
 *                         timestamp: new Date()
 *                     ])
 *
 *                     // Access build information
 *                     def buildInfo = splunkins.getBuildInfo()
 *                     echo "Build ID: ${buildInfo.buildId}"
 *                 }
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Implementation Details</h2>
 * <p>The variable returns a delegate object ({@link RunDelegate}) that wraps the current
 * {@link Run} and provides convenient methods. It is created per pipeline execution and
 * provides access to:</p>
 * <ul>
 *     <li>Build variables and parameters</li>
 *     <li>TaskListener for console output</li>
 *     <li>Custom event sending capabilities</li>
 * </ul>
 *
 * <h2>Reflection Usage</h2>
 * <p>This implementation uses reflection to access {@link WorkflowRun} listener field to
 * obtain the {@link TaskListener} for the current build. Reflection is used because the
 * listener field is not publicly accessible, but provides essential functionality for:</p>
 * <ul>
 *     <li>Adding console log annotations</li>
 *     <li>Logging messages at various log levels</li>
 * </ul>
 *
 * <p>If reflection fails (due to Jenkins API changes or security restrictions), it
 * gracefully falls back to a {@link LogTaskListener} that logs to a class-level logger.
 * This fallback preserves functionality but without console context.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>The variable is created per-pipeline execution and should not be cached or reused
 * across different builds. Each {@link CpsScript} has its own instance.</p>
 *
 * <h2>Pipeline Compatibility</h2>
 * <ul>
 *     <li>Declarative Pipeline: Supported</li>
 *     <li>Scripted Pipeline: Supported</li>
 *     <li>Shared Libraries: Supported</li>
 * </ul>
 *
 * The reflection-based access to WorkflowRun listener is tested against Jenkins
 * core versions. If the field name changes in future Jenkins versions, the reflection will
 * fail gracefully and fall back to the LogTaskListener approach.
 *
 * @see GlobalVariable
 * @see RunDelegate
 * @since 1.0.0
 */
@Extension(optional = true)
public class SplunkinsDslVariable extends GlobalVariable {

    /**
     * Returns the name of this global variable.
     *
     * <p>The name is used in pipeline scripts to reference this variable. The standard name
     * {@code "splunkins"} is chosen to be short, memorable, and consistent with the plugin name.</p>
     *
     * @return the constant value "splunkins"
     */
    @NonNull
    @Override
    public String getName() {
        return "splunkins";
    }

    /**
     * Returns the value of this global variable for the given script context.
     *
     * <p>This method is invoked by the pipeline engine when the {@code splunkins} variable
     * is referenced in a pipeline script. It creates and returns a {@link RunDelegate}
     * that provides the actual functionality.
     * </p>
     *
     * <h4>Execution Flow</h4>
     * <ol>
     *     <li>Extracts the current {@link Run} from the {@link CpsScript}</li>
     *     <li>Attempts to access the {@link WorkflowRun} listener field via reflection (see below)</li>
     *     <li>Gathers build variables and parameters via {@link com.splunk.splunkjenkins.utils.LogEventHelper#getBuildVariables}</li>
     *     <li>Creates and returns a {@link RunDelegate} instance</li>
     * </ol>
     *
     * <h4>Reflection-Based listener Access</h4>
     * <p>The implementation attempts to access the private {@code listener} field of
     * {@link WorkflowRun} to obtain the {@link TaskListener} for the current build.</p>
     *
     * <p>This approach is necessary because:</p>
     * <ul>
     *     <li>The listener is required for console output operations</li>
     *     <li>It's not publicly accessible through WorkflowRun's API</li>
     *     <li>The CpsScript doesn't expose the listener directly</li>
     * </ul>
     *
     * <p><strong>Reflection Failure Handling:</strong> If reflection fails (due to security
     * restrictions or API changes), the implementation gracefully falls back to a
     * {@link LogTaskListener} that logs to a class-level logger with INFO level.</p>
     *
     * <h4>Build Variables Collection</h4>
     * <p>The method calls {@link com.splunk.splunkjenkins.utils.LogEventHelper#getBuildVariables(Run)} to collect all build parameters
     * and variables that should be passed to Splunk with each event. This includes:</p>
     * <ul>
     *     <li>Build parameters</li>
     *     <li>Environment variables from {@link Run#getEnvironment(TaskListener)}</li>
     *     <li>Build-specific metadata</li>
     * </ul>
     *
     * @param script the current pipeline CPS script context
     * @return a new {@link RunDelegate} providing Splunk integration functionality
     * @throws IllegalStateException if no build can be found from the script context
     * @throws Exception if reflection access fails or the delegate cannot be created
     *
     * @see RunDelegate
     * @see GlobalVariable#getValue(CpsScript)
     */
    @NonNull
    @Override
    public Object getValue(@NonNull CpsScript script) throws Exception {
        Run<?, ?> build = script.$build();
        if (build == null) {
            throw new IllegalStateException("cannot find associated build");
        }
        // try to access WorkflowRun.listener
        TaskListener listener = TaskListener.NULL;
        try {
            Field field = WorkflowRun.class.getDeclaredField("listener");
            field.setAccessible(true);
            listener = (TaskListener) field.get(build);
        } catch (Exception e) {
            listener = new LogTaskListener(Logger.getLogger(SplunkinsDslVariable.class.getName()), Level.INFO);
        }
        Map buildParameters = getBuildVariables(build);
        RunDelegate delegate = new RunDelegate(build, buildParameters, listener);
        return delegate;
    }
}
