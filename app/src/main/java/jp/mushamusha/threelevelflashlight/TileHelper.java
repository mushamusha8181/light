package jp.mushamusha.threelevelflashlight;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.service.quicksettings.TileService;

public final class TileHelper {
    private TileHelper() {}

    public static void requestRefresh(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        try {
            TileService.requestListeningState(
                    context.getApplicationContext(),
                    new ComponentName(context, QuickTileService.class)
            );
        } catch (RuntimeException ignored) {
            // Quick Settings が利用できない端末でもライト本体には影響させない。
        }
    }
}
