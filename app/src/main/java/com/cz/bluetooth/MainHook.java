package com.cz.bluetooth;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;

public class MainHook implements IXposedHookLoadPackage {
    private static final String CALL_WINDOW_PKG = "com.tw.bt";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        // ===== 统一日志：先看清自己在什么环境 =====
        XposedBridge.log(
                "[BlueToothHook] load pkg=" + lpparam.packageName +
                        " process=" + lpparam.processName +
                        " cl=" + lpparam.classLoader
        );

        if (CALL_WINDOW_PKG.equals(lpparam.packageName)) {
            // 处理通话中悬浮窗
            CallingHook callingHook = new CallingHook();
            callingHook.hookCallingWindowSize(
                    "CALLING_WINDOW",
                    lpparam.classLoader
            );
            // 处理来电悬浮窗
            IncommingHook incommingHook = new IncommingHook();
            incommingHook.hookIncomingWindowSize(
                    "INCOMING_WINDOW",
                    lpparam.classLoader
            );
            // 处理悬浮窗位置（解决拖动到顶部的问题）
            WindowPositionHook windowPositionHook = new WindowPositionHook();
            windowPositionHook.hookWindowPositions(
                    "WINDOW_POSITION", lpparam.classLoader
            );
            // 处理蓝牙默认打开媒体界面
            BluetoothHook bluetoothHook = new BluetoothHook();
            bluetoothHook.hookBluetoothConnection(
                    "BLUETOOTH_WINDOW",
                    lpparam.classLoader
            );

            bluetoothHook.hookBlueA2DP("BLUETOOTH_A2DP", lpparam.classLoader);
            bluetoothHook.hookA2dpFragment("BLUETOOTH_A2DP_FRAGMENT", lpparam.classLoader);
            bluetoothHook.hookTextViewSetSelected("BLUETOOTH_A2DP_FRAGMENT_TEXT_VIEW_SELECTED", lpparam.classLoader);
        }
    }
}