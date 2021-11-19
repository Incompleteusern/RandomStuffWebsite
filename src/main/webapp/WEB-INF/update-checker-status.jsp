<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@ page import="java.util.List, static org.apache.commons.text.StringEscapeUtils.escapeHtml4, static com.max480.randomstuff.gae.UpdateCheckerStatusService.LatestUpdatesEntry"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
	<title>Everest Update Checker status page</title>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="author" content="max480">
	<meta name="description" content="Check the status of the Everest Update Checker here.">
	<meta property="og:title" content="Everest Update Checker status page">
	<meta property="og:description" content="Check the status of the Everest Update Checker here.">
    <meta http-equiv="refresh" content="60" >

    <link rel="shortcut icon" href="/celeste/favicon.ico">

	<link rel="stylesheet"
		href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
		integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
		crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common-v7.css">
    <link rel="stylesheet" href="/css/update-checker-status-v1.css">
</head>

<body>
	<div class="container">
        <div id="nav">
            <a href="/celeste/custom-entity-catalog">Custom&nbsp;Entity&nbsp;Catalog</a> <span class="sep">|</span>
            <a href="/celeste/everest-yaml-validator">everest.yaml&nbsp;validator</a> <span class="sep">|</span>
            <a href="/celeste/update-checker-status" class="active">Update&nbsp;Checker&nbsp;status</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/banana-mirror-browser">Banana&nbsp;Mirror&nbsp;Browser</a> <span class="sep break">|</span>
            <a href="/celeste/font-generator">Font&nbsp;Generator</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/wipe-converter">Wipe&nbsp;Converter</a> <span class="sep">|</span>
            <a href="/discord-bots">Discord&nbsp;Bots</a> <span class="sep">|</span>
            <a href="/celeste/news-network-subscription">#celeste_news&nbsp;Subscription</a>
        </div>

        <h1 class="mt-4">Everest Update Checker status page</h1>

        <% if ((boolean) request.getAttribute("up")) { %>
            <div class="alert alert-success"><b>The update checker is up and running!</b></div>
        <% } else { %>
            <div class="alert alert-danger">
                <b>The update checker is currently having issues.</b>
                The mod database might not be up-to-date.
            </div>
        <% } %>

        <% if (request.getAttribute("lastUpdatedAt") != null) { %>
            <p>
                The database was last updated successfully on
                <b>
                    <span class="timestamp-long" data-timestamp="<%= request.getAttribute("lastUpdatedTimestamp") %>">
                        <%= request.getAttribute("lastUpdatedAt") %>
                    </span>
                    (<%= request.getAttribute("lastUpdatedAgo") %>)
                </b>.
                The update check took <%= request.getAttribute("duration") %> seconds.
            </p>
        <% } %>

        <b><%= request.getAttribute("modCount") %></b> mods are currently registered.

        <% if (!((List<LatestUpdatesEntry>) request.getAttribute("latestUpdates")).isEmpty()) { %>
            <h2>Latest updates</h2>

            <table class="table table-striped">
                <% for (LatestUpdatesEntry entry : (List<LatestUpdatesEntry>) request.getAttribute("latestUpdates")) { %>
                    <tr>
                        <td>
                            <% if (entry.isAddition) { %>
                                &#x2705;
                            <% } else { %>
                                &#x274c;
                            <% } %>
                        </td>
                        <td>
                            <span class="timestamp-short" data-timestamp="<%= entry.timestamp %>">
                                <%= escapeHtml4(entry.date) %>
                            </span>
                        </td>
                        <td>
                            <% if (entry.isAddition) { %>
                                <b><%= escapeHtml4(entry.name) %></b> was updated to version <b><%= escapeHtml4(entry.version) %></b>
                            <% } else { %>
                                <b><%= escapeHtml4(entry.name) %></b> was deleted
                            <% } %>
                        </td>
                    </tr>
                <% } %>
            </table>
        <% } %>

        <!-- Developed by max480 - version 1.0 - last updated on Mar 20, 2021 -->
        <!-- What are you doing here? :thinkeline: -->
	</div>
</body>
</html>