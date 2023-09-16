# Maddie's Random Stuff Website
This is the source code for the [maddie480.ovh](https://maddie480.ovh/) website, an app running on Java 17 using [Eclipse Jetty](https://www.eclipse.org/jetty/).

The `src` folder contains the source for everything provided by the Java servlet:
- [the Everest Update Checker frontend service](https://maddie480.ovh/celeste/everest_update.yaml), exposing files produced by the Update Checker
- [the Celeste custom entity catalog](https://maddie480.ovh/celeste/custom-entity-catalog), and [its API version](https://maddie480.ovh/celeste/custom-entity-catalog.json)
- [the everest.yaml validator](https://maddie480.ovh/celeste/everest-yaml-validator)
- [an online version of Olympus News](https://maddie480.ovh/celeste/olympus-news)
- [the Everest Update Checker status page](https://maddie480.ovh/celeste/update-checker-status)
- [the Celeste font generator](https://maddie480.ovh/celeste/font-generator)
- [the Celeste Mod Structure Verifier](https://maddie480.ovh/celeste/mod-structure-verifier)
- [the #celeste_news_network subscription service](https://maddie480.ovh/celeste/news-network-subscription) - the Mastodon update checking code is not part of the frontend and can be found [on the Random Backend Stuff repository](https://github.com/maddie480/RandomBackendStuff/blob/main/src/main/java/com/max480/randomstuff/backend/celeste/crontabs/MastodonUpdateChecker.java)
- [the help page for the Mod Structure Verifier bot](https://maddie480.ovh/celeste/mod-structure-verifier-help?collabName=CollabName&collabMapName=CollabMapName&assets&xmls&nomap&multiplemaps&badmappath&badenglish&misplacedyaml&noyaml&yamlinvalid&multiyaml&missingassets&missingentities&missingfonts)
- [the "Show Arbitrary Mods on Profile" GameBanana app](https://gamebanana.com/apps/752)
- [the Discord Games Bot](https://maddie480.ovh/discord-bots#games-bot) - the "bot" is actually a webhook that gets called by Discord
- [the Discord Custom Slash Commands app](https://maddie480.ovh/discord-bots#custom-slash-commands)
- [the Discord Timezone Bot](https://maddie480.ovh/discord-bots#timzeone-bot) - this repository only includes the "without timezone roles" variant; the one with timezone roles needs a bot user to work, and as such is [part of the backend](https://github.com/maddie480/RandomBackendStuff/tree/main/src/main/java/com/max480/randomstuff/backend/discord/timezonebot)
- Some [GameBanana](https://gamebanana.com)-related APIs extending the official API, described below
- [the BananaBot app for Discord](https://maddie480.ovh/discord-bots#bananabot) - allows searching for Celeste mods on GameBanana, using the same search as Olympus and the Banana Mirror Browser
- the [Everest](https://maddie480.ovh/celeste/everest-versions) and [Olympus](https://maddie480.ovh/celeste/olympus-versions) versions list APIs
- ... and some other things of more limited use that are on the website.

The `front-vue` folder contains the source for more dynamic frontend parts made in [Vue.js](https://vuejs.org/):
- [the Banana Mirror Browser](https://maddie480.ovh/celeste/banana-mirror-browser), a website that can substitute to [the GameBanana Celeste section](https://gamebanana.com/games/6460) if it is down. Files and images are [hosted on 0x0ade's server](https://celestemodupdater.0x0a.de/). Mod info and files on this server are kept up-to-date by [the Everest update checker](https://github.com/maddie480/EverestUpdateCheckerServer).
- [the Celeste Map Tree Viewer](https://maddie480.ovh/celeste/map-tree-viewer), a tool allowing you to see the raw contents of your map .bin as a tree, in order to find out what it is or search for a specific entity, trigger, decal or styleground.
- [the Celeste Wipe Converter](https://maddie480.ovh/celeste/wipe-converter), a service to turn custom Celeste screen wipes into a format the game can use (with the [Maddie's Helping Hand](https://github.com/maddie480/MaddieHelpingHand) mod).
- [the Celeste File Searcher](https://maddie480.ovh/celeste/file-searcher), a tool to find in which Celeste mod(s) a file is on GameBanana, based on its path in the zip.

If you want to check how the update checker's everest_update.yaml file is generated, check [the Everest Update Checker Server repo](https://github.com/maddie480/EverestUpdateCheckerServer) instead.

## GameBanana search API

This API uses the mod search database generated by [the Everest Update Checker server](https://github.com/maddie480/EverestUpdateCheckerServer) to find mods based on keywords. **This searches Celeste mods only**.

It is used by [Olympus](https://github.com/EverestAPI/Olympus), the Everest installer and mod manager, to search Celeste mods on GameBanana.

To use this API, call `https://maddie480.ovh/celeste/gamebanana-search?q=[search]`. The answer is in JSON format, and is a list of the top 20 matches. For example:

```
$ curl "https://maddie480.ovh/celeste/gamebanana-search?q=spring+collab+2020&full=true"
```
```json
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

The URL is `https://maddie480.ovh/celeste/gamebanana-list?sort=[sort]&type=[type]&category=[category]&page=[page]` where:
- `sort` is the info to sort on (**mandatory**). It can be `latest`, `likes`, `views` or `downloads`
- `type` (or `itemtype`) is the GameBanana type to filter on (optional and case-insensitive). For example `Map`, `Gamefile` or `Tool`
- `category` is the GameBanana mod category ID to filter on (optional), this is returned by [the GameBanana categories list API](#gamebanana-categories-list-api). For example `944`
- `page` is the page to get, first page being 1 (optional, default is 1). Each page contains 20 elements.

The output format is the same as the GameBanana search API, [see the previous section](#gamebanana-search-api). You also get the total amount of mods in the list, as a `X-Total-Count` header.

## GameBanana featured mods list API

Hit the following URL: `https://maddie480.ovh/celeste/gamebanana-featured` to get a list of all mods that are shown in the front page of Celeste.

The output is JSON, in the same as the GameBanana search API ([see the corresponding section](#gamebanana-search-api)).

Each mod has a `Featured` key if it is featured. The `Category` is one of the following:
- `today`: Best of today
- `week`: Best of this week
- `month`: Best of this month
- `3month`: Best of 3 months
- `6month`: Best of 6 months
- `year`: Best of this year
- `alltime`: Best of all time

This API sorts the mods in the same order as they are on the website: by `Category` (following the order above) then by `Position`.

## GameBanana mod info API

This API provides info on a mod, based on its `itemtype` and `itemid`, in the same as the GameBanana search API ([see the corresponding section](#gamebanana-search-api)).

The URL is `https://maddie480.ovh/celeste/gamebanana-info?itemtype=[itemtype]&itemid=[itemid]`:
```
$ curl "https://maddie480.ovh/celeste/gamebanana-info?itemtype=Mod&itemid=150813"
```
```json
{
   "CategoryId":6800,
   "Screenshots":[
      "https://images.gamebanana.com/img/ss/mods/5f590aced2b97.jpg",
      "https://images.gamebanana.com/img/ss/mods/5f46e9ee53c9b.jpg"
   ],
   "Description":"A collaboration involving 100+ people to commemorate Celeste",
   "Views":511063,
   "GameBananaType":"Mod",
   "TokenizedName":[
      "the",
      "2020",
      "celeste",
      "spring",
      "community",
      "collab"
   ],
   "UpdatedDate":1668375864,
   "GameBananaId":150813,
   "Featured":{
      "Position":0,
      "Category":"alltime"
   },
   "Text":"<span class=\"GreenColor\"><b>The 2020 Celeste Spring Collab is a project that has been in the making for over 8 months with 100+ people involved. <\/b>After waiting patiently for so long, we're proud to present you the final product!<br><\/span> [...]",
   "ModifiedDate":1668375829,
   "Name":"The 2020 Celeste Spring Community Collab",
   "PageURL":"https://gamebanana.com/mods/150813",
   "MirroredScreenshots":[
      "https://celestemodupdater.0x0a.de/banana-mirror-images/img_ss_mods_5f590aced2b97.png",
      "https://celestemodupdater.0x0a.de/banana-mirror-images/img_ss_mods_5f46e9ee53c9b.png"
   ],
   "CreatedDate":1599670994,
   "Author":"Spring Collab 2020 Team",
   "CategoryName":"Maps",
   "Downloads":100049,
   "Likes":154,
   "Files":[
      {
         "Description":"v1.7.5",
         "HasEverestYaml":true,
         "Size":56290503,
         "CreatedDate":1668375774,
         "Downloads":12687,
         "URL":"https://gamebanana.com/dl/890982",
         "Name":"springcollab2020_bdf5e.zip"
      },
      {
         "Description":"Audio v1.0.0",
         "HasEverestYaml":true,
         "Size":513411848,
         "CreatedDate":1599575103,
         "Downloads":26737,
         "URL":"https://gamebanana.com/dl/484937",
         "Name":"springcollab2020audio_135a4.zip"
      },
      {
         "Description":"Outdated - Old Grandmaster HS",
         "HasEverestYaml":true,
         "Size":159602,
         "CreatedDate":1616693898,
         "Downloads":918,
         "URL":"https://gamebanana.com/dl/539975",
         "Name":"springcollab2020oldgmhs_ee692.zip"
      }
   ]
}
```

## GameBanana categories list API

This API allows getting a list of GameBanana item types _that have at least one Celeste mod in it_ (contrary to [the official GameBanana v2 API for this](https://api.gamebanana.com/Core/Item/Data/AllowedItemTypes?&help)), along with how many mods there are for each category.

The counts returned by this API might not match the numbers displayed on the GameBanana website; that's because GameBanana counts mods that do not show up in the list.

Each entry can have an `itemtype` and a `categoryid`, that can be used as `type` and `category` parameters on the [list API](#gamebanana-sorted-list-api).

The URL is `https://maddie480.ovh/celeste/gamebanana-categories` and the result looks like:
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

## GameBanana category RSS feed API

_Note that this API works for all GameBanana categories, not only Celeste ones._

This API uses the "get mods for a category" API from GameBanana, then turns the result in an RSS format.

Usage is very similar to the "vanilla" GameBanana API, since it calls it directly behind the scenes: for example, getting the latest Celeste helpers (https://gamebanana.com/mods/cats/5081 :arrow_right: ID is 5081) is done with
```
https://gamebanana.com/apiv8/Mod/ByCategory?_csvProperties=@gbprofile&_aCategoryRowIds[]=5081&_sOrderBy=_tsDateAdded,DESC&_nPerpage=10
```

You can get them in the RSS format by just replacing the URL and carrying over all query params except `_csvProperties`:
```
https://maddie480.ovh/gamebanana/rss-feed?_aCategoryRowIds[]=5081&_sOrderBy=_tsDateAdded,DESC&_nPerpage=10
```

You can copy this URL replacing `5081` with the category of your choice to get a feed for the latest mods in this category.
To include mod updates (and not only new mods), replace `_tsDateAdded` with `_tsDateUpdated`.

If the GameBanana API returns an error (for example if you pass an invalid parameter), this API will return it as is.

## Random Celeste map button

Click [here](https://maddie480.ovh/celeste/random-map) to get redirected to a random Celeste map.

## Everest versions list API

This API is available at `https://maddie480.ovh/celeste/everest-versions`, and returns all the Everest versions available, in JSON format.  For example:
```json
[
    {
        "date": "2023-07-15T20:47:39.2562228Z",
        "mainFileSize": 19034073,
        "mainDownload": "https://dev.azure.com/EverestAPI/Everest/_apis/build/builds/3371/artifacts?artifactName=main&api-version=5.0&%24format=zip",
        "olympusMetaDownload": "https://dev.azure.com/EverestAPI/Everest/_apis/build/builds/3371/artifacts?artifactName=olympus-meta&api-version=5.0&%24format=zip",
        "author": "Kalobi",
        "olympusBuildDownload": "https://dev.azure.com/EverestAPI/Everest/_apis/build/builds/3371/artifacts?artifactName=olympus-build&api-version=5.0&%24format=zip",
        "description": "Fix update notification not showing on older installs",
        "branch": "dev",
        "version": 4071,
        "isNative": false
    },
    [...]
]
```

- `version` is the Everest version number.
- `branch` is either `dev`, `beta` or `stable`. Other branches might be created to test specific features in the future.
- `isNative` indicates whether this is a .NET Core build. Native builds (using .NET Core) are currently experimental, but non-native builds (.NET Framework) will be deprecated in the future.
- `mainDownload` is the download for use by Everest to install an update.
- `mainFileSize` is the size of the `mainDownload` file, in bytes.
- `date` is the date at which the version was published, in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format.
- `olympusMetaDownload` is the download that Olympus uses to get the build size.
- `olympusBuildDownload` is the download for use by Olympus to install Everest.

Two extra fields are specified for automatic builds made after a single change (commit or pull request merge):
- `author` is the author of the change (GitHub username), either the author of the commit or the creator of the pull request.
- `description` is the description of the change, either the message of the commit or the title of the pull request.

If the version of Everest/Olympus you are calling this API from supports installing native builds (.NET Core builds), you should pass the `supportsNativeBuilds=true` query parameter to include them.
As of now, this adds an extra branch to the output of the API, called `core`.


## Olympus versions list API

This API is available at `https://maddie480.ovh/celeste/olympus-versions`, and returns all the Olympus versions available, in JSON format.  For example:
```json
[
    {
        "date": "2023-09-14T22:04:54.9319239Z",
        "windowsDownload": "https://dev.azure.com/EverestAPI/Olympus/_apis/build/builds/3518/artifacts?artifactName=windows.main&api-version=5.0&%24format=zip",
        "macDownload": "https://dev.azure.com/EverestAPI/Olympus/_apis/build/builds/3518/artifacts?artifactName=macos.main&api-version=5.0&%24format=zip",
        "linuxDownload": "https://dev.azure.com/EverestAPI/Olympus/_apis/build/builds/3518/artifacts?artifactName=linux.main&api-version=5.0&%24format=zip"
        "changelog": "Make sure Lonn is run with the same Love2d install as Olympus itself on Linux",
        "branch": "main",
        "version": "23.09.14.03"
    },
    [...]
]
```

- `version` is the Olympus version number.
- `branch` is either `windows-init`, `main` or `stable`. `windows-init` is the version initially installed by the Windows installer, and `main` is the development branch.
- `windowsDownload`, `macDownload` and `linuxDownload` are download links for each platform.
- `date` is the date at which the version was published, in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format.
- `changelog` is the changelog of the version, pulled straight from the `changelog.txt` file on the repository. (That's where in-app update changelogs come from.)
