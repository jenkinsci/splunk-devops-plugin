package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.console.SplunkTaskListenerFactory;
import com.splunk.splunkjenkins.model.LoggingJobExtractor;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Extension
public class PipelineRunSupport extends LoggingJobExtractor<WorkflowRun> {
    private static final Logger LOG = Logger.getLogger(PipelineRunSupport.class.getName());

    @Override
    public Map<String, Object> extract(WorkflowRun workflowRun, boolean jobCompleted) {
        Map<String, Object> info = new HashMap<String, Object>();
        if (jobCompleted) {
            FlowExecution execution = workflowRun.getExecution();
            if (execution != null) {
                List<Map<String, Object>> stages = PipelineStageCollector.collect(execution);
                LOG.log(Level.FINE, "found stages count {0}", stages.size());
                if (!stages.isEmpty()) {
                    info.put("stages", stages);
                }
            }
            SplunkTaskListenerFactory.removeCache(workflowRun);
        }
        return info;
    }
}
