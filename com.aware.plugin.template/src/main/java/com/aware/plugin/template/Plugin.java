package com.aware.plugin.template;

import android.content.Intent;
import android.net.Uri;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.ui.PermissionsHandler;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Likert;
import com.aware.ui.esms.ESM_PAM;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.PluginsManager;
import com.aware.utils.Scheduler;

import org.json.JSONException;

public class Plugin extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();

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

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.TableOne_Data.CONTENT_URI }; //this syncs dummy TableOne_Data to server
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

                PluginsManager.enablePlugin(this, "com.aware.plugin.template"); //modify to match your plugin package name

                DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

                //Initialize our plugin's settings
                Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);

                //Ask AWARE to start ESM
                Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
                Aware.startESM(this);

                //Setting PAM question
                try {
                    //Using PAM to get users' affect state
                    ESMFactory esmFactory = new ESMFactory();

                    ESM_PAM pam_question = new ESM_PAM();
                    pam_question.setTitle("PAM")
                            .setInstructions("How are you feeling today?")
                            .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
    //                        .setNotificationTimeout(5 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
                            .setSubmitButton("OK");

                    esmFactory.addESM(pam_question);

                    //Schedule this question for the pam, updates old schedule
                    Scheduler.Schedule pam = Scheduler.getSchedule(this, "pam_question");
    //                if (pam == null) {
                    pam = new Scheduler.Schedule("pam_question"); //schedule with pam_question as ID
                    pam.addHour(3).addHour(4).random(30,1);
                    pam.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
                    pam.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
                    pam.addActionExtra(ESM.EXTRA_ESM, esmFactory.build()); //add the questions from the factory

                    Scheduler.saveSchedule(this, pam); //save the questionnaire and schedule it
    //                Log.d(TAG,"Schedule ID for PAM: "+pam.getScheduleID());
    //                }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //Setting Likert question
                try {
                    //Using Likert scale to get users' rating of the day
                    ESMFactory esmFactory = new ESMFactory();

                    ESM_Likert likert_question = new ESM_Likert();
                    likert_question.setLikertMax(5)
                            .setLikertMinLabel("Awful")
                            .setLikertMaxLabel("Awesome!")
                            .setLikertStep(1)
                            .setTitle("Likert")
                            .setInstructions("How would you rate today?")
                            .setExpirationThreshold(0) //no expiration = shows a notification the user can use to answer at any time
    //                        .setNotificationTimeout(5 * 60) //the notification is automatically removed and the questionnaire expired after 5 minutes ( 5 * 60 seconds)
                            .setSubmitButton("OK");

                    esmFactory.addESM(likert_question);

                    //Schedule this question for the likert, updates old schedule
                    Scheduler.Schedule likert = Scheduler.getSchedule(this, "likert_question");
    //                if (likert == null) {
                    likert = new Scheduler.Schedule("likert_question"); //schedule with likert_question as ID
                    likert.setInterval(10);
                    likert.setActionType(Scheduler.ACTION_TYPE_BROADCAST); //sending a request to the client via broadcast
                    likert.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM); //with the action of ACTION_AWARE_QUEUE_ESM, i.e., queueing a new ESM
                    likert.addActionExtra(ESM.EXTRA_ESM, esmFactory.build()); //add the questions from the factory

                    Scheduler.saveSchedule(this, likert); //save the questionnaire and schedule it
    //                Log.d(TAG,"Schedule ID for likert: "+likert.getScheduleID());
    //                }

                    //Initialise AWARE instance in plugin
                    Aware.startAWARE(this);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                } else {
                    Intent permissions = new Intent(this, PermissionsHandler.class);
                    permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
                    permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(permissions);
                }


            return START_STICKY;

//        return super.onStartCommand(intent, flags, startId);

        }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Stop AWARE instance in plugin
        Aware.stopAWARE(this);
    }
}
