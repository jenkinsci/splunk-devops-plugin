/*
 * The MIT License
 *
 * Copyright 2023 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.model.EventType;
import com.splunk.splunkjenkins.model.MetaDataConfigItem;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public final class SplunkJenkinsInstallationTest {

    @Rule
    public final JenkinsRule r = new JenkinsRule();

    @Test
    public void reload() throws Exception {
        SplunkJenkinsInstallation cfg = SplunkJenkinsInstallation.get();
        cfg.setMetadataItemSet(Collections.singleton(new MetaDataConfigItem(EventType.BUILD_EVENT.toString(), "index", "value000")));
        cfg.save();
        File xmlFile = new File(Jenkins.get().getRootDir(), cfg.getId() + ".xml");
        String xml = FileUtils.readFileToString(xmlFile, StandardCharsets.UTF_8);
        assertThat(xml, containsString("value000"));
        xml = xml.replace("value000", "value999");
        FileUtils.writeStringToFile(xmlFile, xml, StandardCharsets.UTF_8);
        cfg.load();
        Set<MetaDataConfigItem> metadataItemSet = cfg.getMetadataItemSet();
        assertThat(metadataItemSet, hasSize(1));
        assertThat(metadataItemSet.iterator().next().getValue(), is("value999"));
    }

}
