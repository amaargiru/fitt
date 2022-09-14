package com.sepulkary.ftrack;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("all")
public class MainActivity extends FragmentActivity implements ActionBar.TabListener { // http://developer.android.com/training/implementing-navigation/lateral.html
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;
    ViewPager mViewPager;

    HourResult CurrentHourRecord;
    static ArrayList<HourResult> HourRecords;
    int LastElementIndex;
    int CurrentHour;

    public static me.grantland.widget.AutofitTextView TodayLostCalories;
    public static me.grantland.widget.AutofitTextView TodayTravelledMeters;

    public static final String PREFS_NAME = "MyPrefsFile";

    private static int EARLY_TAB_STATE_HOURLY = 0;
    private static int EARLY_TAB_STATE_DAYLY = 1;
    private static int EARLY_TAB_STATE_MONTHLY = 2;
    static int EarlyTabState;

    final static int STATE_ZERO = 0; // Данные пользователя (вес, рост, пол) не введены
    final static int STATE_PAUSE = 1; // Данные пользователя введены но работа не запущена (или приостановлена; например, при движении на автомобиле)
    final static int STATE_RUN = 2; // Данные введены, работаем
    static int appState = STATE_ZERO;

    final static int REQUEST_CODE_USERS = 0;
    final static int REQUEST_CODE_PERIOD = 1;

    static float userWeight;
    static int userHeight;
    static boolean userGender;
    static float userBmrPerSecond; // basal metabolic rate, ккал в секунду

    Timer myTimer = new Timer();

    static final String TAG = "MainActivity";

    static PowerManager.WakeLock wl;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdView mAdView = (AdView) findViewById(R.id.adView); // AdMob ad
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        actionBar.addTab(actionBar.newTab().setText("Today").setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText("Early").setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText("Social").setTabListener(this));

