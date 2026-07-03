package com.cz.bluetooth;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class CallingHook {
    private static final String CALLING_WINDOW_CLASS = "K0.e";
    private static final String CALLING_WINDOW_METHOD = "p";

    // 缓存 View 的修改后大小值，避免重复缩小
    private final Map<String, ViewSizeInfo> viewSizeCache = new HashMap<>();

    // 存储 View 大小信息的内部类
    private static class ViewSizeInfo {
        int width;
        int height;
        int paddingLeft;
        int paddingTop;
        int paddingRight;
        int paddingBottom;
        int marginLeft;
        int marginTop;
        int marginRight;
        int marginBottom;
        float textSize;
        float lineSpacing;
        float letterSpacing;

        ViewSizeInfo(int width, int height, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom,
                     int marginLeft, int marginTop, int marginRight, int marginBottom,
                     float textSize, float lineSpacing, float letterSpacing) {
            this.width = width;
            this.height = height;
            this.paddingLeft = paddingLeft;
            this.paddingTop = paddingTop;
            this.paddingRight = paddingRight;
            this.paddingBottom = paddingBottom;
            this.marginLeft = marginLeft;
            this.marginTop = marginTop;
            this.marginRight = marginRight;
            this.marginBottom = marginBottom;
            this.textSize = textSize;
            this.lineSpacing = lineSpacing;
            this.letterSpacing = letterSpacing;
        }
    }

    /**
     * Hook 通话悬浮窗大小
     */
    public void hookCallingWindowSize(String tag, ClassLoader cl) {
        XposedBridge.log("[BluetoothHook][" + tag + "] start hook calling window size in " + cl);
        try {
            // 找到 K0.e 类 (通话中悬浮框)
            XposedBridge.log("[BluetoothHook][" + tag + "] trying to find " + CALLING_WINDOW_CLASS + " class");
            Class<?> callingViewClass = XposedHelpers.findClass(CALLING_WINDOW_CLASS, cl);
            XposedBridge.log("[BluetoothHook][" + tag + "] found " + CALLING_WINDOW_CLASS + " class: " + callingViewClass);

            // Hook p() 方法，调整窗口大小
            XposedBridge.log("[BluetoothHook][" + tag + "] hooking " + CALLING_WINDOW_METHOD + "() method");
            XposedHelpers.findAndHookMethod(callingViewClass, CALLING_WINDOW_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[BluetoothHook][" + tag + "] after " + CALLING_WINDOW_METHOD + "() method");
                    super.afterHookedMethod(param);

                    try {
                        // 获取 K0.e 实例
                        Object callingViewInstance = param.thisObject;
                        XposedBridge.log("[BluetoothHook][" + tag + "] got callingViewInstance: " + callingViewInstance);

                        // 在一个循环中获取所有需要的属性
                        WindowManager.LayoutParams params = null;
                        Object sharedPreferences = null;
                        Object context = null;
                        Object view = null;
                        Class<?> cls = callingViewInstance.getClass();
                        java.lang.reflect.Field[] fields = cls.getDeclaredFields();

                        // 一次遍历，获取所有需要的字段
                        for (java.lang.reflect.Field field : fields) {
                            field.setAccessible(true);
                            try {
                                Object value = field.get(callingViewInstance);

                                // 查找 WindowManager.LayoutParams
                                if (value instanceof WindowManager.LayoutParams) {
                                    params = (WindowManager.LayoutParams) value;
                                    XposedBridge.log("[BluetoothHook][" + tag + "] found LayoutParams field: " + field.getName());
                                }

                                // 查找 SharedPreferences
                                else if (value != null && value.getClass().getName().contains("SharedPreferences")) {
                                    sharedPreferences = value;
                                    XposedBridge.log("[BluetoothHook][" + tag + "] found SharedPreferences field: " + field.getName());
                                }

                                // 查找 Context
                                else if (value != null && value.getClass().getName().contains("Context")) {
                                    context = value;
                                    XposedBridge.log("[BluetoothHook][" + tag + "] found Context field: " + field.getName());
                                }

                                // 查找根视图，只选择非ImageView的View
                                else if (value instanceof View && !(value instanceof ImageView)) {
                                    view = value;
                                    XposedBridge.log("[BluetoothHook][" + tag + "] found root view field: " + field.getName() + ", type: " + value.getClass().getSimpleName());
                                }
                            } catch (Exception e) {
                                // 忽略异常，继续查找
                            }
                        }

                        if (params != null) {
                            XposedBridge.log("[BluetoothHook][" + tag + "] original size: " + params.width + "x" + params.height);

                            // 修改窗口大小为原始大小的一半
                            params.width = params.width > 0 ? params.width / 2 : params.width;
                            params.height = params.height > 0 ? params.height / 2 : params.height;
                            XposedBridge.log("[BluetoothHook][" + tag + "] new size: " + params.width + "x" + params.height);

                            // 读取缓存的位置
                            int savedX = -1;
                            if (sharedPreferences != null) {
                                try {
                                    savedX = (Integer) XposedHelpers.callMethod(sharedPreferences, "getInt", "CallingViewX", -1);
                                    XposedBridge.log("[BluetoothHook][" + tag + "] savedX: " + savedX);
                                } catch (Exception e) {
                                    XposedBridge.log("[BluetoothHook][" + tag + "] failed to get saved position: " + e.getMessage());
                                }
                            }

                            // 如果没有缓存位置（使用默认居中），重新计算居中位置
                            if (savedX == -1 && context != null) {
                                try {
                                    Object windowManager = XposedHelpers.callMethod(context, "getSystemService", "window");
                                    if (windowManager != null) {
                                        Object display = XposedHelpers.callMethod(windowManager, "getDefaultDisplay");
                                        if (display != null) {
                                            int screenWidth = (Integer) XposedHelpers.callMethod(display, "getWidth");
                                            XposedBridge.log("[BluetoothHook][" + tag + "] found screenWidth from Context: " + screenWidth);
                                            params.x = (screenWidth - params.width) / 2;
                                            XposedBridge.log("[BluetoothHook][" + tag + "] calculated new x position: " + params.x);
                                        }
                                    }
                                } catch (Exception e) {
                                    XposedBridge.log("[BluetoothHook][" + tag + "] failed to get screenWidth: " + e.getMessage());
                                }
                            }

                            if (view != null) {
                                XposedHelpers.callMethod(view, "setLayoutParams", params);
                                XposedBridge.log("[BluetoothHook][" + tag + "] successfully set layout params");
                                XposedBridge.log("[BluetoothHook][" + tag + "] final size: " + params.width + "x" + params.height + ", position: " + params.x + "," + params.y);

                                // 递归处理所有子view，将宽高、margin和padding改为原值的1/2
                                if (view instanceof ViewGroup) {
                                    XposedBridge.log("[BluetoothHook][" + tag + "] starting to process child views");
                                    processChildViews((ViewGroup) view);
                                    XposedBridge.log("[BluetoothHook][" + tag + "] finished processing child views");
                                }
                            }

                            // 修复触摸区域
                            fixTouchArea(view);
                        }
                    } catch (Throwable t) {
                        // 兜底，防止崩溃
                        XposedBridge.log("[BluetoothHook][" + tag + "] failed to adjust calling window size");
                        XposedBridge.log(t);
                    }
                }
            });

            XposedBridge.log(
                    "[BluetoothHook][" + tag + "] hook calling window size success in " + cl
            );

        } catch (Throwable t) {
            // 类不存在 / ClassLoader 不对 / 尚未加载
            XposedBridge.log(
                    "[BluetoothHook][" + tag + "] hook calling window size failed"
            );
            XposedBridge.log(t);
        }
    }

    /**
     * 递归处理所有子view，将宽高、margin和padding改为原值的1/2
     */
    private void processChildViews(ViewGroup viewGroup) {
        try {
            int childCount = (Integer) XposedHelpers.callMethod(viewGroup, "getChildCount");
            XposedBridge.log("[BluetoothHook] processing " + childCount + " child views");

            for (int i = 0; i < childCount; i++) {
                Object childView = XposedHelpers.callMethod(viewGroup, "getChildAt", i);
                if (childView != null) {
                    // 获取View的名称作为缓存key
                    String viewKey = getViewKey((View) childView, i);

                    // 检查缓存
                    if (!viewSizeCache.containsKey(viewKey)) {
                        // 处理子view的宽高和margin
                        processViewLayoutParams(childView, viewKey);

                        // 处理子view的padding
                        processViewPadding(childView, viewKey);

                        // 处理子view的其他属性
                        processViewOtherProperties(childView, viewKey);

                        // 缓存view的大小信息
                        try {
                            Object layoutParams = XposedHelpers.callMethod(childView, "getLayoutParams");
                            if (layoutParams != null) {
                                int width = 0, height = 0;
                                int marginLeft = 0, marginTop = 0, marginRight = 0, marginBottom = 0;
                                int paddingLeft = 0, paddingTop = 0, paddingRight = 0, paddingBottom = 0;
                                float textSize = 0, lineSpacing = 0, letterSpacing = 0;

                                // 获取宽高
                                if (XposedHelpers.findFieldIfExists(layoutParams.getClass(), "width") != null &&
                                        XposedHelpers.findFieldIfExists(layoutParams.getClass(), "height") != null) {
                                    width = XposedHelpers.getIntField(layoutParams, "width");
                                    height = XposedHelpers.getIntField(layoutParams, "height");
                                }

                                // 获取margin
                                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                                    marginLeft = marginLayoutParams.leftMargin;
                                    marginTop = marginLayoutParams.topMargin;
                                    marginRight = marginLayoutParams.rightMargin;
                                    marginBottom = marginLayoutParams.bottomMargin;
                                }

                                // 获取padding
                                paddingLeft = (Integer) XposedHelpers.callMethod(childView, "getPaddingLeft");
                                paddingTop = (Integer) XposedHelpers.callMethod(childView, "getPaddingTop");
                                paddingRight = (Integer) XposedHelpers.callMethod(childView, "getPaddingRight");
                                paddingBottom = (Integer) XposedHelpers.callMethod(childView, "getPaddingBottom");

                                // 获取TextView属性
                                if (childView instanceof TextView) {
                                    TextView textView = (TextView) childView;
                                    textSize = textView.getTextSize();
                                    lineSpacing = textView.getLineSpacingExtra();
                                    letterSpacing = textView.getLetterSpacing();
                                }

                                // 创建缓存信息
                                ViewSizeInfo sizeInfo = new ViewSizeInfo(width, height, paddingLeft, paddingTop, paddingRight, paddingBottom,
                                        marginLeft, marginTop, marginRight, marginBottom,
                                        textSize, lineSpacing, letterSpacing);
                                viewSizeCache.put(viewKey, sizeInfo);
                                XposedBridge.log("[BluetoothHook] cached view size info: " + viewKey + ", size: " + width + "x" + height);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("[BluetoothHook] failed to cache view size info: " + t.getMessage());
                        }
                    } else {
                        // 即使已经缓存，也调用这三个方法以确保缓存的值被正确应用
                        processViewLayoutParams(childView, viewKey);
                        processViewPadding(childView, viewKey);
                        processViewOtherProperties(childView, viewKey);
                        XposedBridge.log("[BluetoothHook] skipping already cached view: " + viewKey);
                    }

                    // 如果子view也是ViewGroup，递归处理
                    if (childView instanceof ViewGroup) {
                        processChildViews((ViewGroup) childView);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to process child views: " + t.getMessage());
            XposedBridge.log(t);
        }
    }

    /**
     * 获取View的名称作为缓存key
     * @param view View对象
     * @param index View在父容器中的索引
     * @return View的名称
     */
    private String getViewKey(View view, int index) {
        try {
            // 尝试获取View的id名称
            if (view.getId() != View.NO_ID) {
                try {
                    String resourceName = view.getResources().getResourceName(view.getId());
                    return resourceName;
                } catch (Exception e) {
                    // 如果无法获取资源名称，使用类名和索引
                    return view.getClass().getSimpleName() + "_" + index;
                }
            } else {
                // 如果没有id，使用类名和索引
                return view.getClass().getSimpleName() + "_" + index;
            }
        } catch (Exception e) {
            // 如果出现异常，使用默认名称
            return "view_" + index;
        }
    }

    /**
     * 处理view的布局参数，将宽高和margin改为原值的1/2
     */
    private void processViewLayoutParams(Object view, String viewKey) {
        try {
            Object layoutParams = XposedHelpers.callMethod(view, "getLayoutParams");
            if (layoutParams != null) {
                int width = 0, height = 0;
                int marginLeft = 0, marginTop = 0, marginRight = 0, marginBottom = 0;

                // 检查缓存
                ViewSizeInfo cachedInfo = viewSizeCache.get(viewKey);
                if (cachedInfo != null) {
                    // 使用缓存的值
                    width = cachedInfo.width;
                    height = cachedInfo.height;
                    marginLeft = cachedInfo.marginLeft;
                    marginTop = cachedInfo.marginTop;
                    marginRight = cachedInfo.marginRight;
                    marginBottom = cachedInfo.marginBottom;
                    XposedBridge.log("[BluetoothHook] using cached size for view: " + viewKey + ", size: " + width + "x" + height);
                } else {
                    // 计算新的大小
                    if (XposedHelpers.findFieldIfExists(layoutParams.getClass(), "width") != null &&
                            XposedHelpers.findFieldIfExists(layoutParams.getClass(), "height") != null) {

                        // 获取原始宽高
                        int originalWidth = XposedHelpers.getIntField(layoutParams, "width");
                        int originalHeight = XposedHelpers.getIntField(layoutParams, "height");

                        // 改为原值的1/2，只对大于0的值进行处理
                        width = originalWidth > 0 ? originalWidth / 2 : originalWidth;
                        height = originalHeight > 0 ? originalHeight / 2 : originalHeight;

                        XposedBridge.log("[BluetoothHook] changed view size from " + originalWidth + "x" + originalHeight + " to " + width + "x" + height);
                    }

                    // 检查是否有margin属性（LayoutParams的子类）
                    if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;

                        // 获取原始margin
                        int originalMarginLeft = marginLayoutParams.leftMargin;
                        int originalMarginTop = marginLayoutParams.topMargin;
                        int originalMarginRight = marginLayoutParams.rightMargin;
                        int originalMarginBottom = marginLayoutParams.bottomMargin;

                        // 改为原值的1/2，只对大于0的值进行处理
                        marginLeft = originalMarginLeft > 0 ? originalMarginLeft / 2 : originalMarginLeft;
                        marginTop = originalMarginTop > 0 ? originalMarginTop / 2 : originalMarginTop;
                        marginRight = originalMarginRight > 0 ? originalMarginRight / 2 : originalMarginRight;
                        marginBottom = originalMarginBottom > 0 ? originalMarginBottom / 2 : originalMarginBottom;

                        XposedBridge.log("[BluetoothHook] changed view margins from " + originalMarginLeft + "," + originalMarginTop + "," + originalMarginRight + "," + originalMarginBottom + " to " + marginLeft + "," + marginTop + "," + marginRight + "," + marginBottom);
                    }
                }

                // 应用宽高
                if (XposedHelpers.findFieldIfExists(layoutParams.getClass(), "width") != null &&
                        XposedHelpers.findFieldIfExists(layoutParams.getClass(), "height") != null) {
                    XposedHelpers.setIntField(layoutParams, "width", width);
                    XposedHelpers.setIntField(layoutParams, "height", height);
                }

                // 应用margin
                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                    marginLayoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom);
                }

                // 应用修改后的布局参数
                XposedHelpers.callMethod(view, "setLayoutParams", layoutParams);
            }
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to process view layout params: " + t.getMessage());
        }
    }

    /**
     * 处理view的padding，将padding改为原值的1/2
     */
    private void processViewPadding(Object view, String viewKey) {
        try {
            int paddingLeft = 0, paddingTop = 0, paddingRight = 0, paddingBottom = 0;

            // 检查缓存
            ViewSizeInfo cachedInfo = viewSizeCache.get(viewKey);
            if (cachedInfo != null) {
                // 使用缓存的值
                paddingLeft = cachedInfo.paddingLeft;
                paddingTop = cachedInfo.paddingTop;
                paddingRight = cachedInfo.paddingRight;
                paddingBottom = cachedInfo.paddingBottom;
                XposedBridge.log("[BluetoothHook] using cached padding for view: " + viewKey);
            } else {
                // 计算新的padding
                // 获取原始padding
                int originalPaddingLeft = (Integer) XposedHelpers.callMethod(view, "getPaddingLeft");
                int originalPaddingTop = (Integer) XposedHelpers.callMethod(view, "getPaddingTop");
                int originalPaddingRight = (Integer) XposedHelpers.callMethod(view, "getPaddingRight");
                int originalPaddingBottom = (Integer) XposedHelpers.callMethod(view, "getPaddingBottom");

                // 改为原值的1/2，只对大于0的值进行处理
                paddingLeft = originalPaddingLeft > 0 ? originalPaddingLeft / 2 : originalPaddingLeft;
                paddingTop = originalPaddingTop > 0 ? originalPaddingTop / 2 : originalPaddingTop;
                paddingRight = originalPaddingRight > 0 ? originalPaddingRight / 2 : originalPaddingRight;
                paddingBottom = originalPaddingBottom > 0 ? originalPaddingBottom / 2 : originalPaddingBottom;

                XposedBridge.log("[BluetoothHook] changed view padding from " + originalPaddingLeft + "," + originalPaddingTop + "," + originalPaddingRight + "," + originalPaddingBottom + " to " + paddingLeft + "," + paddingTop + "," + paddingRight + "," + paddingBottom);
            }

            // 应用新的padding
            XposedHelpers.callMethod(view, "setPadding", paddingLeft, paddingTop, paddingRight, paddingBottom);
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to process view padding: " + t.getMessage());
        }
    }

    /**
     * 处理view的其他属性，确保整体缩小一致
     */
    private void processViewOtherProperties(Object view, String viewKey) {
        try {
            // 处理文本相关属性
            if (view instanceof TextView) {
                processTextViewProperties((TextView) view, viewKey);
            }

            // 处理图片相关属性
            if (view instanceof ImageView) {
                processImageViewProperties((ImageView) view);
            }
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to process other properties: " + t.getMessage());
        }
    }

    /**
     * 处理TextView的属性
     */
    private void processTextViewProperties(TextView textView, String viewKey) {
        try {
            float textSize = 0, lineSpacing = 0, letterSpacing = 0;

            // 检查缓存
            ViewSizeInfo cachedInfo = viewSizeCache.get(viewKey);
            if (cachedInfo != null) {
                // 使用缓存的值
                textSize = cachedInfo.textSize;
                lineSpacing = cachedInfo.lineSpacing;
                letterSpacing = cachedInfo.letterSpacing;
                XposedBridge.log("[BluetoothHook] using cached text properties for view: " + viewKey);
            } else {
                // 计算新的文本属性
                // 处理文本大小
                float originalTextSize = textView.getTextSize();
                textSize = originalTextSize > 0 ? originalTextSize / 2 : originalTextSize;

                // 处理行间距
                float originalLineSpacing = textView.getLineSpacingExtra();
                lineSpacing = originalLineSpacing > 0 ? originalLineSpacing / 2 : originalLineSpacing;

                // 处理字间距
                float originalLetterSpacing = textView.getLetterSpacing();
                letterSpacing = originalLetterSpacing > 0 ? originalLetterSpacing / 2 : originalLetterSpacing;

                XposedBridge.log("[BluetoothHook] changed text size from " + originalTextSize + " to " + textSize);
                XposedBridge.log("[BluetoothHook] changed line spacing from " + originalLineSpacing + " to " + lineSpacing);
                XposedBridge.log("[BluetoothHook] changed letter spacing from " + originalLetterSpacing + " to " + letterSpacing);
            }

            // 应用新的文本属性
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            textView.setLineSpacing(lineSpacing, textView.getLineSpacingMultiplier());
            textView.setLetterSpacing(letterSpacing);
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to process TextView properties: " + t.getMessage());
        }
    }

    /**
     * 处理ImageView的属性
     */
    private void processImageViewProperties(ImageView imageView) {
        try {
            // 处理图片缩放
            // 注意：这里保持图片原始比例，通过布局参数控制大小
            XposedBridge.log("[BluetoothHook] processed ImageView properties");
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to process ImageView properties: " + t.getMessage());
        }
    }

    /**
     * 修复触摸区域
     */
    private void fixTouchArea(Object view) {
        try {
            // 重新测量和布局View，确保触摸区域与视觉大小一致
            if (view instanceof View) {
                View v = (View) view;
                v.requestLayout();
                v.invalidate();
                XposedBridge.log("[BluetoothHook] fixed touch area for view: " + v);
            }
        } catch (Throwable t) {
            XposedBridge.log("[BluetoothHook] failed to fix touch area: " + t.getMessage());
        }
    }
}