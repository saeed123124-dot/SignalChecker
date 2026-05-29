package com.example.signalchecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class SignalMonitorService extends Service {

    private static final String CHANNEL_ID = "SignalMonitorChannel";
    private static final int NOTIFICATION_ID = 1;
    private TelephonyManager telephonyManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("پایش سیگنال")
                .setContentText("در حال بررسی وضعیت آنتن‌دهی...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        startSignalMonitoring();
        return START_NOT_STICKY;
    }

    private void startSignalMonitoring() {
        if (!isSimCardReady()) {
            triggerAlarm();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                    getMainExecutor(),
                    new TelephonyCallback.SignalStrengthsListener() {
                        @Override
                        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                            checkSignalLevel(signalStrength);
                        }
                    }
            );
        }
    }

    private boolean isSimCardReady() {
        int simState = telephonyManager.getSimState();
        return simState == TelephonyManager.SIM_STATE_READY;
    }

    private void checkSignalLevel(SignalStrength signalStrength) {
        int dbm = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var cellInfos = signalStrength.getCellSignalStrengths();
            if (cellInfos != null && !cellInfos.isEmpty()) {
                dbm = cellInfos.get(0).getDbm();
            }
        }

        if (dbm < -110) {
            triggerAlarm();
        }
    }

    private void triggerAlarm() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder alarmBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("هشدار قطعی آنتن!")
                .setContentText("سیم‌کارت فعال نیست یا سیگنال بسیار ضعیف است.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        notificationManager.notify(2, alarmBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "پایش سیگنال",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
