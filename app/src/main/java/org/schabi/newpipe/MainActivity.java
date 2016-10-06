package org.schabi.newpipe;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.schabi.newpipe.settings.SettingsActivity;

/**
 * Created by Christian Schabesberger on 02.08.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * DownloadActivity.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class MainActivity extends AppCompatActivity {

    private Fragment mainFragment = null;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private String mainVersion = "";
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle("BigHistory");
        actionBar.setDisplayShowTitleEnabled(true);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mainFragment = getSupportFragmentManager()
                .findFragmentById(R.id.search_fragment);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        activity = this;

//        fetchVersion();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.main_menu, menu);

        mainFragment.onCreateOptionsMenu(menu, inflater);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                NavUtils.navigateUpTo(this, intent);
                return true;
            }
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_page1: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri u = Uri.parse("https://school.bighistoryproject.com/bhplive");
                intent.setData(u);
                startActivity(intent);
                return true;
            }
            case R.id.action_page2: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri u = Uri.parse("http://ibhanet.org");
                intent.setData(u);
                startActivity(intent);
                return true;
            }
            case R.id.action_show_downloads: {
                Intent intent = new Intent(this, org.schabi.newpipe.download.DownloadActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return mainFragment.onOptionsItemSelected(item) ||
                        super.onOptionsItemSelected(item);
        }
    }

    private void fetchVersion() {
        String appVersion = "";
        try {
            appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        long cacheExpiration = 3600; // 1 hour in seconds.
        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled())
            cacheExpiration = 0;

        // [START fetch_config_with_callback]
        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        final String finalAppVersion = appVersion;

        Toast.makeText(MainActivity.this, mFirebaseRemoteConfig.getString("version"),
                Toast.LENGTH_SHORT).show();

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mainVersion = mFirebaseRemoteConfig.getString("version");
                            Toast.makeText(MainActivity.this, mainVersion + " ::" + finalAppVersion,
                                    Toast.LENGTH_SHORT).show();

                            mFirebaseRemoteConfig.activateFetched();

                            if(!finalAppVersion.equals(mainVersion)) {
                                // 다이얼로그 바디
                                AlertDialog.Builder alertdialog = new AlertDialog.Builder(activity);
                                // 다이얼로그 메세지
                                alertdialog.setMessage("새로운 버전이 나왔습니다");

                                // 확인버튼
                                alertdialog.setPositiveButton("update", new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                        } catch (android.content.ActivityNotFoundException anfe) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                        }
                                    }
                                });

                                // 취소버튼
                                alertdialog.setNegativeButton("cancle", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                                // 메인 다이얼로그 생성
                                AlertDialog alert = alertdialog.create();
                                // 타이틀
                                alert.setTitle("업데이트");
                                // 다이얼로그 보기
                                alert.show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "fail!!!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        // [END fetch_config_with_callback]
    }
}