        EarlyTabState = EARLY_TAB_STATE_HOURLY;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE); // Wake lock CPU
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "comSepulkaryFtrackTag");

        myTimer = new Timer(); // По таймеру сообщаем пользователю о приросте потраченной энергии
        final Handler uiHandler = new Handler();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (appState == STATE_RUN) {// Ведем учет калорий и пройденного расстояния. Если пользователь, например, едет на машине, то можно перейти в состояние "Пауза", тогда будет фиксироваться только расход калорий по основному обмену
                            int RealHour = getHour(new Date());
                            if ((RealHour > CurrentHour) || ((RealHour == 0) && (CurrentHour == 23)))// В начале часа создаем новую запись
                            {
                                Date CurrentDate = toWholeHour(new Date()); // Оставляем только дату и часы
                                CurrentHourRecord = new HourResult();
                                CurrentHourRecord.setHourStartDate(CurrentDate);
                                CurrentHour = getHour(CurrentDate);
                                HourRecords.add(CurrentHourRecord);
                                LastElementIndex = HourRecords.size() - 1;
                            }
                            HourRecords.get(LastElementIndex).setUserCaloriesBurned(HourRecords.get(LastElementIndex).getUserCaloriesBurned() + userBmrPerSecond); //!!!
                            HourRecords.get(LastElementIndex).setUserMetersTravelled(HourRecords.get(LastElementIndex).getUserMetersTravelled() + 1); //!!!
                            if (TodayLostCalories != null)
                                TodayLostCalories.setText(" " + String.format("%.1f", HourRecords.get(LastElementIndex).getUserCaloriesBurned()) + " "); // !!!
                            if (TodayTravelledMeters != null)
                                TodayTravelledMeters.setText(" " + HourRecords.get(LastElementIndex).getUserMetersTravelled() + " "); // !!!
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RestoreSettings();

        Log.v(TAG, "onResume");

        Date CurrentDate = toWholeHour(new Date()); // Оставляем только дату и часы
        CurrentHour = getHour(CurrentDate);

        if (HourRecords == null) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            String records = settings.getString("records", "");

            if (records.equals("")) {
                HourRecords = new ArrayList<HourResult>();
                CurrentHourRecord = new HourResult();
                CurrentHourRecord.setHourStartDate(CurrentDate);
                HourRecords.add(CurrentHourRecord);
                LastElementIndex = 0;
            } else
                HourRecords = new Gson().fromJson(records, new TypeToken<ArrayList<HourResult>>() {
                }.getType());// http://stackoverflow.com/questions/12384064/gson-convert-from-json-to-a-typed-arraylistt
        }
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
        SaveSettings();
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public static class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new TodaySectionFragment();
                case 1:
                    return new EarlySectionFragment();
                default:
                    return new SocialSectionFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Section " + (position + 1);
        }
    }

    public static class TodaySectionFragment extends Fragment {
        private ImageButton todaySettingsButton;
        private ImageButton startStopButton;

        void TodaySectionInterfaceRedraw() {
            switch (appState) {
                case STATE_ZERO:
                    todaySettingsButton.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.anim_alpha_infinite));
                    startStopButton.clearAnimation();
                    startStopButton.setImageResource(R.drawable.play_inactive);
                    break;
                case STATE_PAUSE:
                    todaySettingsButton.clearAnimation();
                    startStopButton.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.anim_alpha_infinite));
                    startStopButton.setImageResource(R.drawable.play);
                    break;
                case STATE_RUN:
                    todaySettingsButton.clearAnimation();
                    startStopButton.clearAnimation();
                    startStopButton.setImageResource(R.drawable.pause);
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.v(TAG, "onActivityResult");

            if (data == null) {
                return;
            }
            if (requestCode == REQUEST_CODE_USERS) {
                userWeight = data.getFloatExtra("userWeight", getResources().getInteger(R.integer.initial_weight));
                userHeight = data.getIntExtra("userHeight", getResources().getInteger(R.integer.initial_height));
                userGender = data.getBooleanExtra("userGender", false);

                Log.v(TAG, "BMR = " + BMRCalculate(userHeight, userWeight, userGender));
                userBmrPerSecond = BMRCalculate(userHeight, userWeight, userGender) / (60.0f * 60.0f * 24.0f);
                Log.v(TAG, "BMR per s = " + userBmrPerSecond);

                if (appState == STATE_ZERO)
                    appState = STATE_PAUSE;

                ((MainActivity) getActivity()).SaveSettings();
                TodaySectionInterfaceRedraw();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_section_today, container, false);
            todaySettingsButton = (ImageButton) rootView.findViewById(R.id.todaySettingsButton);
            startStopButton = (ImageButton) rootView.findViewById(R.id.startStopButton);
            TodayLostCalories = (me.grantland.widget.AutofitTextView) rootView.findViewById(R.id.TodayLostCalories);
            TodayTravelledMeters = (me.grantland.widget.AutofitTextView) rootView.findViewById(R.id.TodayTravelledMeters);

            TodaySectionInterfaceRedraw();

            todaySettingsButton.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Animation animAlpha = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.anim_alpha);
                    arg0.startAnimation(animAlpha);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            Intent intent = new Intent(getActivity().getApplicationContext(), UserActivity.class);
                            intent.putExtra("userWeight", userWeight);
                            intent.putExtra("userHeight", userHeight);
                            intent.putExtra("userGender", userGender);
                            startActivityForResult(intent, REQUEST_CODE_USERS);
                        }
                    }, 350);
                }
            });

            startStopButton.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (appState == STATE_PAUSE) {
                        appState = STATE_RUN;
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.app_start), Toast.LENGTH_LONG).show();

                        wl.acquire();// Wake lock CPU
                    } else if (appState == STATE_RUN) {
                        appState = STATE_PAUSE;
                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.app_stop), Toast.LENGTH_LONG).show();

                        wl.release();// Release CPU
                    }

                    TodaySectionInterfaceRedraw();
                }
            });

            return rootView;
        }
    }

    public static class EarlySectionFragment extends Fragment {
        private BarChart earlyChart;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_section_early, container, false);
            earlyChart = (BarChart) rootView.findViewById(R.id.earlyChart);

            return rootView;
        }

        @Override
        public void setMenuVisibility(final boolean visible) { // http://stackoverflow.com/questions/10024739/how-to-determine-when-fragment-becomes-visible-in-viewpager
            super.setMenuVisibility(visible);
            if (visible) {
                if (!HourRecords.isEmpty()) {
                    ArrayList<BarEntry> entries = new ArrayList<>();

                    ArrayList<String> labels = new ArrayList<String>();

                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    if (EarlyTabState == EARLY_TAB_STATE_HOURLY)
                        df = new SimpleDateFormat("HH:mm");
                    else if (EarlyTabState == EARLY_TAB_STATE_DAYLY)
                        df = new SimpleDateFormat("MM/dd");
                    else if (EarlyTabState == EARLY_TAB_STATE_MONTHLY)
                        df = new SimpleDateFormat("MM/yyyy");

                    for (int i = 0; i < HourRecords.size(); i++) {
                        String label = "";

                        float cal = HourRecords.get(i).getUserCaloriesBurned();
                        int m = HourRecords.get(i).getUserMetersTravelled();
                        entries.add(new BarEntry(new float[]{cal, (float) m}, i));
                        label = df.format(HourRecords.get(i).getHourStartDate());
                        labels.add(label);
                    }

                    BarDataSet dataset = new BarDataSet(entries, "");
                    dataset.setColors(new int[]{getResources().getColor(android.R.color.holo_orange_dark), getResources().getColor(android.R.color.holo_blue_light)});
                    dataset.setStackLabels(new String[]{"Calories burned", "Meters travelled"});

                    BarData data = new BarData(labels, dataset);
                    earlyChart.setData(data);

                    earlyChart.setDescription("");
                    earlyChart.animateXY(1000, 2000);

                    earlyChart.getAxisLeft().setEnabled(false);
                    earlyChart.getAxisRight().setEnabled(false);
                }
            }
        }
    }

    public static class SocialSectionFragment extends Fragment {
        private BarChart socialChart;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_section_social, container, false);
            socialChart = (BarChart) rootView.findViewById(R.id.socialChart);

            return rootView;
        }

        @Override
        public void setMenuVisibility(final boolean visible) {
            super.setMenuVisibility(visible);
            if (visible) {
                ArrayList<BarEntry> entries = new ArrayList<>();

                ArrayList<String> labels = new ArrayList<String>();

                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                if (EarlyTabState == EARLY_TAB_STATE_HOURLY)
                    df = new SimpleDateFormat("HH:mm");
                else if (EarlyTabState == EARLY_TAB_STATE_DAYLY)
                    df = new SimpleDateFormat("MM/dd");
                else if (EarlyTabState == EARLY_TAB_STATE_MONTHLY)
                    df = new SimpleDateFormat("MM/yyyy");

                for (int i = 0; i < HourRecords.size(); i++) {
                    String label = "";

                    float cal = HourRecords.get(i).getUserCaloriesBurned();
                    int m = HourRecords.get(i).getUserMetersTravelled();
                    entries.add(new BarEntry(new float[]{cal, (float) m}, i));
                    label = df.format(HourRecords.get(i).getHourStartDate());
                    labels.add(label);
                }

                BarDataSet dataset = new BarDataSet(entries, "");
                dataset.setColors(new int[]{getResources().getColor(android.R.color.holo_orange_dark), getResources().getColor(android.R.color.holo_blue_light)});
                dataset.setStackLabels(new String[]{"Calories burned", "Meters travelled"});

                BarData data = new BarData(labels, dataset);
                socialChart.setData(data);

                socialChart.setDescription("");
                socialChart.animateXY(1000, 2000);

                socialChart.getAxisLeft().setEnabled(false);
                socialChart.getAxisRight().setEnabled(false);
            }
        }
    }

    void SaveSettings() {
        Log.v(TAG, "SaveSettings");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        String records = new Gson().toJson(HourRecords, ArrayList.class);// http://ru.stackoverflow.com/questions/257554/%D0%A5%D1%80%D0%B0%D0%BD%D0%B5%D0%BD%D0%B8%D0%B5-arraylist-%D0%B2-%D1%84%D0%B0%D0%B9%D0%BB%D0%B5-%D0%BD%D0%B0%D1%81%D1%82%D1%80%D0%BE%D0%B5%D0%BA%D1%82-sharedpreferences
        editor.putString("records", records);

        editor.putInt("appState", appState);
        editor.putFloat("userWeight", userWeight);
        editor.putInt("userHeight", userHeight);
        editor.putBoolean("userGender", userGender);
        editor.putFloat("userBmrPerSecond", userBmrPerSecond);

        editor.apply();
    }

    void RestoreSettings() {
        Log.v(TAG, "RestoreSettings");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        appState = settings.getInt("appState", STATE_ZERO);
        userWeight = settings.getFloat("userWeight", getResources().getInteger(R.integer.initial_weight));
        userHeight = settings.getInt("userHeight", getResources().getInteger(R.integer.initial_height));
        userGender = settings.getBoolean("userGender", true);
        userBmrPerSecond = settings.getFloat("userBmrPerSecond", 0);
    }

    static Date toWholeHour(Date d) {
        Calendar c = new GregorianCalendar();
        c.setTime(d);

        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTime();
    }

    static int getHour(Date d) {
        Calendar c = new GregorianCalendar();
        c.setTime(d);

        return c.get(Calendar.HOUR_OF_DAY); // 24 h format
    }

    // Вычисляет так называемый основной обмен (basal metabolic rate), http://en.wikipedia.org/wiki/Harris%E2%80%93Benedict_equation
    public static float BMRCalculate(int UserHeight, float UserWeight, boolean Gender) {
        float CalculatedResult = 0;

        final float FEMALE_K0 = 447.593f;
        final float FEMALE_K_WEIGHT = 9.247f;
        final float FEMALE_K_HEIGHT = 3.098f;
        final float FEMALE_K_AGE = 4.330f;

        final float MALE_K0 = 88.362f;
        final float MALE_K_WEIGHT = 13.397f;
        final float MALE_K_HEIGHT = 4.799f;
        final float MALE_K_AGE = 5.677f;

        final float K_EXERCISE = 1.2f;

        if (Gender) {// Female
            CalculatedResult = K_EXERCISE * (FEMALE_K0 + FEMALE_K_WEIGHT * UserWeight + FEMALE_K_HEIGHT * UserHeight + FEMALE_K_AGE * 25.0f);// Можно добавить ввод возраста в UserActivity
            return CalculatedResult;
        } else {// Male
            CalculatedResult = K_EXERCISE * (MALE_K0 + MALE_K_WEIGHT * UserWeight + MALE_K_HEIGHT * UserHeight + MALE_K_AGE * 25.0f);
            return CalculatedResult;
        }
    }

    // Вычисляет энергозатраты на ходьбу (Energy Expenditure of Walking and Running - Stanford University - https://www.google.ru/url?sa=t&rct=j&q=&esrc=s&source=web&cd=2&cad=rja&ved=0CDcQFjAB&url=http%3A%2F%2Fwww.stanford.edu%2F~clint%2FRun_Walk2004a.rtf&ei=_1eKUqumFai04AS_34GoBw&usg=AFQjCNHowQNRPAgV04aU1Zg83USHbSFEQQ&sig2=X1LPTUHhmSHYUENH8PdXyQ)
    // Pandolf, K., B. Givoni, and R. Goldman. Predicting energy expenditure with loads while standing or walking very slowly. J. Appl. Physiol. 43:577-581, 1978.
    public static double PandolfCalculate(double UserWeight, double userWalkedDistance, double millisTimeOfStartWalk, double millisTimeOfEndWalk) {
        double userWalkEnergyLoss = 0;

        final double walkLimit = 3.3; // 3.3 м/с = 12 км/ч
        final double jToKcal = 0.000239005736; // Дж в ккал
        double walkTime = millisTimeOfEndWalk / 1000.0 - millisTimeOfStartWalk / 1000.0;

        double userVelocity = userWalkedDistance / walkTime;
        if (userVelocity > walkLimit)
            userVelocity = walkLimit;

        userWalkEnergyLoss = jToKcal * walkTime * (1.5 * UserWeight + 1.5 * Math.pow(userVelocity, 2) * UserWeight);

        return userWalkEnergyLoss;
    }
}