package com.fourchette.onchemobile;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.webkit.WebSettings;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallbackLegacy;
    private static final int REQUEST_CODE_FILE_PICKER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                try {
                    startActivityForResult(fileChooserParams.createIntent(), REQUEST_CODE_FILE_PICKER);
                    filePathCallbackLegacy = filePathCallback;
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Sélecteur de fichier indisponible", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webView.loadUrl("https://onche.org/forum/1/blabla-general/");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webView.loadUrl("javascript:(function() { " +
                        "var element = document.getElementsByClassName('adsbygoogle');" +
                        "for (var i = 0; i < element.length; i++) {" +
                        "   element[i].style.display='none';" +
                        "}" +
                        "})()");
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FILE_PICKER && filePathCallbackLegacy != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            filePathCallbackLegacy.onReceiveValue(results);
            filePathCallbackLegacy = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
