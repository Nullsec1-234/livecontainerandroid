package com.livecontainer

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileInputStream
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class ClassLoaderHelper {

    private fun loadClasses(apkPath: String, baseContext: Context): java.lang.ClassLoader? {
        val apkFile = java.io.File(apkPath)
        if (!apkFile.exists()) {
            return null
        }

        // Create optimized directory for DexClassLoader
        val optDir = File(baseContext.cacheDir, "dex-opt")
        optDir.mkdirs()

        // Setup library search path - extract .so files from APK first
        val libDir = extractNativeLibs(apkFile, baseContext)
        libDir?.mkdirs()

        return DexClassLoader(
            apkFile.absolutePath,
            optDir.absolutePath,
            libDir?.absolutePath ?: "",
            baseContext.classLoader
        )
    }

    /** Extract native .so libraries from APK to appropriate architecture directory */
    private fun extractNativeLibs(apkFile: File, context: Context): File? {
        val arch = when {
            context.architecture == null -> "arm64-v8a"
            context.architecture.contains("arm64") -> "arm64-v8a"
            context.architecture.contains("armeabi") -> "armeabi-v7a"
            context.architecture.contains("x86") -> "x86"
            context.architecture.contains("x86_64") -> "x86_64"
            else -> "arm64-v8a"
        }

        val libDir = File(context.getDir("extracted_libs", Context.MODE_PRIVATE), arch)
        if (libDir.exists()) {
            libDir.deleteRecursively()
        }
        libDir.mkdirs()

        // Walk through APK entries and extract .so files
        val fis = FileInputStream(apkFile)
        try {
            val is = JarInputStream(fis)
            var entry: JarEntry?
            while ((entry = is.nextEntry()) != null) {
                if (entry.name.startsWith("lib/") && entry.name.endsWith(".so")) {
                    val destFile = File(libDir, entry.name.replace("lib/", ""))
                    destFile.parentFile.mkdirs()
                    val baos = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    var count: Int
                    while (count = is.read(buffer).let { it } ) > 0 {
                        baos.write(buffer, 0, count)
                    }
                    destFile.writeBytes(baos.toByteArray())
                }
            }
            is.close()
        } catch (e: Exception) {
            Log.e("ClassLoaderHelper", "Error extracting native libs", e)
            // Close fis if still open
            try { fis.close() } catch (_: Exception) {}
            return null
        } finally {
            try { fis.close() } catch (_: Exception) {}
        }

        return libDir
    }

    fun loadClass(apkPath: String, className: String, baseContext: Context): Class<*>? {
        val classLoader = loadClasses(apkPath, baseContext)
        return classLoader?.loadClass(className)?.run { checkIsInitialized() }
    }

    fun loadClasses(apkPath: String, baseContext: Context): java.lang.ClassLoader? {
        return loadClasses(apkPath, baseContext)
    }
}