package jp.mushamusha.threelevelflashlight;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public final class AutoOffScheduler {
    private AutoOffScheduler() {}

    static final String EXTRA_TOKEN = "auto_off_token";
    static final String EXTRA_SESSION = "auto_off_session";

    private static final int REQUEST_CODE = 2201;
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static Runnable pendingTask;
    private static CameraManager cameraManager;
    private static CameraManager.TorchCallback monitorCallback;
    private static String monitoredCameraId;
    private static Context activeContext;
    private static TorchController.FlashInfo activeFlashInfo;
    private static int activeToken;
    private static int tokenCounter;

    public static synchronized void scheduleIfNeeded(
            Context context,
            TorchController.FlashInfo flashInfo
    ) {
        cancelLocked();

        int minutes = LightPrefs.getAutoOffMinutes(context);
        if (minutes <= 0
                || flashInfo == null
                || !LightPrefs.isTorchOwnedByApp(context)) {
            return;
        }

        activeContext = context.getApplicationContext();
        activeFlashInfo = flashInfo;
        int session = LightPrefs.getTorchSession(activeContext);
        activeToken = nextToken();
        long delayMillis = minutes * 60_000L;

        registerStateMonitor(activeContext, flashInfo);

        int token = activeToken;
        pendingTask = () -> turnOffIfSessionMatches(activeContext, token, session);
        HANDLER.postDelayed(pendingTask, delayMillis);
        scheduleIdleSafeBackup(activeContext, token, session, delayMillis);
    }

    public static synchronized void cancel() {
        cancelLocked();
    }

    static void handleAlarm(Context context, int token, int session) {
        synchronized (AutoOffScheduler.class) {
            if (token == 0 || token != activeToken || activeContext == null) {
                return;
            }
        }
        turnOffIfSessionMatches(context.getApplicationContext(), token, session);
    }

    private static int nextToken() {
        tokenCounter++;
        if (tokenCounter == 0) {
            tokenCounter++;
        }
        return tokenCounter;
    }

    private static void scheduleIdleSafeBackup(
            Context context,
            int token,
            int session,
            long delayMillis
    ) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        long triggerAt = SystemClock.elapsedRealtime() + delayMillis;
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent(context, token, session)
        );
    }

    private static PendingIntent pendingIntent(Context context, int token, int session) {
        Intent intent = new Intent(context, TorchOffReceiver.class)
                .putExtra(EXTRA_TOKEN, token)
                .putExtra(EXTRA_SESSION, session);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static void registerStateMonitor(
            Context context,
            TorchController.FlashInfo flashInfo
    ) {
        cameraManager = context.getSystemService(CameraManager.class);
        if (cameraManager == null) {
            return;
        }

        monitoredCameraId = flashInfo.cameraId;
        monitorCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                if (!cameraId.equals(monitoredCameraId) || enabled) {
                    return;
                }
                synchronized (AutoOffScheduler.class) {
                    cancelLocked();
                }
                LightPrefs.markTorchOff(context);
                TileHelper.requestRefresh(context);
            }

            @Override
            public void onTorchModeUnavailable(String cameraId) {
                if (!cameraId.equals(monitoredCameraId)) {
                    return;
                }
                synchronized (AutoOffScheduler.class) {
                    cancelLocked();
                }
                LightPrefs.markTorchOff(context);
                TileHelper.requestRefresh(context);
            }
        };

        try {
            cameraManager.registerTorchCallback(monitorCallback, HANDLER);
        } catch (IllegalArgumentException | SecurityException ignored) {
            cameraManager = null;
            monitorCallback = null;
            monitoredCameraId = null;
        }
    }

    private static void cancelLocked() {
        if (pendingTask != null) {
            HANDLER.removeCallbacks(pendingTask);
            pendingTask = null;
        }

        if (activeContext != null && activeToken != 0) {
            AlarmManager alarmManager = activeContext.getSystemService(AlarmManager.class);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent(activeContext, activeToken,
                        LightPrefs.getTorchSession(activeContext)));
            }
        }

        if (cameraManager != null && monitorCallback != null) {
            try {
                cameraManager.unregisterTorchCallback(monitorCallback);
            } catch (RuntimeException ignored) {
                // 後処理なので無視する。
            }
        }

        cameraManager = null;
        monitorCallback = null;
        monitoredCameraId = null;
        activeFlashInfo = null;
        activeContext = null;
        activeToken = 0;
    }

    private static void turnOffIfSessionMatches(Context context, int token, int session) {
        TorchController.FlashInfo flashInfo;
        synchronized (AutoOffScheduler.class) {
            if (token == 0 || token != activeToken) {
                return;
            }
            flashInfo = activeFlashInfo;
            pendingTask = null;
        }

        if (flashInfo == null
                || session != LightPrefs.getTorchSession(context)
                || !LightPrefs.isTorchOn(context)
                || !LightPrefs.isTorchOwnedByApp(context)) {
            synchronized (AutoOffScheduler.class) {
                cancelLocked();
            }
            return;
        }

        if (context.checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            LightPrefs.markTorchOff(context);
            synchronized (AutoOffScheduler.class) {
                cancelLocked();
            }
            TileHelper.requestRefresh(context);
            return;
        }

        try {
            TorchController.turnOff(context, flashInfo);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException ignored) {
            // CameraManager のコールバックが実状態を補正する。
        } finally {
            LightPrefs.markTorchOff(context);
            synchronized (AutoOffScheduler.class) {
                cancelLocked();
            }
            TileHelper.requestRefresh(context);
        }
    }
}
