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

    //Déclaration de la WebView et du callback de chargement de fichier
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallbackLegacy;

    private static final int REQUEST_CODE_FILE_PICKER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Utilisation du layout activity_main.xml pour définir l'interface utilisateur
        setContentView(R.layout.activity_main);

        //Trouver l'élément WebView dans le layout
        webView = findViewById(R.id.webview);

        // Activer JavaScript
        webView.getSettings().setJavaScriptEnabled(true);

        // Load the webpage with default cache mode
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.loadUrl("https://onche.org/forum/1/blabla-general/");

        // Activer les plugins
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);

        // Activer le chargement de contenu mixte (http et https)
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Active le zoom/dézoom
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // Activer le stockage DOM (Document Object Model) pour la gestion des données locales
        webView.getSettings().setDomStorageEnabled(true);

        // Cache les pubs sur le site
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

        // Autoriser les téléchargements de fichiers
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, REQUEST_CODE_FILE_PICKER);
                    filePathCallbackLegacy = filePathCallback;
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Sélecteur de fichier indisponible", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }

    // Revenir à la page précédente lorsque le bouton retour est pressé
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Gérer les téléchargements de fichiers
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FILE_PICKER && filePathCallbackLegacy != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            filePathCallbackLegacy.onReceiveValue(results);
            filePathCallbackLegacy = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
