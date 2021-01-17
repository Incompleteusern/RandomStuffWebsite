# max480-random-stuff website

This is (part of) the source code for the max480-random-stuff.appspot.com website, a [Google App Engine](https://cloud.google.com/appengine/) app.

It contains the full source for:
- [the Everest Update Checker frontend service](https://max480-random-stuff.appspot.com/celeste/everest_update.yaml)
- [the Celeste custom entity catalog](https://max480-random-stuff.appspot.com/celeste/custom-entity-catalog)
- [the everest.yaml validator](https://max480-random-stuff.appspot.com/celeste/everest-yaml-validator)
- [the GameBanana search API](https://max480-random-stuff.appspot.com/celeste/gamebanana-search)

If you want to check how the update checker's everest_update.yaml file is generated, check [the Everest Update Checker Server repo](https://github.com/max4805/EverestUpdateCheckerServer) instead.

## The GameBanana search API

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
  - one of the **authors**: `author: max480`
  - the **summary** (line that appears first on the page): `summary: (grab bag)`
  - the **description**: `description: "flag touch switches"`

For a full list of supported syntax, check [the Lucene documentation](https://lucene.apache.org/core/8_7_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description).

## The GameBanana sorted list API

This API allows to get a sorted list of most downloaded, liked or viewed Celeste mods on GameBanana.

Since the information used by this API can be late by up to 30 minutes, it does not allow to retrieve the _latest_ mods. For that, use [the real GameBanana API](https://api.gamebanana.com/docs/endpoints/Core/List/New) instead.

The URL is `https://max480-random-stuff.appspot.com/celeste/gamebanana-list?sort=[sort]&type=[type]&page=[page]` where:
- `sort` is the info to sort on (**mandatory**). It can be `likes`, `views` or `downloads`
- `type` is the GameBanana type to filter on (optional and case-insensitive). For example `Map`, `Gamefile` or `Tool`
- `page` is the page to get, first page being 1 (optional, default is 1). Each page contains 20 elements.
