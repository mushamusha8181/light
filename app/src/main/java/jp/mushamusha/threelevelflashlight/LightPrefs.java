package jp.mushamusha.threelevelflashlight;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

public final class LightPrefs {
    private LightPrefs() {}

    private static final String PREFS = "simple_light_prefs";
    private static final String KEY_CAMERA_PERMISSION_REQUESTED = "camera_permission_requested";
    private static final String KEY_LAST_LEVEL = "last_level";
    private static final String KEY_CURRENT_LEVEL = "current_level";
    private static final String KEY_AUTO_ON = "auto_on";
    private static final String KEY_AUTO_OFF_MINUTES = "auto_off_minutes";
    private static final String KEY_TORCH_ON = "torch_on";
    private static final String KEY_OWNER_PROCESS_TOKEN = "owner_process_token";
    private static final String KEY_TORCH_SESSION = "torch_session";

    private static final long PROCESS_TOKEN = createProcessToken();

    public static SharedPreferences get(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }


    public static boolean wasCameraPermissionRequested(Context context) {
        return get(context).getBoolean(KEY_CAMERA_PERMISSION_REQUESTED, false);
    }

    public static void markCameraPermissionRequested(Context context) {
        get(context).edit().putBoolean(KEY_CAMERA_PERMISSION_REQUESTED, true).apply();
    }

    public static int getLastLevel(Context context) {
        return clampLevel(get(context).getInt(KEY_LAST_LEVEL, 2));
    }

    public static int getCurrentLevel(Context context) {
        int level = get(context).getInt(KEY_CURRENT_LEVEL, 0);
        return level == 0 ? 0 : clampLevel(level);
    }

    public static boolean isAutoOn(Context context) {
        return get(context).getBoolean(KEY_AUTO_ON, false);
    }

    public static void setAutoOn(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_AUTO_ON, enabled).apply();
    }

    public static int getAutoOffMinutes(Context context) {
        int value = get(context).getInt(KEY_AUTO_OFF_MINUTES, 0);
        return value == 1 || value == 5 || value == 10 || value == 30 ? value : 0;
    }

    public static void setAutoOffMinutes(Context context, int minutes) {
        int safe = minutes == 1 || minutes == 5 || minutes == 10 || minutes == 30 ? minutes : 0;
        get(context).edit().putInt(KEY_AUTO_OFF_MINUTES, safe).apply();
    }

    public static boolean isTorchOn(Context context) {
        return get(context).getBoolean(KEY_TORCH_ON, false);
    }

    public static boolean isTorchOwnedByApp(Context context) {
        SharedPreferences prefs = get(context);
        return prefs.getBoolean(KEY_TORCH_ON, false)
                && prefs.getLong(KEY_OWNER_PROCESS_TOKEN, 0L) == PROCESS_TOKEN;
    }

    public static int getTorchSession(Context context) {
        return get(context).getInt(KEY_TORCH_SESSION, 0);
    }

    public static synchronized void beginOwnedTorchSession(Context context, int level) {
        SharedPreferences prefs = get(context);
        int nextSession = prefs.getInt(KEY_TORCH_SESSION, 0) + 1;
        int safeLevel = clampLevel(level);
        prefs.edit()
                .putInt(KEY_TORCH_SESSION, nextSession)
                .putBoolean(KEY_TORCH_ON, true)
                .putLong(KEY_OWNER_PROCESS_TOKEN, PROCESS_TOKEN)
                .putInt(KEY_CURRENT_LEVEL, safeLevel)
                .putInt(KEY_LAST_LEVEL, safeLevel)
                .apply();
    }

    public static void setObservedTorchOn(Context context, int level) {
        SharedPreferences prefs = get(context);
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(KEY_TORCH_ON, true);
        if (prefs.getLong(KEY_OWNER_PROCESS_TOKEN, 0L) != PROCESS_TOKEN) {
            editor.remove(KEY_OWNER_PROCESS_TOKEN);
        }
        if (level >= 1 && level <= 3) {
            editor.putInt(KEY_CURRENT_LEVEL, level);
        }
        editor.apply();
    }

    public static synchronized void markTorchOff(Context context) {
        SharedPreferences prefs = get(context);
        boolean hadActiveState = prefs.getBoolean(KEY_TORCH_ON, false)
                || prefs.contains(KEY_OWNER_PROCESS_TOKEN);
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(KEY_TORCH_ON, false)
                .remove(KEY_OWNER_PROCESS_TOKEN)
                .putInt(KEY_CURRENT_LEVEL, 0);
        if (hadActiveState) {
            editor.putInt(KEY_TORCH_SESSION, prefs.getInt(KEY_TORCH_SESSION, 0) + 1);
        }
        editor.apply();
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(3, level));
    }

    private static long createProcessToken() {
        long token = new SecureRandom().nextLong();
        return token == 0L ? 1L : token;
    }
}
