package com.aware.plugin.template;

import android.content.Intent;
import android.net.Uri;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Likert;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

public class Plugin extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::" + getResources().getString(R.string.app_name);

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

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{Provider.TableOne_Data.CONTENT_URI}; //this syncs dummy TableOne_Data to server
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

            //Initialise AWARE instance in plugin
            Aware.startPlugin(this, "com.aware.plugin.template");

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

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
