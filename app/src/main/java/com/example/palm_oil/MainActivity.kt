package com.example.palm_oil

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val REQUEST_CODE_PERMISSIONS = 1001

    // Keep this list minimal but useful for DJI SDK runtime needs.
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- NEW: ensure runtime permissions needed by camera/location/storage are granted ---
        ensurePermissions()

        // --- NEW: a safe runtime check for DJI SDK registration; uses reflection so it won't
        // hard-depend at compile-time and won't crash if DJI classes are missing. ---
        checkAndTriggerDjiRegistrationIfNeeded()

        // Connect buttons by their IDs (unchanged)
        val buttonR = findViewById<Button>(R.id.buttonR)
        val buttonH = findViewById<Button>(R.id.buttonH)

        buttonR.setOnClickListener {
            // Redirect to ReconHomeActivity
            val intent = Intent(this, ReconHome::class.java)
            startActivity(intent)
        }
        buttonH.setOnClickListener {
            // Redirect to HarvesterHome
            val intent = Intent(this, HarvesterHome::class.java)
            startActivity(intent)
        }
    }

    // Keep existing behavior: if permissions missing, ask once.
    private fun ensurePermissions() {
        val missing = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    // Log simple permission results; don't block app flow.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            permissions.forEachIndexed { i, perm ->
                val granted = grantResults.getOrNull(i) == PackageManager.PERMISSION_GRANTED
                Log.i(TAG, "Permission $perm granted=$granted")
            }
            Toast.makeText(this, "Permissions processed", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Reflection-based check for dji.v5.manager.SDKManager registration status.
     *
     * Why reflection?
     * - avoids hard compile-time coupling in case SDK classes are not available,
     * - prevents accidental class-loading before Helper.install(...) executed in Application.
     *
     * What it does:
     * - If SDKManager.isRegistered() exists and returns false, we attempt to call registerApp()
     *   (many apps need an explicit registerApp call after SDK init; your Application may already
     *   do this — calling it here is guarded and safe).
     */
    private fun checkAndTriggerDjiRegistrationIfNeeded() {
        try {
            val sdkManagerClass = Class.forName("dji.v5.manager.SDKManager")
            val getInstance = sdkManagerClass.getMethod("getInstance")
            val sdkManager = getInstance.invoke(null)

            val isRegisteredMethod = sdkManagerClass.getMethod("isRegistered")
            val isRegistered = (isRegisteredMethod.invoke(sdkManager) as? Boolean) ?: false

            Log.i(TAG, "DJI SDK registered: $isRegistered")

            if (!isRegistered) {
                // Attempt to call registerApp() if present.
                try {
                    val registerAppMethod = sdkManagerClass.getMethod("registerApp")
                    registerAppMethod.invoke(sdkManager)
                    Log.i(TAG, "Called SDKManager.registerApp() via reflection")
                    Toast.makeText(this, "Triggered DJI registerApp()", Toast.LENGTH_SHORT).show()
                } catch (noMethod: NoSuchMethodException) {
                    // If registerApp isn't available on this SDK version, just warn and continue.
                    Log.w(TAG, "SDKManager.registerApp() not found (no action taken).")
                } catch (invokeEx: Throwable) {
                    Log.w(TAG, "Failed to invoke SDKManager.registerApp()", invokeEx)
                }
            }
        } catch (cnfe: ClassNotFoundException) {
            // DJI SDK not present on classpath — skip check (safe).
            Log.w(TAG, "DJI SDK classes not found on classpath; skipping SDK status check.")
        } catch (t: Throwable) {
            // Any other reflection / invocation failures should not crash the app.
            Log.w(TAG, "Error while checking or registering DJI SDK via reflection", t)
        }
    }
}
