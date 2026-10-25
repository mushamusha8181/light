package jp.mushamusha.threelevelflashlight;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import jp.mushamusha.mushinglass.MushinGlass;

public class MainActivity extends Activity {

    public static final String EXTRA_REQUEST_CAMERA_PERMISSION =
            "jp.mushamusha.threelevelflashlight.REQUEST_CAMERA_PERMISSION";

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final String STATE_AUTO_ON_ATTEMPTED = "auto_on_attempted";

    private TorchController.FlashInfo flashInfo;
    private boolean autoOnAttempted;
    private boolean torchCallbackRegistered;
    private CameraManager cameraManager;
    private CameraManager.TorchCallback torchCallback;

    private TextView lampIcon;
    private TextView statusText;
    private TextView supportText;
    private Button weakButton;
    private Button mediumButton;
    private Button strongButton;
    private Button offButton;
    private Button screenLightButton;
    private Button settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarInsets();
        autoOnAttempted = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_AUTO_ON_ATTEMPTED, false);

        lampIcon = findViewById(R.id.lampIcon);
        statusText = findViewById(R.id.statusText);
        supportText = findViewById(R.id.supportText);
        weakButton = findViewById(R.id.weakButton);
        mediumButton = findViewById(R.id.mediumButton);
        strongButton = findViewById(R.id.strongButton);
        offButton = findViewById(R.id.offButton);
        screenLightButton = findViewById(R.id.screenLightButton);
        settingsButton = findViewById(R.id.settingsButton);

        weakButton.setOnClickListener(v -> setLight(1, v));
        mediumButton.setOnClickListener(v -> setLight(2, v));
        strongButton.setOnClickListener(v -> setLight(3, v));
        offButton.setOnClickListener(this::turnOff);
        screenLightButton.setOnClickListener(
                v -> startActivity(new Intent(this, ScreenLightActivity.class))
        );
        settingsButton.setOnClickListener(v -> showSettingsDialog());

        updateUi(0);

        if (hasCameraPermission()) {
            initializeFlash();
        } else {
            setControlsEnabled(false);
            boolean requestedBefore = wasCameraPermissionRequested();
            if (!requestedBefore) {
                supportText.setText(R.string.permission_waiting);
                requestCameraPermission();
            } else {
                showPermissionRecoveryHint();
                if (getIntent().getBooleanExtra(EXTRA_REQUEST_CAMERA_PERMISSION, false)) {
                    supportText.post(this::handleMissingPermission);
                }
            }
        }
    }

    private void applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return;
        }
        View root = findViewById(R.id.rootScroll);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        root.requestApplyInsets();
    }

    private boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean wasCameraPermissionRequested() {
        return LightPrefs.wasCameraPermissionRequested(this);
    }

    private void requestCameraPermission() {
        if (hasCameraPermission()) {
            initializeFlash();
            return;
        }
        LightPrefs.markCameraPermissionRequested(this);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            supportText.setOnClickListener(null);
            supportText.setClickable(false);
            setControlsEnabled(true);
            supportText.setText(R.string.support_checking);
            initializeFlash();
            registerTorchCallbackIfNeeded();
            return;
        }

        setControlsEnabled(false);
        showPermissionRecoveryHint();
        Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show();
        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showPermissionSettingsDialog();
        }
    }

    private void showPermissionRecoveryHint() {
        supportText.setText(R.string.permission_denied_tap);
        supportText.setClickable(true);
        supportText.setOnClickListener(v -> handleMissingPermission());
    }

    private void showPermissionSettingsDialog() {
        if (!wasCameraPermissionRequested() || hasCameraPermission() || isFinishing()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> openAppSettings())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lampIcon == null) {
            return;
        }

        if (hasCameraPermission()) {
            supportText.setOnClickListener(null);
            supportText.setClickable(false);
            if (flashInfo == null) {
                setControlsEnabled(true);
                supportText.setText(R.string.support_checking);
                initializeFlash();
            }
            registerTorchCallbackIfNeeded();
        } else if (wasCameraPermissionRequested()) {
            unregisterTorchCallback();
            setControlsEnabled(false);
            showPermissionRecoveryHint();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_AUTO_ON_ATTEMPTED, autoOnAttempted);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        unregisterTorchCallback();
        super.onPause();
    }

    private void initializeFlash() {
        if (!hasCameraPermission()) {
            flashInfo = null;
            setControlsEnabled(false);
            showPermissionRecoveryHint();
            return;
        }

        try {
            flashInfo = TorchController.inspect(this);
            if (flashInfo == null) {
                disableControls(getString(R.string.no_flash));
                return;
            }

            setControlsEnabled(true);
            updateSupportText();

            if (!autoOnAttempted) {
                autoOnAttempted = true;
                if (LightPrefs.isAutoOn(this)) {
                    setLight(LightPrefs.getLastLevel(this), null);
                }
            }
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            disableControls(getString(R.string.light_error));
        }
    }

    private void updateSupportText() {
        if (flashInfo == null) {
            return;
        }
        if (flashInfo.adjustable && flashInfo.maxStrength >= 3) {
            supportText.setText(getString(R.string.support_adjustable, flashInfo.maxStrength));
        } else if (flashInfo.adjustable) {
            supportText.setText(getString(R.string.support_two_levels, flashInfo.maxStrength));
        } else {
            supportText.setText(R.string.support_fixed);
        }
    }

    private void registerTorchCallbackIfNeeded() {
        if (!hasCameraPermission() || flashInfo == null || torchCallbackRegistered) {
            return;
        }

        cameraManager = getSystemService(CameraManager.class);
        if (cameraManager == null) {
            return;
        }

        if (torchCallback == null) {
            torchCallback = new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    if (!isSelectedCamera(cameraId)) {
                        return;
                    }
                    if (enabled) {
                        int level = LightPrefs.getCurrentLevel(MainActivity.this);
                        if (level == 0) {
                            level = LightPrefs.getLastLevel(MainActivity.this);
                        }
                        LightPrefs.setObservedTorchOn(MainActivity.this, level);
                        updateUi(level);
                    } else {
                        LightPrefs.markTorchOff(MainActivity.this);
                        AutoOffScheduler.cancel();
                        updateUi(0);
                    }
                    setControlsEnabled(true);
                    updateSupportText();
                    TileHelper.requestRefresh(MainActivity.this);
                }

                @Override
                public void onTorchModeUnavailable(String cameraId) {
                    if (!isSelectedCamera(cameraId)) {
                        return;
                    }
                    LightPrefs.markTorchOff(MainActivity.this);
                    AutoOffScheduler.cancel();
                    updateUi(0);
                    weakButton.setEnabled(false);
                    mediumButton.setEnabled(false);
                    strongButton.setEnabled(false);
                    supportText.setText(R.string.support_unavailable);
                    TileHelper.requestRefresh(MainActivity.this);
                }

                @Override
                public void onTorchStrengthLevelChanged(String cameraId, int newStrengthLevel) {
                    if (!isSelectedCamera(cameraId) || flashInfo == null) {
                        return;
                    }
                    int logicalLevel = TorchController.logicalLevelForStrength(
                            flashInfo.maxStrength,
                            newStrengthLevel
                    );
                    if (logicalLevel > 0) {
                        LightPrefs.setObservedTorchOn(MainActivity.this, logicalLevel);
                        updateUi(logicalLevel);
                    }
                }
            };
        }

        try {
            cameraManager.registerTorchCallback(
                    torchCallback,
                    new Handler(Looper.getMainLooper())
            );
            torchCallbackRegistered = true;
        } catch (IllegalArgumentException | SecurityException ignored) {
            torchCallbackRegistered = false;
        }
    }

    private void unregisterTorchCallback() {
        if (!torchCallbackRegistered || cameraManager == null || torchCallback == null) {
            return;
        }
        try {
            cameraManager.unregisterTorchCallback(torchCallback);
        } catch (RuntimeException ignored) {
            // Activity終了時の後処理なのでUIには出さない。
        }
        torchCallbackRegistered = false;
    }

    private boolean isSelectedCamera(String cameraId) {
        return flashInfo != null && flashInfo.cameraId.equals(cameraId);
    }

    private void setLight(int level, View source) {
        if (!hasCameraPermission()) {
            handleMissingPermission();
            return;
        }

        if (flashInfo == null) {
            initializeFlash();
            if (flashInfo == null) {
                Toast.makeText(this, R.string.no_flash, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (source != null) {
            source.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }

        try {
            TorchController.turnOn(this, flashInfo, level);
            LightPrefs.beginOwnedTorchSession(this, level);
            AutoOffScheduler.scheduleIfNeeded(this, flashInfo);
            updateUi(level);
            TileHelper.requestRefresh(this);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            Toast.makeText(this, R.string.light_error, Toast.LENGTH_LONG).show();
        }
    }

    private void handleMissingPermission() {
        if (hasCameraPermission()) {
            initializeFlash();
            return;
        }
        if (!wasCameraPermissionRequested()) {
            requestCameraPermission();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_rationale_title)
                    .setMessage(R.string.permission_rationale_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.allow, (dialog, which) -> requestCameraPermission())
                    .show();
        } else {
            showPermissionSettingsDialog();
        }
    }

    private void turnOff(View source) {
        if (source != null) {
            source.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        turnOffInternal(true);
    }

    private void turnOffInternal(boolean showError) {
        try {
            if (hasCameraPermission() && flashInfo != null) {
                TorchController.turnOff(this, flashInfo);
            }
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            if (showError) {
                Toast.makeText(this, R.string.light_error, Toast.LENGTH_LONG).show();
            }
            return;
        }

        LightPrefs.markTorchOff(this);
        AutoOffScheduler.cancel();
        updateUi(0);
        TileHelper.requestRefresh(this);
    }

    private void showSettingsDialog() {
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        Switch autoOnSwitch = new Switch(this);
        autoOnSwitch.setText(R.string.setting_auto_on);
        autoOnSwitch.setChecked(LightPrefs.isAutoOn(this));
        autoOnSwitch.setTextSize(14);
        autoOnSwitch.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        box.addView(autoOnSwitch);

        TextView timerTitle = new TextView(this);
        timerTitle.setText(R.string.setting_auto_off);
        timerTitle.setTextSize(15);
        timerTitle.setPadding(0, pad, 0, pad / 3);
        box.addView(timerTitle);

        RadioGroup group = new RadioGroup(this);
        int[] minutes = {0, 1, 5, 10, 30};
        int[] labels = {
                R.string.timer_none,
                R.string.timer_1,
                R.string.timer_5,
                R.string.timer_10,
                R.string.timer_30
        };
        int current = LightPrefs.getAutoOffMinutes(this);
        for (int i = 0; i < minutes.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(labels[i]);
            radioButton.setTextSize(14);
            radioButton.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
            radioButton.setTag(minutes[i]);
            radioButton.setChecked(minutes[i] == current);
            group.addView(radioButton);
        }
        box.addView(group);

        Button quickTileButton = new Button(this);
        quickTileButton.setText(R.string.add_quick_tile);
        quickTileButton.setTextSize(13);
        quickTileButton.setAllCaps(false);
        quickTileButton.setTextColor(getColor(R.color.text_primary));
        quickTileButton.setBackgroundResource(R.drawable.bg_secondary_button);
        LinearLayout.LayoutParams quickTileParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.round(48 * getResources().getDisplayMetrics().density)
        );
        quickTileParams.topMargin = pad / 2;
        box.addView(quickTileButton, quickTileParams);
        quickTileButton.setOnClickListener(v -> requestQuickTile());

        MushinGlass.applyToView(this, box);
        ScrollView settingsScroll = new ScrollView(this);
        settingsScroll.setFillViewport(false);
        settingsScroll.addView(box);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.settings)
                .setView(settingsScroll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                LightPrefs.setAutoOn(this, autoOnSwitch.isChecked());
                int checkedId = group.getCheckedRadioButtonId();
                RadioButton checked = checkedId == -1 ? null : group.findViewById(checkedId);
                int value = checked == null ? 0 : (int) checked.getTag();
                LightPrefs.setAutoOffMinutes(this, value);
                if (LightPrefs.isTorchOn(this) && LightPrefs.isTorchOwnedByApp(this)) {
                    AutoOffScheduler.scheduleIfNeeded(this, flashInfo);
                } else {
                    AutoOffScheduler.cancel();
                }
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void requestQuickTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StatusBarManager statusBarManager = getSystemService(StatusBarManager.class);
            if (statusBarManager == null) {
                Toast.makeText(this, R.string.tile_manual, Toast.LENGTH_LONG).show();
                return;
            }
            ComponentName component = new ComponentName(this, QuickTileService.class);
            Icon icon = Icon.createWithResource(this, R.drawable.ic_flash_tile);
            try {
                statusBarManager.requestAddTileService(
                        component,
                        getString(R.string.tile_label),
                        icon,
                        getMainExecutor(),
                        result -> {
                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                Toast.makeText(this, R.string.tile_added, Toast.LENGTH_SHORT).show();
                            } else if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, R.string.tile_add_failed, Toast.LENGTH_LONG).show();
                            }
                        }
                );
            } catch (RuntimeException e) {
                Toast.makeText(this, R.string.tile_manual, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.tile_manual, Toast.LENGTH_LONG).show();
        }
    }

    private void updateUi(int level) {
        weakButton.setSelected(level == 1);
        mediumButton.setSelected(level == 2);
        strongButton.setSelected(level == 3);

        int normalText = getColor(R.color.text_primary);
        int selectedText = getColor(R.color.accent_text);
        weakButton.setTextColor(level == 1 ? selectedText : normalText);
        mediumButton.setTextColor(level == 2 ? selectedText : normalText);
        strongButton.setTextColor(level == 3 ? selectedText : normalText);

        if (level == 1) {
            statusText.setText(R.string.status_weak);
            lampIcon.setTextColor(getColor(R.color.accent));
            lampIcon.setAlpha(0.45f);
        } else if (level == 2) {
            statusText.setText(R.string.status_medium);
            lampIcon.setTextColor(getColor(R.color.accent));
            lampIcon.setAlpha(0.72f);
        } else if (level == 3) {
            statusText.setText(R.string.status_strong);
            lampIcon.setTextColor(getColor(R.color.accent));
            lampIcon.setAlpha(1.0f);
        } else {
            statusText.setText(R.string.status_off);
            lampIcon.setTextColor(getColor(R.color.button_stroke));
            lampIcon.setAlpha(1.0f);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        weakButton.setEnabled(enabled);
        mediumButton.setEnabled(enabled);
        strongButton.setEnabled(enabled);
        offButton.setEnabled(true);
        screenLightButton.setEnabled(true);
        settingsButton.setEnabled(true);
    }

    private void disableControls(String message) {
        flashInfo = null;
        weakButton.setEnabled(false);
        mediumButton.setEnabled(false);
        strongButton.setEnabled(false);
        offButton.setEnabled(true);
        supportText.setText(message);
    }
}
