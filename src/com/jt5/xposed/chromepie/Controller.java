package com.jt5.xposed.chromepie;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class Controller {

    private static final String TAG = "ChromePie:Controller: ";
    private final ClassLoader mClassLoader;
    private final Activity mActivity;
    private Unhook mFullscreenWindowFocusHook;
    private int mBrandColor;
    private final Boolean mIsDocumentMode;

    private final Class<?> mTabModelUtilsClass;
    private final Class<?> mLoadUrlParamsClass;
    private final Class<?> mUrlConstantsClass;
    private final Class<?> mDeviceUtilsClass;
    private final Class<?> mFeatureUtilsClass;
    private final Class<?> mDomDistillerUrlUtilsClass;
    private final Class<?> mBrandColorUtilsClass;
    private final Class<?> mServiceBridgeClass;
    private final Class<?> mChromeApplicationClass;

    private static final String[] CLASS_TAB_MODEL_UTILS = {
        "org.chromium.chrome.browser.tabmodel.TabModelUtils",
        "com.google.android.apps.chrome.tabmodel.TabModelUtils"
    };
    private static final String[] CLASS_LOAD_URL_PARAMS = {
        "org.chromium.content_public.browser.LoadUrlParams",
        "org.chromium.content.browser.LoadUrlParams"
    };
    private static final String[] CLASS_URL_CONSTANTS = {
        "org.chromium.chrome.browser.UrlConstants",
        "com.google.android.apps.chrome.UrlConstants"
    };
    private static final String[] CLASS_DEVICE_UTILS = {
        "org.chromium.ui.base.DeviceFormFactor",
        "org.chromium.content.browser.DeviceUtils"
    };
    private static final String[] CLASS_FEATURE_UTILS = {
        "org.chromium.chrome.browser.util.FeatureUtilities",
        "com.google.android.apps.chrome.utilities.FeatureUtilities"
    };
    private static final String[] CLASS_DOM_DISTILLER_UTILS = {
        "org.chromium.components.dom_distiller.core.DomDistillerUrlUtils"
    };
    private static final String[] CLASS_BRAND_COLOR_UTILS = {
        "org.chromium.chrome.browser.document.BrandColorUtils",
        "com.google.android.apps.chrome.utilities.DocumentUtilities"
    };
    private static final String[] CLASS_SERVICE_BRIDGE = {
        "org.chromium.chrome.browser.preferences.PrefServiceBridge",
        "com.google.android.apps.chrome.preferences.ChromeNativePreferences"
    };
    private static final String[] CLASS_CHROME_APPLICATION = {
        "org.chromium.chrome.browser.ChromeApplication",
        "org.chromium.chrome.browser.ChromeMobileApplication",
        "com.google.android.apps.chrome.ChromeMobileApplication"
    };

    Controller(Activity chromeActivity, ClassLoader classLoader) {
        mClassLoader = classLoader;
        mActivity = chromeActivity;

        mTabModelUtilsClass = getClass(CLASS_TAB_MODEL_UTILS);
        mLoadUrlParamsClass = getClass(CLASS_LOAD_URL_PARAMS);
        mUrlConstantsClass = getClass(CLASS_URL_CONSTANTS);
        mDeviceUtilsClass = getClass(CLASS_DEVICE_UTILS);
        mFeatureUtilsClass = getClass(CLASS_FEATURE_UTILS);
        mDomDistillerUrlUtilsClass = getClass(CLASS_DOM_DISTILLER_UTILS);
        mBrandColorUtilsClass = getClass(CLASS_BRAND_COLOR_UTILS);
        mServiceBridgeClass = getClass(CLASS_SERVICE_BRIDGE);
        mChromeApplicationClass = getClass(CLASS_CHROME_APPLICATION);

        mIsDocumentMode = isDocumentMode();
    }

    private Class<?> getClass(String[] classes) {
        for (String clazz : classes) {
            try {
                return XposedHelpers.findClass(clazz, mClassLoader);
            } catch (ClassNotFoundError cnfe) {

            }
        }
        XposedBridge.log(TAG + "ClassNotFoundError: " + classes[0]);
        return null;
    }

    Activity getChromeActivity() {
        return mActivity;
    }

    int getResIdentifier(String id) {
        return getResIdentifier(id, "id");
    }

    private int getResIdentifier(String id, String type) {
        return mActivity.getResources().getIdentifier(id, type, ChromePie.CHROME_PACKAGE);
    }

    Boolean itemSelected(int id) {
        if (id != 0) {
            try {
                if (ChromePie.sMenuActionMethod.getParameterTypes().length == 1) {
                    return (Boolean) ChromePie.sMenuActionMethod.invoke(mActivity, id);
                } else {
                    return (Boolean) ChromePie.sMenuActionMethod.invoke(mActivity, id, false);
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + t);
            }
        }
        return false;
    }

    Object getCurrentTab() {
        try {
            return callMethod(mActivity, "getActivityTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(getTabModel(), "getCurrentTab");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(mActivity, "getCurrentTab");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return new Object();
        }
    }

    private Object getDocumentModel(boolean incognito) {
        if (mChromeApplicationClass == null) return new Object();
        try {
            return callMethod(XposedHelpers.callStaticMethod(mChromeApplicationClass, "getDocumentTabModelSelector"), "getModel", incognito);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return new Object();
    }

    Object getTabModel() {
        if (isDocumentMode()) {
            try {
                return XposedHelpers.getObjectField(mActivity, "mTabModel");
            } catch (NoSuchFieldError nsfe) {

            }
            try {
                return XposedHelpers.getObjectField(mActivity, "mTabList");
            } catch (NoSuchFieldError nsfe) {
                XposedBridge.log(TAG + nsfe);
            }
        } else {
            try {
                return callMethod(mActivity, "getCurrentTabModel");
            } catch (NoSuchMethodError nsme) {
                XposedBridge.log(TAG + nsme);
            }
        }
        return new Object();
    }

    Integer getTabIndex(Object tab) {
        try {
            return (Integer) callMethod(getTabModel(), "indexOf", tab);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return -1;
        }
    }

    Boolean tabExistsAtIndex(int i) {
        return getTabAt(getTabIndex(getCurrentTab()) + i) != null;
    }

    private Object getTabAt(int index) {
        try {
            return callMethod(getTabModel(), "getTabAt", index);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return callMethod(getTabModel(), "getTab", index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    private Object getTabById(int id) {
        if (mTabModelUtilsClass == null) return null;
        try {
            return XposedHelpers.callStaticMethod(mTabModelUtilsClass, "getTabById", getTabModel(), id);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    void showTabByIndex(int index) {
        try {
            callMethod(getTabModel(), "setIndex", index);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        if (mTabModelUtilsClass == null) return;
        try {
            XposedHelpers.callStaticMethod(mTabModelUtilsClass, "setIndex", getTabModel(), index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private void showNextTab(Object tab) {
        if (mTabModelUtilsClass == null) return;
        try {
            Object model = getDocumentModel((Boolean) callMethod(tab, "isIncognito"));
            int index = (Integer) callMethod(model, "indexOf", tab);
            XposedHelpers.callStaticMethod(mTabModelUtilsClass, "setIndex", model, index);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void loadUrl(String url) {
        Object tab = getCurrentTab();
        try {
            callMethod(tab, "loadUrl", url, null, null, 2);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object urlParams = getLoadUrlParams(url);
            if (urlParams != null) {
                callMethod(tab, "loadUrl", urlParams);
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private Object getLoadUrlParams(String url) {
        if (mLoadUrlParamsClass == null) return null;
        try {
            return XposedHelpers.newInstance(mLoadUrlParamsClass, url);
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
        return null;
    }

    private Object getVideoView() {
        try {
            Class<?> contentVideoView = XposedHelpers.findClass("org.chromium.content.browser.ContentVideoView", mClassLoader);
            return XposedHelpers.callStaticMethod(contentVideoView, "getContentVideoView");
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return null;
    }

    void closeCurrentTab() {
        Object model = getTabModel();
        Object tabToClose = getCurrentTab();
        try {
            callMethod(model, "closeTab", tabToClose, true, false, true);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            callMethod(model, "closeTab", tabToClose);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    void closeDocumentTab() {
        Object nextTab = getNextTabIfClosed();
        closeCurrentTab();
        if (nextTab != null) {
            showNextTab(nextTab);
        }
    }

    private Object getNextTabIfClosed() {
        Object tabToClose = getCurrentTab();
        try {
            int closingTabIndex = getTabIndex(tabToClose);
            Object adjacentTab = getTabAt((closingTabIndex == 0) ? 1 : closingTabIndex - 1);
            Object parentTab = getTabById((Integer) callMethod(tabToClose, "getParentId"));

            // Determine which tab to select next according to these rules:
            // * Select the parent tab if it exists.
            // * Otherwise, select an adjacent tab if one exists.
            // * Otherwise, if closing the last incognito tab, select the current normal tab.
            // * Otherwise, select nothing.

            Object nextTab = null;
            if (parentTab != null) {
                nextTab = parentTab;
            } else if (adjacentTab != null) {
                nextTab = adjacentTab;
            } else if (isIncognito()) {
                if (mTabModelUtilsClass != null) {
                    nextTab = XposedHelpers.callStaticMethod(mTabModelUtilsClass, "getCurrentTab", getDocumentModel(false));
                }
            }
            return nextTab;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    public int getTabCount() {
        try {
            return (Integer) callMethod(getTabModel(), "getCount");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return 0;
        }
    }

    private String getUrl() {
        try {
            return (String) callMethod(getCurrentTab(), "getUrl");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return "";
        }
    }

    public void requestTabFocus() {
        Object tab = getCurrentTab();
        if (tab == null) {
            return;
        }
        try {
            callMethod(tab, "requestFocus", true);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            callMethod(tab, "requestFocus");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    public Boolean isInOverview() {
        if (isTablet() || isDocumentMode()) {
            return getTabCount() == 0;
        }
        try {
            Object ovLayout = callMethod(mActivity, "getAndSetupOverviewLayout");
            if (ovLayout != null) {
                return (Boolean) callMethod(ovLayout, "overviewVisible");
            }
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object layoutMan = callMethod(mActivity, "getLayoutManager");
            if (layoutMan != null) {
                return (Boolean) callMethod(layoutMan, "overviewVisible");
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    Boolean isDesktopUserAgent() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "getUseDesktopUserAgent");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean isLoading() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "isLoading");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean tabSupportsFinding() {
        Object tab = getCurrentTab();
        try {
            return (Boolean) callMethod(tab, "supportsFinding");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Boolean isNativePage = (Boolean) callMethod(tab, "isNativePage");
            return !isNativePage && getWebContents() != null;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    Boolean bookmarkExists() {
        try {
            return (Long) callMethod(getCurrentTab(), "getBookmarkId") == -1L;
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean canGoBack() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "canGoBack");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    Boolean canGoForward() {
        try {
            return (Boolean) callMethod(getCurrentTab(), "canGoForward");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return true;
        }
    }

    public Boolean isInFullscreenVideo() {
        try {
            return (Boolean) callMethod(mActivity, "isFullscreenVideoPlaying");
        } catch (NoSuchMethodError nsme) {
            return getVideoView() != null;
        }
    }

    @SuppressLint("InlinedApi")
    void setFullscreen(boolean fullscreen) {
        final Window window = mActivity.getWindow();
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            // Immersive mode supported
            final View decorView = window.getDecorView();
            final int windowFlags = WindowManager.LayoutParams.FLAG_FULLSCREEN | (android.os.Build.VERSION.SDK_INT >= 21
                                  ? WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS : 0);
            if (fullscreen) {
                window.addFlags(windowFlags);
                final int fullscreenVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                decorView.setSystemUiVisibility(fullscreenVisibility);

                // Listener re-enables immersive mode after closing the soft keyboard
                decorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        decorView.setSystemUiVisibility(fullscreenVisibility);
                    }
                });

                if (mFullscreenWindowFocusHook == null) {
                    // Hook re-enables immersive mode when returning to Chrome after leaving
                    mFullscreenWindowFocusHook = XposedHelpers.findAndHookMethod(decorView.getClass(),
                            "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            if (isFullscreen() && (Boolean) param.args[0]) {
                                decorView.setSystemUiVisibility(fullscreenVisibility);
                            }
                        }
                    });
                }
            } else {
                window.clearFlags(windowFlags);
                decorView.setSystemUiVisibility(0);
                decorView.setOnSystemUiVisibilityChangeListener(null);
                if (mFullscreenWindowFocusHook != null) {
                    mFullscreenWindowFocusHook.unhook();
                    mFullscreenWindowFocusHook = null;
                }
            }
        } else {
            if (fullscreen) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    @SuppressLint("InlinedApi")
    Boolean isFullscreen() {
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            int visibility = mActivity.getWindow().getDecorView().getSystemUiVisibility();
            return (visibility & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0;
        } else {
            int flags = mActivity.getWindow().getAttributes().flags;
            return (flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        }
    }

    Boolean isIncognito() {
        try {
            return (Boolean) callMethod(getTabModel(), "isIncognito");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
            return false;
        }
    }

    Boolean isOnNewTabPage() {
        String url = getUrl();
        return (url.startsWith("chrome://") || url.startsWith("chrome-native://"));
    }

    String getMostVisitedUrl() {
        if (mUrlConstantsClass == null) return "chrome-native://newtab/";
        try {
            return (String) XposedHelpers.getStaticObjectField(mUrlConstantsClass, "MOST_VISITED_URL");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            return (String) XposedHelpers.getStaticObjectField(mUrlConstantsClass, "NTP_URL");
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
        }
        return "chrome-native://newtab/";
    }

    Boolean isTablet() {
        try {
            return (Boolean) callMethod(mActivity, "isTablet");
        } catch (NoSuchMethodError nsme) {

        }
        if (mDeviceUtilsClass == null) return false;
        try {
            return (Boolean) XposedHelpers.callStaticMethod(mDeviceUtilsClass, "isTablet", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    Boolean syncSupported() {
        if (mFeatureUtilsClass == null) return true;
        try {
            return (Boolean) XposedHelpers.callStaticMethod(mFeatureUtilsClass, "canAllowSync", mActivity);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean printingEnabled() {
        if (mServiceBridgeClass == null) return true;
        try {
            return (Boolean) callMethod(XposedHelpers.callStaticMethod(mServiceBridgeClass, "getInstance"), "isPrintingEnabled");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean editBookmarksSupported() {
        Class<?> bookmarksBridge = null;
        try {
            bookmarksBridge = XposedHelpers.findClass("org.chromium.chrome.browser.BookmarksBridge", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
            return true;
        }
        try {
            return (Boolean) callMethod(callMethod(getToolbarManager(), "getBookmarksBridge"), "isEditBookmarksEnabled");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object profile = callMethod(callMethod(getCurrentTab(), "getProfile"), "getOriginalProfile");
            return (Boolean) XposedHelpers.callStaticMethod(bookmarksBridge, "isEditBookmarksEnabled", profile);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Boolean) XposedHelpers.callStaticMethod(bookmarksBridge, "isEditBookmarksEnabled");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    Boolean addToHomeSupported() {
        try {
            Class<?> bookmarkUtils = XposedHelpers.findClass("org.chromium.chrome.browser.BookmarkUtils", mClassLoader);
            return (Boolean) XposedHelpers.callStaticMethod(bookmarkUtils, "isAddToHomeIntentSupported", mActivity);
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return true;
    }

    void distillCurrentPage() {
        try {
            Class<?> distillerTabUtils = XposedHelpers.findClass("org.chromium.chrome.browser.dom_distiller.DomDistillerTabUtils", mClassLoader);
            XposedHelpers.callStaticMethod(distillerTabUtils, "distillCurrentPageAndView", getWebContents());
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
    }

    Boolean isDistilledPage() {
        if (mDomDistillerUrlUtilsClass == null) return false;
        try {
            return (Boolean) XposedHelpers.callStaticMethod(mDomDistillerUrlUtilsClass, "isDistilledPage", getUrl());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return false;
    }

    Boolean nativeIsUrlDistillable() {
        if (mDomDistillerUrlUtilsClass == null) return true;
        try {
            return (Boolean) XposedHelpers.callStaticMethod(mDomDistillerUrlUtilsClass, "nativeIsUrlDistillable", getUrl());
        } catch (NoSuchMethodError nsme) {

        }
        return true;
    }

    String getOriginalUrl() {
        if (mDomDistillerUrlUtilsClass == null) return "";
        try {
            return (String) XposedHelpers.callStaticMethod(mDomDistillerUrlUtilsClass, "getOriginalUrlFromDistillerUrl", getUrl());
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return "";
    }

    ComponentName getShareComponentName() {
        try {
            Class<?> shareHelper = XposedHelpers.findClass("org.chromium.chrome.browser.share.ShareHelper", mClassLoader);
            return (ComponentName) XposedHelpers.callStaticMethod(shareHelper, "getLastShareComponentName", mActivity);
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return null;
    }

    Object getContentViewCore() {
        Object tab = getCurrentTab();
        if (tab == null) {
            return null;
        }
        try {
            return callMethod(tab, "getContentViewCore");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return null;
    }

    Object getWebContents() {
        try {
            return callMethod(getCurrentTab(), "getWebContents");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return new Object();
    }

    public Integer getTopControlsHeight() {
        Object contentViewCore = getContentViewCore();
        if (contentViewCore == null) {
            return getTopControlsDimen();
        }
        try {
            return (Integer) callMethod(contentViewCore, "getTopControlsLayoutHeightPix");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Integer) callMethod(contentViewCore, "getViewportSizeOffsetHeightPix");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return getTopControlsDimen();
    }

    int getTopControlsDimen() {
        int controlHeightId = getResIdentifier("control_container_height", "dimen");
        return controlHeightId == 0 ? 0 : mActivity.getResources().getDimensionPixelSize(controlHeightId);
    }

    Boolean isDocumentMode() {
        if (mIsDocumentMode != null) {
            return mIsDocumentMode;
        }
        if (mFeatureUtilsClass == null) return false;
        try {
            return (Boolean) XposedHelpers.callStaticMethod(mFeatureUtilsClass, "isDocumentMode", mActivity);
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Class<?> featureUtilsInternal = XposedHelpers.findClass("com.google.android.apps.chrome.utilities.FeatureUtilitiesInternal", mClassLoader);
            return (Boolean) XposedHelpers.callStaticMethod(featureUtilsInternal, "isDocumentMode", mActivity);
        } catch (ClassNotFoundError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }
        return false;
    }

    Object getLocationBar() {
        try {
            return callMethod(mActivity, "getLocationBar");
        } catch (NoSuchMethodError nsme) {

        }
        Object locationBar = mActivity.findViewById(getResIdentifier("location_bar"));
        return (locationBar == null) ? new Object() : locationBar;
    }

    EditText getUrlBar() {
        Object locationBar = getLocationBar();
        try {
            return (EditText) callMethod(locationBar, "getUrlBar");
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (EditText) XposedHelpers.getObjectField(locationBar, "mUrlBar");
        } catch (NoSuchFieldError nsfe) {
            XposedBridge.log(TAG + nsfe);
        }
        return null;
    }

    Boolean isVoiceSearchEnabled() {
        try {
            return (Boolean) callMethod(getLocationBar(), "isVoiceSearchEnabled");
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return true;
    }

    void toggleRecentApps() {
        try {
            Class<?> serviceClass = XposedHelpers.findClass("android.os.ServiceManager", mClassLoader);
            IBinder statusBarBinder = (IBinder) XposedHelpers.callStaticMethod(serviceClass, "getService", "statusbar");
            Class<?> statusBarClass = XposedHelpers.findClass(statusBarBinder.getInterfaceDescriptor(), mClassLoader).getClasses()[0];
            Object statusBar = XposedHelpers.callStaticMethod(statusBarClass, "asInterface", statusBarBinder);
            callMethod(statusBar, "toggleRecentApps");
        } catch (Throwable t) {
            XposedBridge.log(TAG + t);
        }
    }

    void goToHomeScreen() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.startActivity(intent);
    }

    Boolean shouldUseThemeColor(int themeColor) {
        return !isDefaultPrimaryColor(themeColor) && !isIncognito();
    }

    private int getDefaultPrimaryColor() {
        int color;
        if (isIncognito()) {
            color = getResIdentifier("incognito_primary_color", "color");
        } else {
            color = getResIdentifier("default_primary_color", "color");
        }
        return color == 0 ? 0 : mActivity.getResources().getColor(color);
    }

    private boolean isDefaultPrimaryColor(int color) {
        return color == getDefaultPrimaryColor() || getDefaultPrimaryColor() == 0 || color == 0;
    }

    Integer getThemeColor() {
        Object tab = getCurrentTab();
        if (tab == null || isIncognito()) {
            return getDefaultPrimaryColor();
        }
        try {
            return (Integer) callMethod(getWebContents(), "getThemeColor", getDefaultPrimaryColor());
        } catch (NoSuchMethodError nsme) {

        }
        try {
            return (Integer) callMethod(tab, "getThemeColor");
        } catch (NoSuchMethodError nsme) {

        }
        if (isDocumentMode()) {
            try {
                return (Integer) callMethod(mActivity, "getThemeColor");
            } catch (NoSuchMethodError nsme) {

            }
        }
        return 0;
    }

    public Integer getStatusBarColor(int color) {
        if (isDefaultPrimaryColor(color)) {
            return Color.BLACK;
        }
        if (mBrandColorUtilsClass == null) return color;
        try {
            return (Integer) XposedHelpers.callStaticMethod(mBrandColorUtilsClass, "computeStatusBarColor", color);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
        return color;
    }

    void applyThemeColors() {
        try {
            int themeColor = getThemeColor();
            if (mBrandColor != themeColor) {
                mBrandColor = themeColor;
                setStatusBarColor(themeColor);
                callMethod(getToolbarManager(), "updatePrimaryColor", themeColor);
                updateToolbarVisuals();
            }
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    private void updateToolbarVisuals() {
        Object toolbar = getToolbar();
        try {
            callMethod(toolbar, "updateVisualsForToolbarState", isInOverview());
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            Object toolbarDelegate = XposedHelpers.getObjectField(toolbar, "mToolbarDelegate");
            callMethod(toolbarDelegate, "updateVisualsForToolbarState", isInOverview());
        } catch (NoSuchFieldError | NoSuchMethodError e) {
            XposedBridge.log(TAG + e);
        }

    }

    private void setStatusBarColor(int themeColor) {
        int statusColor = getStatusBarColor(themeColor);
        Class<?> apiCompatUtils = null;
        try {
            apiCompatUtils = XposedHelpers.findClass("org.chromium.base.ApiCompatibilityUtils", mClassLoader);
        } catch (ClassNotFoundError cnfe) {
            XposedBridge.log(TAG + cnfe);
            return;
        }
        try {
            XposedHelpers.callStaticMethod(apiCompatUtils, "setStatusBarColor", mActivity.getWindow(), statusColor);
            return;
        } catch (NoSuchMethodError nsme) {

        }
        try {
            XposedHelpers.callStaticMethod(apiCompatUtils, "setStatusBarColor", mActivity, statusColor);
        } catch (NoSuchMethodError nsme) {
            XposedBridge.log(TAG + nsme);
        }
    }

    Object getToolbarManager() {
        try {
            return XposedHelpers.getObjectField(mActivity, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            Object helper = XposedHelpers.getObjectField(mActivity, "mToolbarHelper");
            return XposedHelpers.getObjectField(helper, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            Object helper = XposedHelpers.getObjectField(mActivity, "mDocumentToolbarHelper");
            return XposedHelpers.getObjectField(helper, "mToolbarManager");
        } catch (NoSuchFieldError nsfe) {
            return new Object();
        }
    }

    private Object getToolbar() {
        try {
            return XposedHelpers.getObjectField(mActivity, "mToolbar");
        } catch (NoSuchFieldError nsfe) {

        }
        try {
            return XposedHelpers.getObjectField(getToolbarManager(), "mToolbar");
        } catch (NoSuchFieldError nsfe) {
            return new Object();
        }
    }

}
