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
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.webkit.WebView.HitTestResult;
import java.net.HttpURLConnection;
import java.net.URL;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallbackLegacy;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private FrameLayout mFullscreenContainer;
    private int mOriginalSystemUiVisibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri[] results = null;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    if (filePathCallbackLegacy != null) {
                        filePathCallbackLegacy.onReceiveValue(results);
                        filePathCallbackLegacy = null;
                    }
                }
        );

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        registerForContextMenu(webView);
        mFullscreenContainer = new FrameLayout(this);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                try {
                    filePathCallbackLegacy = filePathCallback;
                    fileChooserLauncher.launch(fileChooserParams.createIntent());
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Sélecteur de fichier indisponible", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) {
                    onHideCustomView();
                    return;
                }

                mCustomView = view;
                mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                mCustomViewCallback = callback;

                ((FrameLayout) getWindow().getDecorView()).addView(mFullscreenContainer,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                mFullscreenContainer.addView(mCustomView,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;

                getWindow().getDecorView().setSystemUiVisibility(mOriginalSystemUiVisibility);

                ((FrameLayout) getWindow().getDecorView()).removeView(mFullscreenContainer);
                mFullscreenContainer.removeAllViews();
                mCustomView = null;
                mCustomViewCallback.onCustomViewHidden();
                mCustomViewCallback = null;
                webView.setVisibility(View.VISIBLE);
            }
        });

        setupWebViewSettings();
        setupWebViewClient();
        setupLongClickListener();
    }

    private void setupWebViewSettings() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);

        webView.loadUrl("https://onche.org/forum/1/blabla-general/");
    }

    private void setupWebViewClient() {
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

    private void setupLongClickListener() {
        webView.setOnLongClickListener(view -> {
            WebView.HitTestResult result = webView.getHitTestResult();
            if (result.getType() == HitTestResult.IMAGE_TYPE ||
                    result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

                String imageURL = result.getExtra();
                String fullResImageURL = convertToFullResolution(imageURL);

                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Alors mon coquin on veux sauvegarder cette image ? :)")
                        .setPositiveButton("Oui *fap*", (dialog, which) -> handleImageDownload(fullResImageURL))
                        .setNegativeButton("Annuler", null)
                        .show();
                return true;
            }
            return false;
        });
    }

    private void handleImageDownload(String imageUrl) {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        URL url = new URL(imageUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("HEAD");
                        connection.connect();
                        String contentType = connection.getContentType();
                        connection.disconnect();
                        return contentType;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }, Executors.newSingleThreadExecutor())
                .thenAcceptAsync(contentType -> {
                    String extension = getExtensionFromContentType(contentType);
                    startDownload(imageUrl, contentType, extension);
                }, getMainExecutor()); // Corrected here
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return ".jpg";
        if (contentType.contains("image/gif")) return ".gif";
        if (contentType.contains("image/png")) return ".png";
        if (contentType.contains("image/webp")) return ".webp";
        return ".jpg";
    }

    private void startDownload(String imageUrl, String contentType, String extension) {
        String filename = "image_" + System.currentTimeMillis() + extension;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        request.setMimeType(contentType);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(getApplicationContext(),
                    "Téléchargement en cours... (Stockée dans le dossier Downloads)",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String convertToFullResolution(String imageUrl) {
        if (imageUrl != null && imageUrl.matches(".*/\\d+$")) {
            return imageUrl.replaceAll("/\\d+$", "");
        }
        return imageUrl;
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}