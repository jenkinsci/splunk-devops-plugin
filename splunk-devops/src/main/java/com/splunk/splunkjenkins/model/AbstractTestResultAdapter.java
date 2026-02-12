package com.splunk.splunkjenkins.model;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import org.jvnet.tiger_types.Types;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import static com.splunk.splunkjenkins.Constants.MAX_JUNIT_STDIO_SIZE;

/**
 * Abstract adapter for test results that can be extended to support different test frameworks.
 * Provides a common interface for extracting test results from various Jenkins test actions.
 */
public abstract class AbstractTestResultAdapter<A extends AbstractTestResultAction> implements ExtensionPoint {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(AbstractTestResultAdapter.class.getName());
    
    /**
     *  refelct class type which invoked for checking Run.getAction(targetType)
     */
    public final Class<A> targetType;

    /**
     * Constructs an AbstractTestResultAdapter and determines the target action type
     */
    public AbstractTestResultAdapter() {
        Type type = Types.getBaseClass(getClass(), AbstractTestResultAdapter.class);
        if (type instanceof ParameterizedType)
            targetType = Types.erasure(Types.getTypeArgument(type, 0));
        else
            throw new IllegalStateException(getClass() + " uses the raw type for extending AbstractTestResultAdapter");

    }

    /**
     * Gets the test result action from the build
     *
     * @param run the Jenkins build
     * @return the test result action
     */
    public A getAction(Run run) {
        return run.getAction(targetType);
    }

    /**
     * Checks if this adapter is applicable to the given build
     *
     * @param build the Jenkins build
     * @return true if the adapter is applicable, false otherwise
     */
    public boolean isApplicable(Run build) {
        return getAction(build) != null;
    }

    /**
     * Gets all test results from the build
     *
     * @param build jenkins build
     * @return all the test results added in the build
     */
    @NonNull
    public static List<TestResult> getTestResult(Run build) {
        return getTestResult(build, Collections.<String>emptyList());
    }

    /**
     * Gets test results from the build, optionally ignoring some test actions
     *
     * @param build          jenkins build
     * @param ignoredActions a list of test action class names to filter out
     * @return the test results filtered by the test action name
     */
    @NonNull
    public static List<TestResult> getTestResult(Run build, @NonNull List<String> ignoredActions) {
        List<AbstractTestResultAdapter> adapters = ExtensionList.lookup(AbstractTestResultAdapter.class);
        List<TestResult> testResults = new ArrayList<>();
        for (AbstractTestResultAdapter adapter : adapters) {
            if (adapter.isApplicable(build)) {
                AbstractTestResultAction action = adapter.getAction(build);
                if (ignoredActions.contains(action.getClass().getName())) {
                    // the test action is ignored
                    continue;
                }
                testResults.addAll(adapter.getTestResult(action));
            }
        }
        return testResults;
    }

    /**
     * Gets the test result from the result action
     *
     * @param resultAction the test result action
     * @return a list of test results
     */
    public abstract <T extends TestResult> List<T> getTestResult(A resultAction);

    /**
     * Truncates large test output messages to prevent excessive memory usage.
     * When a message exceeds MAX_JUNIT_STDIO_SIZE, logs a warning with
     * case name and build URL to help diagnose the large output source.
     *
     * @param message the test output message to process
     * @param caseName test case name for warning logs
     * @param url build URL for warning logs
     * @return the original message if small enough, otherwise a truncated message
     */
    public static String trimToLimit(String message, String caseName, String url) {
        String truncatedMessage = "...truncated";
        if (MAX_JUNIT_STDIO_SIZE < truncatedMessage.length() || message == null || message.length() <= MAX_JUNIT_STDIO_SIZE) {
            return message;
        }
        // setUniqueName was called before setStdout/setStderr in JunitResultAdapter/TestNGResultAdapter
        LOG.log(Level.WARNING, "build_url={0} testcase={1} message=\"stdout or stderr too large\" length={2,number,#}" +
                        " truncated_size={3,number,#}\n" +
                        "please adjust jenkins startup option -Dsplunkins.junitStdioLimit=x if you want to avoid this",
                new Object[]{url, caseName, message.length(), MAX_JUNIT_STDIO_SIZE});
        return message.substring(0, MAX_JUNIT_STDIO_SIZE - truncatedMessage.length()) + truncatedMessage;
    }
}
