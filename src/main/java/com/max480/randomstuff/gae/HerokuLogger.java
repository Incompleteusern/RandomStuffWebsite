package com.max480.randomstuff.gae;

import com.github.palindromicity.syslog.SyslogParser;
import com.github.palindromicity.syslog.SyslogParserBuilder;
import com.github.palindromicity.syslog.SyslogSpecification;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@WebServlet(name = "HerokuLogger", urlPatterns = {"/heroku-logs"})
public class HerokuLogger extends HttpServlet {
    private final Logger logger = Logger.getLogger("HerokuLogger");
    private final Logging logging = LoggingOptions.getDefaultInstance().getService();
    private final SyslogParser parser = new SyslogParserBuilder().forSpecification(SyslogSpecification.HEROKU_HTTPS_LOG_DRAIN).build();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!Constants.LOGGING_EXPECTED_AUTH_HEADER.equals(request.getHeader("Authorization"))) {
            response.setStatus(401);
        } else {
            try (Reader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
                List<LogEntry> logs = new ArrayList<>();
                parser.parseLines(reader, (map) -> {
                    logs.add(LogEntry.newBuilder(Payload.JsonPayload.of(map))
                            .setSeverity(Severity.DEFAULT)
                            .setLogName("heroku-logs")
                            .setResource(MonitoredResource.newBuilder("global").build())
                            .build());
                }, (line, throwable) -> {
                    logger.severe("Could not parse log line: " + line);
                    throwable.printStackTrace();
                });
                logging.write(logs);
            }
        }
    }
}
