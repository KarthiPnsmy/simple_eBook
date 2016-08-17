package com.titut.ebook;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private Toolbar mToolbar;
    private ArrayList<HashMap<String,String>> mArticleList = new ArrayList<>();
    private TextView mAppNameView;
    private ImageView mHeaderIcon;
    private WebView mWebView;
    private MenuItem mPreMenuItem;
    private int mCurrentArticleIndex = 0;
    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mWebView = (WebView) findViewById(R.id.webview);
        setSupportActionBar(mToolbar);
        initNavigationDrawer();

        showWebContent(mCurrentArticleIndex);
    }

    public void initNavigationDrawer() {

        mNavigationView = (NavigationView)findViewById(R.id.navigation_view);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                showWebContent(menuItem.getItemId());
                menuItem.setChecked(true);

                return true;
            }
        });
        View header = mNavigationView.getHeaderView(0);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer);

        mAppNameView = (TextView)header.findViewById(R.id.app_name);
        mHeaderIcon = (ImageView) header.findViewById(R.id.header_icon);


        Menu navigationViewMenu = mNavigationView.getMenu();

        ArrayList<HashMap<String,String>> articleList = parseContent();
        for(int j=0; j<articleList.size(); j++){
            HashMap<String, String> mMap = articleList.get(j);
            //navigationViewMenu.add((String) mMap.get("title"));
            navigationViewMenu.add(1,j,Menu.NONE,(String) mMap.get("title"));
        }

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,mToolbar,R.string.drawer_open,R.string.drawer_close){

            @Override
            public void onDrawerClosed(View v){
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.prev:
                showWebContent(mCurrentArticleIndex-1);
                break;
            case R.id.next:
                showWebContent(mCurrentArticleIndex+1);
                break;
            default:
                break;
        }

        return true;
    }

    private void showWebContent(int index){
        if((index >= mArticleList.size()) || (index < 0)){
            mSnackbar = Snackbar
                    .make(findViewById(android.R.id.content), "You reached end", Snackbar.LENGTH_LONG)
                    .setAction("Done", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mSnackbar.dismiss();
                        }
                    });

            mSnackbar.setActionTextColor(Color.YELLOW);

            View sbView = mSnackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            sbView.setBackgroundColor(Color.DKGRAY);
            textView.setTextColor(Color.WHITE);
            mSnackbar.show();
            return;
        }
        HashMap<String, String> selectedArticle = mArticleList.get(index);

        if(mPreMenuItem != null){
            mPreMenuItem.setChecked(false);
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        if(selectedArticle.get("url") != null){
            mWebView.loadUrl("file:///android_asset/"+selectedArticle.get("url"));
        }
        mDrawerLayout.closeDrawers();
        MenuItem currentMenuItem = mNavigationView.getMenu().findItem(index);
        currentMenuItem.setChecked(true);
        mCurrentArticleIndex = index;
        mPreMenuItem = currentMenuItem;
    }

    private ArrayList<HashMap<String,String>> parseContent(){
        String jsonString = readFromAsset();
        mArticleList = new ArrayList<>();

        try {
            JSONObject mainObject = new JSONObject(jsonString);
            if(mainObject.get("title") != null){
                String appTitle = (String) mainObject.get("title");
                if(mAppNameView != null){
                    mAppNameView.setText(appTitle);
                }
            }

            if(mainObject.get("icon") != null){
                String appIconUrl = (String) mainObject.get("icon");
                if(mHeaderIcon != null){
                    mHeaderIcon.setImageBitmap(getBitmapFromAssets(appIconUrl));
                }
            }

            if(mainObject.get("toc") != null){
                JSONArray tocArray = mainObject.getJSONArray("toc");
                for(int i=0; i<tocArray.length(); i++){
                    JSONObject tocObject = (JSONObject) tocArray.get(i);
                    HashMap<String, String> mMap = new HashMap<>();
                    if(tocObject.get("title") != null){
                        String articleTitle = (String) tocObject.get("title");
                        mMap.put("title", articleTitle);
                    }

                    if(tocObject.get("url") != null){
                        String articleUrl = (String) tocObject.get("url");
                        mMap.put("url", articleUrl);
                    }

                    mArticleList.add(mMap);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mArticleList;
    }

    private String readFromAsset(){
        AssetManager assetManager = getAssets();
        InputStream inputStream;
        String jsonString = "";

        try {
            inputStream = assetManager.open("book.json");

            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            jsonString = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonString;
    }

    public Bitmap getBitmapFromAssets(String fileName) {
        AssetManager assetManager = getAssets();

        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        return bitmap;
    }

}
