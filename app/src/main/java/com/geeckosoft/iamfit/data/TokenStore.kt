package com.geeckosoft.iamfit.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda el token de Sanctum cifrado con una llave AES del Android Keystore.
 *
 * No usa `androidx.security:security-crypto` a propósito (evita una dependencia
 * extra): el cifrado es AES/GCM con IV aleatorio por escritura y la llave nunca
 * sale del Keystore.
 */
object TokenStore {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "iamfit_sanctum_key"
    private const val PREFS = "iamfit_secure"
    private const val PREF_TOKEN = "sanctum_token"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    fun save(context: Context, token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)

        prefs(context).edit().putString(PREF_TOKEN, payload).apply()
    }

    fun get(context: Context): String? {
        val payload = prefs(context).getString(PREF_TOKEN, null) ?: return null

        return try {
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, IV_BYTES)
            val cipherText = bytes.copyOfRange(IV_BYTES, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))

            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            // Llave rotada o payload corrupto: se descarta y se pide login de nuevo.
            clear(context)
            null
        }
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(PREF_TOKEN).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )

        return generator.generateKey()
    }
}
