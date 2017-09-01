/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.internal;

import android.os.Build;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.log.RealmLog;


public class Util {

    public static String getTablePrefix() {
        return nativeGetTablePrefix();
    }

    static native String nativeGetTablePrefix();

    /**
     * Normalizes a input class to it's original RealmObject class so it is transparent whether or not the input class
     * was a RealmProxy class.
     */
    public static Class<? extends RealmModel> getOriginalModelClass(Class<? extends RealmModel> clazz) {
        // This cast is correct because 'clazz' is either the type
        // generated by RealmProxy or the original type extending directly from RealmObject.
        @SuppressWarnings("unchecked")
        Class<? extends RealmModel> superclass = (Class<? extends RealmModel>) clazz.getSuperclass();

        if (!superclass.equals(Object.class) && !superclass.equals(RealmObject.class)) {
            clazz = superclass;
        }

        return clazz;
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Gets the stack trace from a Throwable as a String.</p>
     * <p>
     * <p>The result of this method vary by JDK version as this method
     * uses {@link Throwable#printStackTrace(java.io.PrintWriter)}.
     * On JDK1.3 and earlier, the cause exception will not be shown
     * unless the specified throwable alters printStackTrace.</p>
     *
     * @param throwable the <code>Throwable</code> to be examined
     * @return the stack trace as generated by the exception's
     * <code>printStackTrace(PrintWriter)</code> method
     * <p>
     * Credit: https://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/exception/ExceptionUtils.html
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    // Credit: http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static boolean isEmptyString(@Nullable String str) {
        return str == null || str.length() == 0;
    }

    public static boolean deleteRealm(String canonicalPath, File realmFolder, String realmFileName) {
        boolean realmDeleted = true;
        final String management = ".management";
        File managementFolder = new File(realmFolder, realmFileName + management);

        // Deletes files in management directory and the directory.
        // There is no subfolders in the management directory.
        File[] files = managementFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                realmDeleted = realmDeleted && file.delete();
            }
        }
        realmDeleted = realmDeleted && managementFolder.delete();

        // Deletes specific files in root directory.
        return realmDeleted && deletes(canonicalPath, realmFolder, realmFileName);
    }

    private static boolean deletes(String canonicalPath, File rootFolder, String realmFileName) {
        final AtomicBoolean realmDeleted = new AtomicBoolean(true);

        List<File> filesToDelete = Arrays.asList(
                new File(rootFolder, realmFileName),
                new File(rootFolder, realmFileName + ".lock"),
                // Old core log file naming styles
                new File(rootFolder, realmFileName + ".log_a"),
                new File(rootFolder, realmFileName + ".log_b"),
                new File(rootFolder, realmFileName + ".log"),
                new File(canonicalPath));
        for (File fileToDelete : filesToDelete) {
            if (fileToDelete.exists()) {
                boolean deleteResult = fileToDelete.delete();
                if (!deleteResult) {
                    realmDeleted.set(false);
                    RealmLog.warn("Could not delete the file %s", fileToDelete);
                }
            }
        }
        return realmDeleted.get();
    }

}