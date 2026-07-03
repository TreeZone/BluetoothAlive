package com.cz.bluetooth;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class WindowPositionHook {
    // Calling 悬浮框
    private static final String CALLING_WINDOW_CLASS = "K0.e";
    private static final String CALLING_WINDOW_METHOD = "c";

    // Incoming 悬浮框
    private static final String INCOMING_WINDOW_CLASS = "K0.h";
    private static final String INCOMING_WINDOW_METHOD = "i";

    /**
     * Hook 所有悬浮框位置计算
     */
    public void hookWindowPositions(String tag, ClassLoader cl) {
        hookCallingWindowPosition(tag, cl);
        hookIncomingWindowPosition(tag, cl);
    }

    /**
     * Hook Calling 悬浮框位置计算
     */
    public void hookCallingWindowPosition(String tag, ClassLoader cl) {
        XposedBridge.log("[WindowPositionHook][" + tag + "] start hook calling window position in " + cl);
        try {
            // 找到 K0.e 类
            Class<?> callingViewClass = XposedHelpers.findClass(CALLING_WINDOW_CLASS, cl);
            XposedBridge.log("[WindowPositionHook][" + tag + "] found " + CALLING_WINDOW_CLASS + " class: " + callingViewClass);

            // Hook c() 方法，处理坐标转换
            XposedBridge.log("[WindowPositionHook][" + tag + "] hooking " + CALLING_WINDOW_METHOD + "() method");
            XposedHelpers.findAndHookMethod(callingViewClass, CALLING_WINDOW_METHOD, callingViewClass, float.class, float.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log("[WindowPositionHook][" + tag + "] before " + CALLING_WINDOW_METHOD + "() method");

                    try {
                        // 获取实例和参数
                        Object callingViewInstance = param.args[0];
                        float rawX = (float) param.args[1];
                        float rawY = (float) param.args[2];

                        XposedBridge.log("[WindowPositionHook][" + tag + "] original coordinates: x=" + rawX + ", y=" + rawY);

                        // 获取 Context
                        Context context = getContextFromInstance(callingViewInstance);
                        if (context != null) {
                            // 计算状态栏高度
                            int statusBarHeight = getStatusBarHeight(context);
                            XposedBridge.log("[WindowPositionHook][" + tag + "] statusBarHeight: " + statusBarHeight);

                            // 转换坐标：减去状态栏高度
                            float adjustedY = rawY - statusBarHeight;
                            XposedBridge.log("[WindowPositionHook][" + tag + "] adjusted coordinates: x=" + rawX + ", y=" + adjustedY);

                            // 更新参数
                            param.args[2] = adjustedY;
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("[WindowPositionHook][" + tag + "] failed to adjust calling window position");
                        XposedBridge.log(t);
                    }
                }
            });

            XposedBridge.log(
                    "[WindowPositionHook][" + tag + "] hook calling window position success in " + cl
            );

        } catch (Throwable t) {
            XposedBridge.log(
                    "[WindowPositionHook][" + tag + "] hook calling window position failed"
            );
            XposedBridge.log(t);
        }
    }

    /**
     * Hook Incoming 悬浮框位置计算
     */
    public void hookIncomingWindowPosition(String tag, ClassLoader cl) {
        XposedBridge.log("[WindowPositionHook][" + tag + "] start hook incoming window position in " + cl);
        try {
            // 找到 K0.h 类
            Class<?> incomingViewClass = XposedHelpers.findClass(INCOMING_WINDOW_CLASS, cl);
            XposedBridge.log("[WindowPositionHook][" + tag + "] found " + INCOMING_WINDOW_CLASS + " class: " + incomingViewClass);

            // Hook i() 方法，处理坐标转换
            XposedBridge.log("[WindowPositionHook][" + tag + "] hooking " + INCOMING_WINDOW_METHOD + "() method");
            XposedHelpers.findAndHookMethod(incomingViewClass, INCOMING_WINDOW_METHOD, incomingViewClass, float.class, float.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    XposedBridge.log("[WindowPositionHook][" + tag + "] before " + INCOMING_WINDOW_METHOD + "() method");

                    try {
                        // 获取实例和参数
                        Object incomingViewInstance = param.args[0];
                        float rawX = (float) param.args[1];
                        float rawY = (float) param.args[2];

                        XposedBridge.log("[WindowPositionHook][" + tag + "] original coordinates: x=" + rawX + ", y=" + rawY);

                        // 获取 Context
                        Context context = getContextFromInstance(incomingViewInstance);
                        if (context != null) {
                            // 计算状态栏高度
                            int statusBarHeight = getStatusBarHeight(context);
                            XposedBridge.log("[WindowPositionHook][" + tag + "] statusBarHeight: " + statusBarHeight);

                            // 转换坐标：减去状态栏高度
                            float adjustedY = rawY - statusBarHeight;
                            XposedBridge.log("[WindowPositionHook][" + tag + "] adjusted coordinates: x=" + rawX + ", y=" + adjustedY);

                            // 更新参数
                            param.args[2] = adjustedY;
                        }
                    } catch (Throwable t) {
                        XposedBridge.log("[WindowPositionHook][" + tag + "] failed to adjust incoming window position");
                        XposedBridge.log(t);
                    }
                }
            });

            XposedBridge.log(
                    "[WindowPositionHook][" + tag + "] hook incoming window position success in " + cl
            );

        } catch (Throwable t) {
            XposedBridge.log(
                    "[WindowPositionHook][" + tag + "] hook incoming window position failed"
            );
            XposedBridge.log(t);
        }
    }

    /**
     * 从实例中获取 Context
     */
    private Context getContextFromInstance(Object instance) {
        try {
            Class<?> cls = instance.getClass();
            java.lang.reflect.Field[] fields = cls.getDeclaredFields();

            // 查找 Context 字段
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(instance);
                    if (value instanceof Context) {
                        return (Context) value;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续查找
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("[WindowPositionHook] failed to get context from instance: " + t.getMessage());
        }
        return null;
    }

    /**
     * 获取状态栏高度
     */
    private int getStatusBarHeight(Context context) {
        int result = 0;
        try {
            int resourceId = context.getResources()
                    .getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Throwable t) {
            XposedBridge.log("getStatusBarHeight error: " + t);
        }
        return result;
    }
}
