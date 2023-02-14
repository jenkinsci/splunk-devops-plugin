package com.splunk.splunkjenkins.console;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.Iterator;

public class ConsoleNoteHandler {
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    }

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
     * parse first <a><a/> or <span></span>
     * @param xml
     * @throws SAXException
     * @throws XMLStreamException
     * @see org.jenkinsci.plugins.workflow.job.console.NewNodeConsoleNote
     * @see hudson.console.HyperlinkNote
     * @see org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApprovalNote
     */
    @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
    public void read(String xml) throws XMLStreamException {
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new StringReader(xml));
        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                String localParts = startElement.getName().getLocalPart();
                if ("a".equals(localParts) || "span".equals(localParts)) {
                    Iterator<Attribute> attrs = startElement.getAttributes();
                    while (attrs.hasNext()) {
                        Attribute attr = attrs.next();
                        String attrName=attr.getName().getLocalPart();
                        switch (attrName) {
                            case "href":
                                href = attr.getValue();
                                break;
                            case "nodeId":
                                nodeId = attr.getValue();
                                break;
                            case "startId":
                                startId = attr.getValue();
                                break;
                            case "enclosingId":
                                enclosingId = attr.getValue();
                                break;
                            case "label":
                                label = attr.getValue();
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                }
            }
        }
    }
}
