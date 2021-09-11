# max480-random-stuff website

This is the source code for the max480-random-stuff.**appspot**.com website, a [Google App Engine](https://cloud.google.com/appengine/) app written in Java, with backend-rendered pages. If you're looking for max480-random-stuff.**herokuapp**.com, [check this repository](https://github.com/max4805/RandomStuffWebsiteJS).

It contains the full source for:
- [the Everest Update Checker frontend service](https://max480-random-stuff.appspot.com/celeste/everest_update.yaml), exposing files produced by the Update Checker
- [the Celeste custom entity catalog](https://max480-random-stuff.appspot.com/celeste/custom-entity-catalog), and [its API version](https://max480-random-stuff.appspot.com/celeste/custom-entity-catalog.json)
- [the everest.yaml validator](https://max480-random-stuff.appspot.com/celeste/everest-yaml-validator)
- [the Everest Update Checker status page](https://max480-random-stuff.appspot.com/celeste/update-checker-status)
- [the Celeste font generator](https://max480-random-stuff.appspot.com/celeste/font-generator)
- [the help page for the Mod Structure Verifier bot](https://max480-random-stuff.appspot.com/celeste/mod-structure-verifier?collabName=CollabName&collabMapName=CollabMapName&assets&xmls&nomap&multiplemaps&badmappath&badenglish&misplacedyaml&noyaml&yamlinvalid&missingassets&missingentities)
- [the "Show Arbitrary Mods on Profile" GameBanana app](https://gamebanana.com/apps/752)
- Some [GameBanana](https://gamebanana.com)-related APIs for the Celeste community described in the few next sections. **A number of those APIs are deprecated since the GameBanana v3 API was published.**

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

## GameBanana sorted list API [deprecated]

**Use the GameBanana v3 API instead**.

For a sorted list of all mods: https://gamebanana.com/apiv3/Mod/ByGame?_aGameRowIds[]=6460&_sRecordSchema=Olympus&_sOrderBy=_tsDateUpdated,DESC&_nPage=1&_nPerpage=20

_sOrderBy can be, among others:
- `_tsDateUpdated,DESC` for most recently updated
- `_nLikeCount,DESC` for most liked
- `_nViewCount,DESC` for most viewed
- `_nDownloadCount,DESC` for most downloaded

For a sorted list of mods in a category: https://gamebanana.com/apiv3/Mod/ByCategory?_sRecordSchema=Olympus&_aCategoryRowIds[]=6800&_sOrderBy=_tsDateUpdated,DESC&_nPage=1&_nPerpage=20

-----

This API allows to get a sorted list of most downloaded, liked or viewed Celeste mods on GameBanana.

If you want to retrieve the latest mod with no type filter, it is recommended to use [the real GameBanana API](https://api.gamebanana.com/docs/endpoints/Core/List/New) instead, for more up-to-date information.

The URL is `https://max480-random-stuff.appspot.com/celeste/gamebanana-list?sort=[sort]&type=[type]&category=[category]&page=[page]` where:
- `sort` is the info to sort on (**mandatory**). It can be `latest`, `likes`, `views` or `downloads`
- `type` (or `itemtype`) is the GameBanana type to filter on (optional and case-insensitive). For example `Map`, `Gamefile` or `Tool`
- `category` is the GameBanana mod category ID to filter on (optional), this is returned by [the GameBanana categories list API](#gamebanana-categories-list-api). For example `944`
- `page` is the page to get, first page being 1 (optional, default is 1). Each page contains 20 elements.

The output format is the same as the GameBanana search API, [see the previous section](#the-gamebanana-search-api).

## GameBanana categories list API [deprecated]

**Use the GameBanana v3 API instead**: https://gamebanana.com/apiv3/ModCategory/ByGame?_aGameRowIds[]=6460&_sRecordSchema=Custom&_csvProperties=_idRow,_sName,_idParentCategoryRow&_nPerpage=50

Root categories can be found by keeping everything that has `_idParentCategoryRow = 0`. This API only lacks submission counts per category.

-----

This API allows getting a list of GameBanana item types _that have at least one Celeste mod in it_ (contrary to [the official GameBanana v2 API for this](https://api.gamebanana.com/Core/Item/Data/AllowedItemTypes?&help)), along with how many mods there are for each category.

The counts returned by this API might not match the numbers displayed on the GameBanana website; that's because GameBanana counts mods that do not show up in the list.

Each entry has either an `itemtype` or a `categoryid` (for mods that have `itemtype` = `Mod`, so that they can be filtered by category instead). The "All" entry is special and has an empty `itemtype`, and carries the total number of mods.

The URL is `https://max480-random-stuff.appspot.com/celeste/gamebanana-categories?version=2` and the result looks like:
```yaml
- itemtype: ''
  formatted: All
  count: 541
- itemtype: Effect
  formatted: Effects
  count: 2
- itemtype: Gamefile
  formatted: Game files
  count: 114
- itemtype: Gui
  formatted: GUIs
  count: 11
- categoryid: 944
  formatted: Textures
  count: 2
...
```

Not passing `?version=2` will result in only `itemtype`s getting returned, with one of them being `Mod`. This is here for backwards compatibility.

## GameBanana Image Mirror API

This API redirects to a mirror of a GameBanana image on [Banana Mirror](https://celestemodupdater.0x0a.de/banana-mirror-images), hosted by 0x0ade.

This contains every 1st and 2nd screenshot of Celeste submissions on GameBanana, downscaled to 220x220, and converted to PNG.

Usage example: `https://max480-random-stuff.appspot.com/celeste/banana-mirror-image?src=https://images.gamebanana.com/img/ss/mods/5b05ac2b4b6da.webp`

_For compatibility reasons, you can also use `webp-to-png` instead of `banana-mirror-image`._

## GameBanana category RSS feed API

_Note that this API works for all GameBanana categories, not only Celeste ones._

This API uses the "get mods for a category" API from GameBanana, then turns the result in an RSS format.

Usage is very similar to the "vanilla" GameBanana API, since it calls it directly behind the scenes: for example, getting the latest Celeste helpers (https://gamebanana.com/mods/cats/5081 :arrow_right: ID is 5081) is done with
```
https://gamebanana.com/apiv5/Mod/ByCategory?_csvProperties=@gbprofile&_aCategoryRowIds[]=5081&_sOrderBy=_tsDateAdded,DESC&_nPerpage=10
```

You can get them in the RSS format by just replacing the URL and carrying over all query params except `_csvProperties`:
```
https://max480-random-stuff.appspot.com/gamebanana/rss-feed?_aCategoryRowIds[]=5081&_sOrderBy=_tsDateAdded,DESC&_nPerpage=10
```

You can copy this URL replacing `5081` with the category of your choice to get a feed for the latest mods in this category.
To include mod updates (and not only new mods), replace `_tsDateAdded` with `_tsDateUpdated`.

If the GameBanana API returns an error (for example if you pass an invalid parameter), this API will return it as is.
