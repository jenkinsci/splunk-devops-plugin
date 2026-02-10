package com.splunk.splunkjenkins.console;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ConsoleNoteHandler {

    private String href;
    private String nodeId;
    private String startId;
    private String enclosingId;
    private String label;

    public String getHref() {
        return href;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getStartId() {
        return startId;
    }

    public String getEnclosingId() {
        return enclosingId;
    }

    public String getLabel() {
        return label;
    }

    /**
     * parse first html tag 'a' or 'span' with nodeId attribute
     *
     * @param tag
     * @see org.jenkinsci.plugins.workflow.job.console.NewNodeConsoleNote
     * @see hudson.console.HyperlinkNote
     * @see org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApprovalNote
     */
    @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
    public void read(String tag) {
        Document doc = Jsoup.parse(tag);
        Element nodeEle = doc.getElementsByTag("a").first();
        if (nodeEle == null) {
            nodeEle = doc.getElementsByTag("span").first();
        }
        if (nodeEle == null) {
            return;
        }
        if (nodeEle.attributesSize() == 0) {
            return;
        }
        Attributes attrs = nodeEle.attributes();
        href = getAttribute(attrs, "href");
        nodeId = getAttribute(attrs, "nodeid");
        startId = getAttribute(attrs, "startId");
        enclosingId = getAttribute(attrs, "enclosingId");
        label = getAttribute(attrs, "label");
    }


    private String getAttribute(Attributes attrs, String attrKey) {
        String key = attrKey.toLowerCase();
        // In the HTML syntax, attribute names may be written with any mix of lower- and uppercase letters that, when converted to all-lowercase, matches the attribute's name; attribute names are case-insensitive.
        if (attrs.hasKey(key)) {
            return attrs.get(key);
        } else {
            return null;
        }
    }
}
