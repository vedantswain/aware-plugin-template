package com.aware.plugin.template;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.aware.Accelerometer;
import com.aware.Aware;
import com.aware.Aware_Preferences;

import com.aware.Screen;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();


        //This allows plugin data to be synced on demand from broadcast Aware#ACTION_AWARE_SYNC_DATA
        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::"+getResources().getString(R.string.app_name);

        /**
         * Plugins share their current status, i.e., context using this method.
         * This method is called automatically when triggering
         * {@link Aware#ACTION_AWARE_CURRENT_CONTEXT}
         **/
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Android M+).
        //By default, AWARE asks access to the #Manifest.permission.WRITE_EXTERNAL_STORAGE

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;
    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }
    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }


    public interface AWARESensorObserver {
        void onDataChanged(ContentValues data);
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

            /**
             * Example of how to enable accelerometer sensing and how to access the data in real-time for your app.
             * In this particular case, we are sending a broadcast that the ContextCard listens to and updates the UI in real-time.
             */
            Aware.startAccelerometer(this);
            Accelerometer.setSensorObserver(new Accelerometer.AWARESensorObserver() {
                @Override
                public void onAccelerometerChanged(ContentValues contentValues) {
                    sendBroadcast(new Intent("ACCELEROMETER_DATA").putExtra("data", contentValues));
                }
            });

            Aware.startScreen(this);
            Screen.setSensorObserver(new Screen.AWARESensorObserver() {
                @Override
                public void onScreenOn() {

                }

                @Override
                public void onScreenOff() {

                }

                @Override
                public void onScreenLocked() {

                }

                @Override
                public void onScreenUnlocked() {

                }
            });

            //Enable our plugin's sync-adapter to upload the data to the server if part of a study
            if (Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
                );
            }

            //Initialise AWARE instance in plugin
            Aware.startAWARE(this);

            try {
                //Using Likert scale to get users' rating of the day
                ESMFactory esmFactory = new ESMFactory();

                ESM_Likert evening_question = new ESM_Likert();
                evening_question.setLikertMax(5)
                        .setLikertMinLabel("Awful")
                        .setLikertMaxLabel("Awesome!")
                        .setLikertStep(1)
                        .setTitle("Evening!")
                        .setInstructions("How would you rate today?")
                        .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
                        .setNotificationTimeout(2 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
                        .setSubmitButton("OK");

                esmFactory.addESM(evening_question);

                //Schedule this question for the evening, only if not yet defined
                Scheduler.Schedule evening = Scheduler.getSchedule(this, "evening_question1.1");
                if (evening == null) {
                    evening = new Scheduler.Schedule("evening_question1.1"); //schedule with morning_question as ID
                    evening.addMinute(3);
                    evening.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
                    evening.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
                    evening.addActionExtra(ESM.EXTRA_ESM, esmFactory.build()); //add the questions from the factory

                    Scheduler.saveSchedule(this, evening); //save the questionnaire and schedule it
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        //Turn off the sync-adapter if part of a study
        if (Aware.isStudy(this) && (getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone))) {
            ContentResolver.removePeriodicSync(
                    Aware.getAWAREAccount(this),
                    Provider.getAuthority(this),
                    Bundle.EMPTY
            );
        }

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
