package com.powerino.prototype;
import android.app.Activity;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by me on 03/11/2017.
 */

public class Parser extends Activity
{
    String concat = "";

    void parse(String bluetooth) {
        //Log.i("Bluetooth:", bluetooth);

        buffer = buffer + bluetooth;
        String str = buffer;

        int pos1 = str.indexOf("\n");
        if(pos1 == -1) {
            return;
        }
        int pos2 = str.lastIndexOf("\n");
        if(pos2 == pos1) {
            return;
        }

        buffer = str.substring(pos2);
        str = str.substring(pos1 + 1, pos2);

        String tokens[] = str.split("\n");

        sample = tokens[tokens.length  - 1];

        for (int i = 0; i < tokens.length; i++) {
            String[] s = tokens[i].split("\t");
            if(s[0].equals("battery")) {
                battery = Double.parseDouble(s[1]);
            }
            else if(s[0].equals("temperature")) {
                temperature = Double.parseDouble(s[1]);
            }
            else if(s[0].equals("eprom")) {
                if (s[1].equals("0") && s[2].equals("48")) {
                    for(int bike = 0; bike < 2; bike++) {
                        calibrate_forward[bike] = parse_eprom(s, 1 * 4 + 3 + bike * EEPROM.ENTRY_SIZE);
                        calibrate_backwards[bike] = parse_eprom(s, 2 * 4 + 3 + bike * EEPROM.ENTRY_SIZE);
                        calibrate_dvdf[bike] = parse_eprom(s, 3 * 4 + 3 + bike * EEPROM.ENTRY_SIZE);
                        calibrate_arm[bike] = parse_eprom(s, 4 * 4 + 3 + bike * EEPROM.ENTRY_SIZE);
                        calibrate_velocity[bike] = parse_eprom(s, 5 * 4 + 3 + bike * EEPROM.ENTRY_SIZE);
                        calibrate_initialized[bike] = parse_eprom(s, 0 * 4 + 3 + bike * EEPROM.ENTRY_SIZE);
                    }
                }
            }
            else if(s[0].equals("OK")) {
                // no-op
            }
            else {
                Point newpoint;
                try {
                    newpoint = create(tokens[i]);
                }
                catch(Exception e) {
                    data_errors++;
                    break;
                }

                // Remove spikes
                if(spikes < 3 && points.size() > 0 && Math.abs(newpoint.voltage - points.get(points.size() - 1).voltage) > 100000) {
                    newpoint.voltage = points.get(points.size() - 1).voltage;
                    newpoint.watt = points.get(points.size() - 1).watt;
                    newpoint.torque = points.get(points.size() - 1).torque;
                    newpoint.kg = points.get(points.size() - 1).kg;
                    spikes++;
                    total_spikes++;
                }
                else {
                    spikes = 0;
                }

                if(points.size() > 0) {
                    newpoint.duration = (float)((newpoint.time - points.get(points.size() - 1).time) / 1000.);
                }
                points.add(newpoint);


                // Calibration
                if(calibration_type == Constants.CalibrationStatus.WEIGHT_IN_PROGRESS) {
                    calibrate_weight += newpoint.voltage;
                    calibration_counter--;
                    if(calibration_counter == 0) {
                        calibrate_weight = calibrate_weight / Constants.CALIBRATION_POINTS;
                        calibration_type = Constants.CalibrationStatus.WEIGHT_DONE; // signal
                    }
                }
                else if(calibration_type == Constants.CalibrationStatus.FORWARDS_IN_PROGRESS) {
                    // This is "zero" calibration
                    calibrate_forward[bike_number] += newpoint.voltage;
                    calibrate_velocity[bike_number] += newpoint.velocity_raw;
                    calibration_counter--;
                    if(calibration_counter == 0) {
                        calibrate_forward[bike_number] = calibrate_forward[bike_number] / Constants.CALIBRATION_POINTS;
                        calibrate_velocity[bike_number] = calibrate_velocity[bike_number] / Constants.CALIBRATION_POINTS;
                        calibration_type = Constants.CalibrationStatus.FORWARDS_DONE;
                    }
                }
                else if(calibration_type == Constants.CalibrationStatus.BACKWARDS_IN_PROGRESS) {
                    calibrate_backwards[bike_number] += newpoint.voltage;
                    calibration_counter--;
                    if(calibration_counter == 0) {
                        calibrate_backwards[bike_number] = calibrate_backwards[bike_number] / Constants.CALIBRATION_POINTS;


                        // delta-voltage per delta-Newton
                        calibrate_dvdf[bike_number] = (calibrate_weight - calibrate_forward[bike_number]) / (calibrate_weight_used * 9.815);
                        calibrate_initialized[bike_number] = 1;
                        calibration_type = Constants.CalibrationStatus.BACKWARDS_DONE;
                    }
                }



            }
        }

    }

    float parse_eprom(String[] bytes, int offset) {
        byte[] b = ByteBuffer.allocate(4).array();
        for(int i = 0; i < 4; i++) {
            Integer tmp = Integer.parseInt(bytes[i + offset]);
            b[i] = tmp.byteValue();
        }
        float ret = ByteBuffer.wrap(b).getFloat();
        return ret;
    }

    Point create(String str) {
        String[] s = str.split("\t");
        Point p = new Point();
        p.time = Integer.parseInt(s[0]);
        float v  = Float.parseFloat(s[1]);
        p.velocity_raw = Integer.parseInt(s[2]);
        p.position = Float.parseFloat(s[3]);
        p.voltage = v;

        if(calibrate_initialized[bike_number] == 1)
        {
            double force = (v - ((calibrate_backwards[bike_number] + calibrate_forward[bike_number]) / 2)) / calibrate_dvdf[bike_number];
            p.torque = force * calibrate_arm[bike_number];
            p.kg = (float)((v - calibrate_forward[bike_number]) / calibrate_dvdf[bike_number] / 9.815);

            // angular velocity in radians/second, forward-pedalling being the positive direction
            p.velocity = (float)(-(p.velocity_raw - calibrate_velocity[bike_number]) / (Math.pow(2., 15.) / 2000.) / 360. * 2. * Math.PI);
            p.cadence = (float)(p.velocity / (2 * Math.PI) * 60.);
            p.watt = (float)((p.velocity > 0 ? p.velocity : 0) * p.torque);
        }
        samples++;
        return p;

    }


    String buffer = "";
    List<Point> points = new ArrayList();

    long calibration_counter = 0;
    Constants.CalibrationStatus calibration_type = Constants.CalibrationStatus.NONE; // 1 = forwards, 2 = backwards, 3 = weight

    // Stored in EEPROM
    double[] calibrate_initialized = {0, 0};
    double[] calibrate_arm = {0, 0};//  2693    0.085 / (3.015 * 9.815); // 0.0028723761;
    double[] calibrate_dvdf = {0, 0};
    double[] calibrate_velocity = {0, 0};
    double[] calibrate_forward = {0, 0};
    double[] calibrate_backwards = {0, 0};//  2693    0.085 / (3.015 * 9.815); // 0.0028723761;

    // Initialized by the Settings screen
    double calibrate_weight = Double.NaN;
    double calibrate_weight_used = Double.NaN;

    double battery = 0;
    double temperature = 0;
    int samples = 0;


    int spikes = 0;
    int total_spikes = 0;

    int data_errors = 0;


    int bike_number = 0;

    String sample = "";

}

