package com.nextbit.aaronhsu.projectdittohost;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by aaronhsu on 4/18/16.
 */
public class InspectService extends IntentService {
    private static final String TAG = "Ditto.InspectService";

    public static final String INSPECT_VIEW_ACTION = "com.nextbit.aaronhsu.INSPECT_VIEW";

    public InspectService() {
        super("InspectService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onHandleIntent started " + action);

        if (INSPECT_VIEW_ACTION.equals(action)) {
            Log.d(TAG, "Inspecting view...");
//            Activity activity = getForegroundActivity();
            getForegroundActivity();
        }
    }

    private void getForegroundActivity() {
        // todo (aaron): Needs to get REAL foreground activity
        ComponentName compName = new ComponentName(this, MainActivity.class);
        try {
            ActivityInfo activityInfo = getPackageManager()
                    .getActivityInfo(compName, PackageManager.GET_META_DATA);
            Log.d(TAG, "Activity found for: " + activityInfo.packageName);
            Log.d(TAG, "Metadata for activity: " + activityInfo.metaData.keySet());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Name not found: " + compName, e);
        }

    }

//    UsageStatsManager usm = (UsageStatsManager) getSystemService("usagestats");
//    long time = System.currentTimeMillis();
//    List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
//            time - 1000 * 1000, time);
//    Log.d(TAG, "usage stats: " + appList.size());
//    if (appList != null && appList.size() > 0) {
//        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
//        for (UsageStats usageStats : appList) {
//            mySortedMap.put(usageStats.getLastTimeUsed(),
//                    usageStats);
//        }
//        if (mySortedMap != null && !mySortedMap.isEmpty()) {
//            Log.d(TAG, "current package: " + mySortedMap.get(mySortedMap.lastKey()).getPackageName());
//        } else {
//            Log.d(TAG, "something fucked up");
//        }
//    }

//    ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
//    List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
//    ComponentName compName = taskInfo.get(0).topActivity;
//    Log.d(TAG, "current package: " + compName.getPackageName());
//    try {
//        ActivityInfo activityInfo = getPackageManager().getActivityInfo(compName, PackageManager.GET_META_DATA);
//        String a = activityInfo.targetActivity;
//        Bundle b = activityInfo.metaData
//    } catch (PackageManager.NameNotFoundException e) {
//        Log.e(TAG, "no package with name " + compName, e);
//    }


//    public static Activity getActivity() throws Exception {
//        Class activityThreadClass = Class.forName("android.app.ActivityThread");
//        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
//        Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
//        activitiesField.setAccessible(true);
//        Map activities = (Map) activitiesField.get(activityThread);
//        for (Object activityRecord : activities.values()) {
//            Class activityRecordClass = activityRecord.getClass();
//            Field pausedField = activityRecordClass.getDeclaredField("paused");
//            pausedField.setAccessible(true);
//            if (!pausedField.getBoolean(activityRecord)) {
//                Field activityField = activityRecordClass.getDeclaredField("activity");
//                activityField.setAccessible(true);
//                Activity activity = (Activity) activityField.get(activityRecord);
//                return activity;
//            }
//        }
//        return null;
//    }
}
