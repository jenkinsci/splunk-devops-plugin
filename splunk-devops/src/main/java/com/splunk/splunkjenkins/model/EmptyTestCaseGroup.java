package com.splunk.splunkjenkins.model;

import hudson.tasks.test.TestResult;

import java.util.Collections;
import java.util.List;

import static com.splunk.splunkjenkins.Constants.NO_TEST_REPORT_FOUND;

/**
 * Empty test case group used when no test results are available.
 */
public class EmptyTestCaseGroup extends JunitTestCaseGroup {
    private String message;

    /**
     * Gets the message indicating why no test results are available
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the number of failures (always 0)
     */
    @Override
    public int getFailures() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the number of passed tests (always 0)
     */
    @Override
    public int getPasses() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the number of skipped tests (always 0)
     */
    @Override
    public int getSkips() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the total number of tests (always 0)
     */
    @Override
    public int getTotal() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the total duration (always 0)
     */
    @Override
    public float getDuration() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * Gets the list of test cases (always empty)
     */
    @Override
    public List<TestResult> getTestcase() {
        return Collections.emptyList();
    }

    /**
     * Sets whether to display a warning about no test results being available
     *
     * @param flag true to set a warning, false to clear it
     */
    public void setWarning(boolean flag) {
        this.message = flag?NO_TEST_REPORT_FOUND:null;
    }
}
