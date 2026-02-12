package com.splunk.splunkjenkins.model;

import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;

/**
 * Individual test case result for Splunk reporting.
 */
public class TestCaseResult extends TestResult {
    /**
     * Duration of the test case in seconds.
     */
    private float duration;
    /**
     * Fully qualified class name of the test.
     */
    private String className;
    /**
     * Name of the test method/case.
     */
    private String testName;
    /**
     * Test group name (for test frameworks that support grouping).
     */
    private String groupName;
    /**
     * Whether this test was skipped.
     */
    private boolean skipped;
    /**
     * Message explaining why the test was skipped.
     */
    private String skippedMessage;
    /**
     * Full stack trace of the error if the test failed.
     */
    private String errorStackTrace;
    /**
     * Error details/message if the test failed.
     */
    private String errorDetails;
    /**
     * Standard output from the test execution.
     */
    private String stdout;
    /**
     * Standard error from the test execution.
     */
    private String stderr;
    /**
     * Build number when this test started failing.
     */
    private int failedSince;
    /**
     * Unique identifier/name for this test case.
     */
    private String uniqueName;
    /**
     * Status of the test (PASSED, FAILURE, SKIPPED).
     */
    private TestStatus status;

    /** {@inheritDoc} */
    @Override
    public TestObject getParent() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public TestResult findCorrespondingResult(String id) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the duration of the test case
     */
    @Override
    public float getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the test case
     *
     * @param duration the duration in seconds
     */
    public void setDuration(float duration) {
        this.duration = duration;
    }

    /**
     * Gets the class name of the test
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the class name of the test
     *
     * @param className the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Gets the name of the test case
     *
     * @return the test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Sets the name of the test case
     *
     * @param testName the test name
     */
    public void setTestName(String testName) {
        this.testName = testName;
    }

    /**
     * Checks if the test was skipped
     *
     * @return true if the test was skipped, false otherwise
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Sets whether the test was skipped
     *
     * @param skipped true if the test was skipped, false otherwise
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    /**
     * Gets the message explaining why the test was skipped
     *
     * @return the skip message
     */
    public String getSkippedMessage() {
        return skippedMessage;
    }

    /**
     * Sets the message explaining why the test was skipped
     *
     * @param skippedMessage the skip message
     */
    public void setSkippedMessage(String skippedMessage) {
        this.skippedMessage = skippedMessage;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the stack trace of the error if the test failed
     */
    @Override
    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    /**
     * Sets the stack trace of the error
     *
     * @param errorStackTrace the error stack trace
     */
    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the details of the error that caused the test to fail
     */
    @Override
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * Sets the details of the error that caused the test to fail
     *
     * @param errorDetails the error details
     */
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the standard output from the test execution
     */
    @Override
    public String getStdout() {
        return stdout;
    }

    /**
     * Sets the standard output from the test execution
     *
     * @param stdout the stdout
     */
    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the standard error from the test execution
     */
    @Override
    public String getStderr() {
        return stderr;
    }

    /**
     * Sets the standard error from the test execution
     *
     * @param stderr the stderr
     */
    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the build number when this test started failing
     */
    @Override
    public int getFailedSince() {
        return failedSince;
    }

    /**
     * Sets the build number when this test started failing
     *
     * @param failedSince the build number when the test started failing
     */
    public void setFailedSince(int failedSince) {
        this.failedSince = failedSince;
    }

    /**
     * Gets the unique name of the test case
     *
     * @return the unique name
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Sets the unique name of the test case
     *
     * @param uniqueName the unique name
     */
    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    /**
     * Gets the status of the test
     *
     * @return the test status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the test
     *
     * @param status the test status
     */
    public void setStatus(TestStatus status) {
        this.status = status;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the number of passed tests (1 if passed, 0 otherwise)
     */
    @Override
    public int getPassCount() {
        return TestStatus.PASSED == status ? 1 : 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the number of failed tests (1 if failed, 0 otherwise)
     */
    @Override
    public int getFailCount() {
        return TestStatus.FAILURE == status ? 1 : 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the number of skipped tests (1 if skipped, 0 otherwise)
     */
    @Override
    public int getSkipCount() {
        return TestStatus.SKIPPED == status ? 1 : 0;
    }

    /**
     * Gets the group name of the test
     *
     * @return the group name
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Sets the group name of the test
     *
     * @param groupName the group name
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
