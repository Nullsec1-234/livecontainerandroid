package com.livecontainer

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class GuestManager(context: Context) {

    private val context: Context = context
    private val guestApkDir: File = context.getDir("guests", Context.MODE_PRIVATE)
    private lateinit var packageManager: PackageManager

    init {
        packageManager = context.packageManager
        scanGuests()
    }

    private fun scanGuests() {
        if (!guestApkDir.exists()) {
            guestApkDir.mkdirs()
        }
    }

    fun getDefaultGuest(): String? {
        val apks = guestApkDir.listFiles()?.filter { it.name.endsWith(".apk") }
        return apks?.firstOrNull()?.absolutePath
    }

    fun getGuestApks(): List<String> {
        return guestApkDir.listFiles()
            ?.filter { it.name.endsWith(".apk") }
            ?.map { it.absolutePath } ?: emptyList()
    }

    /** Extract AndroidManifest.xml from APK and parse for main activity */
    fun getMainActivityName(apkPath: String): String? {
        try {
            val manifestFile = extractManifestFromApk(apkPath)
            if (manifestFile != null) {
                val content = String(manifestFile.readBytes())
                return parseMainActivity(content)
            }
        } catch (e: Exception) {
            Log.e("GuestManager", "Error extracting manifest", e)
        }
        return null
    }

    private fun extractManifestFromApk(apkPath: String): java.io.File? {
        val apkFile = java.io.File(apkPath)
        if (!apkFile.exists()) return null

        val manifestDir = File(context.cacheDir, "manifests")
        manifestDir.mkdirs()

        val manifestFile = File(manifestDir, "AndroidManifest.xml")

        // Extract AndroidManifest.xml from APK using JarInputStream
        try (fis = java.io.FileInputStream(apkFile)) {
            val is = java.util.jar.JarInputStream(fis)
            var entry: JarEntry?
            while ((entry = is.nextEntry()) != null) {
                if (entry.name == "AndroidManifest.xml") {
                    val baos = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    var count: Int
                    while (count = is.read(buffer).let { it } ) > 0 {
                        baos.write(buffer, 0, count)
                    }
                    manifestFile.writeBytes(baos.toByteArray())
                    is.close()
                    return manifestFile
                }
            }
            is.close()
        } catch (e: Exception) {
            Log.e("GuestManager", "Error extracting manifest", e)
        } finally {
            try { fis.close() } catch (_: Exception) {}
        }

        return null
    }

    private fun parseMainActivity(manifestContent: String): String? {
        val tagStart = manifestContent.indexOf("<activity")
        if (tagStart < 0) return null

        // Find the first activity with android:name and android:exported="true"
        // or just find android:name attribute
        val namePattern = "android:name=\"[^\"]*\""
        val importPattern = "android:exported=\"true\""

        val nameStart = manifestContent.indexOf("android:name=\"", tagStart)
        if (nameStart < 0) return null

        val nameEnd = manifestContent.indexOf("\"", nameStart + 13)
        if (nameEnd < 0) return null

        return manifestContent.substring(nameStart + 13, nameEnd)
    }

    private var fis: java.io.FileInputStream? = null

    fun extractApkToDir(apkPath: String, destDir: String): File? {
        val apkFile = java.io.File(apkPath)
        if (!apkFile.exists()) return null

        val targetDir = File(destDir)
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        try (fis = java.io.FileInputStream(apkFile)) {
            val is = java.util.jar.JarInputStream(fis)
            var entry: JarEntry?
            while ((entry = is.nextEntry()) != null) {
                if (entry.name != null && !entry.name.startsWith("/")) {
                    val file = File(targetDir, entry.name)
                    file.parentFile.mkdirs()

                    if (entry.isDirectory()) {
                        file.mkdirs()
                    } else {
                        val os = java.io.FileOutputStream(file)
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (count = is.read(buffer).let { it } ) > 0 {
                            os.write(buffer, 0, count)
                        }
                        os.close()
                    }
                }
            }
            is.close()
        } catch (e: Exception) {
            Log.e("GuestManager", "Error extracting APK", e)
            return null
        } finally {
            try { fis.close() } catch (_: Exception) {}
        }

        return targetDir
    }
}