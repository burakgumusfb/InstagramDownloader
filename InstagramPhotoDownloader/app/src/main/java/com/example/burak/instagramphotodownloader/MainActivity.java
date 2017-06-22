package com.example.burak.instagramphotodownloader;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import core.DirectoryProgress;
import core.EntryData;
import core.Graphql;
import core.PostPage;
import core.SharedResponseTemplate;
import core.ShortcodeMedia;
import core.VideoDownloader;
import helper.CommonHelper;
import helper.HttpHelper;

public class MainActivity extends AppCompatActivity {
    private ShortcodeMedia _shortcodeMedia;
    private String JSON;
    private Button btnDownloader;
    private EditText tvShareUrl;
    private ImageView ivImage;
    private ImageView ivUserPhoto;
    private RelativeLayout upPanel;
    private SlidingUpPanelLayout slidePanel;
    private Button btnCloseSlideUpPanel;
    private TextView twUserName;
    private AdView adView;
    private LinearLayout llAdd;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CommonHelper.ThreadPolicy();

        isStoragePermissionGranted();


        tvShareUrl = (EditText) findViewById(R.id.twShareUrlUrl);
        btnDownloader = (Button) findViewById(R.id.btnDownload);
        ivImage = (ImageView) findViewById(R.id.ivImage);
        slidePanel = (SlidingUpPanelLayout) findViewById(R.id.slidepUpPanel);
        upPanel = (RelativeLayout) findViewById(R.id.upPanel);
        btnCloseSlideUpPanel = (Button) findViewById(R.id.btnCloseSlideUp);

        slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        //  slidePanel.setTouchEnabled(false);
        ivUserPhoto = (ImageView) findViewById(R.id.ivUserPhoto);
        twUserName = (TextView) findViewById(R.id.twUserName);
        llAdd = (LinearLayout) findViewById(R.id.llAdd);
        btnSave = (Button) findViewById(R.id.btnSaveMedia);

        btnDownloader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CommonHelper.CreateVideoDirectory();
                CommonHelper.CreatePhotoDirectory();
                        if (CommonHelper.checkNetworkStatus(getApplicationContext())) {
                            try {
                                GetMedia getMedia = new GetMedia();
                                if(!tvShareUrl.getText().toString().matches(""))
                             getMedia.execute(String.valueOf(tvShareUrl.getText().toString()));
                        else
                            Toast.makeText(getApplicationContext(),"You Must  Be Fill The Area",Toast.LENGTH_LONG).show();
                    } catch (Exception err) {
                        Toast.makeText(getApplicationContext(), "Error... Please Check Share Url.", Toast.LENGTH_LONG);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please Check Internet Connection", Toast.LENGTH_LONG).show();
                }

            }
        });


        btnCloseSlideUpPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (_shortcodeMedia != null) {
                    new DownloadMedia().execute(_shortcodeMedia);
                } else {
                    Toast.makeText(getApplicationContext(), "Not Found Media For Download", Toast.LENGTH_LONG).show();
                }
            }
        });
        final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                tvShareUrl.setText(clipboardManager.getPrimaryClip().getItemAt(0).getText());
            }
        });
        tvShareUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                {
                    InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }


    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CommonHelper.CreateVideoDirectory();
            CommonHelper.CreatePhotoDirectory();

        }
    }

    private String Replace(String element) {

        element = element.toString().replace("<script type=\"text/javascript\">", "");
        element = element.toString().replace(";</script>", "");
        element = element.toString().replace("window._sharedData = ", "");
        return element;
    }

    private ShortcodeMedia GetMediaInfo(String json) {
        SharedResponseTemplate template = CommonHelper.ConvertToJson(json);
        EntryData entryData = template.getEntryData();


        ArrayList<PostPage> postPage = entryData.getPostPage();
        for (PostPage page : postPage) {
            Graphql graphql = page.getGraphql();
            ShortcodeMedia shortcodeMedia = graphql.getShortcodeMedia();
            return shortcodeMedia;
        }
        return null;
    }

    public class GetMedia extends AsyncTask<String, String, ShortcodeMedia> {
        ProgressDialog progressDialog;

        @Override
        protected ShortcodeMedia doInBackground(String... shareUrl) {

            Connection.Response response = null;

            try {
                response = Jsoup.connect(String.valueOf(shareUrl[0])).timeout(30000).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Document doc = null;
            try {
                if (response != null)
                    doc = response.parse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (doc == null)
                    return null;
                Elements elements = doc.getElementsByTag("script");
                for (Element element : elements) {
                    if (element.toString().contains("window._sharedData")) {

                        JSON = Replace(element.toString());
                        return GetMediaInfo(JSON);

                    }
                }
            } catch (Exception err) {
                err.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            try {
                LoadAd();
                progressDialog = ProgressDialog.show(MainActivity.this, "Please Wait", "Previewing...");
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error... Please Check Share Url.", Toast.LENGTH_LONG).show();
            }


        }

        @Override
        protected void onPostExecute(ShortcodeMedia shortcodeMedia) {
            Preview(shortcodeMedia);

            _shortcodeMedia = shortcodeMedia;
            progressDialog.hide();
            slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        }
    }

    public class DownloadMedia extends AsyncTask<ShortcodeMedia, Void, Void> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {

            progressDialog = progressDialog.show(MainActivity.this, "Please Wait", "Downloading...");
        }

        @Override
        protected Void doInBackground(ShortcodeMedia... shortcodeMedias) {
            ShortcodeMedia[] medias = (ShortcodeMedia[]) shortcodeMedias;
            if (medias.length < 1)
                return null;
            if (!medias[0].getIsVideo()) {
                SaveImage(medias[0]);
            } else {
                SaveVideo(medias[0]);
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.hide();
        }
    }

    private void Preview(ShortcodeMedia shortcodeMedia) {
        if (shortcodeMedia != null) {


            Bitmap image = HttpHelper.getBitmapFromURL(shortcodeMedia.getDisplayUrl());
            if (image != null)
                ivImage.setImageBitmap(image);

            Bitmap mediaOwner = HttpHelper.getBitmapFromURL(shortcodeMedia.getOwner().getProfilePicUrl());
            if (mediaOwner != null)
                ivUserPhoto.setImageBitmap(mediaOwner);

            twUserName.setText(shortcodeMedia.getOwner().getUsername());
        } else {
            Toast.makeText(getApplicationContext(), "Error... Please Check Share Url.", Toast.LENGTH_LONG).show();
        }

    }

    private void SaveImage(ShortcodeMedia shortcodeMedia) {
        Bitmap image = HttpHelper.getBitmapFromURL(shortcodeMedia.getDisplayUrl());
        String fileName = CommonHelper.CreateFileNameForImage(shortcodeMedia.getDisplayUrl());
        DirectoryProgress.SaveImage(image, fileName);

    }

    private void SaveVideo(ShortcodeMedia shortcodeMedia) {
        String fileName = CommonHelper.CreateFileNameForVideo(shortcodeMedia.getVideo_url());
        VideoDownloader.Download(shortcodeMedia.getVideo_url(), fileName);
    }

    private void LoadAd() {
        llAdd.removeAllViews();
        adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getString(R.string.add_id));
        llAdd.addView(adView);

        // AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void GetClipboard() {

    }
}