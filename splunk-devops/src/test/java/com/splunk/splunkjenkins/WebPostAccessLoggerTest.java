package com.splunk.splunkjenkins;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link WebPostAccessLogger} pattern matching.
 * These tests verify that the audit trail filter correctly identifies
 * URLs that should be logged for security auditing purposes.
 */
public class WebPostAccessLoggerTest {

    // ==================== configSubmit tests ====================

    @Test
    public void testConfigSubmitMatches() {
        assertTrue("configSubmit should match", WebPostAccessLogger.FILTER_PATTERN.matcher("/job/test/configSubmit").find());
        assertTrue("configSubmit should match", WebPostAccessLogger.FILTER_PATTERN.matcher("/manage/configSubmit").find());
    }

    // ==================== createSubmit tests ====================

    @Test
    public void testCreateSubmitMatchesCredentials() {
        // Credential creation - the main use case this fix addresses
        assertTrue("createSubmit for credentials should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/manage/credentials/store/system/domain/_/createSubmit").find());
        assertTrue("createSubmit for folder credentials should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/job/folder/credentials/store/folder/domain/_/createSubmit").find());
    }

    @Test
    public void testCreateSubmitMatchesOtherResources() {
        // View creation
        assertTrue("createSubmit for views should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/view/All/createSubmit").find());
        // Generic createSubmit
        assertTrue("generic createSubmit should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/createSubmit").find());
    }

    // ==================== updateSubmit tests ====================

    @Test
    public void testUpdateSubmitMatches() {
        assertTrue("updateSubmit for credentials should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/manage/credentials/store/system/domain/_/credential/my-cred/updateSubmit").find());
        assertTrue("updateSubmit for job should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/job/test/updateSubmit").find());
    }

    // ==================== script tests ====================

    @Test
    public void testScriptMatches() {
        // Script console - security sensitive
        assertTrue("script console should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/script").find());
        assertTrue("scriptText should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/manage/scriptText").find());
        assertTrue("folder scriptText should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/job/folder/scriptText").find());
    }

    // ==================== doDelete tests ====================

    @Test
    public void testDoDeleteMatches() {
        assertTrue("doDelete for credentials should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/manage/credentials/store/system/domain/_/credential/my-cred/doDelete").find());
        assertTrue("doDelete for job should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/job/test/doDelete").find());
    }

    // ==================== Negative tests - should NOT match ====================

    @Test
    public void testRegularRequestsDoNotMatch() {
        // Regular API calls should not trigger audit
        assertFalse("API json should not match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/api/json").find());
        assertFalse("Job build should not match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/job/test/build").find());
        assertFalse("Job console should not match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/job/test/1/console").find());
        assertFalse("Credentials list should not match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/credentials/store/system/domain/_/").find());
    }

    @Test
    public void testPatternMatchesSuffixVariations() {
        // The pattern uses .find() which matches if /keyword appears as substring
        // This means suffixes after the keyword will still match
        assertTrue("configSubmittal contains /configSubmit - should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/configSubmittal").find());
        assertTrue("doDeleteAll contains /doDelete - should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/doDeleteAll").find());
        assertTrue("createSubmitForm contains /createSubmit - should match",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/createSubmitForm").find());
    }

    @Test
    public void testPatternDoesNotMatchPrefixVariations() {
        // Prefix before the keyword breaks the /keyword pattern
        assertFalse("myCreateSubmit - no /createSubmit pattern",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/myCreateSubmit").find());
        assertFalse("preConfigSubmit - no /configSubmit pattern",
                WebPostAccessLogger.FILTER_PATTERN.matcher("/preConfigSubmit").find());
    }
}
