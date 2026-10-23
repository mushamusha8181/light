package jp.mushamusha.threelevelflashlight;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ScreenLightActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_light);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = 1.0f;
        getWindow().setAttributes(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        View root = findViewById(R.id.screenLightRoot);
        TextView hint = findViewById(R.id.screenLightHint);
        root.setBackgroundColor(Color.WHITE);
        hint.setTextColor(Color.DKGRAY);
        root.setOnClickListener(v -> finish());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) hint.getLayoutParams();
                int baseMargin = Math.round(16 * getResources().getDisplayMetrics().density);
                layoutParams.bottomMargin = baseMargin + insets.getSystemWindowInsetBottom();
                hint.setLayoutParams(layoutParams);
                return insets;
            });
            root.requestApplyInsets();
        }
    }
}
