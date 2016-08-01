package com.hannaford.android;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.basecamp.turbolinks.TurbolinksAdapter;
import com.basecamp.turbolinks.TurbolinksSession;
import com.basecamp.turbolinks.TurbolinksView;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.hannaford.android.barcodereader.BarcodeCaptureActivity;
import com.hannaford.android.databinding.ActivityMainBinding;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.FeedbackManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements TurbolinksAdapter {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getExtras()!=null) {
                boolean connected = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (connected) {
                    TurbolinksSession.getDefault(MainActivity.this).getWebView().getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                } else {
                    TurbolinksSession.getDefault(MainActivity.this).getWebView().getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                }
            }
        }
    }

    private final ConnectivityBroadcastReceiver connectivityBroadcastReceiver = new ConnectivityBroadcastReceiver();

    private static final String ACTION_ADVANCE="advance";
    private static final String ACTION_RESTORE="restore";
    private static final String ACTION_REPLACE="replace";
    private static final String DEFAULT_RELATIVE_START_URL = "/";
    private static final String SEARCH_RELATIVE_URL_PREFIX = "/catalog/search.cmd?keyword=";
    private static final String CAMERA_RELATIVE_URL = "/camera.jsp";
    private static final String SHELL_RELATIVE_URL = "/shell.html";
    private static final String OFFLINE_RELATIVE_URL = "/offline.html";

    private String baseUrl;
    private Pattern urlFixerPattern;

    String location;

    private ExpandableListView navigationExpandableListView;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private TurbolinksView turbolinksView;
    ActivityMainBinding binding;
    private ShareActionProvider shareActionProvider;
    private WebView webView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        FeedbackManager.register(this);

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search_menu, menu);
        menuInflater.inflate(R.menu.share_menu, menu);
        menuInflater.inflate(R.menu.print_menu, menu);
        menuInflater.inflate(R.menu.feedback_menu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem shareItem = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        updateShareIntent();

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.menu_item_search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        MenuItem cameraSearchItem = menu.findItem(R.id.menu_item_camera_search);
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            cameraSearchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    handleLocation(baseUrl + CAMERA_RELATIVE_URL);
                    return true;
                }
            });
        }else{
            cameraSearchItem.setVisible(false);
        }

        MenuItem feedbackItem = menu.findItem(R.id.menu_item_feedback);
        feedbackItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                FeedbackManager.showFeedbackActivity(MainActivity.this);
                return true;
            }
        });

        MenuItem printItem = menu.findItem(R.id.menu_item_print);
        printItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                // Get a PrintManager instance
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

                String title = getTitle().toString();

                // Get a print adapter instance
                PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter();

                printManager.print(title, printAdapter,
                        new PrintAttributes.Builder().build());
                return true;
            }
        });

        // Return true to display menu
        return true;
    }

    private void updateShareIntent(){
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, location);
        shareActionProvider.setShareIntent(shareIntent);
    }

    @Override
    protected void onNewIntent(Intent intent){
        handleIntent(intent);
    }

    /**
     * Handle the given intent. Returns true if this activity should keep running and handle the intent; false if the activity should not handle the intent.
     * @param intent
     */
    private void handleIntent(Intent intent){
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            handleLocation(baseUrl + SEARCH_RELATIVE_URL_PREFIX + Uri.encode(query)) ;
        }else if(Intent.ACTION_MAIN.equals(intent.getAction())){
            handleLocation(baseUrl + DEFAULT_RELATIVE_START_URL);
        }else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            handleLocation(intent.getDataString());
        }else{
            throw new IllegalStateException("Unknown intent used to start MainActivity: " + intent.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkForUpdates();
        CrashManager.register(this);

        baseUrl = getResources().getString(R.string.base_url);
        urlFixerPattern = Pattern.compile("^https?://(?:" + Pattern.quote(getResources().getString(R.string.www_host)) + "|" + Pattern.quote(getResources().getString(R.string.host)) + ")", Pattern.CASE_INSENSITIVE);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        turbolinksView = binding.turbolinksView;
        navigationView = binding.navigationView;
        navigationExpandableListView = binding.navigationExpandableList;


        navigationExpandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int position, long id) {
                try {
                    JSONObject group = (JSONObject) navigationExpandableListView.getExpandableListAdapter().getGroup(position);
                    if (group.has("children")) {
                        return false;
                    } else {
                        final String absoluteUri = URI.create(location).resolve(group.getString("href")).toString();
                        handleLocation(absoluteUri);
                        binding.drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                }catch(JSONException e){
                    throw new RuntimeException(e);
                }
            }
        });
        navigationExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPositon, long id) {
                try {
                    JSONObject item = (JSONObject) navigationExpandableListView.getExpandableListAdapter().getChild(groupPosition,childPositon);
                    final String absoluteUri = URI.create(location).resolve(item.getString("href")).toString();
                    handleLocation(absoluteUri);
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }catch(JSONException e){
                    throw new RuntimeException(e);
                }
            }
        });

        setSupportActionBar(binding.myToolbar);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                binding.drawerLayout,         /* DrawerLayout object */
                binding.myToolbar,
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        drawerToggle.syncState();

        // Set the drawer toggle as the DrawerListener
        //drawerLayout.addDrawerListener(drawerToggle);

        webView = TurbolinksSession.getDefault(this).getWebView();
        TurbolinksSession.getDefault(this).setDebugLoggingEnabled(BuildConfig.DEBUG);
        String userAgent = "HannafordAndroid/" + BuildConfig.VERSION_CODE;
        if(!webView.getSettings().getUserAgentString().contains(userAgent)) {
            webView.getSettings().setUserAgentString(webView.getSettings().getUserAgentString() + " " + userAgent);
        }
        webView.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        webView.getSettings().setAppCacheEnabled(true);
        webView.addJavascriptInterface(this,"NativeApp");

        TurbolinksSession.getDefault(this)
                .activity(this)
                .adapter(this)
                .view(turbolinksView);

        handleLocation(baseUrl + SHELL_RELATIVE_URL);

        handleIntent(getIntent());

    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(connectivityBroadcastReceiver);
        unregisterManagers();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterManagers();
    }

    @Override
    protected void onResume(){
        super.onResume();
        CrashManager.register(this);
        registerReceiver(connectivityBroadcastReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Since the webView is shared between activities, we need to tell Turbolinks
        // to load the location from the previous activity upon restarting
        handleLocation(location);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }

        // Otherwise defer to system default behavior.
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS && data!=null) {
                Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                handleLocation(baseUrl + SEARCH_RELATIVE_URL_PREFIX + Uri.encode(barcode.rawValue));
            }else{
                if(this.location==null){
                    finish();
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPageFinished() {
        // This callback will only be fired once upon cold booting. If there is any action you need to take after the first full page load is complete, just once, this is the place to do it.
    }

    @Override
    public void onReceivedError(int errorCode) {
        // This is a callback that's executed at the end of the standard WebViewClient's onReceivedError method.
        // We recommend you implement this method. Otherwise, your user will see an endless progress view/spinner without something that handles the error. You can handle the error however you like -- send the user to a different page, show a native error screen, etc.
        Toast.makeText(this, "onReceivedError: " + errorCode, Toast.LENGTH_LONG).show();
        if(errorCode <= 0 && !location.startsWith(baseUrl + OFFLINE_RELATIVE_URL)) handleLocation(baseUrl + OFFLINE_RELATIVE_URL + "#" + location);
    }

    @Override
    public void pageInvalidated() {
        // This is a callback from Turbolinks telling you that a change has been detected in a resource/asset in the <HEAD>, and as a result the Turbolinks state has been invalidated. Most likely the web app has been updated while the app was using it.
        // The library will automatically fall back to cold booting the location (which it must do since resources have been changed) and then will notify you via this callback that the page was invalidated. This is an opportunity for you to clean up any UI state that you might have lingering around that may no longer be valid (like a screenshot, title data, etc.)
    }

    @Override
    public void requestFailedWithStatusCode(int statusCode) {
        // This is a callback from Turbolinks telling you that an XHR request has failed.
        // We recommend you implement this method. Otherwise, your user will see an endless progress view/spinner without something that handles the error. You can handle the error however you like -- send the user to a different page, show a native error screen, etc.
        Toast.makeText(this, "requestFailedWithStatusCode: " + statusCode, Toast.LENGTH_LONG).show();
        if(statusCode <= 0 && !location.startsWith(baseUrl + OFFLINE_RELATIVE_URL)) handleLocation(baseUrl + OFFLINE_RELATIVE_URL + "#" + location);
    }

    @Override
    public void visitCompleted() {
        // This is a callback from Turbolinks telling you it considers the visit completed. The request has been fulfilled successfully and the page fully rendered.
        // It's similar conceptually to onPageFinished, except this callback will be called for every Turbolinks visit. This is a good time to take actions that you need on every page, such as reading data-attributes (or other metadata) from the loaded page.
        location = webView.getUrl();
        setTitle(webView.getTitle().replaceAll("Hannaford - ",""));
        updateShareIntent();
        webView.evaluateJavascript("getNavigationObject()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String navigationJson) {
                try{
                    JSONArray jsonArray = new JSONArray(navigationJson);
                    navigationExpandableListView.setAdapter(new NavigationExpandableListAdapter(MainActivity.this,jsonArray));
                }catch(JSONException e){
                    Log.e(TAG,"Exception parsing json: " + navigationJson,e);
                }
            }
        });
    }

    @Override
    public void visitProposedToLocationWithAction(final String location, final String action) {
        // The starting point for any href clicked inside a Turbolinks enabled site. In a simple case
        // you can just open another activity, or in more complex cases, this would be a good spot for
        // routing logic to take you to the right place within your app.

        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                // handleLocation handles the routing to different activities for different locations
                handleLocation(location);
            }
        });
    }

    void handleLocation(String location){
        location = urlFixerPattern.matcher(location).replaceFirst(baseUrl);
        if(location.startsWith(baseUrl)) {
            if(location.startsWith(baseUrl + CAMERA_RELATIVE_URL)){
                Intent intent = new Intent(this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.BarcodeFormats, Barcode.UPC_A | Barcode.UPC_E | Barcode.EAN_8 | Barcode.EAN_13);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }else {
                //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(location), this, MainActivity.class);
                //setIntent(intent);
                this.location = location;
                TurbolinksSession.getDefault(this).progressView(LayoutInflater.from(this).inflate(R.layout.turbolinks_progress, turbolinksView, false), R.id.turbolinks_progress_indicator, 500);
                TurbolinksSession.getDefault(this).visit(location);
            }
        }else{
            // not a URL we handle, so open something else (probably a web browser to take care of it)
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(location)));
            if(this.location==null){
                finish();
            }
        }
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        if(! BuildConfig.DEBUG) {
            UpdateManager.register(this);
        }
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
        FeedbackManager.unregister();
    }

    @JavascriptInterface
    private void toast(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }
}
