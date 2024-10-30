package com.splunk.splunkjenkins.console;

import junit.framework.TestCase;

public class ConsoleNoteHandlerTest extends TestCase {

    public void testRead() {
        String tag = "<span class=warning-inline label=test nodeId=testId ></span>";
        ConsoleNoteHandler handler = new ConsoleNoteHandler();
        handler.read(tag);
        assertEquals("test", handler.getLabel());
        assertEquals("testId", handler.getNodeId());
    }
}