### 1.10.1 (February 15, 2023)
- allow user to hide splunk hyperlink, thanks to [StefanSpieker](https://github.com/StefanSpieker)
- update splunk hyperlink for [Splunk App for Jenkins](https://splunkbase.splunk.com/app/3332), thanks to [Michael Fitoussi](https://github.com/mifitous)

### 1.10.0 (July 4, 2022)
- SECURITY-2128 plugin logs unmasked credentials, thanks to [Pierson Yieh](https://github.com/pyieh)
- JENKINS-68775 pipeline build step is considered a stage in splunk plugin, thanks to [Guilherme Mota](https://github.com/guilhermemotadock)
- JENKINS-68440 null pointer exception in LabelMarkupText, thanks to [Kyle Cronin](https://github.com/cronik)

### 1.9.9 (January 8, 2022)
- JENKINS-67492 fix xml entity not declared error when parsing console node
### 1.9.8 (December 18, 2021)
- JENKINS-67295 ignore empty line
### 1.9.7 (August 18, 2021)
- JENKINS-66323 forward compatibility with upcoming guava version, thanks to [Tim Jacomb](https://github.com/timja)
### 1.9.6 (March 8, 2021)
- add more granular queue timing event for waiting, buildable, blocked events (e.g. type=dequeue_waiting OR type=dequeue_buildable OR type=dequeue_blocked ), thanks to [Reka Ajanthan](https://github.com/rekathiru)

### 1.9.5 (October 25, 2020) 
- hudson.util.spring.ClosureScript → org.kohsuke.stapler.jelly.groovy.GroovyClosureScript, thanks to [Jesse Glick](https://github.com/jglick)
- discard console log containing only whitespaces, thanks to [karjsim](https://github.com/karjsim)

### 1.9.4 (July 13, 2020) 
- fix JENKINS-62663 support remote TaskListener

### 1.9.3 (May 5, 2020) 
- disable jdk logger when console_log is disabled

### 1.9.2 (February 13, 2020) 
-   use system properties(http.proxyHost http.proxyPort http.nonProxyHosts) for configuration, thanks to [Mike Noseworthy](https://github.com/noseworthy)

### 1.9.1 (January 3, 2020) 
-   audit trail for http post submit (configSubmit, updateSubmit, doDelete)

### 1.9.0 (December 31, 2019)
-   update wording slave to agent for display
-   pipeline job: send all pipeline console logs if it is enabled globally, `sendSplunkConsoleLog` step is not needed any more
-   pipeline job: remove ansi color escape code
-   internal: migrate Wiki content to github 

### 1.8.2 (November 1, 2019) 
-   truncate long junit standard output/error before sending

### 1.8.1 (August 20, 2019) 

-   fix JENKINS-58866 Splunk plugin not sending json files

### 1.8.0 (August 14, 2019) 

-   fix groovy syntax validation security issue
-   update minimum jenkins version requirement to 2.60.3, script-security
    to 1.61

### 1.7.4 (June 14, 2019) 

-   fix disabled check issue for jenkins pipeline
    step sendSplunkConsoleLog

### 1.7.3 (May 28, 2019) 

-   Support RFC 6265 compliant cookie policy thanks to [Jonas Linde](https://github.com/krakan)
-   Add splunkins.verifySSL property to toggle SSL certification
    verification on or off

### 1.7.2 (May 20, 2019) 

-   JENKINS-57410 connection leak after clicking 'Test Connection'
    button
-   respect TestNG is-config settings (beforeClass, beforeMethod) for
    counting test methods
-   add splunkins.allowConsoleLogPattern and
    splunkins.ignoreConfigChangePattern

### 1.7.1 (Dec 7, 2018)  

-   truncate single line text at 100000 (a sign of garbage data) to get
    in align with splunk source type text:jenkins, it can be adjusted
    via splunkins.lineTruncate system property

-   add user authenticated log information

### 1.7.0 (August 20, 2018)  

-   support multiple http event collector(HEC) hosts which are separated
    by comma
-   optimize event congestion handling
-   allow garbage collector to release unsent log under memory demand,
    to prevent OOM
-   allow user to adjust log queue size via
    -Dcom.splunk.splunkjenkins.utils.SplunkLogService.queueSize=x
-   add LogConsumer thread alive check
-   prefer tls 1.2

  
<details>
 <summary>Click to view older version change log...</summary>
  

### 1.6.3 (Dec 1, 2017)  

-   fix configuration migration issue for versions prior to 1.5.0

### 1.6.2 (Nov 28, 2017)  

-   defer LogHandler hook registration
-   add covered number and total number in addition to percentage for
    code coverage (index=jenkins event\_tag=coverage)

### 1.6.1 (Oct 15, 2017)  

-   remove restricted computer.getDisplayExecutors api call
-   add splunkins.buffer property which can be added to jenkins start up
    parameter (such as -Dsplunkins.buffer=4096) to adjust console log
    buffer

### 1.6.0 (August 15, 2017)  

-   add splunkins.getJunitReport(int pageSize, List\<String\>
    ignoredTestResultActions = null) which allow user to ignore specific
    test result formats

-   unify junit test results with xunit and cucumber test results

-   defer updateCache operation to JOB\_LOADED phase

-   send JVM memory pool usage,  can be searched via

    ``` syntaxhighlighter-pre
    index="jenkins_statistics" event_tag=jvm_memory
    ```

### 1.5.3 (July 25, 2017)  

-   fix SECURITY-479 (Arbitrary code execution vulnerability in rare
    circumstances)

### 1.5.2 (May 22, 2017)  

-   convert Float.NaN or Double.NaN to null
-   make sure workspace exists before sending files, thanks
    to [ctran](https://github.com/ctran)
-   fix Log type and allow verbose logging

### 1.5.1 (April 24, 2017)  

-   Fix log congestion issue when slave launcher generated verbose logs
    during Jenkins restart phase

### 1.5.0 (April 16, 2017)  

-   Use SecureGroovyScript to address security issues mentioned
    on <https://jenkins.io/security/advisory/2017-04-10/> . If you hit
    errors like   

    ``` console-output
    org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException: script not yet approved for use
    ```
      
    you need go to "Manage Jenkins -\> In-process Script Approval"
     (JENKINS\_URL/scriptApproval) page to review the script and approve it.

-   Add support for [jacoco-plugin](https://wiki.jenkins-ci.org/display/JENKINS/JaCoCo+Plugin)

### 1.4.3 (Mar 3, 2017)

-   Do not extract scm info for job start event, since the info maybe
    obtained from last build, not current build
-   Add null check for Node
-   Use job's full name instead of url to get compliance with
    env.JOB\_NAME

### 1.4.2 (Jan 4, 2017)

-   Improve retry handling when Splunk is busy

### 1.4.1 (Dec 19, 2016)

-   Send separate event for running jobs, used for long running job
    alert

### 1.4 (Dec 19, 2016)

-   Support Coverage Report generated by [Clover plugin](https://wiki.jenkins-ci.org/display/JENKINS/Clover+Plugin) and [Cobertura
    plugin](https://wiki.jenkins-ci.org/display/JENKINS/Cobertura+Plugin)
-   Rewrite the metadata configuration page to improve the readability.
-   Shaded org.apache.http package to avoid conflicts with other plugin
    which is using an older version
-   Improve http posting performance by using Gzip.

### 1.3.1 (Oct 27, 2016)

-   Masked Password parameter, send \*\*\*
-   Do not send whole Environment variable list, only send build
    parameters.
-   Added BuildInfoArchiver to send historical data

### 1.3 (Oct 19, 2016)

-   Support Test Report generated by [cucumber-testresult-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Cucumber+Test+Result+Plugin)
-   FIXED TestNG Summary Display issue

### 1.2 (Oct 16, 2016)

-   Support Test Report generated by [TestNG plugin](https://wiki.jenkins-ci.org/display/JENKINS/testng-plugin)

### 1.1 (Oct 14, 2016)

-   Simplify metadata configuration
-   Fixed No signature of method: static
    com.splunk.splunkjenkins.utils.LogEventHelper.sendFiles() is
    applicable for argument types:
    (org.jenkinsci.plugins.workflow.job.WorkflowRun ...

### 1.0 (Oct 8, 2016)

-   Initial release

</details>