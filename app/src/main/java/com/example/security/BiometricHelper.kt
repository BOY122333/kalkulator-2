package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi

sealed class BiometricStatus {
    object Available : BiometricStatus()
    data class Unavailable(val reason: String) : BiometricStatus()
}

object BiometricHelper {

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val packageManager = context.packageManager
        val hasFingerprint = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasBiometrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_FACE) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS) ||
            hasFingerprint
        } else {
            hasFingerprint
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceSecure = keyguardManager?.isDeviceSecure ?: false

        if (!hasBiometrics && !isDeviceSecure) {
            return BiometricStatus.Unavailable("Perangkat tidak memiliki sensor biometrik atau PIN layar aktif.")
        }

        if (!isDeviceSecure) {
            return BiometricStatus.Unavailable("Kunci layar (PIN/Pola/Biometrik) belum diaktifkan pada pengaturan sistem perangkat.")
        }

        return BiometricStatus.Available
    }

    /**
     * Prompts standard BiometricPrompt on Android 28+ with fallback.
     */
    fun showBiometricPrompt(
        context: Context,
        title: String = "Autentikasi Ruang Rahasia",
        subtitle: String = "Gunakan sidik jari atau biometrik perangkat untuk membuka",
        negativeButtonText: String = "Gunakan PIN Vault",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onNegativeClick: () -> Unit
    ): CancellationSignal {
        val cancellationSignal = CancellationSignal()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = context.mainExecutor
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                            onError(errString?.toString() ?: "Autentikasi biometrik gagal.")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Sidik jari tidak dikenali. Coba lagi.")
                    }
                }

                val prompt = BiometricPrompt.Builder(context)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButton(negativeButtonText, executor) { _, _ ->
                        onNegativeClick()
                    }
                    .build()

                prompt.authenticate(cancellationSignal, executor, callback)
            } catch (e: Exception) {
                onError("Gagal memulai biometrik: ${e.message}")
            }
        } else {
            // Older API fallback
            onNegativeClick()
        }

        return cancellationSignal
    }
}
