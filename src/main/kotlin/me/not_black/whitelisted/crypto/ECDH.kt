package me.not_black.whitelisted.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.security.Key
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

object ECDH {
    @JvmStatic
    fun generateECKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(256) // 使用 secp256r1 曲线
    }.generateKeyPair()

    @JvmStatic
    fun ecdhSharedSecret(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): SecretKey {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        val salt = byteArrayOf(0x11, 0x45, 0x14, 0x7b, 0x0a, 0x4d)
        val info = "aes-256-gcm-key".toByteArray()
        val derivedKey = ByteArray(32)

        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(sharedSecret, salt, info))
        hkdf.generateBytes(derivedKey, 0, derivedKey.size)

        return SecretKeySpec(derivedKey, "AES")
    }

    val Key.base64: String get() = Base64.encode(encoded)

    fun publicKeyFromBase64(base64: String): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(base64)))
}