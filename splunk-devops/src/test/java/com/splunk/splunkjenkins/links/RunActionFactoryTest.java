package com.splunk.splunkjenkins.links;

import org.junit.Rule;
import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class RunActionFactoryTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @LocalData
    @Test
    public void verifyTestResultLink() throws ExecutionException, InterruptedException {
        FreeStyleProject project = (FreeStyleProject) j.getInstance().getItem("testresult_job1");
        Run run = project.scheduleBuild2(0).get();
        RunActionFactory factory = new RunActionFactory();
        Collection actions = factory.createFor(run);
        assertTrue("should have a link", !actions.isEmpty());
        LinkSplunkAction link = (LinkSplunkAction) actions.iterator().next();
        String url = link.getUrlName();
        assertTrue(url + " should have testAnalysis", url.contains("testAnalysis"));

    }
}