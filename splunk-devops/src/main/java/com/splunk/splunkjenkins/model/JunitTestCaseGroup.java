package com.splunk.splunkjenkins.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.tasks.test.TestResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for grouped JUnit test case results.
 */
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class JunitTestCaseGroup implements Serializable{
    /**
     * Number of failed tests in this group.
     */
    int failures;
    /**
     * Number of passed tests in this group.
     */
    int passes;
    /**
     * Number of skipped tests in this group.
     */
    int skips;
    /**
     * Total number of tests in this group.
     */
    int total;
    /**
     * Total duration of all tests in seconds.
     */
    float duration;
    /**
     * Alias for total, used for JSON serialization.
     */
    int tests;
    /**
     * Alias for duration, used for JSON serialization.
     */
    float time;
    /**
     * Number of errors, maintained for backward compatibility with JUnit 3 XML format.
     */
    int errors = 0;
    /**
     * List of individual test case results.
     */
    List<TestResult> testcase = new ArrayList<>();

    /**
     * Adds a test result to the test case group and accumulates statistics
     *
     * @param result the test result to add
     */
    public void add(TestResult result) {
        this.failures += result.getFailCount();
        this.passes += result.getPassCount();
        this.skips += result.getSkipCount();
        this.total += result.getTotalCount();
        this.duration += result.getDuration();
        //update alias
        this.tests = this.total;
        this.time = this.duration;
        this.testcase.add(result);
    }

    /**
     * Gets the number of test failures
     *
     * @return the number of failures
     */
    public int getFailures() {
        return failures;
    }

    /**
     * Gets the number of tests that passed
     *
     * @return the number of passed tests
     */
    public int getPasses() {
        return passes;
    }

    /**
     * Gets the number of skipped tests
     *
     * @return the number of skipped tests
     */
    public int getSkips() {
        return skips;
    }

    /**
     * Gets the total number of tests
     *
     * @return the total number of tests
     */
    public int getTotal() {
        return total;
    }

    /**
     * Gets the total duration of all tests
     *
     * @return the total duration in seconds
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Gets the list of individual test cases
     *
     * @return the list of test cases
     */
    public List<TestResult> getTestcase() {
        return testcase;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "failures: " + failures +
                ", passes: " + passes +
                ", skips: " + skips +
                ", errors: " + errors +
                ", total: " + total +
                ", duration: " + Util.getTimeSpanString(1000L * (long) duration);

    }
}
