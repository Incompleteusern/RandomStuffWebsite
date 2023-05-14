package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * A small webhook that is called by Zapier with new tweets from celeste_game as a payload.
 * <p>
 * ... because paying for Zapier turns out to be 5x less expensive than paying for Twitter, and you can actually
 * do other stuff with Zapier too.
 */
@WebServlet(name = "ZapierWebhookService", urlPatterns = {"/celeste/zapier-webhook"})
@MultipartConfig
public class ZapierWebhookService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(StaticAssetsAndRouteNotFoundServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (("key=" + SecretConstants.ZAPIER_WEBHOOK_SECRET).equals(request.getQueryString())) {
            // read the new tweet from the request payload
            JSONObject newTweet;
            try (Reader r = request.getReader()) {
                newTweet = new JSONObject(IOUtils.toString(r));
            }

            log.info("Received tweet from Zapier:\n{}", newTweet.toString(2));
            unstringifyArrays(newTweet);
            log.info("Unstringified tweet from Zapier:\n{}", newTweet.toString(2));

            // read the old tweets from disk
            JSONArray oldTweets;
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/celeste-game-twitter-raw.json"))) {
                oldTweets = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
            }

            // add the new tweet at the start of the list
            JSONArray newTweets = new JSONArray();
            newTweets.put(0, newTweet);
            for (int i = 0; i < oldTweets.length() && newTweets.length() < 50; i++) {
                newTweets.put(i + 1, oldTweets.get(i));
            }

            // save!
            try (OutputStream os = Files.newOutputStream(Paths.get("/shared/celeste/celeste-game-twitter-raw.json"))) {
                IOUtils.write(newTweets.toString(), os, StandardCharsets.UTF_8);
            }

            response.setContentType("text/plain");
            response.getWriter().write("OK");
        } else {
            // invalid secret
            log.warn("Invalid key");
            response.setStatus(403);
        }
    }

    private static void unstringifyArrays(JSONObject source) {
        for (String key : source.keySet()) {
            Object item = source.get(key);
            if (item instanceof JSONObject) {
                unstringifyArrays((JSONObject) item);
            } else if (item instanceof String && Arrays.asList("urls", "hashtags", "media", "user_mentions", "symbols", "withheld_in_countries", "display_text_range").contains(key)) {
                // this is a JSON array in disguise
                String array = ((String) item).replace("'", "\"");
                try {
                    JSONArray converted = new JSONArray(array);
                    source.put(key, converted);
                } catch (JSONException e) {
                    log.warn("Could not destringify array {}", item, e);
                }
            }
        }
    }
}
