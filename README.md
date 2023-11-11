# FREE Browser

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="150"/> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width="150"/>  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width="150"/> 

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04.png" width="150"/> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05.png" width="150"/> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/06.png" width="150"/>


FREE Browser is a web browser for optimal privacy

- fully open source
- no trackers
- no unnecessary permissions

<a href="https://f-droid.org/packages/org.woheller69.browser/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>


## FEATURES

- AdBlocker using StevenBlack host list
- Measures against browser fingerprinting
- Advanced settings for javascript, cookies and DOM-storage (domain/bookmark based)
- Support for Greasemonkey style scripts
- Optimized for one hand handling (toolbar at bottom)
- TAB control (switch, open, close, unlimited tabs)
- Fast toggle for most important settings
- Search current website
- Web search (from marked text via context menu)
- Save as PDF
- Open links in other apps (for example YouTube)
- Backup
- etc

## LICENSE

This app is licensed under the GPLv3.

The app uses code from:
- FOSS-Browser, https://github.com/woheller69/browser, published under GPLv3 (at time of fork)
- Ninja, https://github.com/mthli/Ninja, published under Apache-2.0 license
- StevenBlack hosts, https://github.com/StevenBlack/hosts, published under MIT license

## INSTRUCTIONS

<pre>Send a coffee to 
woheller69@t-online.de 
<a href= "https://www.paypal.com/signin"><img  align="left" src="https://www.paypalobjects.com/webstatic/de_DE/i/de-pp-logo-150px.png"></a></pre>

### Main Navigation
<img src="Instructions.png" width="300"/>

The main navigation features are depicted in the image above.

For each tab it is possible to enable/disable:
- AdBlock
- Anti-Browser-Fingerprinting measures
- Desktop Mode
- DOM-Storage
- JavaScript

These settings (except desktop mode) are inherited from global settings when a new tab is created.
They will always be applied when a new web site is opened.

FREE Browser allows bookmark specific settings for JavaScript, DOM-Storage, and Desktop mode.
If a bookmark is opened these settings will be applied, no matter which other settings are valid for the tab.
If this is the case the bookmark symbol in "Exceptions" will be highlighted. When browsing within the domain of the
bookmark these settings will remain.

In addition you can define domains where Cookies, DOM-Storage, and JavaScript are always allowed (see Settings -> Browser Settings).
Cookies will override the global cookies setting. DOM-Storage and JavaScript will override the tab specific settings.
If one of these exceptions is active the respective icon will also be highlighted in "Exceptions". 
A click on the icon will add/remove an exception.

In additions there are settings which are only available as global settings:
- Allow location access
- Allow camera access
- Allow microphone access
- Download images. This allows to save data, **when not connected to WIFI**. If WIFI is available images will always be loaded.

### Greasemonkey style scripts

FREE Browser supports simple user scripts in Greasemonkey style.
The following tags:
- @match
- @run-at
- @name

@run-at:  
If defined as "document-start" scripts run in onPageStarted() of Android WebView, 
otherwise scripts run in onPageFinished.

Other tags are **NOT** supported at the moment, e.g.
- @include
- @exclude
- @grant
- @required

### Browser Settings

In this section you can define your favourite start page, search engine, etc.
You can select your favourite StevenBlack AdBlock list.
And this is the place to manage exceptions for cookies, javascript, and DOM storage.


### Backup / restore

You can save / restore app data (=databases), bookmarks, and preferences.
Data will be stored in Documents/browser_backup.

# OTHER APPS

| **RadarWeather** | **Gas Prices** | **Smart Eggtimer** | 
|:---:|:---:|:---:|
| [<img src="https://github.com/woheller69/weather/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.weather/)| [<img src="https://github.com/woheller69/spritpreise/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.spritpreise/) | [<img src="https://github.com/woheller69/eggtimer/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.eggtimer/) |
| **Bubble** | **hEARtest** | **GPS Cockpit** |
| [<img src="https://github.com/woheller69/Level/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.level/) | [<img src="https://github.com/woheller69/audiometry/blob/new/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.audiometry/) | [<img src="https://github.com/woheller69/gpscockpit/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.gpscockpit/) |
| **Audio Analyzer** | **LavSeeker** | **TimeLapseCam** |
| [<img src="https://github.com/woheller69/audio-analyzer-for-android/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.audio_analyzer_for_android/) |[<img src="https://github.com/woheller69/lavatories/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.lavatories/) | [<img src="https://github.com/woheller69/TimeLapseCamera/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.TimeLapseCam/) |
| **Arity** | **omWeather** | **solXpect** |
| [<img src="https://github.com/woheller69/arity/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.arity/) | [<img src="https://github.com/woheller69/omweather/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.omweather/) | [<img src="https://github.com/woheller69/solXpect/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.solxpect/) |
| **gptAssist** | **dumpSeeker** | **huggingAssist** |
| [<img src="https://github.com/woheller69/gptassist/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.gptassist/) | [<img src="https://github.com/woheller69/dumpseeker/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.dumpseeker/) | [<img src="https://github.com/woheller69/huggingassist/blob/master/fastlane/metadata/android/en-US/images/icon.png" width="50">](https://f-droid.org/packages/org.woheller69.hugassist/) |
