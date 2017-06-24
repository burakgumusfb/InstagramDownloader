package com.example.burak.instagramphotodownloader;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

import constants.constants;
import core.DirectoryProgress;
import core.PhotoDownloader;
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
    private LinearLayout lLbackground;
    private LinearLayout lLbottomAdd;
    private RecyclerView horizontal_recycler_view;
    private ArrayList<String> horizontalList;
    private HorizontalAdapter horizontalAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CommonHelper.ThreadPolicy();
        isStoragePermissionGranted();

        DirectoryProgress.GetPictures();

        horizontal_recycler_view= (RecyclerView) findViewById(R.id.horizontal_recycler_view_photo);
        horizontalList=new ArrayList<>();
        horizontalList.add("horizontal 1");
        horizontalList.add("horizontal 2");
        horizontalList.add("horizontal 3");
        horizontalList.add("horizontal 4");
        horizontalList.add("horizontal 5");
        horizontalList.add("horizontal 6");
        horizontalList.add("horizontal 7");
        horizontalList.add("horizontal 8");
        horizontalList.add("horizontal 9");
        horizontalList.add("horizontal 10");
        horizontalAdapter=new HorizontalAdapter(horizontalList);

        LinearLayoutManager horizontalLayoutManagaer
                = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
        horizontal_recycler_view.setLayoutManager(horizontalLayoutManagaer);
        horizontal_recycler_view.setAdapter(horizontalAdapter);

        tvShareUrl = (EditText) findViewById(R.id.twShareUrlUrl);
        btnDownloader = (Button) findViewById(R.id.btnDownload);
        ivImage = (ImageView) findViewById(R.id.ivImage);
        slidePanel = (SlidingUpPanelLayout) findViewById(R.id.slidepUpPanel);
        upPanel = (RelativeLayout) findViewById(R.id.upPanel);
        btnCloseSlideUpPanel = (Button) findViewById(R.id.btnCloseSlideUp);

        slidePanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        //slidePanel.setTouchEnabled(false);
        ivUserPhoto = (ImageView) findViewById(R.id.ivUserPhoto);
        twUserName = (TextView) findViewById(R.id.twUserName);
        llAdd = (LinearLayout) findViewById(R.id.llAdd);
        btnSave = (Button) findViewById(R.id.btnSaveMedia);
        lLbackground = (LinearLayout) findViewById(R.id.llBackground);
        lLbottomAdd = (LinearLayout) findViewById(R.id.bottomAdd);

        btnDownloader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CommonHelper.CreateVideoDirectory();
                CommonHelper.CreatePhotoDirectory();
                HideKeyboard();

                if (CommonHelper.checkNetworkStatus(getApplicationContext())) {
                    try {
                        GetMedia getMedia = new GetMedia();
                        if (!tvShareUrl.getText().toString().matches("")) {
                            if (CommonHelper.IsUrl(tvShareUrl.getText().toString())) {
                                getMedia.execute(String.valueOf(tvShareUrl.getText().toString()));
                            } else {
                                Toast.makeText(getApplicationContext(), "Please Write Instagram Url", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "You Must  Be Fill The Area", Toast.LENGTH_LONG).show();
                        }
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
        lLbackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HideKeyboard();
            }
        });

    }

    private void HideKeyboard() {

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

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


    public class GetMedia extends AsyncTask<String, String, ShortcodeMedia> {
        ProgressDialog progressDialog;

        @Override
        protected ShortcodeMedia doInBackground(String... shareUrl) {

            PhotoDownloader downloader = new PhotoDownloader();
            return downloader.ConnectImage(shareUrl[0]);
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


    public class DownloadMedia extends AsyncTask<ShortcodeMedia, Void, ShortcodeMedia> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {

            progressDialog = progressDialog.show(MainActivity.this, "Please Wait", "Downloading...");
        }

        @Override
        protected ShortcodeMedia doInBackground(ShortcodeMedia... shortcodeMedias) {
            ShortcodeMedia media = shortcodeMedias[0];
            if (media == null)
                return null;
            if (!media.getIsVideo()) {
                return SaveImage(media);

            } else {
                return SaveVideo(media);
            }
        }


        @Override
        protected void onPostExecute(ShortcodeMedia media) {
            progressDialog.hide();

            if (!media.getIsVideo()) {
                String fileName = CommonHelper.CreateFileNameForImage(media.getDisplayUrl());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent = intent.setDataAndType(Uri.parse("file://"+Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + constants.PhotoDirName + "/" + fileName), "image/*");
                startActivity(intent);


            }
            else  {
                String fileName = CommonHelper.CreateFileNameForVideo(media.getVideo_url());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent = intent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + constants.VideoDirName + "/" + fileName), "video/*");
                startActivity(intent);
            }

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

    private ShortcodeMedia SaveImage(ShortcodeMedia shortcodeMedia) {
        Bitmap image = HttpHelper.getBitmapFromURL(shortcodeMedia.getDisplayUrl());
        String fileName = CommonHelper.CreateFileNameForImage(shortcodeMedia.getDisplayUrl());
        PhotoDownloader.SaveImage(image, fileName);
        return shortcodeMedia;
    }

    private ShortcodeMedia SaveVideo(ShortcodeMedia shortcodeMedia) {
        String fileName = CommonHelper.CreateFileNameForVideo(shortcodeMedia.getVideo_url());
        VideoDownloader.Download(shortcodeMedia.getVideo_url(), fileName);
        return shortcodeMedia;
    }

    private void LoadAd() {
        llAdd.removeAllViews();
        //lLbottomAdd.removeAllViews();

        adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getString(R.string.add_id));

        llAdd.addView(adView);
        //lLbottomAdd.addView(adView);

        // AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public class HorizontalAdapter extends RecyclerView.Adapter<HorizontalAdapter.MyViewHolder> {

        private List<String> horizontalList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView txtView;

            public MyViewHolder(View view) {
                super(view);
                txtView = (TextView) view.findViewById(R.id.txtView);

            }
        }


        public HorizontalAdapter(List<String> horizontalList) {
            this.horizontalList = horizontalList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.horizontal_item_view, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            holder.txtView.setText(horizontalList.get(position));

            holder.txtView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this,holder.txtView.getText().toString(),Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return horizontalList.size();
        }
    }
}
