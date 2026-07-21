package com.cz.bluetooth;

import java.lang.reflect.Member;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class BluetoothHook {

    private ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

    public void hookWriteAsync(final String tag, ClassLoader classLoader) {
        try {
            Class<?> c0006cClass = XposedHelpers.findClass("a0.c", classLoader);

            hookWrite1(tag, c0006cClass);
            hookWrite2(tag, c0006cClass);
            hookWrite3(tag, c0006cClass);
            hookWrite4(tag, c0006cClass);
            hookWrite5(tag, c0006cClass);
            hookWrite6(tag, c0006cClass);
            hookWrite7(tag, c0006cClass);

            XposedBridge.log("[BluetoothHook][" + tag + "] C0006c.write() 全异步化Hook注册成功");
        } catch (Throwable e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] C0006c.write() 全异步化Hook注册失败: " + e.getMessage());
            XposedBridge.log(e);
        }
    }

    private void hookWrite1(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int)异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int)异常: " + e.getMessage());
        }
    }

    private void hookWrite2(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final int arg2 = (int) param.args[1];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1, arg2});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int,int)异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int,int)异常: " + e.getMessage());
        }
    }

    private void hookWrite3(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final int arg2 = (int) param.args[1];
                    final int arg3 = (int) param.args[2];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1, arg2, arg3});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int,int,int)异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int,int,int)异常: " + e.getMessage());
        }
    }

    private void hookWrite4(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, int.class, int.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final int arg2 = (int) param.args[1];
                    final int arg3 = (int) param.args[2];
                    final String arg4 = (String) param.args[3];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1, arg2, arg3, arg4});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int,int,int,String)异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int,int,int,String)异常: " + e.getMessage());
        }
    }

    private void hookWrite5(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, int.class, int.class, Object.class, Object.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final int arg2 = (int) param.args[1];
                    final int arg3 = (int) param.args[2];
                    final Object arg4 = param.args[3];
                    final Object arg5 = param.args[4];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1, arg2, arg3, arg4, arg5});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int,int,int,Object,Object)异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int,int,int,Object,Object)异常: " + e.getMessage());
        }
    }

    private void hookWrite6(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, int.class, int.class, String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final int arg2 = (int) param.args[1];
                    final int arg3 = (int) param.args[2];
                    final String arg4 = (String) param.args[3];
                    final String arg5 = (String) param.args[4];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1, arg2, arg3, arg4, arg5});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int,int,int,String,String)异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int,int,int,String,String)异常: " + e.getMessage());
        }
    }

    private void hookWrite7(final String tag, Class<?> clazz) {
        try {
            XposedHelpers.findAndHookMethod(clazz, "write", int.class, int.class, int.class, byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final Object thisObj = param.thisObject;
                    final int arg1 = (int) param.args[0];
                    final int arg2 = (int) param.args[1];
                    final int arg3 = (int) param.args[2];
                    final byte[] arg4 = (byte[]) param.args[3];
                    final Member method = param.method;
                    param.setResult(null);
                    writeExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                XposedBridge.invokeOriginalMethod(method, thisObj, new Object[]{arg1, arg2, arg3, arg4});
                            } catch (Throwable e) {
                                XposedBridge.log("[BluetoothHook][" + tag + "] 异步write(int,int,int,byte[])异常: " + e.getMessage());
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[BluetoothHook][" + tag + "] 注册write(int,int,int,byte[])异常: " + e.getMessage());
        }
    }

}