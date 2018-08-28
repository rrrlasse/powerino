package com.powerino.prototype;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class DrawView extends View {
    Paint paint = new Paint();

    private void init() {
        paint.setColor(Color.BLACK);
    }

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private int mod(int x, int y)
    {
        int result = x % y;
        if (result < 0)
        {
            result += y;
        }
        return result;
    }

    @Override
    public void onDraw(Canvas canvas) {

        try {
            final int angles_per_rev = 6;
            final int angles = (int)(Math.PI * 2 * angles_per_rev);

            double angle_max = 0;
            double newton_max = 0;
            float[] newton = new float[angles + 1];
            int[] count = new int[angles + 1];

            {

                double sum_position = 0;
                double sum_duration = 0;
                int index = display.points.size();

                while (index > 0 && sum_duration < seconds) {
                    index--;
                    sum_position += display.points.get(index).velocity * display.points.get(index).duration;
                    sum_duration += display.points.get(index).duration;

                    double angle = sum_position % (2 * Math.PI);
                    int idx = (int) (angle * angles_per_rev);
                    newton[idx] += display.points.get(index).torque;
                    count[idx]++;
                }

                for (int i = 0; i < angles; i++) {
                    if (count[i] > 0) {
                        float n = newton[i] / count[i];
                        if (n > newton_max) {
                            newton_max = n;
                            angle_max = (float) i / angles_per_rev;
                        }
                        newton[i] = n;
                    }
                }

            }


            setBackgroundColor(Color.WHITE);
            paint.setStrokeWidth(6);
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(0xFF000000);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);

            int center_x = 400;
            int center_y = 600;

            canvas.drawCircle(center_x, center_y, 100, paint);

            for (int i = 0; i < angles; i++) {
                double angle1 = 2 * Math.PI / angles * (mod(i - 1, angles)) - angle_max;
                double angle2 = 2 * Math.PI / angles * i - angle_max;

                double scale = 200 / newton_max;

                double x1 = Math.cos(angle1) * newton[mod(i - 1, angles)];
                double y1 = Math.sin(angle1) * newton[mod(i - 1, angles)];
                double x2 = Math.cos(angle2) * newton[i];
                double y2 = Math.sin(angle2) * newton[i];

                try {
                    x1 += Math.cos(angle1) * newton[(mod(i - 1, angles) + (angles / 2)) % angles];
                    y1 += Math.sin(angle1) * newton[(mod(i - 1, angles) + (angles / 2)) % angles];
                    x2 += Math.cos(angle2) * newton[(i + (angles / 2)) % angles];
                    y2 += Math.sin(angle2) * newton[(i + (angles / 2)) % angles];
                }
                catch (Exception r) {
                    int ghi = 43;
                }

                x1 *= scale;
                y1 *= scale;
                x2 *= scale;
                y2 *= scale;

                canvas.drawLine(center_x + (int) x1, center_y + (int) y1, center_x + (int) x2, center_y + (int) y2, paint);


            }

            //canvas.drawColor(0xFFAAAAAA);


        }
    catch(Exception e) {
        int iii = 123;
        canvas.drawText("ERROR", 0, 0, paint);
        }
    }

    Display display;
    int seconds = 4;

}




