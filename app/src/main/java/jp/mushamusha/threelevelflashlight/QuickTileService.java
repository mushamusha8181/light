package jp.mushamusha.threelevelflashlight;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class QuickTileService extends TileService {

    private TorchController.FlashInfo flashInfo;
    private CameraManager cameraManager;
    private CameraManager.TorchCallback torchCallback;
    private boolean callbackRegistered;

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (!hasCameraPermission()) {
            LightPrefs.markTorchOff(this);
            AutoOffScheduler.cancel();
            setTileNeedsPermission();
            return;
        }
        if (!ensureFlashInfo()) {
            setTileUnavailable();
            return;
        }
        registerTorchCallback();
        updateTile();
    }

    @Override
    public void onStopListening() {
        unregisterTorchCallback();
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (!hasCameraPermission()) {
            launchMainForPermission();
            return;
        }

        if (!ensureFlashInfo()) {
            setTileUnavailable();
            return;
        }

        boolean currentlyOn = LightPrefs.isTorchOn(this);
        try {
            if (currentlyOn) {
                TorchController.turnOff(this, flashInfo);
                LightPrefs.markTorchOff(this);
                AutoOffScheduler.cancel();
            } else {
                int level = LightPrefs.getLastLevel(this);
                TorchController.turnOn(this, flashInfo, level);
                LightPrefs.beginOwnedTorchSession(this, level);
                AutoOffScheduler.scheduleIfNeeded(this, flashInfo);
            }
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            if (!currentlyOn) {
                LightPrefs.markTorchOff(this);
                AutoOffScheduler.cancel();
            }
        }

        updateTile();
    }

    private boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean ensureFlashInfo() {
        if (flashInfo != null) {
            return true;
        }
        try {
            flashInfo = TorchController.inspect(this);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException ignored) {
            flashInfo = null;
        }
        return flashInfo != null;
    }

    private void launchMainForPermission() {
        Intent launch = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_REQUEST_CAMERA_PERMISSION, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    3301,
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapse(launch);
        }
    }

    private void registerTorchCallback() {
        if (callbackRegistered || flashInfo == null) {
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
                        int level = LightPrefs.getCurrentLevel(QuickTileService.this);
                        if (level == 0) {
                            level = LightPrefs.getLastLevel(QuickTileService.this);
                        }
                        LightPrefs.setObservedTorchOn(QuickTileService.this, level);
                    } else {
                        LightPrefs.markTorchOff(QuickTileService.this);
                        AutoOffScheduler.cancel();
                    }
                    updateTile();
                }

                @Override
                public void onTorchModeUnavailable(String cameraId) {
                    if (!isSelectedCamera(cameraId)) {
                        return;
                    }
                    LightPrefs.markTorchOff(QuickTileService.this);
                    AutoOffScheduler.cancel();
                    setTileUnavailable();
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
                        LightPrefs.setObservedTorchOn(QuickTileService.this, logicalLevel);
                        updateTile();
                    }
                }
            };
        }

        try {
            cameraManager.registerTorchCallback(
                    torchCallback,
                    new Handler(Looper.getMainLooper())
            );
            callbackRegistered = true;
        } catch (IllegalArgumentException | SecurityException ignored) {
            callbackRegistered = false;
        }
    }

    private void unregisterTorchCallback() {
        if (!callbackRegistered || cameraManager == null || torchCallback == null) {
            return;
        }
        try {
            cameraManager.unregisterTorchCallback(torchCallback);
        } catch (RuntimeException ignored) {
            // TileService終了時の後処理なので無視する。
        }
        callbackRegistered = false;
    }

    private boolean isSelectedCamera(String cameraId) {
        return flashInfo != null && flashInfo.cameraId.equals(cameraId);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean on = LightPrefs.isTorchOn(this);
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.tile_label));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(on ? getString(R.string.tile_on) : getString(R.string.tile_off));
        }
        tile.updateTile();
    }


    private void setTileNeedsPermission() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.tile_label));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(R.string.tile_permission_required));
        }
        tile.updateTile();
    }

    private void setTileUnavailable() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(Tile.STATE_UNAVAILABLE);
        tile.setLabel(getString(R.string.tile_label));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(R.string.tile_unavailable));
        }
        tile.updateTile();
    }
}
