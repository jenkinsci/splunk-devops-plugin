package com.splunk.splunkjenkins.model;

import hudson.Extension;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResultAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for JUnit test results.
 * Converts JUnit test results to Splunk format for analysis.
 */
@Extension(optional = true)
public class JunitResultAdapter extends AbstractTestResultAdapter<TestResultAction> {
    /**
     * {@inheritDoc}
     *
     * Gets the test results from a JUnit test result action
     */
    @Override
    public List<TestCaseResult> getTestResult(TestResultAction resultAction) {
        List<TestCaseResult> caseResults = new ArrayList<>();
        hudson.tasks.junit.TestResult result = resultAction.getResult();
        for (SuiteResult suite : result.getSuites()) {
            for (CaseResult testCase : suite.getCases()) {
                caseResults.add(convert(testCase, suite.getName()));
            }
        }
        return caseResults;
    }

    /**
     * Converts a JUnit case result to a unified test case result
     *
     * @param methodResult the JUnit method result
     * @param suiteName the test suite name
     * @return the unified test case result
     */
    private TestCaseResult convert(CaseResult methodResult, String suiteName) {
        String buildUrl = "";
        if (methodResult.getRun() != null) {
            buildUrl = methodResult.getRun().getUrl();
        }
        TestCaseResult testCaseResult = new TestCaseResult();
        testCaseResult.setTestName(methodResult.getName());
        testCaseResult.setUniqueName(methodResult.getFullName());
        testCaseResult.setDuration(methodResult.getDuration());
        testCaseResult.setClassName(methodResult.getClassName());
        testCaseResult.setErrorDetails(methodResult.getErrorDetails());
        testCaseResult.setErrorStackTrace(methodResult.getErrorStackTrace());
        testCaseResult.setSkippedMessage(methodResult.getSkippedMessage());
        testCaseResult.setFailedSince(methodResult.getFailedSince());
        testCaseResult.setStderr(trimToLimit(methodResult.getStderr(), methodResult.getFullName(), buildUrl));
        testCaseResult.setStdout(trimToLimit(methodResult.getStdout(), methodResult.getFullName(), buildUrl));
        testCaseResult.setGroupName(suiteName);
        TestStatus status = TestStatus.SKIPPED;
        if (!methodResult.isSkipped()) {
            status = methodResult.isPassed() ? TestStatus.PASSED : TestStatus.FAILURE;
        }
        testCaseResult.setStatus(status);
        return testCaseResult;
    }
}
