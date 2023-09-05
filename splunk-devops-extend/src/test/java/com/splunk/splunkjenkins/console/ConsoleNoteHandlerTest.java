package com.splunk.splunkjenkins.console;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import hudson.tasks._maven.Maven3MojoNote;
import org.junit.Test;

public class ConsoleNoteHandlerTest {
    @Test
    public void testReadMavenNote() throws Exception {
        ConsoleNoteHandler handler = new ConsoleNoteHandler();
        handler.read("<b class=maven-mojo></b>");
        assertNull(handler.getHref());
        assertNull(handler.getNodeId());
        assertNull(handler.getEnclosingId());
        assertNull(handler.getStartId());
    }

    @Test
    public void testReadMavenWarningNote() throws Exception {
        ConsoleNoteHandler handler = new ConsoleNoteHandler();
        handler.read("<span class=warning-inline></span>");
        assertNull(handler.getHref());
        assertNull(handler.getNodeId());
        assertNull(handler.getEnclosingId());
        assertNull(handler.getStartId());
    }

    @Test
    public void testReadMavenErrorNote() throws Exception {
        ConsoleNoteHandler handler = new ConsoleNoteHandler();
        handler.read("<span class=error-inline></span>");
        assertNull(handler.getHref());
        assertNull(handler.getNodeId());
        assertNull(handler.getEnclosingId());
        assertNull(handler.getStartId());
    }
}