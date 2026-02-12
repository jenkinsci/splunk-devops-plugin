package com.splunk.splunkjenkins.model;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Run;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts build data from Jenkins jobs for Splunk logging.
 */
public abstract class LoggingJobExtractor<R extends Run> implements ExtensionPoint {
    /**
     * The target Run type that this extractor handles
     */
    public final Class<R> targetType;

    /**
     * Constructs a LoggingJobExtractor and determines the target Run type
     */
    public LoggingJobExtractor() {
        Type type = Types.getBaseClass(getClass(), LoggingJobExtractor.class);
        if (type instanceof ParameterizedType)
            targetType = Types.erasure(Types.getTypeArgument(type, 0));
        else
            throw new IllegalStateException(getClass() + " uses the raw type for extending LoggingJobExtractor");
    }

    /**
     * Extracts build data from a Jenkins run for Splunk logging
     *
     * @param r the Jenkins build run
     * @param completed whether the build is completed
     * @return extracted data as a map
     */
    public abstract Map<String, Object> extract(R r, boolean completed);

    /**
     * <p>all.</p>
     *
     * @return Returns all the registered {@link LoggingJobExtractor}s
     */
    public static ExtensionList<LoggingJobExtractor> all() {
        return ExtensionList.lookup(LoggingJobExtractor.class);
    }

    /**
     * Finds all LoggingJobExtractor instances that can apply to the given run
     *
     * @param run the Jenkins build run
     * @return list of applicable extractors
     */
    public static List<LoggingJobExtractor> canApply(Run run) {
        List<LoggingJobExtractor> extensions = new ArrayList<>();
        for (LoggingJobExtractor extendListener : LoggingJobExtractor.all()) {
            if (extendListener.targetType.isInstance(run)) {
                extensions.add(extendListener);
            }
        }
        return extensions;
    }
}
