package com.livecontainer

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

        val nameStart = manifestContent.indexOf("android:name=", tagStart)
        if (nameStart < 0) return null

        val nameEnd = manifestContent.indexOf("\"", nameStart + 13)
        if (nameEnd < 0) return null

        return manifestContent.substring(nameStart + 13, nameEnd)
    }

    private var fis: java.io.FileInputStream? = null

    /** Extract the launcher icon bitmap from an APK */
    fun getLauncherIcon(apkPath: String): Bitmap? {
        val apkFile = java.io.File(apkPath)
        if (!apkFile.exists()) return null

        // First try: extract APK to dir and find icon in res
        val extractDir = extractApkToDir(apkPath, "$apkDir/icons")
        if (extractDir != null) {
            val icon = findIconInResDir(extractDir)
            if (icon != null) return icon
        }

        // Fallback: try to read icon directly from APK using JarInputStream
        return readIconFromApk(apkFile)
    }

    private fun extractApkToDir(apkPath: String, destDir: String): File? {
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

    private iconDir: File? = null

    private fun findIconInResDir(dir: File): Bitmap? {
        // Look for common launcher icon names
        val iconNames = arrayOf("ic_launcher", "ic_launcher_round", "launcher", "app_icon")
        
        // Search through drawable directories
        val drawableDirs = dir.listFiles { f -> f.isDirectory && f.name == "drawable" }
        drawableDirs?.forEach { drawableDir ->
            iconNames.forEach { iconName ->
                val iconFile = File(drawableDir, "$iconName.png")
                if (iconFile.exists()) {
                    return BitmapFactory.decodeFile(iconFile.absolutePath)
                }
                val webpIcon = File(drawableDir, "$iconName.webp")
                if (webpIcon.exists()) {
                    return BitmapFactory.decodeFile(webpIcon.absolutePath)
                }
            }
        }

        // Also search recursively for any image that could be an icon
        dir.walkFiles { file ->
            if (file.extension == "png" || file.extension == "webp") {
                val nameLower = file.nameLower
                if (nameLower.contains("icon") || nameLower.contains("launcher")) {
                    return BitmapFactory.decodeFile(file.absolutePath)
                }
            }
        }

        return null
    }

    private fun readIconFromApk(apkFile: java.io.File): Bitmap? {
        try (is = java.util.jar.JarInputStream(java.io.FileInputStream(apkFile))) {
            var entry: JarEntry?
            while ((entry = is.nextEntry()) != null) {
                if (entry.name != null) {
                    // Look for ic_launcher files in the APK
                    if (entry.name.contains("ic_launcher") && entry.name.endsWith(".png")) {
                        val baos = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var count: Int
                        while (count = is.read(buffer).let { it } ) > 0 {
                            baos.write(buffer, 0, count)
                        }
                        return BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GuestManager", "Error reading icon from APK", e)
        } finally {
            try { is.close() } catch (_: Exception) {}
        }
        return null
    }

    fun getAppInfo(apkPath: String): AppItem {
        val icon = getLauncherIcon(apkPath)
        val name = getMainActivityName(apkPath) ?: "Unknown"
        val version = "?"
        val bundle = "?"
        val packageName = apkPath
        return AppItem(icon, name, version, bundle, packageName)
    }
}

data class AppItem(
    val icon: Bitmap?,      // Changed from iconRes: Int
    val name: String,
    val version: String,
    val bundle: String,
    val packageName: String
)