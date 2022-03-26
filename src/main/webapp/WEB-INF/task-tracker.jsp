<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <% if ("font-generate".equals(request.getAttribute("type"))) { %>
        <title>Celeste Font Generator</title>
    <% } else { %>
        <title>Celeste Mod Structure Verifier</title>
    <% } %>

    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link rel="stylesheet"
        href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
        integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
        crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/task-tracker.css">

    <% if((boolean) request.getAttribute("taskOngoing")) { %>
        <meta http-equiv="refresh" content="<%= request.getAttribute("refreshIn") %>" >
    <% } %>
</head>

<body>
    <div class="container">
        <div id="nav">
            <a href="/celeste/custom-entity-catalog">Custom&nbsp;Entity&nbsp;Catalog</a> <span class="sep">|</span>
            <a href="/celeste/everest-yaml-validator">everest.yaml&nbsp;validator</a> <span class="sep">|</span>
            <a href="/celeste/update-checker-status">Update&nbsp;Checker&nbsp;status</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/celeste/banana-mirror-browser">Banana&nbsp;Mirror&nbsp;Browser</a> <span class="sep">|</span>
            <% if ("font-generate".equals(request.getAttribute("type"))) { %>
                <a href="/celeste/mod-structure-verifier">Mod&nbsp;Structure&nbsp;Verifier</a> <span class="sep break">|</span>
                <a href="/celeste/font-generator" class="active">Font&nbsp;Generator</a> <span class="sep">|</span>
            <% } else { %>
                <a href="/celeste/mod-structure-verifier" class="active">Mod&nbsp;Structure&nbsp;Verifier</a> <span class="sep break">|</span>
                <a href="/celeste/font-generator">Font&nbsp;Generator</a> <span class="sep">|</span>
            <% } %>
            <a href="https://max480-random-stuff.herokuapp.com/celeste/wipe-converter">Wipe&nbsp;Converter</a> <span class="sep">|</span>
            <a href="/discord-bots">Discord&nbsp;Bots</a> <span class="sep">|</span>
            <a href="/celeste/news-network-subscription">#celeste_news_network&nbsp;Subscription</a>
        </div>

        <% if ("font-generate".equals(request.getAttribute("type"))) { %>
            <h1>Celeste Font Generator</h1>
        <% } else { %>
            <h1>Celeste Mod Structure Verifier</h1>
        <% } %>

        <% if((boolean) request.getAttribute("taskNotFound")) { %>
            <div class="alert alert-danger">
                <b>This task does not exist!</b> Please try running it again.
            </div>
        <% } else if((boolean) request.getAttribute("fileNotFound")) { %>
            <div class="alert alert-danger">
                <b>This file does not exist!</b> Please go back to the previous page or try running the task again.
            </div>
        <% } else if((boolean) request.getAttribute("taskOngoing")) { %>
            <div class="alert alert-info">
                <b>Please wait...</b> This page will refresh automatically.
                The task was started <%= request.getAttribute("taskCreatedAgo") %>.
            </div>
        <% } else { %>
            <div class="alert alert-<%= request.getAttribute("taskResultType") %>">
                <%= request.getAttribute("taskResult") %>

                <% if (!((List<String>) request.getAttribute("attachments")).isEmpty()) { %>
                    <div class="attachment-list">
                        <% for(int i = 0; i < ((List<String>) request.getAttribute("attachments")).size(); i++) { %>
                            <a href="/celeste/task-tracker/<%= request.getAttribute("type") %>/<%= request.getAttribute("id") %>/download/<%= i %>"
                                class="btn btn-outline-dark" target="_blank">
                                &#x1F4E5; <%= ((List<String>) request.getAttribute("attachments")).get(i) %>
                            </a>
                        <% } %>
                    </div>
                <% } %>
            </div>
        <% } %>

        <% if(!((boolean) request.getAttribute("taskOngoing"))) { %>
            <div class="back-link">
                <% if ("font-generate".equals(request.getAttribute("type"))) { %>
                    <a class="btn btn-outline-secondary" href="/celeste/font-generator">&#x2B05; Back to Font Generator</a>
                <% } else { %>
                    <a class="btn btn-outline-secondary" href="/celeste/mod-structure-verifier">&#x2B05; Back to Mod Structure Verifier</a>
                <% } %>
            </div>
        <% } %>
    </div>
</body>
</html>