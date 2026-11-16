package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the Supabase refresh token encrypted with an Android Keystore AES key. */
internal class SecureSessionStore(context: Context) {
  private val preferences = context.applicationContext.getSharedPreferences("admin_session", Context.MODE_PRIVATE)
  private val keyAlias = "shree_banquet_admin_session_key"

  fun saveRefreshToken(token: String) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
    preferences.edit()
      .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
      .putString("token", Base64.encodeToString(encrypted, Base64.NO_WRAP))
      .apply()
  }

  fun readRefreshToken(): String? {
    val ivText = preferences.getString("iv", null) ?: return null
    val tokenText = preferences.getString("token", null) ?: return null
    return try {
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP)))
      String(cipher.doFinal(Base64.decode(tokenText, Base64.NO_WRAP)), Charsets.UTF_8)
    } catch (_: Exception) {
      clear()
      null
    }
  }

  fun clear() {
    preferences.edit().clear().apply()
  }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        keyAlias,
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
