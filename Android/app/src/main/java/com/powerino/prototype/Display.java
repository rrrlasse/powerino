package com.powerino.prototype;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.NumberPicker;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;
import android.widget.NumberPicker;


/**
 * Created by me on 03/11/2017.
 */

public class Display {
    double total_joule = 0;
    double watt_revs = 0;

    List<Point> points = new ArrayList<Point>();

    void feed(Point p) {
        total_joule += p.watt * p.duration;
        points.add(p);
        if(points.size() > 5000) {
            points.subList(0, 1000).clear();
        }
    }

    // wattage last second
    double watt(int seconds) {
        double sum_position = 0;
        double sum_duration = 0;
        double sum_watt = 0;

        double full_position = 0;
        double full_watt = 0;
        watt_revs = 0;

        int count = 0;
        int index = points.size();
        while(index > 0 && sum_duration < seconds) {
            index--;
            count++;
            sum_position += points.get(index).velocity * points.get(index).duration;
            sum_duration += points.get(index).duration;
            sum_watt += points.get(index).watt;

            if(sum_position - full_position >= 2*Math.PI) {
                full_watt = sum_watt / count;
                full_position = sum_position;
                watt_revs++;
            }
        }

        if(count == 0) {
            return 0;
        }

        if(full_position != 0) {
            return full_watt;
        }
        else {
            return sum_watt / count;
        }
    }

    int smoothness() {
        Vector<Double> balances = new Vector<Double>();

        double sum_position = 0;
        int index;

        index = points.size() - 1;


        for(int revs = 0; revs < 10; revs++) {
            sum_position = 0;
            double sum_torque = 1;
            long count = 0;
            double max_torque = 0;
            while (index > 0 && sum_position < 2 * Math.PI) {
                sum_position += points.get(index).velocity * points.get(index).duration;
                sum_torque += points.get(index).torque;
                max_torque = points.get(index).torque > max_torque ? points.get(index).torque : max_torque;
                count++;
                index--;
            }

            if (sum_position < 2 * Math.PI) {
                return 0;
            }

            double balance = (sum_torque / count) / max_torque * 100;
            balances.add(balance);
        }

        double avg = 0;
        for (int i = 0; i < balances.size(); i++) {
            avg += balances.get(i);
        }
        int a = (int) (avg / balances.size());
        a = a > 100 ? 100 : (a < 0 ? 0 : a);
        return a;
    }


    double cadance() {
        double sum_cad = 0;
        double sum_duration = 0;

        int count = 0;
        int index = points.size();
        while(index > 0 && sum_duration < 0.25) {
            index--;
            count++;
            sum_cad += points.get(index).cadence;
            sum_duration += points.get(index).duration;
        }

        if(count > 0)
            sum_cad = sum_cad / count;

        return sum_cad;
    }

    double kg() {
        double sum_cad = 0;
        double sum_duration = 0;

        int count = 0;
        int index = points.size();
        while(index > 0 && sum_duration < 1) {
            index--;
            count++;
            sum_cad += points.get(index).kg;
            sum_duration += points.get(index).duration;
        }

        if(count > 0)
            sum_cad = sum_cad / count;

        return sum_cad;
    }

    double voltage() {
        double sum_cad = 0;
        double sum_duration = 0;

        int count = 0;
        int index = points.size();
        while(index > 0 && sum_duration < 1) {
            index--;
            count++;
            sum_cad += points.get(index).voltage;
            sum_duration += points.get(index).duration;
        }

        if(count > 0)
            sum_cad = sum_cad / count;

        return sum_cad;
    }



}
