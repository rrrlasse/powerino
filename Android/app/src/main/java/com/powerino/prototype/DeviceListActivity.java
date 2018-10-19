package com.powerino.prototype;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.powerino.prototype.R;


public class DeviceListActivity extends Activity implements LocationListener {
    @Override
    public void onProviderDisabled(String provider) {
    }
    @Override
    public void onLocationChanged(android.location.Location location) {
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }

    // Debugging for LOGCAT
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

  
    // declare button for launching website and textview for connection status
    Button tlbutton;
    TextView textView1;
    private TimerTask timerTask;

    // EXTRA string to send on to mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_list);
    }
    
    @Override
    public void onResume() 
    {
    	super.onResume();
    	//*************** 
    	checkBTState();



        // Activate GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, this);
        criteria.setAccuracy(Criteria.ACCURACY_FINE );
        bestProvider = locationManager.getBestProvider(criteria, true);



    	textView1 = (TextView) findViewById(R.id.connecting);
    	textView1.setTextSize(40);
    	textView1.setText(" ");

    	// Initialize array adapter for paired devices
    	mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

    	// Find and set up the ListView for paired devices
    	ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
    	pairedListView.setAdapter(mPairedDevicesArrayAdapter);
    	pairedListView.setOnItemClickListener(mDeviceClickListener);

    	// Get the local Bluetooth adapter
    	mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    	if(Constants.ENABLE_BLUETOOTH) {
            // Get a set of currently paired devices and append to 'pairedDevices'
            Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

            // Add previosuly paired devices to the array
            if (pairedDevices.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);//make title viewable
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                String noDevices = getResources().getText(R.string.none_paired).toString();
                mPairedDevicesArrayAdapter.add(noDevices);
            }
        }


        if(!Constants.ENABLE_BLUETOOTH) {
            Intent i = new Intent(DeviceListActivity.this, MainActivity.class);
            startActivity(i);
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                TextView tv = (TextView)findViewById(R.id.precision);
                if(tv != null) {
                    Location location = locationManager.getLastKnownLocation(bestProvider);
                    if(location == null) {
                        setText(tv, "GPS precision: Finding satelites...");
                    }
                    else {
                        float f = location.getAccuracy();
                        setText(tv, "GPS precision: " + Integer.toString((int) f) + " m");
                    }
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 2000);

    }

    public void onPause() {

        locationManager.removeUpdates(this);
        super.onPause();
    }
    // Set up on-click listener for the list (nicked this - unsure)
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            timer.cancel();

        	textView1.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity while taking an extra which is the MAC address.
			Intent i = new Intent(DeviceListActivity.this, MainActivity.class);
            i.putExtra(EXTRA_DEVICE_ADDRESS, address);
			startActivity(i);
        }
    };

    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
    	 mBtAdapter=BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if(mBtAdapter==null) { 
        	Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
          if (mBtAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
          } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
 
            }
          }
        }

    LocationManager locationManager;
    Criteria criteria = new Criteria();
    String bestProvider;
    private Timer timer = new Timer();

    private void setText(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }
}