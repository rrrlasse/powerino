package com.powerino.prototype;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;

import com.powerino.prototype.R;

import org.w3c.dom.Text;

import static android.content.Context.INPUT_METHOD_SERVICE;


public class ScreenSlidePageFragment extends Fragment implements View.OnClickListener {
    private IFragmentToActivity mCallback;
    Button buttonWeight, buttonBackwards, buttonForwards, buttonZero;
    TextView editText_PedalArm;
    TextView editText_Weight;
    RadioButton radioButton_Bike1;
    RadioButton radioButton_Bike2;

    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */

    public static ScreenSlidePageFragment create(int pageNumber) {

        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);

        editText_PedalArm = (TextView) getActivity().findViewById(R.id.editText_PedalArm);
        editText_Weight = (TextView) getActivity().findViewById(R.id.editText_Weight);
        radioButton_Bike1 = (RadioButton) getActivity().findViewById(R.id.radioButton_Bike1);
        radioButton_Bike2 = (RadioButton) getActivity().findViewById(R.id.radioButton_Bike2);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(mPageNumber == 1) {

            // Inflate the layout containing a title and body text.
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.settings, container, false);

            //Link the buttons and textViews to respective views
            buttonForwards = (Button) rootView.findViewById(R.id.buttonForwards);
            buttonWeight = (Button) rootView.findViewById(R.id.buttonWeight);
            buttonBackwards = (Button) rootView.findViewById(R.id.buttonBackwards);

            editText_PedalArm = (TextView) rootView.findViewById(R.id.editText_PedalArm);
            editText_Weight = (TextView) rootView.findViewById(R.id.editText_Weight);
            radioButton_Bike1 = (RadioButton) rootView.findViewById(R.id.radioButton_Bike1);
            radioButton_Bike2 = (RadioButton) rootView.findViewById(R.id.radioButton_Bike2);
            TextView TextView_log = (TextView) rootView.findViewById(R.id.textView_log);
            TextView_log.setText("All trips are logged automatically and the files are saved in the Documents directory at: " + ((MainActivity) getActivity()).dir.getPath() + "\n\nThey take up a few megabytes per hour of riding.");

            RadioGroup radioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup);

            buttonForwards.setOnClickListener(this);
            buttonWeight.setOnClickListener(this);
            buttonBackwards.setOnClickListener(this);

            buttonBackwards.setEnabled(false);
            buttonForwards.setEnabled(false);

            int i = ((MainActivity)getActivity()).parser.bike_number;
            radioButton_Bike1.setChecked(i == 0);
            radioButton_Bike2.setChecked(i == 1);

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    boolean one_checked = radioButton_Bike1.isChecked();
                    int v = one_checked ? 0 : 1;
                    ((MainActivity)getActivity()).parser.bike_number = v;
                    getActivity().getPreferences(Context.MODE_PRIVATE).edit().putInt("bike_number", v).commit();
                }
            });

            editText_Weight.setText(getActivity().getPreferences(Context.MODE_PRIVATE).getString("weight", "3000"));
            editText_PedalArm.setText(getActivity().getPreferences(Context.MODE_PRIVATE).getString("arm", "172.5"));

            return rootView;
        }
        else if(mPageNumber == 0) {
            // Inflate the layout containing a title and body text.
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.main, container, false);
            TextView textView_kg = (TextView) rootView.findViewById(R.id.textView_kg);
            if(textView_kg != null) {
                textView_kg.setOnClickListener(this);
            }

            return rootView;
        }
        else {
            // Inflate the layout containing a title and body text.
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.debug, container, false);

            return rootView;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

            @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (IFragmentToActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement IFragmentToActivity");
        }
    }

    @Override
    public void onClick(View v) {


        if (!((MainActivity)getActivity()).bluetoothConnected) {
            Toast.makeText(getActivity(), "Error: Not connected to Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        int bike = ((MainActivity)getActivity()).parser.bike_number;

        if(v.getId() == R.id.buttonForwards) {
            ((MainActivity) getActivity()).parser.calibrate_weight_used = Integer.parseInt(editText_Weight.getText().toString()) / 1000.;
            ((MainActivity) getActivity()).parser.calibrate_arm[bike] = Float.parseFloat(editText_PedalArm.getText().toString()) / 1000.;
            double d = ((MainActivity) getActivity()).parser.calibrate_weight_used;

            buttonWeight.setEnabled(false);
            buttonForwards.setEnabled(false);
            buttonBackwards.setEnabled(true);
        }

        if(v.getId() == R.id.buttonForwards || v.getId() == R.id.textView_kg) {
            ((MainActivity)getActivity()).parser.calibration_counter = Constants.CALIBRATION_POINTS;
            ((MainActivity)getActivity()).parser.calibrate_forward[bike] = 0;
            ((MainActivity)getActivity()).parser.calibrate_velocity[bike] = 0;
            ((MainActivity)getActivity()).parser.calibration_type = Constants.CalibrationStatus.FORWARDS_IN_PROGRESS;
        }
        if(v.getId() == R.id.buttonWeight) {
            buttonWeight.setEnabled(false);
            buttonForwards.setEnabled(true);
            buttonBackwards.setEnabled(false);

            editText_Weight.clearFocus();
            editText_PedalArm.clearFocus();

            getActivity().getPreferences(Context.MODE_PRIVATE).edit().putString("weight", editText_Weight.getText().toString()).commit();
            getActivity().getPreferences(Context.MODE_PRIVATE).edit().putString("arm", editText_PedalArm.getText().toString()).commit();

            ((MainActivity)getActivity()).parser.calibrate_weight = 0;
            ((MainActivity)getActivity()).parser.calibration_counter = Constants.CALIBRATION_POINTS;
            ((MainActivity)getActivity()).parser.calibration_type = Constants.CalibrationStatus.WEIGHT_IN_PROGRESS;
        }
        if(v.getId() == R.id.buttonBackwards) {
            buttonWeight.setEnabled(true);
            buttonForwards.setEnabled(false);
            buttonBackwards.setEnabled(false);

            ((MainActivity)getActivity()).parser.calibrate_backwards[bike] = 0;
            ((MainActivity)getActivity()).parser.calibration_counter = Constants.CALIBRATION_POINTS;
            ((MainActivity)getActivity()).parser.calibration_type = Constants.CalibrationStatus.BACKWARDS_IN_PROGRESS;
        }
    }

}
//                mCallback.showToast("Hello from Fragment 2");


