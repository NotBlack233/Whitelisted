package me.not_black.whitelisted.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

object AES256GCM {

    private const val AES_KEY_SIZE = 256  // 位
    private const val GCM_TAG_LENGTH = 128 // 位
    private const val GCM_IV_LENGTH = 12   // 字节（推荐 12 字节）

    /**
     * 生成一个随机的 AES-256 密钥
     */
    @JvmStatic
    fun generateKey(): SecretKey {
        val key = ByteArray(AES_KEY_SIZE / 8)
        SecureRandom().nextBytes(key)
        return SecretKeySpec(key, "AES")
    }

    /**
     * 将 Base64 字符串还原为 SecretKey
     */
    @JvmStatic
    private fun decodeKey(base64Key: String): SecretKey {
        val keyBytes = Base64.decode(base64Key)
        require(keyBytes.size == AES_KEY_SIZE / 8) { "Invalid key length" }
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 加密明文
     * @param plaintext 明文（字节数组）
     * @param key 密钥
     * @return 密文（格式：IV + 密文 + 认证标签，自动合并）
     */
    @JvmStatic
    fun encrypt(plaintext: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        // 生成随机 IV
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        // 合并 IV + 密文（含标签）
        return iv + ciphertext
    }

    /**
     * 解密密文
     * @param ciphertext 密文（由 encrypt 生成）
     * @param key 密钥
     * @return 明文（字节数组）
     */
    @JvmStatic
    fun decrypt(ciphertext: String, key: SecretKey): ByteArray {
        val data = Base64.decode(ciphertext)

        // 提取 IV 和实际密文
        val iv = data.sliceArray(0 until GCM_IV_LENGTH)
        val ciphertext = data.sliceArray(GCM_IV_LENGTH until data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        return cipher.doFinal(ciphertext)
    }
}