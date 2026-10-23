package jp.mushamusha.threelevelflashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TorchOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int token = intent.getIntExtra(AutoOffScheduler.EXTRA_TOKEN, 0);
        int session = intent.getIntExtra(AutoOffScheduler.EXTRA_SESSION, -1);
        AutoOffScheduler.handleAlarm(context, token, session);
    }
}
