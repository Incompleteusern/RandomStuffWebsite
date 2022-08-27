# max480-random-stuff website

This is the source code for the [max480-random-stuff.appspot.com](https://max480-random-stuff.appspot.com/) website, a [Google App Engine](https://cloud.google.com/appengine/) app written in Java. The frontend part is made of backend-rendered pages and some pages made with [Vue.js](https://vuejs.org/).

It contains the full source for:
- [the Everest Update Checker frontend service](https://max480-random-stuff.appspot.com/celeste/everest_update.yaml), exposing files produced by the Update Checker
- [the Celeste custom entity catalog](https://max480-random-stuff.appspot.com/celeste/custom-entity-catalog), and [its API version](https://max480-random-stuff.appspot.com/celeste/custom-entity-catalog.json)
- [the everest.yaml validator](https://max480-random-stuff.appspot.com/celeste/everest-yaml-validator)
- [the Everest Update Checker status page](https://max480-random-stuff.appspot.com/celeste/update-checker-status)
- [the Celeste font generator](https://max480-random-stuff.appspot.com/celeste/font-generator)
- [the Celeste Mod Structure Verifier](https://max480-random-stuff.appspot.com/celeste/mod-structure-verifier)
- [the Banana Mirror Browser](https://max480-random-stuff.appspot.com/banana-mirror-browser), a website that can substitute to [the GameBanana Celeste section](https://gamebanana.com/games/6460) if it is down. Files and images are [hosted on 0x0ade's server](https://celestemodupdater.0x0a.de/). Mod info and files on this server are kept up-to-date by [the Everest update checker](https://github.com/max4805/EverestUpdateCheckerServer).
- [the Wipe Converter](https://max480-random-stuff.appspot.com/wipe-converter), a service to turn custom Celeste screen wipes into a format the game can use (with the [max480's Helping Hand](https://github.com/max4805/MaxHelpingHand) mod).
- [the #celeste_news_network subscription service](https://max480-random-stuff.appspot.com/celeste/news-network-subscription) - the Twitter update checking code is not part of the frontend and can be found [on the Random Backend Stuff repository](https://github.com/max4805/RandomBackendStuff/blob/main/src/celeste-backend-crontabs/TwitterUpdateChecker.java)
- [the help page for the Mod Structure Verifier bot](https://max480-random-stuff.appspot.com/celeste/mod-structure-verifier-help?collabName=CollabName&collabMapName=CollabMapName&assets&xmls&nomap&multiplemaps&badmappath&badenglish&misplacedyaml&noyaml&yamlinvalid&missingassets&missingentities&missingfonts)
- [the "Show Arbitrary Mods on Profile" GameBanana app](https://gamebanana.com/apps/752)
- [the Discord Games Bot](https://max480-random-stuff.appspot.com/discord-bots#games-bot) - the "bot" is actually a webhook that gets called by Discord
- [the Discord Custom Slash Commands app](https://max480-random-stuff.appspot.com/discord-bots#custom-slash-commands)
- Some [GameBanana](https://gamebanana.com)-related APIs extending the official API, described below
- [the Olympus News API](https://max480-random-stuff.appspot.com/celeste/olympus-news)
- [the Everest versions list API](https://max480-random-stuff.appspot.com/celeste/everest-versions)
- ... and some other things of more limited use that are on the website.

If you want to check how the update checker's everest_update.yaml file is generated, check [the Everest Update Checker Server repo](https://github.com/max4805/EverestUpdateCheckerServer) instead.

## GameBanana search API

This API uses the mod search database generated by [the Everest Update Checker server](https://github.com/max4805/EverestUpdateCheckerServer) to find mods based on keywords. **This searches Celeste mods only**.

It is used by [Olympus](https://github.com/EverestAPI/Olympus), the Everest installer and mod manager, to search Celeste mods on GameBanana.

To use this API, call `https://max480-random-stuff.appspot.com/celeste/gamebanana-search?q=[search]`. The answer is in yaml format, and is a list of the top 20 matches. For example:

```
$ curl https://max480-random-stuff.appspot.com/celeste/gamebanana-search?q=spring+collab+2020
- {itemtype: Map, itemid: 211745}
- {itemtype: Gamefile, itemid: 13452}
- {itemtype: Map, itemid: 212317}
- {itemtype: Gamefile, itemid: 13185}
- {itemtype: Gui, itemid: 35325}
- {itemtype: Gamefile, itemid: 13258}
- {itemtype: Map, itemid: 212999}
- {itemtype: Gamefile, itemid: 12784}
- {itemtype: Gamefile, itemid: 9486}
```

Here the top 2 results are https://gamebanana.com/maps/211745 (The 2020 Celeste Spring Community Collab) and https://gamebanana.com/gamefiles/13452 (2020 Spring Collab Randomizer).

The search engine is powered by [Apache Lucene](https://lucene.apache.org/). It supports, among other things:
- searching for a **phrase**: `"Spring Collab"`
- **OR** and **NOT** keywords: `Spring Collab NOT Randomizer`
- searching for other fields in GameBanana submissions:
  - the **name** (default): `name: (Spring Collab 2020)`
  - the GameBanana **type**: `type: gamefile`, `type: map`, etc.
  - the GameBanana **ID**: `id: 9486` (those can be found at the end of links to mods)
  - the GameBanana **category**: `category: Textures`
  - one of the **authors**: `author: max480`
  - the **summary** (line that appears first on the page): `summary: (grab bag)`
  - the **description**: `description: "flag touch switches"`

For a full list of supported syntax, check [the Lucene documentation](https://lucene.apache.org/core/8_7_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description).

You can pass `full=true` to the API to get the full info on mods instead of just their IDs, **in JSON instead of YAML**:

```yaml
$ curl "https://max480-random-stuff.appspot.com/celeste/gamebanana-search?q=spring+collab+2020&full=true"
[
  {
    "CategoryId": 4632,
    "Screenshots": [
      "https://images.gamebanana.com/img/ss/mods/5fcaf5f6990f6.jpg",
      "https://images.gamebanana.com/img/ss/mods/5fcaf5ffe2893.jpg"
    ],
    "Description": "Made by KawaiiDawn",
    "Views": 3456,
    "GameBananaType": "Mod",
    "GameBananaId": 53717,
    "Text": "NOTE: THIS RANDOMIZER WILL SOON BE OBSOLETED BY ANOTHER COLLAB RANDOMIZER THAT IS FARTHER IN PROGRESS THAN THIS ONE, BE SURE TO DISABLE THIS MOD WHEN THAT MOD COMES OUT (You can still enjoy this until that comes out, think of this as a preview ;D)<br>Bigkahuna is making the updated one btw<br><br>This mod adds in randomizer options for maps from the 2020 Celeste Community Spring Collab. Currently, only beginner maps are available, but I am still actively working to add in every map.&nbsp;If you have any questions feel free to post here or message me on discord:&nbsp;?KawaiiDawn?#2795<br>FINISHED:<br>Beginner Maps<br>PLANNED:<br>Intermediate Maps, Advanced Maps, Expert Maps, Grandmaster Maps<br>KNOWN ISSUES:<br>Crystal Enigma causes the game to crash at the randomizer menu, so that map is currently absent from the list of available maps.<br>Screens from certain maps may appear much less often, I don't know why this happens.<br>The game randomizes the custom tileset slots, but I plan to keep this as I think randomized tilesets is a neat feature.<br>Some rooms may be impossible. I went through every room, but there still may be issues in some. Contact me if you find an impossible room.",
    "Name": "2020 Spring Collab Randomizer (v0.1)",
    "PageURL": "https://gamebanana.com/mods/53717",
    "MirroredScreenshots": [
      "https://celestemodupdater.0x0a.de/banana-mirror-images/img_ss_mods_5fcaf5f6990f6.png",
      "https://celestemodupdater.0x0a.de/banana-mirror-images/img_ss_mods_5fcaf5ffe2893.png"
    ],
    "CreatedDate": 1607137213,
    "Author": "KawaiiDawn",
    "CategoryName": "Other/Misc",
    "Downloads": 209,
    "Likes": 1,
    "Files": [
      {
        "Description": "",
        "HasEverestYaml": true,
        "Size": 8859,
        "CreatedDate": 1607136797,
        "Downloads": 209,
        "URL": "https://gamebanana.com/dl/499384",
        "Name": "sc2020rando.zip"
      }
    ]
  },
  [...]
]
```

## GameBanana sorted list API

This API allows to get a sorted list of most downloaded, liked or viewed Celeste mods on GameBanana.

If you want to retrieve the latest mod with no type filter, it is recommended to use [the real GameBanana API](https://api.gamebanana.com/docs/endpoints/Core/List/New) instead, for more up-to-date information.

The URL is `https://max480-random-stuff.appspot.com/celeste/gamebanana-list?sort=[sort]&type=[type]&category=[category]&page=[page]` where:
- `sort` is the info to sort on (**mandatory**). It can be `latest`, `likes`, `views` or `downloads`
- `type` (or `itemtype`) is the GameBanana type to filter on (optional and case-insensitive). For example `Map`, `Gamefile` or `Tool`
- `category` is the GameBanana mod category ID to filter on (optional), this is returned by [the GameBanana categories list API](#gamebanana-categories-list-api). For example `944`
- `page` is the page to get, first page being 1 (optional, default is 1). Each page contains 20 elements.

The output format is the same as the GameBanana search API, [see the previous section](#the-gamebanana-search-api). You also get the total amount of mods in the list, as a `X-Total-Count` header.

You can pass `full=true` to the API to get the full info on mods instead of just their IDs, **in JSON**. The format is also the same as the GameBanana search API with `full=true`.

## GameBanana featured mods list API

Hit the following URL: `https://max480-random-stuff.appspot.com/celeste/gamebanana-featured` to get a list of all mods that are shown in the front page of Celeste.

The output is JSON, in the same as the GameBanana search API with the `full=true` option ([see the previous section](#the-gamebanana-search-api)).

Each mod has a `Featured` key if it is featured. The `Category` is one of the following:
- `today`: Best of today
- `week`: Best of this week
- `month`: Best of this month
- `3month`: Best of 3 months
- `6month`: Best of 6 months
- `year`: Best of this year
- `alltime`: Best of all time

This API sorts the mods in the same order as they are on the website: by `Category` (following the order above) then by `Position`.

## GameBanana categories list API

This API allows getting a list of GameBanana item types _that have at least one Celeste mod in it_ (contrary to [the official GameBanana v2 API for this](https://api.gamebanana.com/Core/Item/Data/AllowedItemTypes?&help)), along with how many mods there are for each category.

The counts returned by this API might not match the numbers displayed on the GameBanana website; that's because GameBanana counts mods that do not show up in the list.

Each entry can have an `itemtype` and a `categoryid`, that can be used as `type` and `category` parameters on the [list API](#gamebanana-sorted-list-api).

The URL is `https://max480-random-stuff.appspot.com/celeste/gamebanana-categories?version=3` and the result looks like:
```yaml
- formatted: All
  count: 1406
- itemtype: Mod
  categoryid: 15655
  formatted: Assets
  count: 26
- itemtype: Mod
  categoryid: 4633
  formatted: Dialog
  count: 20
- itemtype: Mod
  categoryid: 1501
  formatted: Effects
  count: 5
- itemtype: Tool
  formatted: Tools
  count: 30
...
```

Older versions are available for backwards compatibility:
- `?version=2` does not include `itemtype: Mod` for mods, and the "All" category has `itemtype: ''`
- `?version=1` (or omitting entirely) only groups by itemtype (so all Mods end up in one big category)

## GameBanana category RSS feed API

_Note that this API works for all GameBanana categories, not only Celeste ones._

This API uses the "get mods for a category" API from GameBanana, then turns the result in an RSS format.

Usage is very similar to the "vanilla" GameBanana API, since it calls it directly behind the scenes: for example, getting the latest Celeste helpers (https://gamebanana.com/mods/cats/5081 :arrow_right: ID is 5081) is done with
```
https://gamebanana.com/apiv8/Mod/ByCategory?_csvProperties=@gbprofile&_aCategoryRowIds[]=5081&_sOrderBy=_tsDateAdded,DESC&_nPerpage=10
```

You can get them in the RSS format by just replacing the URL and carrying over all query params except `_csvProperties`:
```
https://max480-random-stuff.appspot.com/gamebanana/rss-feed?_aCategoryRowIds[]=5081&_sOrderBy=_tsDateAdded,DESC&_nPerpage=10
```

You can copy this URL replacing `5081` with the category of your choice to get a feed for the latest mods in this category.
To include mod updates (and not only new mods), replace `_tsDateAdded` with `_tsDateUpdated`.

If the GameBanana API returns an error (for example if you pass an invalid parameter), this API will return it as is.

## Random Celeste map button

Click [here](https://max480-random-stuff.appspot.com/celeste/random-map) to get redirected to a random Celeste map.

## Olympus News API

This API is available at `https://max480-random-stuff.appspot.com/celeste/olympus-news`, and returns the news to display in Olympus, in JSON format. For example:
```json
[
  {
    "preview": "Shattering Strawberries is a mod that lets you make your favourite collectable explode, and was created entirely by catapillie! Featuring customisable settings for amount of jooce and shards, you can check it out here:",
    "image": "https://pbs.twimg.com/media/FRfYg5zUcAAIa1a.jpg",
    "link": "https://gamebanana.com/mods/374294",
    "title": "Shattering Strawberries"
  }
]
```

The fields are all optional, and are:
- `title`: the title of the news
- `preview`: the text directly displayed in the sidebar of Olympus
- `image`: an image that will be displayed between the `title` and the `preview`
- `link`: a link that can be opened by clicking an "Open in browser" button (links in the `preview` are not clickable)
- `text`: longer text that can be viewed in a popup by clicking a button at the bottom of the news

The contents of the API are based on:
- The 10 most recent tweets of [the @EverestAPI Twitter](https://twitter.com/EverestAPI)
  - `preview` is filled with the tweet text, excluding non-ascii characters
  - `image` is filled with the first image of the tweet. If there is none, the embed image of the first link that has one in the message will be taken instead.
  - `title` is filled with the embed title of the first link that has one in the message. To accomodate GameBanana links better, `[Celeste] [Mods]` is also removed from it.
  - `link` is filled with the first link in the tweet. This link is also removed from the `preview`.
- [a static file from the RandomBackendStuff repository](https://github.com/max4805/RandomBackendStuff/blob/main/olympusnews.json)
  - This file follows the same format as this API, with an extra field: `position` that is either `top` or `bottom`, and decides whether the news should be shown before or after the tweets.
  
## Everest versions list API

This API is available at `https://max480-random-stuff.appspot.com/celeste/everest-versions`, and returns all the Everest versions available, in JSON format.  For example:
```json
[
    {
        "version": 3520,
        "branch": "dev",
        "author": "bigkahuna443",
        "description": "Handle duplicate room names",
        "date": "2022-08-10T15:09:33.8105837Z",
        "mainDownload": "https://dev.azure.com/EverestAPI/Everest/_apis/build/builds/2820/artifacts?artifactName=main&api-version=5.0&%24format=zip",
        "olympusMetaDownload": "https://dev.azure.com/EverestAPI/Everest/_apis/build/builds/2820/artifacts?artifactName=olympus-meta&api-version=5.0&%24format=zip",
        "olympusBuildDownload": "https://dev.azure.com/EverestAPI/Everest/_apis/build/builds/2820/artifacts?artifactName=olympus-build&api-version=5.0&%24format=zip"
    },
    [...]
]
```

- `version` is the Everest version number.
- `branch` is eiter `dev`, `beta` or `stable`. Other branches might be created to test specific features in the future.
- `mainDownload` is the download for use by Everest to install an update.
- `date` is the date at which the version was published, in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format.
- `olympusMetaDownload` is the download that Olympus uses to get the build size.
- `olympusBuildDownload` is the download for use by Olympus to install Everest.

Two extra fields are specified for automatic builds made after a single change (commit or pull request merge):
- `author` is the author of the change (GitHub username), either the author of the commit or the creator of the pull request.
- `description` is the description of the change, either the message of the commit or the title of the pull request.

## GameBanana Image Mirror API (deprecated)

**Deprecated: You can get the direct URL to the mirrored images by checking `MirroredScreenshots`** in the response of the [search](#gamebanana-search-api) and [list](#gamebanana-sorted-list-api) APIs (by passing `full=true`), and in the [featured mod list](#gamebanana-featured-mods-list-api) API.

--------

This API redirects to a mirror of a GameBanana image on [Banana Mirror](https://celestemodupdater.0x0a.de/banana-mirror-images), hosted by 0x0ade.

This contains every 1st and 2nd screenshot of Celeste submissions on GameBanana, downscaled to 220x220, and converted to PNG.

Usage example: `https://max480-random-stuff.appspot.com/celeste/banana-mirror-image?src=https://images.gamebanana.com/img/ss/mods/5b05ac2b4b6da.webp`

_For compatibility reasons, you can also use `webp-to-png` instead of `banana-mirror-image`._
