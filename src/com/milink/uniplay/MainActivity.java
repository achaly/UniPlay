
package com.milink.uniplay;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;

import com.milink.api.v1.MilinkClientManager;
import com.milink.api.v1.type.DeviceType;
import com.milink.uniplay.audio.AudioTabContentFragment;
import com.milink.uniplay.image.ImageTabContentFragment;
import com.milink.uniplay.video.VideoTabContentFragment;
import com.milink.uniplay.R;

public class MainActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    private MilinkClientManager mMilinkClientManager = null;

    private String imageTabName = "image";
    private String audioTabName = "audio";
    private String videoTabName = "video";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar mActionBar = getActionBar();
        mActionBar.addTab(mActionBar
                .newTab()
                .setText(imageTabName)
                .setTabListener(new TabListener(new ImageTabContentFragment(this))));

        mActionBar.addTab(mActionBar
                .newTab()
                .setText(audioTabName)
                .setTabListener(new TabListener(new AudioTabContentFragment(this))));

        mActionBar.addTab(mActionBar
                .newTab()
                .setText(videoTabName)
                .setTabListener(new TabListener(new VideoTabContentFragment(this))));

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        Log.d(TAG, "onCreate");

        MilinkClient.mMilinkClient = new MilinkClient(this);
        mMilinkClientManager = MilinkClient.mMilinkClient.getManagerInstance();
        mMilinkClientManager.setDelegate(MilinkClient.mMilinkClient);
        mMilinkClientManager.setDataSource(MilinkClient.mMilinkClient);
        mMilinkClientManager.setDeviceName("zhgnphone");
        mMilinkClientManager.open();

        Device nullDevice = new Device("127.0.0.1", "Local Device", DeviceType.Unknown);
        MilinkClient.mDeviceList.add(nullDevice);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMilinkClientManager.close();
        MilinkClient.mDeviceList.clear();
    }

    private class TabListener implements ActionBar.TabListener {
        private Fragment mFragment;

        public TabListener(Fragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
            Log.d(TAG, "onTabReselected");
        }

        @Override
        public void onTabSelected(Tab arg0, FragmentTransaction ft) {
            Log.d(TAG, "onTabSelected");
            ft.add(R.id.fragment_content, mFragment, null);
        }

        @Override
        public void onTabUnselected(Tab arg0, FragmentTransaction ft) {
            Log.d(TAG, "onTabUnselected");
            ft.remove(mFragment);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
