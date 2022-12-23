package com.max480.randomstuff.gae;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.max480.randomstuff.backend.celeste.crontabs.UpdateCheckerTracker.ModInfo;

/**
 * This servlet provides the GameBanana search API, and other APIs that are used by Olympus or the Banana Mirror Browser.
 */
@WebServlet(name = "CelesteModSearchService", loadOnStartup = 2, urlPatterns = {"/celeste/gamebanana-search",
        "/celeste/gamebanana-search-reload", "/celeste/gamebanana-list", "/celeste/gamebanana-categories", "/celeste/webp-to-png",
        "/celeste/banana-mirror-image", "/celeste/random-map", "/celeste/gamebanana-featured", "/celeste/olympus-news", "/celeste/olympus-news-reload",
        "/celeste/everest-versions", "/celeste/everest-versions-reload"})
public class CelesteModSearchService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModSearchService");

    private static List<ModInfo> modDatabaseForSorting = Collections.emptyList();
    private Map<Integer, String> modCategories;

    private byte[] olympusNews;
    private byte[] everestVersions;

    @Override
    public void init() {
        try {
            refreshModDatabase();
            refreshOlympusNews();
            refreshEverestVersions();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/celeste/gamebanana-search-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshModDatabase();
            } else {
                // invalid secret
                logger.warning("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if (request.getRequestURI().equals("/celeste/random-map")) {
            List<ModInfo> maps = modDatabaseForSorting.stream()
                    .filter(i -> "Mod".equals(i.type) && i.categoryId == 6800) // Map
                    .collect(Collectors.toList());

            // pick a map and redirect to it. that's it.
            ModInfo drawnMod = maps.get((int) (Math.random() * maps.size()));
            response.setStatus(302);
            response.setHeader("Location", "https://gamebanana.com/mods/" + drawnMod.id);
            return;
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-search")) {
            String queryParam = request.getParameter("q");
            boolean fullInfo = "true".equals(request.getParameter("full"));

            if (queryParam == null || queryParam.trim().isEmpty()) {
                // the user didn't give any search!
                response.setHeader("Content-Type", "text/plain");
                logger.warning("Bad request");
                response.setStatus(400);
                response.getWriter().write("\"q\" query parameter expected");
            } else {
                final String[] tokenizedRequest = tokenize(queryParam);

                Stream<ModInfo> searchStream = modDatabaseForSorting.stream()
                        .filter(mod -> scoreMod(tokenizedRequest, (String[]) mod.fullInfo.get("TokenizedName")) > 0.2f * tokenizedRequest.length)
                        .sorted(Comparator.comparing(mod -> -scoreMod(tokenizedRequest, (String[]) mod.fullInfo.get("TokenizedName"))));


                // send out the response
                if (fullInfo) {
                    List<Map<String, Object>> responseBody = searchStream
                            .map(mod -> mod.fullInfo)
                            .limit(20)
                            .collect(Collectors.toList());

                    response.setHeader("Content-Type", "application/json");
                    response.getWriter().write(new JSONArray(responseBody).toString());
                } else {
                    List<Map<String, Object>> responseBody = searchStream
                            .map(mod -> ImmutableMap.<String, Object>of(
                                    "itemtype", mod.type,
                                    "itemid", mod.id))
                            .limit(20)
                            .collect(Collectors.toList());

                    response.setHeader("Content-Type", "text/yaml");
                    YamlUtil.dump(responseBody, response.getOutputStream());
                }
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-list")) {
            boolean fullInfo = "true".equals(request.getParameter("full"));
            String sortParam = request.getParameter("sort");
            String pageParam = request.getParameter("page");
            String typeParam = request.getParameter("type") == null ? request.getParameter("itemtype") : request.getParameter("type");
            String categoryParam = request.getParameter("category");

            if (!Arrays.asList("latest", "likes", "views", "downloads").contains(sortParam)) {
                // invalid sort!
                response.setHeader("Content-Type", "text/plain");
                logger.warning("Bad request");
                response.setStatus(400);
                response.getWriter().write("expected \"sort\" parameter with value \"latest\", \"likes\", \"views\" or \"downloads\"");
            } else {
                // parse the page number: if page number is absent or invalid, assume 1
                int page = 1;
                if (pageParam != null) {
                    try {
                        page = Integer.parseInt(pageParam);
                    } catch (NumberFormatException e) {
                        logger.info("Invalid page number, assuming 1");
                    }
                }

                // is there a type and/or a category filter?
                Predicate<ModInfo> typeFilter = info -> true;
                if (typeParam != null) {
                    if (categoryParam != null) {
                        typeFilter = info -> typeParam.equalsIgnoreCase(info.type) && Integer.toString(info.categoryId).equals(categoryParam);
                    } else {
                        typeFilter = info -> typeParam.equalsIgnoreCase(info.type);
                    }
                } else if (categoryParam != null) {
                    typeFilter = info -> Integer.toString(info.categoryId).equals(categoryParam);
                }

                // determine the field on which we want to sort. Sort by descending id to tell equal values apart.
                Comparator<ModInfo> sort;
                switch (sortParam) {
                    case "views":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.views).thenComparingInt(i -> -i.id);
                        break;
                    case "likes":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.likes).thenComparingInt(i -> -i.id);
                        break;
                    case "downloads":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.downloads).thenComparingInt(i -> -i.id);
                        break;
                    case "latest":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.createdDate).thenComparingInt(i -> -i.id);
                        break;
                    default:
                        sort = null;
                        break;
                }

                // then sort on it.
                Stream<ModInfo> responseBodyStream = modDatabaseForSorting.stream()
                        .filter(typeFilter);

                if (sort != null) {
                    responseBodyStream = responseBodyStream.sorted(sort);
                }

                final List<Map<String, Object>> responseBody = responseBodyStream
                        .skip((page - 1) * 20L)
                        .limit(20)
                        .map(modInfo -> {
                            if (fullInfo) {
                                return modInfo.fullInfo;
                            } else {
                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("itemtype", modInfo.type);
                                result.put("itemid", modInfo.id);
                                return result;
                            }
                        })
                        .collect(Collectors.toList());

                // count the amount of results and put it as a header.
                response.setHeader("X-Total-Count", Long.toString(modDatabaseForSorting.stream()
                        .filter(typeFilter)
                        .count()));

                // send out the response.
                if (fullInfo) {
                    response.setHeader("Content-Type", "application/json");
                    response.getWriter().write(new JSONArray(responseBody).toString());
                } else {
                    response.setHeader("Content-Type", "text/yaml");
                    YamlUtil.dump(responseBody, response.getOutputStream());
                }
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-featured")) {
            final List<String> catOrder = Arrays.asList("today", "week", "month", "3month", "6month", "year", "alltime");
            final List<Map<String, Object>> responseBody = modDatabaseForSorting.stream()
                    .filter(mod -> mod.fullInfo.containsKey("Featured"))
                    .sorted((a, b) -> {
                        Map<String, Object> aInfo = (Map<String, Object>) a.fullInfo.get("Featured");
                        Map<String, Object> bInfo = (Map<String, Object>) b.fullInfo.get("Featured");

                        // sort by category, then by position.
                        if (aInfo.get("Category").equals(bInfo.get("Category"))) {
                            return (int) aInfo.get("Position") - (int) bInfo.get("Position");
                        }
                        return catOrder.indexOf(aInfo.get("Category")) - catOrder.indexOf(bInfo.get("Category"));
                    })
                    .map(mod -> mod.fullInfo)
                    .collect(Collectors.toList());

            response.setHeader("Content-Type", "application/json");
            response.getWriter().write(new JSONArray(responseBody).toString());
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-categories")) {
            boolean v3 = "3".equals(request.getParameter("version"));
            boolean v2 = v3 || "2".equals(request.getParameter("version"));

            // go across all mods and aggregate stats per category.
            HashMap<Object, Integer> categoriesAndCounts = new HashMap<>();
            for (ModInfo modInfo : modDatabaseForSorting) {
                Object category = modInfo.type;
                if (v2 && category.equals("Mod")) {
                    category = modInfo.categoryId;
                }
                if (!categoriesAndCounts.containsKey(category)) {
                    // first mod encountered in this category
                    categoriesAndCounts.put(category, 1);
                } else {
                    // add 1 to the mod count in the category
                    categoriesAndCounts.put(category, categoriesAndCounts.get(category) + 1);
                }
            }

            // format the map for the response...
            List<Map<String, Object>> categoriesList = categoriesAndCounts.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        if (entry.getKey() instanceof String) {
                            // itemtype
                            result.put("itemtype", entry.getKey());
                            result.put("formatted", formatGameBananaItemtype(entry.getKey().toString(), true));
                        } else {
                            // mod category
                            if (v3) result.put("itemtype", "Mod");
                            result.put("categoryid", entry.getKey());
                            result.put("formatted", modCategories.get(entry.getKey()));
                        }
                        result.put("count", entry.getValue());
                        return result;
                    })
                    .sorted(Comparator.comparing(result -> result.get("formatted").toString()))
                    .collect(Collectors.toList());

            // also add an "All" option to pass the total number of mods.
            Map<String, Object> all = new HashMap<>();
            if (!v3) all.put("itemtype", "");
            all.put("formatted", "All");
            all.put("count", modDatabaseForSorting.size());

            // the final list is "All" followed by all the categories.
            List<Map<String, Object>> responseBody = new ArrayList<>();
            responseBody.add(all);
            responseBody.addAll(categoriesList);

            // send out the response (the "block" flow style works better with Olympus).
            response.setHeader("Content-Type", "text/yaml");
            YamlUtil.dump(responseBody, response.getOutputStream());
        }

        // "redirect to matching image on Banana Mirror" service, that also responds to /celeste/webp-to-png for backwards compatibility
        if (request.getRequestURI().equals("/celeste/webp-to-png") || request.getRequestURI().equals("/celeste/banana-mirror-image")) {
            String imagePath = request.getParameter("src");
            if (imagePath == null) {
                // no image path passed!
                response.setHeader("Content-Type", "text/plain");
                logger.warning("Bad request");
                response.setStatus(400);
                response.getWriter().write("expected \"src\" parameter");
            } else if ((!imagePath.startsWith("https://screenshots.gamebanana.com/") && !imagePath.startsWith("https://images.gamebanana.com/"))) {
                // the URL passed is not from GameBanana.
                logger.warning("Returned 403 after trying to use conversion with non-GB URL");
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(403);
                response.getWriter().write("this API can only be used with GameBanana");
            } else {
                // find out what the ID on the mirror is going to be, and redirect to it.
                String screenshotId;
                if (imagePath.startsWith("https://screenshots.gamebanana.com/")) {
                    screenshotId = imagePath.substring("https://screenshots.gamebanana.com/".length());
                } else {
                    screenshotId = imagePath.substring("https://images.gamebanana.com/".length());
                }
                screenshotId = screenshotId.substring(0, screenshotId.lastIndexOf(".")).replace("/", "_") + ".png";

                if (request.getRequestURI().equals("/celeste/webp-to-png")) {
                    // for compatibility, remove the 220-90 prefix.
                    screenshotId = screenshotId.replace("220-90_", "");
                }

                response.setStatus(302);
                response.setHeader("Location", "https://celestemodupdater.0x0a.de/banana-mirror-images/" + screenshotId);
            }
        }

        if (request.getRequestURI().equals("/celeste/olympus-news-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshOlympusNews();
            } else {
                // invalid secret
                logger.warning("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if (request.getRequestURI().equals("/celeste/everest-versions-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshEverestVersions();
            } else {
                // invalid secret
                logger.warning("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if ("/celeste/olympus-news".equals(request.getRequestURI())) {
            // send olympus_news.json we downloaded earlier
            response.setHeader("Content-Type", "application/json");
            IOUtils.write(olympusNews, response.getOutputStream());
        }

        if ("/celeste/everest-versions".equals(request.getRequestURI())) {
            // send olympus_news.json we downloaded earlier
            response.setHeader("Content-Type", "application/json");
            IOUtils.write(everestVersions, response.getOutputStream());
        }
    }

    private static String[] tokenize(String string) {
        string = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9* ]", "");
        while (string.contains("  ")) string = string.replace("  ", " ");
        return string.split(" ");
    }

    private static double scoreMod(String[] query, String[] modName) {
        double score = 0;

        for (String tokenSearch : query) {
            if (tokenSearch.endsWith("*")) {
                // "starts with" search: add 1 if there's a word starting with the prefix
                String tokenSearchStart = tokenSearch.substring(0, tokenSearch.length() - 1);
                for (String tokenModName : modName) {
                    if (tokenModName.startsWith(tokenSearchStart)) {
                        score++;
                        break;
                    }
                }
            } else {
                // "equals" search: take the score of the word that is closest to the token
                double tokenScore = 0;
                for (String tokenModName : modName) {
                    tokenScore = Math.max(tokenScore, Math.pow(0.5, LevenshteinDistance.getDefaultInstance().apply(tokenSearch, tokenModName)));
                }
                score += tokenScore;
            }
        }

        return score;
    }

    public static String formatGameBananaItemtype(String input, boolean pluralize) {
        // specific formatting for a few categories
        if (input.equals("Gamefile")) {
            return pluralize ? "Game files" : "Game file";
        } else if (input.equals("Wip")) {
            return pluralize ? "WiPs" : "WiP";
        } else if (input.equals("Gui")) {
            return pluralize ? "GUIs" : "GUI";
        }

        // apply the spaced pascal case from Everest
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(input.charAt(i - 1)))
                builder.append(' ');

            if (i != 0 && builder.charAt(builder.length() - 1) == ' ') {
                builder.append(Character.toUpperCase(c));
            } else {
                builder.append(c);
            }
        }

        // capitalize the first letter
        String result = builder.toString();
        result = result.substring(0, 1).toUpperCase() + result.substring(1);

        if (!pluralize) {
            return result;
        }

        // pluralize
        if (result.charAt(result.length() - 1) == 'y') {
            return result.substring(0, result.length() - 1) + "ies";
        }
        return result + "s";
    }

    // mapping takes an awful amount of time on App Engine (~2 seconds) so we can't make it when the user calls the API.
    private void refreshModDatabase() throws IOException {
        // get and deserialize the mod list from Cloud Storage.
        try (ObjectInputStream is = new ObjectInputStream(CloudStorageUtils.getCloudStorageInputStream("mod_search_database.ser"))) {
            modDatabaseForSorting = (List<ModInfo>) is.readObject();
            modCategories = (Map<Integer, String>) is.readObject();
            logger.fine("There are " + modDatabaseForSorting.size() + " mods in the search database.");
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    private void refreshOlympusNews() throws IOException {
        olympusNews = IOUtils.toByteArray(CloudStorageUtils.getCloudStorageInputStream("olympus_news.json"));
        logger.fine("Reloaded Olympus news! " + olympusNews.length + " bytes preloaded.");
    }

    private void refreshEverestVersions() throws IOException {
        everestVersions = IOUtils.toByteArray(CloudStorageUtils.getCloudStorageInputStream("everest_version_list.json"));
        logger.fine("Reloaded Everest versions! " + everestVersions.length + " bytes preloaded.");
    }

    public static ModInfo getModInfoByTypeAndId(String itemtype, int itemid) {
        return modDatabaseForSorting.stream()
                .filter(m -> m.type.equals(itemtype) && m.id == itemid)
                .findFirst()
                .orElse(null);
    }
}
