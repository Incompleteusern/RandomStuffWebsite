package com.max480.randomstuff.gae;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This follows the progress of a font generation, or of a mod structure verification, and shows the result.
 */
@WebServlet(name = "TaskTrackerService", urlPatterns = {"/celeste/task-tracker/font-generate/*", "/celeste/task-tracker/mod-structure-verify/*"})
@MultipartConfig
public class TaskTrackerService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("TaskTrackerService");
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    private final Pattern trackerPageUrlPattern = Pattern.compile("^/celeste/task-tracker/([a-z-]+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/?$");
    private final Pattern downloadPageUrlPattern = Pattern.compile("^/celeste/task-tracker/([a-z-]+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/download/([0-9]+)/?$");
    private final Pattern modStructureVerifierHelpPattern = Pattern.compile("^Click here for more help: https://max480-random-stuff.appspot.com/celeste/mod-structure-verifier-help\\?([A-Za-z0-9&;=]+)$");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean regexMatch = false;

        // initialize all status flags to false by default.
        request.setAttribute("taskNotFound", false);
        request.setAttribute("taskOngoing", false);
        request.setAttribute("fileNotFound", false);

        // 1/ tracking page: /celeste/task-tracker/[type]/[id]
        Matcher trackerPageUrlMatch = trackerPageUrlPattern.matcher(request.getRequestURI());
        if (trackerPageUrlMatch.matches()) {
            regexMatch = true;

            String type = trackerPageUrlMatch.group(1);
            String id = trackerPageUrlMatch.group(2);
            request.setAttribute("type", type);
            request.setAttribute("id", id);

            String timestampFile = type + "-" + id + "-timestamp.txt";
            if (!fileExists(timestampFile)) {
                // if this file does not exist, it means the task was never created in the first place.
                logger.warning(timestampFile + " does not exist => task not found!");
                request.setAttribute("taskNotFound", true);
            } else {
                // get the start timestamp
                long taskCreateTimestamp;
                try (InputStream is = getCloudStorageInputStream(timestampFile)) {
                    taskCreateTimestamp = Long.parseLong(IOUtils.toString(is, StandardCharsets.UTF_8));
                }

                String report = type + "-" + id + "-" + type + "-" + id + ".json";
                if (!fileExists(report)) {
                    // the result does not exist, it means the task is still ongoing
                    request.setAttribute("taskOngoing", true);

                    // we will refresh in a bit.
                    int waitTime = getWaitTime(taskCreateTimestamp);
                    logger.fine("Task is not finished yet, waiting for " + waitTime + " seconds before checking again.");
                    request.setAttribute("refreshIn", waitTime);
                    request.setAttribute("taskCreatedAgo", formatTimeAgo(taskCreateTimestamp));
                } else {
                    // task is done!
                    logger.fine("Task is finished!");

                    // parse the result
                    JSONObject result;
                    try (InputStream is = getCloudStorageInputStream(report)) {
                        result = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                    }
                    request.setAttribute("taskResult", toHtml(result.getString("responseText")));
                    request.setAttribute("taskResultType", getResultType(result.getString("responseText")));
                    request.setAttribute("attachments", getAttachmentsFor(type, result));
                }
            }
        }

        // 2/ download handling: /celeste/task-tracker/[type]/[id]/download/[index]
        Matcher downloadPageUrlMatch = downloadPageUrlPattern.matcher(request.getRequestURI());
        if (downloadPageUrlMatch.matches()) {
            regexMatch = true;

            String type = downloadPageUrlMatch.group(1);
            String id = downloadPageUrlMatch.group(2);
            int index = Integer.parseInt(downloadPageUrlMatch.group(3));

            String report = type + "-" + id + "-" + type + "-" + id + ".json";
            if (!fileExists(report)) {
                // the task doesn't exist (or isn't over) in the first place.
                logger.warning(report + " does not exist => task not found!");
                request.setAttribute("taskNotFound", true);
            } else {
                // parse the task result
                JSONObject result;
                try (InputStream is = getCloudStorageInputStream(report)) {
                    result = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                }

                JSONArray attachmentNames = result.getJSONArray("attachments");
                if (index < 0 || index >= attachmentNames.length()) {
                    // we asked for a file that was out of bounds!
                    logger.warning("File " + index + " is out of range => file not found!");
                    request.setAttribute("fileNotFound", true);
                } else {
                    // get the file name from the task attachment list
                    String fileName = attachmentNames.getString(index);
                    logger.fine("File name: " + fileName);

                    // send the file from Cloud Storage
                    response.setHeader("Content-Type", getContentType(fileName));
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    try (InputStream is = getCloudStorageInputStream(fileName)) {
                        IOUtils.copy(is, response.getOutputStream());
                    }
                    return;
                }
            }
        }

        if (!regexMatch) {
            // the URL the user tried to access is invalid, so let's just answer the task was not found.
            logger.warning("URI does not match regex => task not found!");
            request.setAttribute("taskNotFound", true);

            request.setAttribute("type", request.getRequestURI().startsWith("/celeste/task-tracker/font-generate") ? "font-generate" : "mod-structure-verify");
        }

        request.getRequestDispatcher("/WEB-INF/task-tracker.jsp").forward(request, response);
    }

    private InputStream getCloudStorageInputStream(String filename) {
        BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", filename);
        return new ByteArrayInputStream(storage.readAllBytes(blobId));
    }

    private boolean fileExists(String filename) {
        BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", filename);
        return storage.get(blobId) != null;
    }

    private String formatTimeAgo(long timestamp) {
        long secondsAgo = (System.currentTimeMillis() - timestamp) / 1000L;

        if (secondsAgo == 0) {
            return "just now";
        } else if (secondsAgo < 60) {
            return secondsAgo + (secondsAgo == 1 ? " second ago" : " seconds ago");
        } else if (secondsAgo < 3600) {
            return (secondsAgo / 60) + ((secondsAgo / 60) == 1 ? " minute" : " minutes")
                    + " and " + (secondsAgo % 60) + ((secondsAgo % 60 == 1) ? " second" : " seconds") + " ago";
        } else {
            // if we reach that point, we have big issues.
            logger.warning("This task was launched more than an hour ago!");
            return "more than an hour ago";
        }
    }

    private int getWaitTime(long timestamp) {
        long secondsAgo = (System.currentTimeMillis() - timestamp) / 1000L;

        if (secondsAgo < 30) {
            return 5;
        } else if (secondsAgo < 60) {
            return 10;
        } else if (secondsAgo < 120) {
            return 15;
        } else if (secondsAgo < 300) {
            return 30;
        } else {
            return 60;
        }
    }

    private String toHtml(String responseText) {
        // escape HTML and handle emojis
        String escapedHtml = StringEscapeUtils.escapeHtml4(responseText
                .replace(":white_check_mark:", "✅")
                .replace(":warning:", "⚠")
                .replace(":x:", "❌")
                .replace(":thinking:", "\uD83E\uDD14")
                .replace(":bomb:", "\uD83D\uDCA3"));

        // handle links
        escapedHtml = escapedHtml
                .replace("&lt;https://max480-random-stuff.appspot.com/celeste/everest-yaml-validator&gt;",
                        "<a href=\"/celeste/everest-yaml-validator\" target=\"_blank\">the everest.yaml validator</a>")
                .replace("&lt;https://max480-random-stuff.appspot.com/celeste/font-generator&gt;",
                        "<a href=\"/celeste/font-generator\" target=\"_blank\">the Font Generator</a>")
                .replace("&lt;https://gamebanana.com/tools/6908&gt;",
                        "<a href=\"https://gamebanana.com/tools/6908\" target=\"_blank\">Dependency Generator</a>");

        // handle bold text
        while (escapedHtml.contains("**") && escapedHtml.replaceFirst("\\*\\*", "").contains("**")) {
            escapedHtml = escapedHtml
                    .replaceFirst("\\*\\*", "<b>")
                    .replaceFirst("\\*\\*", "</b>");
        }

        // handle inline code
        while (escapedHtml.contains("`") && escapedHtml.replaceFirst("`", "").contains("`")) {
            escapedHtml = escapedHtml
                    .replaceFirst("`", "<code>")
                    .replaceFirst("`", "</code>");
        }

        // handle lists
        boolean ulOpen = false;

        StringBuilder newHtml = new StringBuilder();
        while (!escapedHtml.isEmpty()) {
            String line = escapedHtml;
            if (line.contains("\n")) {
                line = escapedHtml.substring(0, escapedHtml.indexOf("\n") + 1);
            }

            escapedHtml = escapedHtml.substring(line.length());

            // turn "click here for more help" into an actual link as well!
            Matcher helpMatch = modStructureVerifierHelpPattern.matcher(line);
            if (helpMatch.matches()) {
                line = "<a href=\"/celeste/mod-structure-verifier-help?" + helpMatch.group(1) + "\" target=\"_blank\">Click here for more help.</a>";
            }

            if (line.startsWith("- ")) {
                if (!ulOpen) {
                    newHtml.append("<ul>");
                    ulOpen = true;
                }
                newHtml.append("<li>").append(line.substring(2)).append("</li>");
            } else {
                if (ulOpen) {
                    newHtml.append("</ul>");
                    ulOpen = false;
                }
                newHtml.append(line.replace("\n", "<br>"));
            }
        }

        return newHtml.toString();
    }

    private String getResultType(String responseText) {
        if (responseText.startsWith(":white_check_mark:")) {
            return "success";
        } else if (responseText.startsWith(":warning:")) {
            return "warning";
        } else {
            return "danger";
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

    private List<String> getAttachmentsFor(String type, JSONObject result) {
        if (type.equals("font-generate")) {
            switch (result.getJSONArray("attachments").length()) {
                case 1:
                    return Collections.singletonList("Font zip file");
                case 2:
                    return Arrays.asList("Font zip file", "Missing characters");
                default:
                    return Collections.emptyList();
            }
        } else if (type.equals("mod-structure-verify")) {
            List<String> attachmentList = new ArrayList<>();
            for (Object o : result.getJSONArray("attachments")) {
                String attachmentName = (String) o;
                attachmentName = attachmentName.substring(attachmentName.lastIndexOf("-") + 1, attachmentName.indexOf("_"));
                attachmentName = attachmentName.substring(0, 1).toUpperCase(Locale.ROOT) + attachmentName.substring(1);
                attachmentList.add("Missing characters in " + attachmentName);
            }
            return attachmentList;
        }

        return Collections.emptyList();
    }
}