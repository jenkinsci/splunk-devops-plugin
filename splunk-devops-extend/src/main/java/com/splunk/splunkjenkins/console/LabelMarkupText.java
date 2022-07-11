package com.splunk.splunkjenkins.console;

import com.splunk.splunkjenkins.utils.LogEventHelper;
import hudson.MarkupText;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.startsWith;

public class LabelMarkupText extends MarkupText {
    private static final Boolean isDisabled = Boolean.getBoolean(LogEventHelper.class.getName() + ".disableLabelMarkup");
    private static final String PARALLEL_BRANCH_LABEL = "Branch: ";
    private static final Logger LOG = Logger.getLogger(LabelMarkupText.class.getName());
    private static final String PARALLEL_LABEL = "parallel_label";
    //remembered enclosing label
    private String encloseLabel = null;
    private SoftReference<Map<String, String>> encloseLabelRef = new SoftReference<>(new HashMap<>());
    private String annotation = null;

    public LabelMarkupText() {
        super("");
    }

    @Override
    public void addMarkup(int startPos, int endPos, String startTag, String endTag) {
        if (isDisabled) {
            return;
        }
        parseTagLabel(startTag + endTag);
    }

    /**
     * @see org.jenkinsci.plugins.workflow.job.console.NewNodeConsoleNote
     * @see hudson.console.HyperlinkNote
     * @see org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApprovalNote
     */
    private void parseTagLabel(String tag) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(tag);
        }
        annotation = "";
        try {
            ConsoleNoteHandler handler = new ConsoleNoteHandler();
            handler.read(tag);
            String href = handler.getHref();
            if (isNotEmpty(href)) {
                // anchor markup
                annotation = "href=" + href;
                return;
            }
            String nodeId = handler.getNodeId();
            // NewNodeConsoleNote
            if (isNotEmpty(nodeId)) {
                // encloseLabelRef lost in gc 
                Map<String, String> encloseLabels = encloseLabelRef.get();
                if (encloseLabels == null) {
                    return;
                }
                if (handler.getStartId() != null) {
                    // BlockEndNode or BlockStartNode
                    encloseLabel = null;
                    String label = handler.getLabel();
                    if (startsWith(label, PARALLEL_BRANCH_LABEL)) {
                        encloseLabels.put(nodeId, label.substring(PARALLEL_BRANCH_LABEL.length()));
                    }
                } else {
                    String enclosingId = handler.getEnclosingId();
                    if (isNotEmpty(enclosingId)) {
                        //pipeline step  (not block level)
                        String nodeLabel = encloseLabels.get(enclosingId);
                        if (nodeLabel != null) {
                            // update the label
                            encloseLabels.put(nodeId, nodeLabel);
                            encloseLabel = PARALLEL_LABEL + "=\"" + StringEscapeUtils.escapeJava(nodeLabel) + "\"";
                        } else {
                            encloseLabel = null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warning("failed to parse html console note " + tag + " exception:" + e);
        }
    }

    public void write(OutputStream out) throws IOException {
        if (isNotEmpty(annotation)) {
            out.write(annotation.getBytes(UTF_8));
            out.write(' ');
            //clear annotation
            annotation = null;
        }
    }

    public void writePreviousLabel(OutputStream out) throws IOException {
        if (isNotEmpty(encloseLabel)) {
            out.write(encloseLabel.getBytes(UTF_8));
            out.write(' ');
        }
    }
}
