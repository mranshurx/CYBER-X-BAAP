package com.example.shizukufilecopy

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikkax.shizuku.Shizuku
import java.io.File

class MainActivity : AppCompatActivity() {

    private val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show()
                copyFileToAndroidData()
            } else {
                Toast.makeText(this, "Shizuku Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register listener for Shizuku permission response
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        val btnCopy = findViewById<Button>(R.id.btnCopyFile)
        btnCopy.setOnClickListener {
            checkAndRequestShizukuPermission()
        }
    }

    private fun checkAndRequestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku service is not running!", Toast.LENGTH_LONG).show()
            return
        }

        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku version too old", Toast.LENGTH_SHORT).show()
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            copyFileToAndroidData()
        } else {
            // Request permission from user via popup
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }

    private fun copyFileToAndroidData() {
        try {
            // Example: Make a dummy local file in app cache to test copying
            val sourceFile = File(cacheDir, "test_file.txt")
            sourceFile.writeText("Hello from Shizuku file copier!")

            // Target path inside restricted Android/data folder
            val targetDir = "/storage/emulated/0/Android/data/com.example.targetgame/files/"
            
            // Ensure target directory exists via shell command
            val mkDirProcess = Shizuku.newProcess(arrayOf("mkdir", "-p", targetDir), null, null)
            mkDirProcess.waitFor()

            val targetFilePath = "$targetDir/test_file.txt"

            // Execute shell copy command using Shizuku privileges
            val copyProcess = Shizuku.newProcess(arrayOf("cp", sourceFile.absolutePath, targetFilePath), null, null)
            val exitCode = copyProcess.waitFor()

            if (exitCode == 0) {
                Toast.makeText(this, "Successfully copied to Android/data!", Toast.LENGTH_LONG).show()
            } else {
                val errorMsg = copyProcess.errorStream.bufferedReader().readText()
                Toast.makeText(this, "Copy failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}
