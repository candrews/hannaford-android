package com.hannaford.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.Tracking;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

/**
 * Created by candrews on 7/29/16.
 */
public class HannafordApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MetricsManager.register(this, this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                Tracking.startUsage(activity);
                CrashManager.register(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Tracking.stopUsage(activity);
                UpdateManager.unregister();
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                UpdateManager.unregister();
            }
        });
    }
}
