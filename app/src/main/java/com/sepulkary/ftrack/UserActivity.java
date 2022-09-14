package com.sepulkary.ftrack;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

@SuppressWarnings("all")
public class UserActivity extends Activity {
    float userWeight, initWeight, minWeight, maxWeight;
    int userHeight, initHeight, minHeight, maxHeight;
    boolean userGender;

    private SeekBar weightSeekBar, heightSeekBar;
    private TextView weightValueTextView, heightValueTextView;
    private RadioGroup genderRadioGroup;
    private RadioButton femaleRadioButton, maleRadioButton;
    private Button enterUserButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_user_param);

        weightSeekBar = (SeekBar) findViewById(R.id.weightSeekBar);
        weightValueTextView = (TextView) findViewById(R.id.weightValueTextView);
        heightSeekBar = (SeekBar) findViewById(R.id.heightSeekBar);
        heightValueTextView = (TextView) findViewById(R.id.heightValueTextView);
        genderRadioGroup = (RadioGroup) findViewById(R.id.genderRadioGroup);
        femaleRadioButton = (RadioButton) findViewById(R.id.femaleRadioButton);
        maleRadioButton = (RadioButton) findViewById(R.id.maleRadioButton);
        enterUserButton = (Button) findViewById(R.id.enterUserButton);

        initWeight = getResources().getInteger(R.integer.initial_weight);
        minWeight = getResources().getInteger(R.integer.min_weight);
        maxWeight = getResources().getInteger(R.integer.max_weight);
        initHeight = getResources().getInteger(R.integer.initial_height);
        minHeight = getResources().getInteger(R.integer.min_height);
        maxHeight = getResources().getInteger(R.integer.max_height);

        Intent intent = getIntent();
        userWeight = intent.getFloatExtra("userWeight", initWeight);
        userHeight = intent.getIntExtra("userHeight", initHeight);
        userGender = intent.getBooleanExtra("userGender", true);

        femaleRadioButton.setChecked(userGender);

        enterUserButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(UserActivity.this, MainActivity.class);
                intent.putExtra("userWeight", userWeight);
                intent.putExtra("userHeight", userHeight);
                intent.putExtra("userGender", femaleRadioButton.isChecked());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        weightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                userWeight = ((progress * progress) / (1000.0f * 1000.0f)) * (maxWeight - minWeight) + minWeight; // Approximate an exponential curve with x^2.
                weightValueTextView.setText(String.format("%.1f", userWeight) + " " + getResources().getString(R.string.weight_units));
            }
        });

        heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    userHeight = progress + minHeight;
                heightValueTextView.setText(String.valueOf(userHeight) + " " + getResources().getString(R.string.height_units));
            }
        });

        genderRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                userGender = femaleRadioButton.isChecked();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        weightValueTextView.setText(String.format("%.1f", userWeight) + " " + getResources().getString(R.string.weight_units));
        weightSeekBar.setProgress((int) (Math.sqrt(((userWeight - minWeight) / (maxWeight - minWeight)) * 1000.0f * 1000.0f))); // Approximate an exponential curve with x^2.

        heightValueTextView.setText(String.valueOf(userHeight) + " " + getResources().getString(R.string.height_units));
        heightSeekBar.setMax(maxHeight - minHeight);
        heightSeekBar.setProgress(userHeight - minHeight);
    }
}