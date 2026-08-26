package com.stevenspureclean.app;
import android.content.Intent;import android.net.Uri;import android.os.Bundle;import android.webkit.*;import androidx.appcompat.app.AppCompatActivity;import androidx.webkit.*;
public class MainActivity extends AppCompatActivity{
 WebView w;
 @Override protected void onCreate(Bundle b){super.onCreate(b);w=new WebView(this);setContentView(w);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);
 final WebViewAssetLoader l=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
 w.setWebViewClient(new WebViewClientCompat(){@Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return l.shouldInterceptRequest(r.getUrl());}@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();String sc=u.getScheme();if("sms".equals(sc)||"tel".equals(sc)||"mailto".equals(sc)){startActivity(new Intent(Intent.ACTION_VIEW,u));return true;}if(("http".equals(sc)||"https".equals(sc))&&u.getHost()!=null&&u.getHost().contains("google.com")){startActivity(new Intent(Intent.ACTION_VIEW,u));return true;}return false;}});
 w.loadUrl("https://appassets.androidplatform.net/assets/index.html");}
 @Override public void onBackPressed(){if(w!=null&&w.canGoBack())w.goBack();else super.onBackPressed();}
}
