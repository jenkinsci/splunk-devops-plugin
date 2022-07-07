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

public abstract class AbstractTestResultAdapter<A extends AbstractTestResultAction> implements ExtensionPoint {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(AbstractTestResultAdapter.class.getName());

    public final Class<A> targetType;

    public AbstractTestResultAdapter() {
        Type type = Types.getBaseClass(getClass(), AbstractTestResultAdapter.class);
        if (type instanceof ParameterizedType)
            targetType = Types.erasure(Types.getTypeArgument(type, 0));
        else
            throw new IllegalStateException(getClass() + " uses the raw type for extending AbstractTestResultAdapter");

    }

    public A getAction(Run run) {
        return run.getAction(targetType);
    }

    public boolean isApplicable(Run build) {
        return getAction(build) != null;
    }

    /**
     * @param build jenkins build
     * @return all the test result added in the build
     */
    @NonNull
    public static List<TestResult> getTestResult(Run build) {
        return getTestResult(build, Collections.<String>emptyList());
    }

    /**
     * @param build          jenkins build
     * @param ignoredActions a list of test action class name
     * @return the test result filtered by the test action name
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

    public abstract <T extends TestResult> List<T> getTestResult(A resultAction);

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
