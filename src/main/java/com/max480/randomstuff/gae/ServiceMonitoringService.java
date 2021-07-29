package com.max480.randomstuff.gae;

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.common.collect.ImmutableMap;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Timestamp;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@WebServlet(name = "ServiceMonitoring", urlPatterns = {"/service-monitoring"})
public class ServiceMonitoringService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!("key=" + Constants.SERVICE_MONITORING_SECRET).equals(request.getQueryString())) {
            response.setStatus(403);
            return;
        }

        try (MetricServiceClient client = MetricServiceClient.create()) {
            response.getWriter().write(new Yaml().dump(ImmutableMap.of(
                    "uptimePercent", getUptimes(client),
                    "responseCountPerCode", getResponseCount(client)
            )));
        }
    }

    private Map<String, Long> getResponseCount(MetricServiceClient client) {
        final MetricServiceClient.ListTimeSeriesPagedResponse timeSeries = client.listTimeSeries(ListTimeSeriesRequest.newBuilder()
                .setName("projects/max480-random-stuff")
                .setFilter("metric.type = \"appengine.googleapis.com/http/server/response_count\"")
                .setInterval(TimeInterval.newBuilder()
                        .setStartTime(Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000 - 86400)
                                .build())
                        .setEndTime(Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000)
                                .build())
                        .build())
                .build());

        Map<String, Long> countPerResponseCode = new TreeMap<>();
        for (TimeSeries series : timeSeries.iterateAll()) {
            long count = 0;
            for (Point p : series.getPointsList()) {
                count += p.getValue().getInt64Value();
            }

            String responseCode = series.getMetric().getLabelsOrThrow("response_code");
            count += countPerResponseCode.getOrDefault(responseCode, 0L);
            countPerResponseCode.put(responseCode, count);
        }
        return countPerResponseCode;
    }

    private Map<String, Double> getUptimes(MetricServiceClient client) {
        final MetricServiceClient.ListTimeSeriesPagedResponse timeSeries = client.listTimeSeries(ListTimeSeriesRequest.newBuilder()
                .setName("projects/max480-random-stuff")
                .setFilter("metric.type = \"monitoring.googleapis.com/uptime_check/check_passed\"")
                .setInterval(TimeInterval.newBuilder()
                        .setStartTime(Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000 - 86400)
                                .build())
                        .setEndTime(Timestamp.newBuilder()
                                .setSeconds(System.currentTimeMillis() / 1000)
                                .build())
                        .build())
                .build());

        long upForWebsite = 0;
        long totalForWebsite = 0;
        long upForBot = 0;
        long totalForBot = 0;

        for (TimeSeries series : timeSeries.iterateAll()) {
            boolean isWebsite;
            if (series.getMetric().getLabelsOrThrow("check_id").equals("bot-healthcheck")) {
                isWebsite = false;
            } else if (series.getMetric().getLabelsOrThrow("check_id").equals("website-healthcheck")) {
                isWebsite = true;
            } else {
                throw new RuntimeException("Encountered bad check_id!" + series.getMetric().getLabelsOrThrow("check_id"));
            }

            for (Point p : series.getPointsList()) {
                if (isWebsite) {
                    totalForWebsite++;
                    if (p.getValue().getBoolValue()) {
                        upForWebsite++;
                    }
                } else {
                    totalForBot++;
                    if (p.getValue().getBoolValue()) {
                        upForBot++;
                    }
                }
            }
        }
        return ImmutableMap.of(
                "website", (double) upForWebsite / totalForWebsite * 100.0,
                "bot", (double) upForBot / totalForBot * 100.0
        );
    }
}
