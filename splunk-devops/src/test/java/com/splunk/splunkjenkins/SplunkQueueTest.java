package com.splunk.splunkjenkins;

import hudson.ExtensionList;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import org.junit.Test;
import org.jvnet.hudson.test.TestExtension;
import org.junit.Before;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.splunk.splunkjenkins.SplunkJenkinsInstallation;
import com.splunk.splunkjenkins.utils.SplunkQueue;
import com.splunk.splunkjenkins.utils.DefaultSplunkQueue;
import com.splunk.splunkjenkins.model.EventType;
import com.splunk.splunkjenkins.model.EventRecord;
import com.splunk.splunkjenkins.utils.SplunkLogService;

// BaseTest had JenkinsRules that allows test to fire up a Jenkins instance
public class SplunkQueueTest extends BaseTest {
    private static final Logger LOG = Logger.getLogger(SplunkQueueTest.class.getName());

    private static StaplerRequest mockRequest;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockRequest = mock(StaplerRequest.class);
        given(mockRequest.bindJSON(any(Class.class), any(JSONObject.class))).willCallRealMethod();
    }

    @Test
    public void testQueueMethods() throws Exception {
        DefaultSplunkQueue testQueue = new DefaultSplunkQueue();

        String line = "127.0.0.1 - admin \"GET /en-US/ HTTP/1.1\"";
        EventRecord record = new EventRecord(line, EventType.LOG);
        boolean added = testQueue.enqueue(record);
        assertTrue(added);
        assertEquals(1, testQueue.size());
    }

    @Test
    public void testDefaultExtension() throws Exception {
        // Check that expected extensions installed
        ExtensionList<SplunkQueue> splunkQueueList = ExtensionList.lookup(SplunkQueue.class);
        assertEquals(1, splunkQueueList.size());

        ExtensionList<DefaultSplunkQueue> specificQueueList = ExtensionList.lookup(DefaultSplunkQueue.class);
        assertEquals(1, specificQueueList.size());

        // Check that queue types are available for selection
        SplunkJenkinsInstallation globalConfig = SplunkJenkinsInstallation.get();
        ListBoxModel configOptions = globalConfig.doFillQueueTypeItems();
        assertEquals(1, configOptions.size());

        String queueRegex = "\\b" + DefaultSplunkQueue.class.getSimpleName() + "\\b";
        Pattern queuePattern = Pattern.compile(queueRegex);
        Matcher queueMatch = queuePattern.matcher(configOptions.toString());
        assertTrue(queueMatch.find());
        
        // Ensure default queue is as expected
        SplunkLogService logService = SplunkLogService.getInstance();
        assertTrue(logService.getQueue() instanceof DefaultSplunkQueue);
        String line = "127.0.0.1 - admin \"GET /en-US/ HTTP/1.1\"";
        EventRecord record = new EventRecord(line, EventType.LOG);
        boolean added = logService.getQueue().enqueue(record);
        assertTrue(added);
    }

    @Test
    public void testAddOneQueueExtension() throws Exception {
        // Check that expected extensions installed
        ExtensionList<SplunkQueue> splunkQueueList = ExtensionList.lookup(SplunkQueue.class);
        assertEquals(2, splunkQueueList.size());

        ExtensionList<TestQueue1> specificQueueList = ExtensionList.lookup(TestQueue1.class);
        assertEquals(1, specificQueueList.size());

        // Check that queue types are available for selection
        SplunkJenkinsInstallation globalConfig = SplunkJenkinsInstallation.get();
        ListBoxModel configOptions = globalConfig.doFillQueueTypeItems();
        assertEquals(2, configOptions.size());
        
        String queueRegex1 = "\\b" + DefaultSplunkQueue.class.getSimpleName() + "\\b";
        Pattern queuePattern1 = Pattern.compile(queueRegex1);
        Matcher queueMatch1 = queuePattern1.matcher(configOptions.toString());
        assertTrue(queueMatch1.find());
        String queueRegex2 = "\\b" + TestQueue1.class.getSimpleName() + "\\b";
        Pattern queuePattern2 = Pattern.compile(queueRegex2);
        Matcher queueMatch2 = queuePattern1.matcher(configOptions.toString());
        assertTrue(queueMatch2.find());

        // Get payload for dropdown option value for new extension
        JSONObject testJsonObject = createConfigBinding(TestQueue1.class, configOptions);

        // Update configuration
        boolean configureResult = globalConfig.configure(mockRequest, testJsonObject);
        assertTrue(configureResult);
        assertTrue(globalConfig.isValid());

        // Ask SplunkLogService what queue type is being used
        SplunkLogService logService = SplunkLogService.getInstance();
        assertTrue(logService.getQueue() instanceof TestQueue1);

        // Ensure new queue is working
        EventRecord testRecord = logService.getQueue().take();
        assertTrue(logService.getQueue().enqueue(testRecord));
        assertTrue(logService.getQueue().offer(testRecord));
        assertEquals(5, logService.getQueueSize());
    }

    @Test
    public void testAddMultipleQueueExtensions() throws Exception {
        // Check that expected extensions installed
        ExtensionList<SplunkQueue> splunkQueueList = ExtensionList.lookup(SplunkQueue.class);
        assertEquals(3, splunkQueueList.size());
        assertNotNull(splunkQueueList);

        ExtensionList<TestQueue2> specificQueueList = ExtensionList.lookup(TestQueue2.class);
        assertEquals(1, specificQueueList.size());

        // Check that queue types are available for selection
        SplunkJenkinsInstallation globalConfig = SplunkJenkinsInstallation.get();
        ListBoxModel configOptions = globalConfig.doFillQueueTypeItems();
        assertEquals(3, configOptions.size());

        String queueRegex1 = "\\b" + DefaultSplunkQueue.class.getSimpleName() + "\\b";
        Pattern queuePattern1 = Pattern.compile(queueRegex1);
        Matcher queueMatch1 = queuePattern1.matcher(configOptions.toString());
        assertTrue(queueMatch1.find());
        String queueRegex2 = "\\b" + TestQueue1.class.getSimpleName() + "\\b";
        Pattern queuePattern2 = Pattern.compile(queueRegex2);
        Matcher queueMatch2 = queuePattern1.matcher(configOptions.toString());
        assertTrue(queueMatch2.find());
        String queueRegex3 = "\\b" + TestQueue2.class.getSimpleName() + "\\b";
        Pattern queuePattern3 = Pattern.compile(queueRegex3);
        Matcher queueMatch3 = queuePattern1.matcher(configOptions.toString());
        assertTrue(queueMatch3.find());

        // Get payload for dropdown option value for new extension
        JSONObject testJsonObject = createConfigBinding(TestQueue2.class, configOptions);

        // Update configuration
        boolean configureResult = globalConfig.configure(mockRequest, testJsonObject);
        assertTrue(configureResult);
        assertTrue(globalConfig.isValid());

        // Ask SplunkLogService what queue type is being used
        SplunkLogService logService = SplunkLogService.getInstance();
        assertTrue(logService.getQueue() instanceof TestQueue2);

        // Ensure new queue is working
        EventRecord testRecord = logService.getQueue().take();
        assertTrue(logService.getQueue().enqueue(testRecord));
        assertTrue(logService.getQueue().offer(testRecord));
        assertEquals(5, logService.getQueueSize());
    }

    private JSONObject createConfigBinding(Class queueClass, ListBoxModel configOptions){
        String selectOptionName = queueClass.getSimpleName();
        String selectOptionValue = "";
        for (ListBoxModel.Option dropDownChoice : configOptions){
            if(dropDownChoice.name.equals(selectOptionName)){
                selectOptionValue = dropDownChoice.value;
                LOG.info("Choosing dropdown option: " + dropDownChoice.name + "=" + selectOptionValue);
                break;
            }
        }

        // Select queue type
        JSONObject testJsonObject = new JSONObject();
        testJsonObject.put("enabled", true);
        testJsonObject.put("queueType", selectOptionValue);

        return testJsonObject;
    }

    @TestExtension({"testAddOneQueueExtension", "testAddMultipleQueueExtensions"})
    public static class TestQueue1 implements SplunkQueue {
        private static final Logger LOG = Logger.getLogger(TestQueue1.class.getName());
        private int capacity = 5;

        public TestQueue1(){
            LOG.info("TestQueue1");
        }

        public TestQueue1(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public void clear(){
            LOG.info("TestQueue1 | clear");
        }

        @Override
        public boolean enqueue(EventRecord record){
            LOG.info("TestQueue1 | enqueue");
            return true;
        }

        @Override
        public boolean offer(EventRecord record){
            LOG.info("TestQueue1 | offer");
            return true;
        }

        @Override
        public int size(){
            LOG.info("TestQueue1 | size");
            return capacity;
        }

        @Override
        public EventRecord take() throws InterruptedException{
            LOG.info("TestQueue1 | take");
            String line = "127.0.0.1 - admin \"GET /en-US/ HTTP/1.1\"";
            EventRecord record = new EventRecord(line, EventType.LOG);
            return record;
        }
    }

    @TestExtension("testAddMultipleQueueExtensions")
    public static class TestQueue2 extends TestQueue1 {
        private static final Logger LOG = Logger.getLogger(TestQueue2.class.getName());
        private int capacity = 10;
        
        public TestQueue2(){
            super(10);
            LOG.info("TestQueue2");
        }
    }
}
