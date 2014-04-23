
package com.milink.uniplay.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.milink.api.v1.MilinkClientManager;
import com.milink.api.v1.type.DeviceType;
import com.milink.api.v1.type.ErrorCode;
import com.milink.api.v1.type.MediaType;
import com.milink.api.v1.type.ReturnCode;
import com.milink.uniplay.Device;
import com.milink.uniplay.MilinkClient;
import com.milink.uniplay.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class VideoActivity extends Activity implements IVideoCallback {
    private String TAG = this.getClass().getSimpleName();

    private MilinkClientManager mMilinkClientManager = null;

    private int CONNECT_TIME_OUT = 5000;
    private int VIDEO_SEP_TIME = 1000;
    private int VIDEO_DURATION = 0;

    private volatile boolean mDeviceConnecting = false;
    private volatile boolean mDeviceConnected = false;
    private volatile boolean mVideoPlaying = false;
    private volatile boolean mVideoStopped = true;
    private int volumeValue = 0;

    private List<Map<String, Object>> mVideoList = null;
    private int mDeviceCurrentPosition = 0;
    private int mCurrentPosition = 0;

    private Timer mTimer = null;
    private TimerTask mTimerTask = null;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == VIDEO_DURATION) {
                TextView tv = (TextView) findViewById(R.id.playtime);
                tv.setText((String) msg.obj);
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_details);

        mMilinkClientManager = MilinkClient.mMilinkClient.getManagerInstance();
        MilinkClient.mMilinkClient.setCallback(this);

        Bundle mBundle = getIntent().getExtras();
        mVideoList = (List<Map<String, Object>>) mBundle.get("videoInfoList");
        mCurrentPosition = (Integer) mBundle.get("position");

        setVideoInfo(mVideoList, mCurrentPosition);
        setVisible(false);
        setVideoPlaying(false);
        setVideoStopped(true);
        setDeviceConnecting(false);
        setDeviceConnected(false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVideo(getCurrentFocus());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem mMenuItem = menu.add("push");
        mMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mMenuItem.setIcon(android.R.drawable.ic_menu_share);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("push")) {
            Log.d(TAG, "push");

            final ArrayList<Device> finalDeviceList = new ArrayList<Device>();
            synchronized (MilinkClient.mDeviceList) {
                finalDeviceList.add(MilinkClient.mDeviceList.get(0));
                for (int i = 1; i < MilinkClient.mDeviceList.size(); ++i) {
                    if (MilinkClient.mDeviceList.get(i).type == DeviceType.TV) {
                        finalDeviceList.add(MilinkClient.mDeviceList.get(i));
                    }
                }
            }
            final ArrayList<String> names = new ArrayList<String>();
            for (Device device : finalDeviceList) {
                names.add(device.name);
            }
            String[] deviceNames = new String[names.size()];
            names.toArray(deviceNames);

            new AlertDialog.Builder(this).setTitle(R.string.deviceListName).setItems(
                    deviceNames,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int pos) {
                            if (pos == 0) {
                                mDeviceCurrentPosition = 0;
                                stopVideo(getCurrentFocus());
                                disconnect();
                            } else if (pos == mDeviceCurrentPosition) {
                                if (!mDeviceConnected && !mDeviceConnecting) {
                                    String deviceId = finalDeviceList.get(pos).id;
                                    connect(deviceId, CONNECT_TIME_OUT);
                                }
                            } else {
                                if (mDeviceCurrentPosition == 0) {
                                    disconnect();
                                }
                                mDeviceCurrentPosition = pos;
                                String deviceId = finalDeviceList.get(pos).id;
                                connect(deviceId, CONNECT_TIME_OUT);
                            }
                        }

                    })
                    .create().show();
        }

        return true;
    }

    private void setVideoInfo(List<Map<String, Object>> list, int pos) {
        Map<String, Object> map = list.get(pos);

        TextView tv1 = (TextView) findViewById(R.id.title);
        TextView tv2 = (TextView) findViewById(R.id.album);
        TextView tv3 = (TextView) findViewById(R.id.artist);
        TextView tv4 = (TextView) findViewById(R.id.discription);
        TextView tv5 = (TextView) findViewById(R.id.data);
        TextView tv6 = (TextView) findViewById(R.id.MIME_TYPE);

        tv1.setText("Title: " + (String) map.get("TITLE"));
        tv2.setText("Album: " + (String) map.get("ALBUM"));
        tv3.setText("Artist: " + (String) map.get("ARTIST"));
        tv4.setText("Description: " + (String) map.get("DESCRIPTION"));
        tv5.setText("Data: " + (String) map.get("DATA"));
        tv6.setText("MIME_TYPE: " + (String) map.get("MIME_TYPE"));

        getActionBar().setTitle((String) map.get("TITLE"));
    }

    private void setVideoPlaying(boolean playing) {
        mVideoPlaying = playing;
    }

    private void setVideoStopped(boolean stop) {
        mVideoStopped = stop;
    }

    private void setDeviceConnecting(boolean connecting) {
        mDeviceConnecting = connecting;
    }

    private void setDeviceConnected(boolean connected) {
        mDeviceConnected = connected;
    }

    private void setVolumn() {
        if (mDeviceConnected) {
            volumeValue = mMilinkClientManager.getVolume();
        }
    }

    public void setVisible(boolean visible) {
        View view0 = findViewById(R.id.playtime);
        View view1 = findViewById(R.id.btnPause);
        View view2 = findViewById(R.id.btnStop);
        View view3 = findViewById(R.id.btnVolInc);
        View view4 = findViewById(R.id.btnVolDec);
        View view5 = findViewById(R.id.btnPrev);
        View view6 = findViewById(R.id.btnNext);
        if (!visible) {
            view0.setVisibility(View.INVISIBLE);
            view1.setVisibility(View.INVISIBLE);
            view2.setVisibility(View.INVISIBLE);
            view3.setVisibility(View.INVISIBLE);
            view4.setVisibility(View.INVISIBLE);
            view5.setVisibility(View.INVISIBLE);
            view6.setVisibility(View.INVISIBLE);
        } else {
            view0.setVisibility(View.VISIBLE);
            view1.setVisibility(View.VISIBLE);
            view2.setVisibility(View.VISIBLE);
            view3.setVisibility(View.VISIBLE);
            view4.setVisibility(View.VISIBLE);
            view5.setVisibility(View.VISIBLE);
            view6.setVisibility(View.VISIBLE);
        }
    }

    private void startTimerTask() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {

            @Override
            public void run() {
                int len = mMilinkClientManager.getPlaybackDuration();
                int pos = mMilinkClientManager.getPlaybackProgress();
                len = len <= 0 ? 0 : len;
                pos = pos <= 0 ? 0 : pos;
                Log.d(TAG, String.format("timer len = %d, pos = %d", len, pos));

                String text = convertTime(pos) + "/" + convertTime(len);
                Message msg = Message.obtain();
                msg.obj = text;
                msg.what = VIDEO_DURATION;
                handler.sendMessage(msg);
            }

            private String convertTime(int time) {
                DateFormat format = new SimpleDateFormat("HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
                return format.format(time);
            }
        };

        mTimer.schedule(mTimerTask, VIDEO_SEP_TIME, VIDEO_SEP_TIME);
    }

    private void stopTimerTask() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void connect(String deviceId, int timeout) {
        mDeviceConnecting = true;
        ReturnCode retcode = mMilinkClientManager.connect(deviceId, timeout);
        Log.d(TAG, "connect ret code: " + retcode);
    }

    public void disconnect() {
        ReturnCode retcode = mMilinkClientManager.disconnect();
        Log.d(TAG, "disconnect ret code: " + retcode);
    }

    public void playVideo(View view) {
        if (mDeviceConnected) {
            Map<String, Object> map = mVideoList.get(mCurrentPosition);
            String title = (String) map.get("TITLE");
            String url = (String) map.get("DATA");

            ReturnCode retcode = mMilinkClientManager
                    .startPlay(url, title, 0, 0.0, MediaType.Video);
            Log.d(TAG, "startPlay ret code: " + retcode);

            startTimerTask();
        }

    }

    public void pauseVideo(View view) {
        if (mDeviceConnected) {
            ReturnCode retcode = null;
            if (mVideoStopped) {
                playVideo(view);
            }
            else if (mVideoPlaying) {
                retcode = mMilinkClientManager.setPlaybackRate(0);
            } else {
                retcode = mMilinkClientManager.setPlaybackRate(1);
            }
            Log.d(TAG, "pause ret code: " + retcode);
        }
    }

    public void stopVideo(View view) {
        if (mDeviceConnected) {
            ReturnCode retcode = mMilinkClientManager.stopPlay();
            Log.d(TAG, "stop ret code: " + retcode);
            stopTimerTask();

            // call back onStop
            setVideoStopped(true);
            setVideoPlaying(false);
            Button btn = (Button) findViewById(R.id.btnPause);
            btn.setText(R.string.playVideo);
        }
    }

    public void volumeInc(View view) {
        if (mDeviceConnected) {
            volumeValue += 10;
            volumeValue = volumeValue > 100 ? 100 : volumeValue;
            ReturnCode retcode = mMilinkClientManager.setVolume(volumeValue);
            Log.d(TAG, "vol inc ret code: " + retcode);
        }
    }

    public void volumeDec(View view) {
        if (mDeviceConnected) {
            volumeValue -= 10;
            volumeValue = volumeValue < 0 ? 0 : volumeValue;
            ReturnCode retcode = mMilinkClientManager.setVolume(volumeValue);
            Log.d(TAG, "vol dec ret code: " + retcode);
        }
    }

    public void prevVideo(View view) {
        if (mDeviceConnected) {
            if (mCurrentPosition == 0) {
                return;
            }
            mCurrentPosition--;
            Map<String, Object> map = mVideoList.get(mCurrentPosition);
            String title = (String) map.get("TITLE");
            String url = (String) map.get("DATA");

            setVideoInfo(mVideoList, mCurrentPosition);
            setVideoStopped(true);
            setVideoPlaying(false);

            ReturnCode retcode = mMilinkClientManager
                    .startPlay(url, title, 0, 0.0, MediaType.Video);
            Log.d(TAG, "startPlay ret code: " + retcode);
        }
    }

    public void nextVideo(View view) {
        if (mDeviceConnected) {
            if (mCurrentPosition == mVideoList.size() - 1) {
                return;
            }
            mCurrentPosition++;
            Map<String, Object> map = mVideoList.get(mCurrentPosition);
            String title = (String) map.get("TITLE");
            String url = (String) map.get("DATA");

            setVideoInfo(mVideoList, mCurrentPosition);
            setVideoStopped(true);
            setVideoPlaying(false);

            ReturnCode retcode = mMilinkClientManager
                    .startPlay(url, title, 0, 0.0, MediaType.Video);
            Log.d(TAG, "startPlay ret code: " + retcode);
        }
    }

    @Override
    public void onConnected() {
        setDeviceConnected(true);
        setDeviceConnecting(false);
        setVideoStopped(true);
        setVideoPlaying(false);
        playVideo(getCurrentFocus());
        setVisible(true);
        Toast.makeText(this, R.string.connected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectedFailed(ErrorCode errorCode) {
        setDeviceConnected(false);
        setDeviceConnecting(false);
        setVideoStopped(true);
        setVideoPlaying(false);
        setVisible(false);
        Toast.makeText(this, R.string.connectFailed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnected() {
        setDeviceConnected(false);
        setDeviceConnecting(false);
        setVideoStopped(true);
        setVideoPlaying(false);
        setVisible(false);
    }

    @Override
    public void onLoading() {
    }

    @Override
    public void onPlaying() {
        Button btn = (Button) findViewById(R.id.btnPause);
        btn.setText(R.string.pauseVideo);
        setVideoStopped(false);
        setVideoPlaying(true);
        setVisible(true);
        setVolumn();
        Toast.makeText(this, R.string.playing, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStopped() {
        stopTimerTask();
        setVideoStopped(true);
        setVideoPlaying(false);
        Button btn = (Button) findViewById(R.id.btnPause);
        btn.setText(R.string.playVideo);
        setVisible(false);
        Toast.makeText(this, R.string.stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaused() {
        Button btn = (Button) findViewById(R.id.btnPause);
        btn.setText(R.string.playVideo);
        setVideoPlaying(false);
        Toast.makeText(this, R.string.paused, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVolume(int volume) {
        setVolumn();
    }

}
