Splunk for Jenkins
---------

To Install Develop Version
----
 - clone the repo
 - `mvn package`
 -  mvn will generate `splunk-devops/target/splunk-devops.hpi` which you can install into Jenkins by the web interface or just put it in the `JENKINS_HOME/plugins` folder


To Setup
----
### Configure plugin

 - Go to https://jenkins-url/configure
 - Enter Hostname, Port, and Token
 - Enable RawEvent support if you are using Splunk version 6.3.1511 or later
 - Click "Test Connection" to verify the config
 - Enable it and Save
 
   ![Screenshot](doc/images/splunk_for_jenkins_config_basic.png)

### Customize Job Data Sent to Splunk

- In the advance configure section, you can customize the post data using groovy DSL
- ``send(Object message)`` will send the information to splunk
- ``AbstractBuild build``, ``Map env`` can be used directly. Variable env is a Map of Environment variables, build is hudson.model.AbstractBuild
- `getBuildEvent()` will return metadata about the build, such as build result, build URL, user who triggered the build
- `getJunitReport(int pageSize)` will return a list of test results, which contains total, passes, failures, skips, time and testcase of type List<hudson.tasks.junit.CaseResult>
- `getJunitReport()` is an alias of `getJunitReport(Integer.MAX_VALUE)[0]`
- sendCoverageReport(pageSize)  send coverage report, with pagination support
- sendTestReport(pageSize)  send Test report, with pagination support
- `archive(String includes, String excludes, boolean uploadFromSlave, String fileSizeLimit)` send log file to splunk
- `archive(String includes)` is an alias of `archive(includes, null, false, "")`
- `getAction(Class type)` is an alias of ` build.getAction(type)`
- `getActionByClassName(String className)` same as `getAction(Class type)` but no need to import the class before use
- `hasPublisherName(String className)` check whether the publisher is configured for the build (applied to AbstractBuild only)
- Here is the default settings for post job data processing

```groovy
//send job metadata and junit reports with page size set to 50 (each event contains max 50 test cases)
splunkins.sendTestReport(50)
//send coverage, each event contains max 50 class metrics
splunkins.sendCoverageReport(50)
//send all logs from workspace to splunk, with each file size limits to 10MB
splunkins.archive("**/*.log", null, false, "10MB")

```

Contributing to the plugin
----
-   clone the repo and update code
-   start splunk, you can get a free trail version from
    [Splunk](https://splunk.com/)
-   `mvn clean verify -Dsplunk-host=localhost -Dsplunk-username=admin -Dsplunk-passwd=changeme`
    to run tests using local splunk instance.
-   send pull requests

Documentation
----
See the [documentation](doc/splunk-devops-usage.md)
