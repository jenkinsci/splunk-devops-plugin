package com.splunk.splunkjenkins.model;

import com.google.common.collect.Lists;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import org.jvnet.tiger_types.Types;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Adapter for extracting code coverage metrics from various coverage plugins.
 * Supports different coverage tools like JaCoCo, Clover, and Cobertura.
 *
 * @param <M> Coverage Action
 */
public abstract class CoverageMetricsAdapter<M extends HealthReportingAction> implements ExtensionPoint {
    /**
     * The target action type that this adapter handles
     */
    public final Class<M> targetType;
    static final String PERCENTAGE_SUFFIX = "_percentage";
    static final String TOTAL_SUFFIX = "_total";
    static final String COVERED_SUFFIX = "_covered";


    /**
     * Constructs a CoverageMetricsAdapter and determines the target action type
     */
    public CoverageMetricsAdapter() {
        Type type = Types.getBaseClass(getClass(), CoverageMetricsAdapter.class);
        if (type instanceof ParameterizedType)
            targetType = Types.erasure(Types.getTypeArgument(type, 0));
        else
            throw new IllegalStateException(getClass() + " uses the raw type for extending CoverageMetricsAdapter");

    }

    /**
     * Gets the coverage action from the build
     *
     * @param run the Jenkins build
     * @return the coverage action
     */
    public M getAction(Run run) {
        return run.getAction(targetType);
    }

    /**
     * Checks if this adapter is applicable to the given build
     *
     * @param build the Jenkins build
     * @return true if the adapter is applicable, false otherwise
     */
    public boolean isApplicable(Run build) {
        return getAction(build) != null;
    }

    /**
     * Gets coverage metrics from the first applicable adapter
     *
     * @param build the Jenkins build
     * @return a map of coverage metrics to values
     */
    @NonNull
    public static Map<Metric, Integer> getMetrics(Run build) {
        List<CoverageMetricsAdapter> adapters = ExtensionList.lookup(CoverageMetricsAdapter.class);
        for (CoverageMetricsAdapter adapter : adapters) {
            if (adapter.isApplicable(build)) {
                return adapter.getMetrics(adapter.getAction(build));
            }
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Gets coverage metrics from the coverage action
     *
     * @param coverageAction the coverage action
     * @return coverage metrics, key is metric, value is percentage
     */
    public abstract Map<Metric, Integer> getMetrics(M coverageAction);

    /**
     * Gets a detailed coverage report from the coverage action
     *
     * @param coverageAction the coverage action
     * @return coverage report, a list of coverage details
     */
    public abstract List<CoverageDetail> getReport(M coverageAction);

    /**
     * Gets a paginated coverage report from the first applicable adapter
     *
     * @param build Jenkins build
     * @param pageSize page size, <code>0</code> will disable pagination
     * @return coverage report with no more than <code>pageSize</code> items per page
     */
    public static List<List<CoverageDetail>> getReport(Run build, int pageSize) {
        List<CoverageMetricsAdapter> adapters = ExtensionList.lookup(CoverageMetricsAdapter.class);
        List<CoverageDetail> reports = new ArrayList<>();
        for (CoverageMetricsAdapter adapter : adapters) {
            if (adapter.isApplicable(build)) {
                reports.addAll(adapter.getReport(adapter.getAction(build)));
            }
        }
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }
        if (pageSize == 0 || reports.size() <= pageSize) {
            return Arrays.asList(reports);
        } else {
            return Lists.partition(reports, pageSize);
        }
    }

    /**
     * Enum representing different code coverage metrics
     */
    public enum Metric {
        /**
         * Package-level coverage metrics
         */
        PACKAGE("packages"),
        /**
         * File-level coverage metrics
         */
        FILE("files"),
        /**
         * Class-level coverage metrics
         */
        CLASS("classes"),
        /**
         * Method-level coverage metrics
         */
        METHOD("methods"),
        /**
         * Conditional/branch coverage metrics
         */
        CONDITIONAL("conditionals"),
        /**
         * Statement coverage metrics
         */
        STATEMENT("statements"),
        /**
         * Line coverage metrics
         */
        LINE("lines"),
        /**
         * Element coverage metrics
         */
        ELEMENT("elements"),
        /**
         * Complexity coverage metrics (for Emma/JaCoCo plugin)
         */
        COMPLEXITY("complexity"),
        /**
         * Branch coverage metrics
         */
        BRANCH("branches"),
        /**
         * Instruction coverage metrics
         */
        INSTRUCTION("instructions");


        private String description;

        Metric(String description) {
            this.description = description;
        }

        public String toString() {
            return description;
        }

        /**
         * Clover and Cobertura use different metrics name, try to align them using nearest value
         *
         * @param name Metrics name
         * @return enum if defined, otherwise null
         */
        public static Metric getMetric(String name) {
            for (Metric metric : values()) {
                String metricsName = metric.name();
                if (metricsName.equals(name) || name.startsWith(metricsName)) {
                    return metric;
                }
            }
            return null;
        }
    }

    /**
     * Used to indicate the level of coverage
     */
    public enum CoverageLevel {
        /**
         * Project-level coverage
         */
        PROJECT,
        /**
         * Package-level coverage
         */
        PACKAGE,
        /**
         * Class-level coverage
         */
        CLASS,
        /**
         * Method-level coverage
         */
        METHOD,
        /**
         * File-level coverage
         */
        FILE
    }

    /**
     * Data structure for holding detailed coverage information
     */
    public static class CoverageDetail {
        Map<String, Object> report = new HashMap<>();

        /**
         * Creates a CoverageDetail with the specified name
         *
         * @param name the coverage name
         */
        public CoverageDetail(String name) {
            report.put("name", name);
        }

        /**
         * Creates a CoverageDetail with the specified name and level
         *
         * @param name the coverage name
         * @param level the coverage level
         */
        public CoverageDetail(String name, CoverageLevel level) {
            report.put("name", name);
            report.put("cov_level", level.toString().toLowerCase());
        }

        /**
         * Gets the coverage report as a map
         *
         * @return the coverage report map
         */
        public Map<String, Object> getReport() {
            return report;
        }

        /**
         * Adds a metric value to the report
         *
         * @param metric the metric enum
         * @param value the metric value
         */
        public void add(Metric metric, int value) {
            report.put(metric.toString(), value);
        }

        /**
         * Adds a metric value to the report by name
         *
         * @param metric metric name, such as classes, methods
         * @param value  percentage value
         */
        public void add(String metric, int value) {
            Metric reportMetric = Metric.getMetric(metric);
            if (reportMetric != null) {
                report.put(reportMetric.toString(), value);
            } else {
                report.put(metric, value);
            }
        }

        /**
         * Adds all metrics from a sub-report to this report
         *
         * @param subReport the sub-report to add
         */
        public void putAll(Map<Metric, Integer> subReport) {
            for (Map.Entry<Metric, Integer> entry : subReport.entrySet()) {
                report.put(entry.getKey().toString(), entry.getValue());
            }
        }

    }
}
