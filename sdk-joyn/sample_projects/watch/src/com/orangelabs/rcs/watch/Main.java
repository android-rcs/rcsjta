package com.orangelabs.rcs.watch;

import com.orangelabs.rcs.watch.WatchConsumerService.LocalBinder;
import com.samsung.android.sdk.accessory.SAPeerAgent;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.Toast;

/**
 * Watch relay for joyn service  
 * 
 * @author Jean-Marc AUFFRET
 */
public class Main extends Activity implements OnClickListener , WatchReceiver {
    public static final String TAG = "joynwatch";

    protected static final int REMOTE_CMD  = 0;
    protected static final int CON_CLOSE   = 2;
    protected static final int CON_SUCCESS = 3;


    private Button mConnect;

    private boolean mIsBound = false;
    
    private WatchConsumerService  mBackendService = null;

    private static WatchHandler mHandler;
    
    private ServiceConnection mConnection = new ServiceConnection() {
    	public void onServiceConnected(ComponentName className, IBinder service) {
    		BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();

            if (!mBTAdapter.isEnabled()) {
            	Toast.makeText(getBaseContext(), R.string.label_no_bluetooth, Toast.LENGTH_SHORT).show();
            }

            LocalBinder binder = (LocalBinder) service;
            mBackendService = binder.getService();
            mBackendService.registerReceiver(Main.this);
            mIsBound = true;

            Log.d(TAG, "Service attached to " + className.getClassName());
        }

        public void onServiceDisconnected(ComponentName className) {
        	mBackendService = null;
            mIsBound = false;
        }
    };

    @Override
    protected void onDestroy() {
    	Log.d(TAG, "onDestroy");
    	
        if(mIsBound == true) {
        	mBackendService.closeConnection();
            unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mHandler = new WatchHandler();
        setContentView(R.layout.main);

        mConnect = (Button) findViewById(R.id.connect);
        mConnect.setOnClickListener(this);

        onConnectionClose();
        startService(new Intent(this, WatchConsumerService.class));

        mIsBound = bindService(new Intent(this, WatchConsumerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        	case R.id.connect:
            {	if (mIsBound) {	mBackendService.findPeers();}
                break;
            }
        }
    }

    @Override
    public void onPeerFound(SAPeerAgent uRemoteAgent) {
    	Log.d(TAG, "onPeerFound enter");
    	
        if (uRemoteAgent != null) {
        	if (mIsBound = true) {
        		Log.d(TAG, "peer agent is found and try to connect");
                mBackendService.establishConnection(uRemoteAgent);
            } else {
            	Log.d(TAG, "Service not bound !!!");
            }
        } else {
        	Log.d(TAG, "no peers are present tell the UI");
            Toast.makeText(this.getApplicationContext(), R.string.label_no_peers_found, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuccess() {
    	Message msg = mHandler.obtainMessage(CON_SUCCESS);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onConnectionClose() {
    	Message msg = mHandler.obtainMessage(CON_CLOSE);
        mHandler.sendMessage(msg);
    }

    /**
     * Message handler to receive remote cmd from phone
     */
    public class WatchHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	if (msg.what == REMOTE_CMD) {
        		String cmd = (String)msg.obj;
        		// TODO
            } else if (msg.what == CON_CLOSE) {
            	mConnect.setVisibility(View.VISIBLE);
            } else if (msg.what == CON_SUCCESS) {
            	mConnect.setVisibility(View.INVISIBLE);
            }
        }
    }

    public static WatchHandler getWatchHandler() {
    	return mHandler;
    }
}
