package de.baumann.browser.browser;

import static de.baumann.browser.database.UserScript.DOC_END;
import static de.baumann.browser.database.UserScript.DOC_START;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.view.Gravity;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.database.UserScript;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.ScriptUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebViewClient extends WebViewClient {

    private final NinjaWebView ninjaWebView;
    private final Context context;
    private final SharedPreferences sp;
    private final AdBlock adBlock;

    private final boolean white;
    private boolean enable;
    public void enableAdBlock(boolean enable) {
        this.enable = enable;
    }

    public NinjaWebViewClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
        this.context = ninjaWebView.getContext();
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.adBlock = new AdBlock(this.context);
        this.white = false;
        this.enable = true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        ninjaWebView.isBackPressed = false;

        if (ninjaWebView.isForeground()) {
            ninjaWebView.invalidate();
        } else {
            ninjaWebView.postInvalidate();
        }

        List<UserScript> list = ScriptUnit.findScriptsToExecute(context, url, DOC_END);
        for (UserScript script : list){
            if (sp.getBoolean("sp_debug",false)) NinjaToast.show(context, script.getName());
            view.evaluateJavascript(script.getScript(),null);
        }

        view.evaluateJavascript("var links=document.getElementsByTagName('video'); for(let i=0;i<links.length;i++){links[i].pause()};", null);

        if (sp.getBoolean("sp_deny_cookie_banners",false)){ //click opt-out if possible
            String bannerBlockScript = BannerBlock.getBannerBlockScriptPageFinished();
            if (bannerBlockScript != null) {new Handler().postDelayed(() -> view.evaluateJavascript(bannerBlockScript,null), 50);}
        }

        //inject printing support via JavaScriptInterface
        view.evaluateJavascript(JavaScriptInterface.injectPrintSupport(), null);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        ninjaWebView.setStopped(false);
        ninjaWebView.resetFavicon();
        super.onPageStarted(view,url,favicon);

        List<UserScript> list = ScriptUnit.findScriptsToExecute(context, url, DOC_START);
        for (UserScript script : list){
            if (sp.getBoolean("sp_debug",false)) NinjaToast.show(context, script.getName());
            view.evaluateJavascript(script.getScript(),null);
        }

        if (sp.getBoolean("sp_deny_cookie_banners",false)){  //inject cookies if possible
            String bannerBlockScript = BannerBlock.getBannerBlockScriptPageStarted();
            if (bannerBlockScript != null) view.evaluateJavascript(bannerBlockScript,null);
        }

        if(ninjaWebView.isFingerPrintProtection()) {

            //Block WebRTC requests which can reveal local IP address
            //Tested with https://diafygi.github.io/webrtc-ips/
            view.evaluateJavascript("['createOffer', 'createAnswer','setLocalDescription', 'setRemoteDescription'].forEach(function(method) {\n" +
                    "    webkitRTCPeerConnection.prototype[method] = function() {\n" +
                    "      console.log('webRTC snoop');\n" +
                    "      return null;\n" +
                    "    };\n" +
                    "  });",null);

            //Prevent canvas fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "Canvas Fingerprint Defender", Firefox plugin, Version 0.1.9, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("\n" +
                    "  const toBlob = HTMLCanvasElement.prototype.toBlob;\n" +
                    "  const toDataURL = HTMLCanvasElement.prototype.toDataURL;\n" +
                    "  const getImageData = CanvasRenderingContext2D.prototype.getImageData;\n" +
                    "  //\n" +
                    "  var noisify = function (canvas, context) {\n" +
                    "    if (context) {\n" +
                    "      const shift = {\n" +
                    "        'r': Math.floor(Math.random() * 10) - 5,\n" +
                    "        'g': Math.floor(Math.random() * 10) - 5,\n" +
                    "        'b': Math.floor(Math.random() * 10) - 5,\n" +
                    "        'a': Math.floor(Math.random() * 10) - 5\n" +
                    "      };\n" +
                    "      //\n" +
                    "      const width = canvas.width;\n" +
                    "      const height = canvas.height;\n" +
                    "      if (width && height) {\n" +
                    "        const imageData = getImageData.apply(context, [0, 0, width, height]);\n" +
                    "        for (let i = 0; i < height; i++) {\n" +
                    "          for (let j = 0; j < width; j++) {\n" +
                    "            const n = ((i * (width * 4)) + (j * 4));\n" +
                    "            imageData.data[n + 0] = imageData.data[n + 0] + shift.r;\n" +
                    "            imageData.data[n + 1] = imageData.data[n + 1] + shift.g;\n" +
                    "            imageData.data[n + 2] = imageData.data[n + 2] + shift.b;\n" +
                    "            imageData.data[n + 3] = imageData.data[n + 3] + shift.a;\n" +
                    "          }\n" +
                    "        }\n" +
                    "        //\n" +
                    "        window.top.postMessage(\"canvas-fingerprint-defender-alert\", '*');\n" +
                    "        context.putImageData(imageData, 0, 0); \n" +
                    "      }\n" +
                    "    }\n" +
                    "  };\n" +
                    "  //\n" +
                    "  Object.defineProperty(HTMLCanvasElement.prototype, \"toBlob\", {\n" +
                    "    \"value\": function () {\n" +
                    "      noisify(this, this.getContext(\"2d\"));\n" +
                    "      return toBlob.apply(this, arguments);\n" +
                    "    }\n" +
                    "  });\n" +
                    "  //\n" +
                    "  Object.defineProperty(HTMLCanvasElement.prototype, \"toDataURL\", {\n" +
                    "    \"value\": function () {\n" +
                    "      noisify(this, this.getContext(\"2d\"));\n" +
                    "      return toDataURL.apply(this, arguments);\n" +
                    "    }\n" +
                    "  });\n" +
                    "  //\n" +
                    "  Object.defineProperty(CanvasRenderingContext2D.prototype, \"getImageData\", {\n" +
                    "    \"value\": function () {\n" +
                    "      noisify(this.canvas, this);\n" +
                    "      return getImageData.apply(this, arguments);\n" +
                    "    }\n" +
                    "  });", null);

            //Prevent WebGL fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "WebGL Fingerprint Defender", Firefox plugin, Version 0.1.5, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("\n" +
                    "  var glconfig = {\n" +
                    "    \"random\": {\n" +
                    "      \"value\": function () {\n" +
                    "        return Math.random();\n" +
                    "      },\n" +
                    "      \"item\": function (e) {\n" +
                    "        var rand = e.length * glconfig.random.value();\n" +
                    "        return e[Math.floor(rand)];\n" +
                    "      },\n" +
                    "      \"number\": function (power) {\n" +
                    "        var tmp = [];\n" +
                    "        for (var i = 0; i < power.length; i++) {\n" +
                    "          tmp.push(Math.pow(2, power[i]));\n" +
                    "        }\n" +
                    "        /*  */\n" +
                    "        return glconfig.random.item(tmp);\n" +
                    "      },\n" +
                    "      \"int\": function (power) {\n" +
                    "        var tmp = [];\n" +
                    "        for (var i = 0; i < power.length; i++) {\n" +
                    "          var n = Math.pow(2, power[i]);\n" +
                    "          tmp.push(new Int32Array([n, n]));\n" +
                    "        }\n" +
                    "        /*  */\n" +
                    "        return glconfig.random.item(tmp);\n" +
                    "      },\n" +
                    "      \"float\": function (power) {\n" +
                    "        var tmp = [];\n" +
                    "        for (var i = 0; i < power.length; i++) {\n" +
                    "          var n = Math.pow(2, power[i]);\n" +
                    "          tmp.push(new Float32Array([1, n]));\n" +
                    "        }\n" +
                    "        /*  */\n" +
                    "        return glconfig.random.item(tmp);\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"spoof\": {\n" +
                    "      \"webgl\": {\n" +
                    "        \"buffer\": function (target) {\n" +
                    "          var proto = target.prototype ? target.prototype : target.__proto__;\n" +
                    "          const bufferData = proto.bufferData;\n" +
                    "          Object.defineProperty(proto, \"bufferData\", {\n" +
                    "            \"value\": function () {\n" +
                    "              var index = Math.floor(glconfig.random.value() * arguments[1].length);\n" +
                    "              var noise = arguments[1][index] !== undefined ? 0.1 * glconfig.random.value() * arguments[1][index] : 0;\n" +
                    "              //\n" +
                    "              arguments[1][index] = arguments[1][index] + noise;\n" +
                    "              window.top.postMessage(\"webgl-fingerprint-defender-alert\", '*');\n" +
                    "              //\n" +
                    "              return bufferData.apply(this, arguments);\n" +
                    "            }\n" +
                    "          });\n" +
                    "        },\n" +
                    "        \"parameter\": function (target) {\n" +
                    "          var proto = target.prototype ? target.prototype : target.__proto__;\n" +
                    "          const getParameter = proto.getParameter;\n" +
                    "          Object.defineProperty(proto, \"getParameter\", {\n" +
                    "            \"value\": function () {\n" +
                    "              window.top.postMessage(\"webgl-fingerprint-defender-alert\", '*');\n" +
                    "              //\n" +
                    "              if (arguments[0] === 3415) return 0;\n" +
                    "              else if (arguments[0] === 3414) return 24;\n" +
                    "              else if (arguments[0] === 36348) return 30;\n" +
                    "              else if (arguments[0] === 7936) return \"WebKit\";\n" +
                    "              else if (arguments[0] === 37445) return \"Google Inc.\";\n" +
                    "              else if (arguments[0] === 7937) return \"WebKit WebGL\";\n" +
                    "              else if (arguments[0] === 3379) return glconfig.random.number([14, 15]);\n" +
                    "              else if (arguments[0] === 36347) return glconfig.random.number([12, 13]);\n" +
                    "              else if (arguments[0] === 34076) return glconfig.random.number([14, 15]);\n" +
                    "              else if (arguments[0] === 34024) return glconfig.random.number([14, 15]);\n" +
                    "              else if (arguments[0] === 3386) return glconfig.random.int([13, 14, 15]);\n" +
                    "              else if (arguments[0] === 3413) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 3412) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 3411) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 3410) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 34047) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 34930) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 34921) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 35660) return glconfig.random.number([1, 2, 3, 4]);\n" +
                    "              else if (arguments[0] === 35661) return glconfig.random.number([4, 5, 6, 7, 8]);\n" +
                    "              else if (arguments[0] === 36349) return glconfig.random.number([10, 11, 12, 13]);\n" +
                    "              else if (arguments[0] === 33902) return glconfig.random.float([0, 10, 11, 12, 13]);\n" +
                    "              else if (arguments[0] === 33901) return glconfig.random.float([0, 10, 11, 12, 13]);\n" +
                    "              else if (arguments[0] === 37446) return glconfig.random.item([\"Graphics\", \"HD Graphics\", \"Intel(R) HD Graphics\"]);\n" +
                    "              else if (arguments[0] === 7938) return glconfig.random.item([\"WebGL 1.0\", \"WebGL 1.0 (OpenGL)\", \"WebGL 1.0 (OpenGL Chromium)\"]);\n" +
                    "              else if (arguments[0] === 35724) return glconfig.random.item([\"WebGL\", \"WebGL GLSL\", \"WebGL GLSL ES\", \"WebGL GLSL ES (OpenGL Chromium\"]);\n" +
                    "              //\n" +
                    "              return getParameter.apply(this, arguments);\n" +
                    "            }\n" +
                    "          });\n" +
                    "        }\n" +
                    "      }\n" +
                    "    }\n" +
                    "  };\n" +
                    "  //\n" +
                    "  glconfig.spoof.webgl.buffer(WebGLRenderingContext);\n" +
                    "  glconfig.spoof.webgl.buffer(WebGL2RenderingContext);\n" +
                    "  glconfig.spoof.webgl.parameter(WebGLRenderingContext);\n" +
                    "  glconfig.spoof.webgl.parameter(WebGL2RenderingContext);", null);

            //Prevent AudioContext fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "AudioContext Fingerprint Defender", Firefox plugin, Version 0.1.6, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("\n" +
                    "    const acontext = {\n" +
                    "    \"BUFFER\": null,\n" +
                    "    \"getChannelData\": function (e) {\n" +
                    "      const getChannelData = e.prototype.getChannelData;\n" +
                    "      Object.defineProperty(e.prototype, \"getChannelData\", {\n" +
                    "        \"value\": function () {\n" +
                    "          const results_1 = getChannelData.apply(this, arguments);\n" +
                    "          if (acontext.BUFFER !== results_1) {\n" +
                    "            acontext.BUFFER = results_1;\n" +
                    "            for (var i = 0; i < results_1.length; i += 100) {\n" +
                    "              let index = Math.floor(Math.random() * i);\n" +
                    "              results_1[index] = results_1[index] + Math.random() * 0.0000001;\n" +
                    "            }\n" +
                    "          }\n" +
                    "          //\n" +
                    "          return results_1;\n" +
                    "        }\n" +
                    "      });\n" +
                    "    },\n" +
                    "    \"createAnalyser\": function (e) {\n" +
                    "      const createAnalyser = e.prototype.__proto__.createAnalyser;\n" +
                    "      Object.defineProperty(e.prototype.__proto__, \"createAnalyser\", {\n" +
                    "        \"value\": function () {\n" +
                    "          const results_2 = createAnalyser.apply(this, arguments);\n" +
                    "          const getFloatFrequencyData = results_2.__proto__.getFloatFrequencyData;\n" +
                    "          Object.defineProperty(results_2.__proto__, \"getFloatFrequencyData\", {\n" +
                    "            \"value\": function () {\n" +
                    "              const results_3 = getFloatFrequencyData.apply(this, arguments);\n" +
                    "              for (var i = 0; i < arguments[0].length; i += 100) {\n" +
                    "                let index = Math.floor(Math.random() * i);\n" +
                    "                arguments[0][index] = arguments[0][index] + Math.random() * 0.1;\n" +
                    "              }\n" +
                    "              //\n" +
                    "              return results_3;\n" +
                    "            }\n" +
                    "          });\n" +
                    "          //\n" +
                    "          return results_2;\n" +
                    "        }\n" +
                    "      });\n" +
                    "    }\n" +
                    "  };\n" +
                    "  //\n" +
                    "  acontext.getChannelData(AudioBuffer);\n" +
                    "  acontext.createAnalyser(AudioContext);\n" +
                    "  acontext.getChannelData(OfflineAudioContext);\n" +
                    "  acontext.createAnalyser(OfflineAudioContext);  ", null);

            //Prevent Font fingerprinting by randomizing
            //can be tested e.g. at https://webbrowsertools.com
            //
            //The Javascript part below is taken from "Font Fingerprint Defender", Firefox plugin, Version 0.1.3, by ilGur
            //The source code has been published originally under Mozilla Public License V2.0. You can obtain a copy of the license at https://mozilla.org/MPL/2.0/
            //The author has given explicit written permission to use his code under GPL V3 in this project.

            view.evaluateJavascript("\n" +
                    "  var rand = {\n" +
                    "    \"noise\": function () {\n" +
                    "      var SIGN = Math.random() < Math.random() ? -1 : 1;\n" +
                    "      return Math.floor(Math.random() + SIGN * Math.random());\n" +
                    "    },\n" +
                    "    \"sign\": function () {\n" +
                    "      const tmp = [-1, -1, -1, -1, -1, -1, +1, -1, -1, -1];\n" +
                    "      const index = Math.floor(Math.random() * tmp.length);\n" +
                    "      return tmp[index];\n" +
                    "    }\n" +
                    "  };\n" +
                    "  //\n" +
                    "  Object.defineProperty(HTMLElement.prototype, \"offsetHeight\", {\n" +
                    "    get () {\n" +
                    "      const height = Math.floor(this.getBoundingClientRect().height);\n" +
                    "      const valid = height && rand.sign() === 1;\n" +
                    "      const result = valid ? height + rand.noise() : height;\n" +
                    "      //\n" +
                    "      if (valid && result !== height) {\n" +
                    "        window.top.postMessage(\"font-fingerprint-defender-alert\", '*');\n" +
                    "      }\n" +
                    "      //\n" +
                    "      return result;\n" +
                    "    }\n" +
                    "  });\n" +
                    "  //\n" +
                    "  Object.defineProperty(HTMLElement.prototype, \"offsetWidth\", {\n" +
                    "    get () {\n" +
                    "      const width = Math.floor(this.getBoundingClientRect().width);\n" +
                    "      const valid = width && rand.sign() === 1;\n" +
                    "      const result = valid ? width + rand.noise() : width;\n" +
                    "      //\n" +
                    "      if (valid && result !== width) {\n" +
                    "        window.top.postMessage(\"font-fingerprint-defender-alert\", '*');\n" +
                    "      }\n" +
                    "      //\n" +
                    "      return result;\n" +
                    "    }\n" +
                    "  });", null);

            //Spoof screen resolution, color depth: set values like in Tor browser, random values for device memory, hardwareConcurrency, remove battery, network connection, keyboard, media devices info, prevent sendBeacon

            view.evaluateJavascript("" +
                    "Object.defineProperty(window, 'devicePixelRatio',{value:1});" +
                    "Object.defineProperty(window.screen, 'width',{value:1000});" +
                    "Object.defineProperty(window.screen, 'availWidth',{value:1000});" +
                    "Object.defineProperty(window.screen, 'height',{value:900});" +
                    "Object.defineProperty(window.screen, 'availHeight',{value:900});" +
                    "Object.defineProperty(window.screen, 'colorDepth',{value:24});" +
                    "Object.defineProperty(window, 'outerWidth',{value:1000});" +
                    "Object.defineProperty(window, 'outerHeight',{value:900});" +
                    "Object.defineProperty(window, 'innerWidth',{value:1000});" +
                    "Object.defineProperty(window, 'innerHeight',{value:900});" +
                    "Object.defineProperty(navigator, 'getBattery',{value:function(){}});" +
                    "const ram=Math.pow(2,Math.floor(Math.random() * 4));Object.defineProperty(navigator, 'deviceMemory',{value:ram});" +
                    "const hw=Math.pow(2,Math.floor(Math.random() * 4));Object.defineProperty(navigator, 'hardwareConcurrency',{value:hw});" +
                    "Object.defineProperty(navigator, 'connection',{value:null});" +
                    "Object.defineProperty(navigator, 'keyboard',{value:null});" +
                    "Object.defineProperty(navigator, 'sendBeacon',{value:null});", null);

            if (!sp.getBoolean("sp_camera",false) && !sp.getBoolean("sp_microphone",false)) {
                view.evaluateJavascript("" +
                        "Object.defineProperty(navigator, 'mediaDevices',{value:null});", null);
            }
        }
    }

    @Override
    public void onLoadResource(WebView view, String url) {

        if(ninjaWebView.isFingerPrintProtection()) {
            view.evaluateJavascript("var test=document.querySelector(\"a[ping]\"); if(test!==null){test.removeAttribute('ping')};", null);
            //do not allow ping on http only pages (tested with http://tests.caniuse.com)
        }

        if (view.getSettings().getUseWideViewPort() && (view.getWidth()<1300)) view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px');", null);
        //  Client-side detection for GlobalPrivacyControl
        view.evaluateJavascript("if (navigator.globalPrivacyControl === undefined) { Object.defineProperty(navigator, 'globalPrivacyControl', { value: true, writable: false,configurable: false});} else {try { navigator.globalPrivacyControl = true;} catch (e) { console.error('globalPrivacyControl is not writable: ', e); }};",null);
        //  Script taken from:
        //
        //  donotsell.js
        //  DuckDuckGo
        //
        //  Copyright © 2020 DuckDuckGo. All rights reserved.
        //
        //  Licensed under the Apache License, Version 2.0 (the "License");
        //  you may not use this file except in compliance with the License.
        //  You may obtain a copy of the License at
        //
        //  http://www.apache.org/licenses/LICENSE-2.0
        //
        //  Unless required by applicable law or agreed to in writing, software
        //  distributed under the License is distributed on an "AS IS" BASIS,
        //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        //  See the License for the specific language governing permissions and
        //  limitations under the License.
        //
        view.evaluateJavascript("if (navigator.doNotTrack === null) { Object.defineProperty(navigator, 'doNotTrack', { value: 1, writable: false,configurable: false});} else {try { navigator.doNotTrack = 1;} catch (e) { console.error('doNotTrack is not writable: ', e); }};",null);
        view.evaluateJavascript("if (window.doNotTrack === undefined) { Object.defineProperty(window, 'doNotTrack', { value: 1, writable: false,configurable: false});} else {try { window.doNotTrack = 1;} catch (e) { console.error('doNotTrack is not writable: ', e); }};",null);
        view.evaluateJavascript("if (navigator.msDoNotTrack === undefined) { Object.defineProperty(navigator, 'msDoNotTrack', { value: 1, writable: false,configurable: false});} else {try { navigator.msDoNotTrack = 1;} catch (e) { console.error('msDoNotTrack is not writable: ', e); }};",null);
}
/*
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {  //do not delete, needed for camera, for whatever reason
        final Uri uri = Uri.parse(url);
        return handleUri(uri);
    }*/

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri uri = request.getUrl();
        return handleUri(uri);
    }

    private boolean handleUri(final Uri uri) {
        if (ninjaWebView.isBackPressed){
            return false;
        } else {
            // handle the url by implementing your logic
            String url = uri.toString();
            if (url.startsWith(BrowserUnit.URL_SCHEME_HTTP) || url.startsWith(BrowserUnit.URL_SCHEME_HTTPS)) {
                ninjaWebView.loadUrl(url);
                return true;
            } else if (url.startsWith(BrowserUnit.URL_SCHEME_INTENT)) {
                Intent intent = null;
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    intent.addCategory("android.intent.category.BROWSABLE");
                    intent.setComponent(null);
                    intent.setSelector(null);
                    context.startActivity(intent);  //if no suitable app is available ActivityNotFoundException is thrown
                    return true;
                } catch (URISyntaxException e) {
                    //not an intent uri, let NinjaWebView handle it
                    return false;
                } catch (ActivityNotFoundException e){
                    //try to find fallback url
                    String fallbackUrl = null;
                    if (intent != null) fallbackUrl = intent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null) {
                        this.ninjaWebView.loadUrl(fallbackUrl);
                        return true;
                    } else {
                        return false; //if no app available and no fallbackUrl defined let NinjaWebView try to handle it
                    }
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try{
                    context.startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e){
                    return false;  //if no app available let NinjaWebView handle it
                }
            }
        }
    }

    /*
    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (enable && !white && adBlock.isAd(url)) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }
        return super.shouldInterceptRequest(view, url);
    }*/

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
/*
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (request.getUrl().toString().contains("utm_")) NinjaToast.show(context,"Tracking URL utm_");
            }
        });*/

        if (ninjaWebView.getBlockNetworkVideo()){
            String extension = MimeTypeMap.getFileExtensionFromUrl(request.getUrl().toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType!= null && mimeType.startsWith("video")) {
                return new WebResourceResponse(
                        BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                        BrowserUnit.URL_ENCODING,
                        new ByteArrayInputStream("".getBytes())
                );
            }

        }

        if (enable && !white && adBlock.isAd(request.getUrl().toString())) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message doNotResend, final Message resend) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(R.string.dialog_content_resubmission);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> resend.sendToTarget());
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(dialog1 -> doNotResend.sendToTarget());
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
        String message = "\"SSL Certificate error.\"";
        switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED:
                message = "\"Certificate authority is not trusted.\"";
                break;
            case SslError.SSL_EXPIRED:
                message = "\"Certificate has expired.\"";
                break;
            case SslError.SSL_IDMISMATCH:
                message = "\"Certificate Hostname mismatch.\"";
                break;
            case SslError.SSL_NOTYETVALID:
                message = "\"Certificate is not yet valid.\"";
                break;
            case SslError.SSL_DATE_INVALID:
                message = "\"Certificate date is invalid.\"";
                break;
            case SslError.SSL_INVALID:
                message = "\"Certificate is invalid.\"";
                break;
        }
        String text = message + " - " + context.getString(R.string.dialog_content_ssl_error);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(text);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> handler.proceed());
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(dialog1 -> handler.cancel());
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host, String realm) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_http_auth, null);

        EditText pass_userNameET = dialogView.findViewById(R.id.edit_userName);
        EditText pass_userPWET = dialogView.findViewById(R.id.edit_PW);
        TextView title = dialogView.findViewById(R.id.title);
        title.setText(host);

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
            String user = pass_userNameET.getText().toString().trim();
            String pass = pass_userPWET.getText().toString().trim();
            handler.proceed(user, pass);
            dialog.cancel();
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        dialog.setOnCancelListener(dialog1 -> {
            handler.cancel();
            dialog1.cancel();
        });
    }
}
