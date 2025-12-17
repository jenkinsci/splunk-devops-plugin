package com.splunk.splunkjenkins;

import com.splunk.splunkjenkins.utils.SplunkLogService;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.splunk.splunkjenkins.Constants.TAG;
import static com.splunk.splunkjenkins.model.EventType.JENKINS_CONFIG;

public class WebPostAccessLogger implements Filter {
    private static final Logger LOG = Logger.getLogger(WebPostAccessLogger.class.getName());
    // Package-visible for testing
    static final Pattern FILTER_PATTERN = Pattern.compile("/(?:configSubmit|createSubmit|updateSubmit|script|doDelete)");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("splunk-filter loaded");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            loggerUserAction(servletRequest);
        } catch (Exception e) {
            //ignore;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void loggerUserAction(ServletRequest servletRequest) {
        if (SplunkJenkinsInstallation.get().isEventDisabled(JENKINS_CONFIG)) {
            return;
        }
        if (!(servletRequest instanceof HttpServletRequest)) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if (!"POST".equals(request.getMethod())) {
            return;
        }
        Authentication auth = Jenkins.getAuthentication();
        String path = request.getRequestURI();
        if (auth == null || path == null || !auth.isAuthenticated()) {
            return;
        }
        if (!FILTER_PATTERN.matcher(path).find()) {
            return;
        }
        Map<String, String> auditInfo = new HashMap();
        auditInfo.put("user", auth.getName());
        auditInfo.put("message", "POST " + request.getRequestURI());
        auditInfo.put(TAG, "audit_trail");
        SplunkLogService.getInstance().send(auditInfo, JENKINS_CONFIG, "web_access");
    }

    @Override
    public void destroy() {

    }
}
