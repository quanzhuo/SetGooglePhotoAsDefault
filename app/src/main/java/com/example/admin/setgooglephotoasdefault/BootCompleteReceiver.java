package com.example.admin.setgooglephotoasdefault;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.PatternMatcher;
import android.telecom.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Admin on 2017/5/4.
 */

public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String CAT_PICTURE = "Pictures";
    private static final String CAT_PICTURE_PNG = "Pictures_png";
    private static final String CAT_PICTURE_JPG = "Pictures_jpg";
    private static final String CAT_PICTURE_BMP = "Pictures_bmp";
    private static final String SYSTEM_DEFAULT_PACKAGENAME = "android";
    private static final String SUPERPOWERSAVING_PACKAGENAME = "com.fihndc.superpowersaving";
    private static final String SETTING_PACKAGENAME = "com.android.settings";
    private static final String CAT_LAUNCHER = "Launcher";
    private static final String CONTACT_NONE_PHONE_ACTIVITY = "com.android.contacts.activities.NonPhoneActivity";
    private static final String CAT_DIALER = "Dialer";
    private static final String TAG = "SetGooglePhotoAsDefault";
    private static final String[] CATEGORIES_ID_PICTURE_LIST = new String[]{
            CAT_PICTURE,
            CAT_PICTURE_PNG,
            CAT_PICTURE_JPG,
            CAT_PICTURE_BMP,
    };

    @Override
    public void onReceive(Context context, Intent intentSys) {
        SharedPreferences preferences = context.getSharedPreferences("config", 0);
        SharedPreferences.Editor editor = preferences.edit();
        boolean isFirstBoot = preferences.getBoolean("FirstBoot", true);
        if (isFirstBoot) {
            editor.putBoolean("FirstBoot", false);
            editor.commit();
        } else {
            editor.commit();
            System.exit(0);
        }
//        Toast.makeText(context, "Set Google Photo as the default photo viewer",
//                Toast.LENGTH_LONG).show();
        for (String cat : CATEGORIES_ID_PICTURE_LIST) {
            Intent intent = getIntentForApplication(cat);
            // Get all the supported activities as a List<ResolveInfo> object
            List<ResolveInfo> resolveInfoList = context.getPackageManager().
                    queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            // Get com.google.android.apps.photos as a ResolveInfo object.
            ResolveInfo googlePhoto = null;
            for (int i = 0; i < resolveInfoList.size(); i++) {
                Log.d(TAG, "i = " + i + resolveInfoList.get(i).activityInfo.packageName);
                if (resolveInfoList.get(i).activityInfo.packageName.equals("com.google.android.apps.photos")) {
                    googlePhoto = resolveInfoList.get(i);
                    break;
                }
            }
            if (googlePhoto != null) {
                Log.d(TAG, "Call SetDefaultActivity on category " + cat);
                // We just call function SetDefaultActivity writen by jinsheng.zheng
                SetDefaultActivity(context, resolveInfoList, googlePhoto, cat, true);
            }
        }
    }

    public static void SetDefaultActivity(Context context,
                                          List<ResolveInfo> resolveInfoList,
                                          ResolveInfo resolveInfo,
                                          String appCategory,
                                          boolean clearOld) {

        PackageManager pm = context.getPackageManager();
        Intent intent = getIntentForApplication(appCategory);
        Log.i(TAG, "SetDefaultActivity intent" + intent);
        int match = 0;

        ResolveInfo resolveActivity = null;
        ArrayList<String> revertCateList = null;

        if (clearOld) {
            Log.i(TAG, "SetDefaultActivity clear curren first appCategory:" + appCategory + " support size:" + getSupportActivityCount(context, appCategory));
            if (intent != null && getSupportActivityCount(context, appCategory) > 1) {
                ResolveInfo preDefaultActivity = pm.resolveActivity(intent, 0);
                String preActivityName = preDefaultActivity.activityInfo.name;

                while (preDefaultActivity != null && !SYSTEM_DEFAULT_PACKAGENAME.equals(preDefaultActivity.activityInfo.packageName)) {
                    Log.i(TAG, "SetDefaultActivity clear appCategory:" + appCategory + " packageName:" + preDefaultActivity.activityInfo.packageName + " activity name:" + preDefaultActivity.activityInfo.name);
                    ArrayList<String> revertCateListTemp = null;
                    if (isPictureCategory(appCategory)) {
                        revertCateListTemp = isSetAsDefaultPictureActivity(context, appCategory, preDefaultActivity.activityInfo.packageName);
                    }
                    if (revertCateListTemp != null && revertCateListTemp.size() > 0) {
                        revertCateList = revertCateListTemp;
                        resolveActivity = preDefaultActivity;
                    }
                    pm.clearPackagePreferredActivities(preDefaultActivity.activityInfo.packageName);
                    preDefaultActivity = pm.resolveActivity(intent, 0);
                    if (preDefaultActivity != null && preActivityName.equals(preDefaultActivity.activityInfo.name)) {
                        Log.i(TAG, "The activity same as we clear,means can't be clear");
                        break;
                    }
                    preActivityName = preDefaultActivity.activityInfo.name;
                }
                ;
            }
        }

        ComponentName[] cmpList = new ComponentName[resolveInfoList.size()];
        for (int i = 0; i < resolveInfoList.size(); i++) {
            if (match < resolveInfoList.get(i).match) {
                match = resolveInfoList.get(i).match;
            }
            cmpList[i] = new ComponentName(resolveInfoList.get(i).activityInfo.packageName, resolveInfoList.get(i).activityInfo.name);
        }
        IntentFilter filter = new IntentFilter(intent.getAction());

        Set<String> categories = intent.getCategories();
        if (categories != null) {
            for (String cat : categories) {
                filter.addCategory(cat);
            }
        }
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        int cat = resolveInfo.match & IntentFilter.MATCH_CATEGORY_MASK;
        Uri data = intent.getData();

        if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
            String mimeType = intent.resolveType(context);
            Log.i(TAG, "mimeType  " + mimeType);
            if (mimeType != null) {
                try {
                    filter.addDataType(mimeType);
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    Log.w(TAG, e.toString());
                    filter = null;
                }
            }
        }
        if (data != null && data.getScheme() != null) {
            if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                    || (!"file".equals(data.getScheme())
                    && !"content".equals(data.getScheme()))) {
                filter.addDataScheme(data.getScheme());
                if (resolveInfo.filter != null) {
                    Iterator<IntentFilter.AuthorityEntry> aIt = resolveInfo.filter.authoritiesIterator();
                    Log.i(TAG, "AIT " + aIt);
                    if (aIt != null) {
                        while (aIt.hasNext()) {
                            IntentFilter.AuthorityEntry a = aIt.next();
                            if (a.match(data) >= 0) {
                                int port = a.getPort();
                                filter.addDataAuthority(a.getHost(),
                                        port >= 0 ? Integer.toString(port) : null);
                                break;
                            }
                        }
                    }
                    Iterator<PatternMatcher> pIt = resolveInfo.filter.pathsIterator();
                    if (pIt != null) {
                        String path = data.getPath();
                        while (path != null && pIt.hasNext()) {
                            PatternMatcher p = pIt.next();
                            if (p.match(path)) {
                                filter.addDataPath(p.getPath(), p.getType());
                                Log.i(TAG, "p.getType " + p.getType());
                                break;
                            }
                        }
                    }
                }
            }
        }

        ComponentName cmp = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        Log.i(TAG, "SetDefaultActivity  cmp" + cmp);
        Log.i(TAG, "SetDefaultActivity packageName:" + cmp.getPackageName() + " activity name:" + cmp.getClassName());
        pm.addPreferredActivity(filter, match, cmpList, cmp);

        for (int i = 0; revertCateList != null && i < revertCateList.size(); i++) {
            SetDefaultActivity(context, getSupportActivityList(context, revertCateList.get(i)), resolveActivity, revertCateList.get(i), false);
        }
    }

    public static Intent getIntentForApplication(String appCategory) {
        Intent intent = new Intent();
        if (appCategory.equals(CAT_PICTURE)) {
            intent.setAction("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse("content://media/external/images/media/"), "image/*");
        } else if (appCategory.equals(CAT_PICTURE_PNG)) {
            intent.setAction("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/png");
        } else if (appCategory.equals(CAT_PICTURE_JPG)) {
            intent.setAction("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/jpeg");
        } else if (appCategory.equals(CAT_PICTURE_BMP)) {
            intent.setAction("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse("content://media/external/images/media/1"), "image/x-ms-bmp");
        }
        return intent;
    }

    public static int getSupportActivityCount(Context context, String appCategory) {
        List<ResolveInfo> resolveInfoList = getSupportActivityList(context, appCategory);
        if (resolveInfoList == null) return 0;
        return resolveInfoList.size();
    }

    public static List<ResolveInfo> getSupportActivityList(Context context, String appCategory) {
        List<ResolveInfo> supportList = context.getPackageManager().queryIntentActivities(getIntentForApplication(appCategory), PackageManager.MATCH_DEFAULT_ONLY);
        if (CAT_DIALER.equals(appCategory)) {
            for (int i = 0; i < supportList.size(); i++) {
                if (CONTACT_NONE_PHONE_ACTIVITY.equals(supportList.get(i).activityInfo.name)) {
                    supportList.remove(i);
                }
            }
        } else if (CAT_LAUNCHER.equals(appCategory)) {//20151215 Jinsheng remove super power saving from launcher list
            for (int i = 0; i < supportList.size(); i++) {
                if (SUPERPOWERSAVING_PACKAGENAME.equals(supportList.get(i).activityInfo.packageName) || SETTING_PACKAGENAME.equals(supportList.get(i).activityInfo.packageName)) {
                    supportList.remove(i);
                    break;
                }
            }
        }
        return supportList;
    }

    public static boolean isPictureCategory(String appCategory) {
        for (int i = 0; i < CATEGORIES_ID_PICTURE_LIST.length; i++) {
            if (appCategory.equals(CATEGORIES_ID_PICTURE_LIST[i])) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<String> isSetAsDefaultPictureActivity(Context context, String setAppCategory, String cleanPackageName) {
        ArrayList<String> setList = new ArrayList<String>();
        for (int i = 0; i < CATEGORIES_ID_PICTURE_LIST.length; i++) {
            if (!setAppCategory.equals(CATEGORIES_ID_PICTURE_LIST[i])) {
                String pn = getCurrentDefaultAppPackageName(context, CATEGORIES_ID_PICTURE_LIST[i]);
                if (cleanPackageName.equals(pn)) {
                    setList.add(CATEGORIES_ID_PICTURE_LIST[i]);
                }
            }
        }
        return setList;
    }

    public static String getCurrentDefaultAppPackageName(Context context, String appCategory) {
        String appName = null;
        PackageManager pm = context.getPackageManager();
        ResolveInfo resolveInfo = getCurrentDefaultActivity(context, appCategory);
        if (resolveInfo != null && !SYSTEM_DEFAULT_PACKAGENAME.equals(resolveInfo.activityInfo.packageName)) {
            appName = resolveInfo.activityInfo.packageName;
        }
        return appName;
    }

    public static ResolveInfo getCurrentDefaultActivity(Context context, String appCategory) {
        return context.getPackageManager().resolveActivity(getIntentForApplication(appCategory), PackageManager.MATCH_DEFAULT_ONLY);
    }
}
