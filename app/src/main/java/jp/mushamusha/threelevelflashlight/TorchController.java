package jp.mushamusha.threelevelflashlight;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;

public final class TorchController {
    private TorchController() {}

    public static final class FlashInfo {
        public final String cameraId;
        public final int maxStrength;
        public final boolean adjustable;

        FlashInfo(String cameraId, int maxStrength) {
            this.cameraId = cameraId;
            this.maxStrength = Math.max(1, maxStrength);
            this.adjustable = this.maxStrength > 1;
        }
    }

    public static FlashInfo inspect(Context context) throws CameraAccessException {
        CameraManager manager = context.getSystemService(CameraManager.class);
        if (manager == null) {
            return null;
        }

        String fallback = null;
        String selected = null;

        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (!Boolean.TRUE.equals(hasFlash)) {
                continue;
            }
            if (fallback == null) {
                fallback = id;
            }
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                selected = id;
                break;
            }
        }

        if (selected == null) {
            selected = fallback;
        }
        if (selected == null) {
            return null;
        }

        int maxStrength = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Integer value = manager.getCameraCharacteristics(selected)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
            maxStrength = value == null ? 1 : Math.max(1, value);
        }
        return new FlashInfo(selected, maxStrength);
    }

    public static void turnOn(Context context, FlashInfo info, int logicalLevel)
            throws CameraAccessException {
        if (info == null) {
            throw new IllegalArgumentException("No flash available");
        }
        CameraManager manager = context.getSystemService(CameraManager.class);
        if (manager == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && info.adjustable) {
            manager.turnOnTorchWithStrengthLevel(
                    info.cameraId,
                    strengthFor(info.maxStrength, logicalLevel)
            );
        } else {
            manager.setTorchMode(info.cameraId, true);
        }
    }

    public static void turnOff(Context context, FlashInfo info) throws CameraAccessException {
        if (info == null) {
            return;
        }
        CameraManager manager = context.getSystemService(CameraManager.class);
        if (manager == null) {
            return;
        }
        manager.setTorchMode(info.cameraId, false);
    }

    public static int logicalLevelForStrength(int maxStrength, int actualStrength) {
        if (maxStrength <= 1) {
            return 0;
        }
        int medium = strengthFor(maxStrength, 2);
        if (actualStrength <= 1) {
            return 1;
        }
        if (actualStrength >= maxStrength) {
            return 3;
        }
        return actualStrength <= medium ? 2 : 3;
    }

    private static int strengthFor(int maxStrength, int logicalLevel) {
        if (logicalLevel <= 1) {
            return 1;
        }
        if (logicalLevel >= 3) {
            return maxStrength;
        }
        return Math.max(1, Math.min(maxStrength, (maxStrength + 1) / 2));
    }
}
