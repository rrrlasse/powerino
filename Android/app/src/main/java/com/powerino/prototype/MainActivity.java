package com.powerino.prototype;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.NumberPicker;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;

import com.powerino.prototype.R;


class Point {
    double torque = 0;
    float kg = 0;
    float velocity = 0;
    float velocity_raw = 0;
    float voltage = 0;
    float cadence = 0;
    float watt = 0;
    float duration = 0;
    float position = 0;
    int time  = 0;
};


public class MainActivity extends FragmentActivity implements LocationListener, IFragmentToActivity {
    public File dir;
    public File file;

    double metab = 4.33;

    public String str(double d) {
        d = d > 9999999 ? 9999999 : (d < -9999999 ? -9999999 : d);
        double a = Math.abs(d);
        String s = String.format(Locale.ROOT, a >= 100 ? "%.0f" : (a >= 10 ? "%.1f" : "%.2f"), d);
        s = s.equals("-0") ? "0" : (s.equals("-0.0") ? "0.0" : (s.equals("-0.00") ? "0.00" : s));
        return s;
    }


    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void communicateToFragment2() {

    }


    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ScreenSlidePageFragment sf = ScreenSlidePageFragment.create(position);

            return sf;

        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }


    private static final int NUM_PAGES = 3;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    long last_gui_refresh = 0;
    long last_stat_refresh = 0;
    int last_samples = 0;
    Vector<Vector<Double>> smooth_avg = new Vector<Vector<Double>>();

    TextView sensorView1, rpm, watts, textView_speed, textView_kcal, textView_value, textView_description;

    Handler bluetoothIn;
    public boolean bluetoothConnected = false;

    NumberPicker np;

    final int handlerState = 0;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    Parser parser = new Parser();
    Display display = new Display();

    long android_start_time;
    long powermeter_start_time = -1;

    OutputStream fo;
    Writer w;

    LocationManager locationManager;
    Criteria criteria = new Criteria();
    String bestProvider;


    // Set the global variable `w` to an output stream
    void create_log_file()
    {
        // Create log file
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());
        String path;

        if (Build.VERSION.SDK_INT >= 19) {
            dir.mkdirs();
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/Powermeter/Powermeter " + currentDateandTime + ".txt");
        }
        else {
            dir.mkdirs();
            file = new File(Environment.getExternalStorageDirectory() + "/Documents" + "/Powermeter/Powermeter " + currentDateandTime + ".txt");
        }

        try {
            file.createNewFile();
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(), "Could not create log file. Try and insert an SD Card.", Toast.LENGTH_SHORT).show();
        }
        try {
            fo = new FileOutputStream(file);
            w = new OutputStreamWriter(fo);
            w.write("# See trip summary at the bottom of this file.\n");
            w.write("#\n");
            w.write("# time, watt, cadence, torque, pedal position, speed\n");
            w.write("\n");
        }
        catch (Exception e) {
        }
    }


    double speed() {
        Location location = locationManager.getLastKnownLocation(bestProvider);

        double d;
        try {
            d = location.getSpeed();
        }
        catch(Exception e) {
            d = -1;
        }
        return d;
    }

    @Override
    public void onLocationChanged(Location location) {
        int t = 34;
    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    String all = "";

    void Sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        }
        catch(Exception e) {

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        parser.bike_number = getPreferences(Context.MODE_PRIVATE).getInt("bike_number", 0);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        watts = (TextView) findViewById(R.id.watts);
        rpm = (TextView) findViewById(R.id.rpm);

        np = (NumberPicker) findViewById(R.id.numberPicker);
        np.setMaxValue(30);
        np.setMinValue(1);
        np.setValue(getPreferences(Context.MODE_PRIVATE).getInt("seconds_average", 4));

        android_start_time = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= 19) {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/Powermeter");
        }
        else {
            dir = new File(Environment.getExternalStorageDirectory() + "/Documents" + "/Powermeter");
        }

        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {

                String str = "";
                if (msg.what != handlerState) {
                    str = "";
                }
                else {
                    str = (String) msg.obj;
                }

                {
                    //all = all.concat(str);

                    String info = "";
                    parser.parse(str);

                    for(int i = 0; i + 1 < parser.points.size(); i++) {
                        Point p = parser.points.get(i);
                        if (powermeter_start_time == -1) {
                            powermeter_start_time = p.time;
                        }

                        if(w != null) {
                            try {
                                w.write(p.time - powermeter_start_time + "\t" + str(p.watt) +
                                        "\t" + str(p.cadence) +
                                        "\t" + str(p.torque) +
                                        "\t" + str(p.position) +
                                        "\t" + str(speed() * 3.6) +
                                        "\n");

                            } catch (Exception e) {

                            }
                        }

                        display.feed(p);
                    }

                    if(parser.points.size() > 1) {
                        parser.points.subList(0, parser.points.size() - 1).clear();
                    }


                    // Refresh the GUI only if:
                    //     1 second elapsed sincelast refresh, and
                    //     power meter is transmitting data, and
                    //     calibration for selected bike is OK
                    if(System.currentTimeMillis() - last_gui_refresh > 1000 && display.points.size() > 0 && parser.calibrate_initialized[parser.bike_number] == 1) {
                        last_gui_refresh = System.currentTimeMillis();
                        int s = np.getValue();
                        double w = Math.abs(display.watt(s)); // abs to avoid String.format giving "-0.0"
                        watts.setText(str(w));
                        rpm.setText(Integer.toString((int)display.cadance())); // again int cast to avoid "-0" of String.format


                        TextView textView_watt_trip = (TextView) findViewById(R.id.textView_watt_trip);
                        if(textView_watt_trip != null && display.points.size() > 0) {
                            TextView textView_30s = (TextView) findViewById(R.id.textView_30s);
                            textView_speed = (TextView) findViewById(R.id.textView_speed);
                            textView_kcal = (TextView) findViewById(R.id.textView_kcal);
                            TextView textView_metabolic = (TextView) findViewById(R.id.textView_metabolic);
                            TextView textView_trip = (TextView) findViewById(R.id.textView_trip);
                            double w2 = display.watt(30);
                            textView_30s.setText(str(w2));
                            textView_speed.setText(str(speed()*3.6));
                            double J = display.total_joule / 4184.;
                            textView_kcal.setText(str(J));
                            J = J * metab; // Assuming 22.5% human body efficiency (studies show it's 20 - 25)
                            textView_metabolic.setText(str(J));
                            long millis = System.currentTimeMillis() - android_start_time;
                            DateFormat df = new SimpleDateFormat("H:mm:ss");
                            df.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                            String st = df.format(new Date(millis));
                            textView_trip.setText(st);


                            textView_watt_trip.setText(str(display.total_joule / (millis / 1000)));

                            TextView textView_smoothness = (TextView) findViewById(R.id.textView_smoothness);
                            textView_smoothness.setText(Integer.toString((int)display.smoothness()) + "%");

                            TextView textView_kg = (TextView) findViewById(R.id.textView_kg);
                            textView_kg.setText(str(display.kg()));

                        }

                        textView_value = (TextView) findViewById(R.id.textView_value);
                        if(textView_value != null && display.points.size() > 0) {

                            double kg = display.kg();
                            String d = "";
                            String v = "";

                            d += "Pedal pressure: \n";              v += str(kg) + " kg\n";
                            d += "Powermeter temp.: \n";            v += str(parser.temperature) + " C\n";
                            d += "Data errors: \n";                 v += parser.data_errors + "\n";
                            d += "Vcc: \n";                         v += str(parser.battery) + " V\n";
                            d += "gauge noise spikes: \n";          v += parser.total_spikes + "\n";
                            d += "gauge voltage: \n";               v += (display.points.size() > 0 ? display.points.get(display.points.size() - 1).voltage : 0) + "\n";

                            double gauge_rate = 0;
                            if(display.points.size() > 50) {
                                int siz = display.points.size();
                                gauge_rate = 50 / ((display.points.get(siz - 1).time - display.points.get(siz - 50).time) / 1000.);
                            }
                            d += "BTooth/gauge rate: ";             v += (parser.samples - last_samples) + " / " + (int)(gauge_rate) +  " samples/sec\n";
                            d += "\n";                              v += "\n";

                            d += "\nCalibration values:\n"; v += "(bike1 / bike2)\n";
                            boolean b1 = parser.calibrate_initialized[0] == 1;
                            boolean b2 = parser.calibrate_initialized[1] == 1;
                            d += "Status: \n";                      v += (b1 ? "OK" : "Missing") + " / " + (b2 ? "OK" : "Missing") + "\n";
                            d += "Arm forwards: \n";                v += str(!b1 ? 0 : parser.calibrate_forward[0]) + " / " + str(!b2 ? 0 : parser.calibrate_forward[1]) + "\n";
                            d += "Arm backwards: \n";               v += str(!b1 ? 0 : parser.calibrate_backwards[0]) + " / " + str(!b2 ? 0 : parser.calibrate_backwards[1]) + "\n";
                            d += "Delta dvdf: \n";                  v += str(!b1 ? 0 : parser.calibrate_dvdf[0]) + " / " + str(!b2 ? 0 : parser.calibrate_dvdf[1]) + "\n";
                            d += "Zero velocity: \n";               v += str(!b1 ? 0 : parser.calibrate_velocity[0]) + " / " + str(!b2 ? 0 : parser.calibrate_velocity[1]) + "\n";
                            d += "Arm length (mm): \n";             v += String.format(Locale.ROOT, "%.1f", !b1 ? 0 : parser.calibrate_arm[0] * 1000) + " / " + String.format(Locale.ROOT, "%.1f", !b2 ? 0 : parser.calibrate_arm[1] * 1000) + "\n";

                            textView_description = (TextView) findViewById(R.id.textView_description);
                            textView_description.setText(d);
                            textView_value.setText(v);

                            TextView textView_sample = (TextView) findViewById(R.id.textView_sample);
                            textView_sample.setText("Last sample (timer, gauge, gyro, accel):\n" + parser.sample.replaceAll("\t", " \t "));

                        }
                        last_samples = parser.samples;
                    }


                    if(System.currentTimeMillis() - last_stat_refresh > 30000) {
                        mConnectedThread.write("s");
                        last_stat_refresh = System.currentTimeMillis();
                    }

                    int bike = parser.bike_number;
                    if(parser.calibration_type == Constants.CalibrationStatus.WEIGHT_DONE) {
                        Toast.makeText(getApplicationContext(), "Weight calibration OK (gauge = " + (int)parser.calibrate_arm[bike] + ")", Toast.LENGTH_LONG).show();
                        parser.calibration_type = Constants.CalibrationStatus.NONE;
                        eprom_write((float)parser.calibrate_arm[bike], EEPROM.ARM[bike]);
                    }
                    if(parser.calibration_type == Constants.CalibrationStatus.FORWARDS_DONE) {
                        Toast.makeText(getApplicationContext(), "Forwards/zero calibration OK (gauge = " + (int)parser.calibrate_forward[bike] + ")", Toast.LENGTH_LONG).show();
                        parser.calibration_type = Constants.CalibrationStatus.NONE;
                        eprom_write((float)parser.calibrate_forward[bike], EEPROM.FORWARDS[bike]);
                        eprom_write((float)parser.calibrate_velocity[bike], EEPROM.VELOCITY[bike]);
                    }
                    if(parser.calibration_type == Constants.CalibrationStatus.BACKWARDS_DONE) {
                        parser.calibration_type = Constants.CalibrationStatus.NONE;
                        Toast.makeText(getApplicationContext(), "Backwards calibration OK (gauge = " + (int)parser.calibrate_backwards[bike] + ")", Toast.LENGTH_LONG).show();
                        mConnectedThread.write("C");
                        eprom_write((float)parser.calibrate_backwards[bike], EEPROM.BACKWARDS[bike]);
                        eprom_write((float)parser.calibrate_dvdf[bike], EEPROM.DVDF[bike]);
                        eprom_write((float)parser.calibrate_initialized[bike], EEPROM.INITIALIZED[bike]); // will be 1
                        mConnectedThread.write("c");
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        //checkBTState();

    }




    void eprom_write(float value, long address) {
        byte[] dvdf_bytes = ByteBuffer.allocate(8).putFloat(value).array();
        // ew addr count byte
        String s = "ew " + address + " 4";
        for (int i = 0; i < 4; i++) {
            s = s + " ";
            int b = dvdf_bytes[i] < 0 ? 256 + dvdf_bytes[i] : dvdf_bytes[i];
            s = s + b;
        }
        s = s + "\n";
        mConnectedThread.write(s);
        Sleep(100);
    }

    void eprom_read(long address) {
        mConnectedThread.write("er " + address + " 4\n");
        Sleep(100);
    }

    void eprom_read(long address, int bytes) {
        mConnectedThread.write("er " + address + " " + bytes + "\n");
        Sleep(100);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        if(Constants.ENABLE_BLUETOOTH) {
            //create device and set the MAC address
            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
            }
            // Establish the Bluetooth socket connection.
            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    //insert code to deal with this
                }
            }
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();


        }

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called

        // Activate GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, this);
        criteria.setAccuracy(Criteria.ACCURACY_FINE );
        bestProvider = locationManager.getBestProvider(criteria, true);

        if(Constants.ENABLE_BLUETOOTH) {
            mConnectedThread.write("c");
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            locationManager.removeUpdates(this);
            getPreferences(Context.MODE_PRIVATE).edit().putInt("seconds_average", np.getValue()).commit();
            if(Constants.ENABLE_BLUETOOTH) {
                mConnectedThread.write("C");
            }

            if(w != null) {
                w.write("# kcal = " + str(display.total_joule / 4184.) + "\n");
                w.write("# metabolic kcal = " + str(display.total_joule / 4184. * metab) + "\n");
                w.write("# average trip wattage = " + str(display.total_joule / ((System.currentTimeMillis() - android_start_time) / 1000)) + "\n");
                w.flush();
                // FIXME: We might have a file handle leak here - how do we know when to close the handle in Android? When it exceeds 1000, the process is killed
                // so that the handles are cleaned. But find a better method...
            }
            if(Constants.ENABLE_BLUETOOTH) {
                //Don't leave Bluetooth sockets open when leaving activity
                btSocket.close();
            }
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {


            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {

            if(Constants.ENABLE_BLUETOOTH) {
                byte[] buffer = new byte[256];
                int bytes;

                try {
                    mmInStream.read(buffer);
                    bluetoothConnected = true;
                }
                catch(Exception e) {
                }

                if(bluetoothConnected) {
                    create_log_file();
                    mConnectedThread.write("C");
                    eprom_read(0, 48);
                    mConnectedThread.write("c");
                }

                // Keep looping to listen for received messages
                while (true) {
                    try {
                        // Send dummy message to refresh the GUI
                        bluetoothIn.obtainMessage(handlerState, 0, -1, "").sendToTarget();
                        Sleep(200);

                        if(bluetoothConnected) {
                            bytes = mmInStream.read(buffer);            //read bytes from input buffer
                            String readMessage = new String(buffer, 0, bytes);
                            // Send the obtained bytes to the UI Activity via handler
                            bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                        }
                    } catch (IOException e) {

                    }

                }
            }
        }
        //write method
        public void write(String input) {
            if(Constants.ENABLE_BLUETOOTH) {
                byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
                try {
                    mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
                } catch (IOException e) {
                    // Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                    //finish();

                }
            }
        }

        void Sleep(long ms)
        {
            try {
                Thread.sleep(ms);
            }
            catch(Exception e) {

            }
        }

    }
}
    
