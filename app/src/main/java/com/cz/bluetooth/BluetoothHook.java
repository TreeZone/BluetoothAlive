package com.cz.bluetooth;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class BluetoothHook {
    private static final String BLUETOOTH_ACTIVITY_CLASS = "com.tw.bt.ATBluetoothActivity";
    private static final int ibA2dpId = 0x7f08004c;
    private static final String PREFS_NAME = "BluetoothHookPrefs";
    private static final String KEY_LAST_PAUSE_TIME = "last_pause_time";
    private static final long TIME_THRESHOLD = 300000;  // 5分钟阈值
    private static final String TARGET_PKG = "com.tw.bt";

    private static boolean isForeground = false;

    public void hookBluetoothConnection(final String tag, final ClassLoader classLoader) {
        try {
            Class<?> bluetoothActivityClass = XposedHelpers.findClass(BLUETOOTH_ACTIVITY_CLASS, classLoader);
            if (bluetoothActivityClass == null) {
                XposedBridge.log("[BluetoothHook][" + tag + "] Activity class not found");
                return;
            }

            hookForegroundState(bluetoothActivityClass, tag);
            hookS1Method(bluetoothActivityClass, tag);
            hookOnResume(bluetoothActivityClass, tag);

            XposedBridge.log("[BluetoothHook][" + tag + "] Hook installed");

        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook][" + tag + "] Hook failed: " + t.getMessage());
        }
    }

    private void hookForegroundState(Class<?> activityClass, final String tag) {
        XposedHelpers.findAndHookMethod(activityClass, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                isForeground = true;
                XposedBridge.log("[BluetoothHook][" + tag + "] App is in foreground");
            }
        });

        XposedHelpers.findAndHookMethod(activityClass, "onPause", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                isForeground = false;
                // 只在离开前台时记录时间
                try {
                    Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putLong(KEY_LAST_PAUSE_TIME, System.currentTimeMillis()).apply();
                } catch (Exception e) {
                    // ignore
                }
                XposedBridge.log("[BluetoothHook][" + tag + "] App is in background");
            }
        });
    }

    private void hookS1Method(Class<?> activityClass, final String tag) {
        XposedHelpers.findAndHookMethod(activityClass, "s1", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int i2 = (int) param.args[0];

                // 蓝牙已连接 && APP在前台
                if (i2 >= 2 && isForeground) {
                    switchToA2dp(param.thisObject, tag);
                }
            }
        });
    }

    private void hookOnResume(Class<?> activityClass, final String tag) {
        XposedHelpers.findAndHookMethod(activityClass, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!isForeground) return;

                try {
                    Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

                    // 获取上次离开前台的时间（只在onPause时更新）
                    long lastPauseTime = prefs.getLong(KEY_LAST_PAUSE_TIME, 0);
                    long currentTime = System.currentTimeMillis();

                    // 判断离开时间是否超过阈值
                    boolean wasAwayLongEnough = (currentTime - lastPauseTime) > TIME_THRESHOLD;

                    if (wasAwayLongEnough) {
                        Object c0005a = XposedHelpers.getObjectField(param.thisObject, "K");
                        if (c0005a == null) return;

                        int bluetoothState = XposedHelpers.getIntField(c0005a, "f908h");
                        int currentTab = XposedHelpers.getIntField(c0005a, "t");

                        // 蓝牙已连接 && 不在媒体界面 && 离开时间超过阈值
                        if (bluetoothState >= 2 && currentTab != ibA2dpId) {
                            XposedBridge.log("[BluetoothHook][" + tag + "] Returning after long absence: switching to A2DP");
                            XposedHelpers.setIntField(c0005a, "t", ibA2dpId);
                            XposedHelpers.callMethod(param.thisObject, "w1", ibA2dpId);
                        }
                    }
                } catch (Exception e) {
                    XposedBridge.log("[BluetoothHook][" + tag + "] Resume check error: " + e.getMessage());
                }
            }
        });
    }

    private void switchToA2dp(Object activity, String tag) {
        try {
            Object c0005a = XposedHelpers.getObjectField(activity, "K");
            if (c0005a == null) return;

            int currentTab = XposedHelpers.getIntField(c0005a, "t");
            if (currentTab != ibA2dpId) {
                XposedBridge.log("[BluetoothHook][" + tag + "] Switching to A2DP");
                XposedHelpers.setIntField(c0005a, "t", ibA2dpId);
                XposedHelpers.callMethod(activity, "w1", ibA2dpId);
            }
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] switchToA2dp error: " + e.getMessage());
        }
    }

    public void hookBlueA2DP(String tag, ClassLoader classLoader) {
        try {
            Class<?> clazz = XposedHelpers.findClass(
                    "b0.d",
                    classLoader
            );

            final int[] runningFlag = {0};

            XposedHelpers.findAndHookMethod(
                    clazz,
                    "h0",
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (runningFlag[0] == 1) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] h0 already running, skipping...");
                                param.setResult(null);
                                return;
                            }
                            try {
                                runningFlag[0] = 1;
                                // 阻止原方法立即执行
                                param.setResult(null);

                                XposedBridge.log("[BluetoothHook][" + tag + "] h0 intercepted, delaying execution...");

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        // 反射调用原方法
                                        XposedHelpers.callMethod(param.thisObject, "h0");
                                    } catch (Throwable t) {
                                        XposedBridge.log("[BluetoothHook][" + tag + "] h0 delayed call failed: " + t);
                                    } finally {
                                        runningFlag[0] = 0;
                                    }
                                }, 500);
                            } catch (Exception e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] h0 error: " + e);
                            }
                        }
                    }
            );

            XposedBridge.log("[BluetoothHook][" + tag + "] Hook h0 success");
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] Hook h0 failed, e = " + e.getMessage());
        }
    }

    // Hook F0.a.l0 初始化控件，运行时移除 tv_song marquee
    public void hookA2dpFragment(String tag, ClassLoader classLoader) {
        Class<?> fragmentCls = XposedHelpers.findClass("F0.a", classLoader);
        try {
            XposedHelpers.findAndHookMethod(fragmentCls, "C0", View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View rootView = (View) param.args[0];
                    @SuppressLint("DiscouragedApi") int tvSongId = rootView.getResources().getIdentifier("tv_song", "id", TARGET_PKG);
                    TextView tvSong = rootView.findViewById(tvSongId);
                    if (tvSong == null) return;

                    // 强制改为普通末尾省略，彻底禁用marquee逻辑
                    tvSong.setEllipsize(TextUtils.TruncateAt.END);
                    // 关闭选中，停止滚动动画
                    tvSong.setSelected(false);
                    XposedBridge.log("[BluetoothHook][" + tag + "] 修复卡死：tv_song 已关闭跑马灯");
                }
            });
            XposedBridge.log("[BluetoothHook][" + tag + "] 修复卡死：tv_song 已关闭跑马灯，注册成功");
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 修复卡死：tv_song 已关闭跑马灯，注册失败，e = " + e.getMessage());
        }
    }

    // 兜底拦截 tv_song.setSelected(true)，防止别处重新开启滚动
    public void hookTextViewSetSelected(String tag, ClassLoader classLoader) {
        Class<?> textViewCls = XposedHelpers.findClass("android.widget.TextView", classLoader);
        try {
            XposedHelpers.findAndHookMethod(textViewCls, "setSelected", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    TextView textView = (TextView) param.thisObject;
                    boolean targetSelect = (boolean) param.args[0];

                    try {
                        String resName = textView.getResources().getResourceEntryName(textView.getId());
                        // 只拦截 tv_song 设置 true
                        if ("tv_song".equals(resName) && targetSelect) {
                            param.setResult(null); // 直接拦截，不执行原方法
                            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截禁止 tv_song.setSelected(true)");
                        }
                    } catch (Throwable e) {
                        // 无资源名的控件直接放行
                    }
                }
            });
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截禁止 tv_song.setSelected(true)，注册成功");
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截禁止 tv_song.setSelected(true)，注册失败，e = " + e.getMessage());
        }
    }

    public void hookMusic(String tag, ClassLoader classLoader) {
        Class<?> fragmentCls = null;
        try {
            fragmentCls = XposedHelpers.findClass("F0.a", classLoader);
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 获取 F0.a 失败，e = " + e.getMessage());
        }
        try {
            XposedHelpers.findAndHookMethod(fragmentCls, "L0", String.class, String.class, String.class, new XC_MethodHook() {
                final int[] running = {0};

                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (running[0] == 1) {
                        // 正在渲染，丢弃本次，不延迟
                        param.setResult(null);
                        return;
                    }
                    running[0] = 1;
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    running[0] = 0;
                }

            });
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截歌曲标题，注册成功");
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截歌曲标题，注册失败，e = " + e.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(fragmentCls, "d", int.class, int.class, new XC_MethodHook() {
                final int[] running = {0};

                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (running[0] == 1) {
                        // 正在渲染，丢弃本次，不延迟
                        param.setResult(null);
                        return;
                    }
                    running[0] = 1;
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    running[0] = 0;
                }
            });
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截进度条更新，注册成功");
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截进度条更新，注册失败，e = " + e.getMessage());
        }

        try {
            Class<?> G0aClass = XposedHelpers.findClass(
                    "G0.a",
                    classLoader
            );
            XposedHelpers.findAndHookMethod(fragmentCls, "D0", G0aClass, new XC_MethodHook() {
                final int[] running = {0};

                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (running[0] == 1) {
                        // 正在渲染，丢弃本次，不延迟
                        param.setResult(null);
                        return;
                    }
                    running[0] = 1;
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    running[0] = 0;
                }
            });
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截主题渲染，注册成功");
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 拦截主题渲染，注册失败，e = " + e.getMessage());
        }
    }

}