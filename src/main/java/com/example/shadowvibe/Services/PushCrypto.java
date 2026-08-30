package com.example.shadowvibe.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

@Component
public class PushCrypto {

    @Value("${app.push.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${app.push.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${app.push.vapid.subject}")
    private String vapidSubject;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ECParameterSpec EC_P256;

    static {
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            EC_P256 = params.getParameterSpec(ECParameterSpec.class);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось инициализировать параметры EC P-256", e);
        }
    }

    static String b64url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    static byte[] b64urlDecode(String s) {
        String fixed = s.replace('-', '+').replace('_', '/');
        int rem = fixed.length() % 4;
        if (rem == 1) {
            fixed += "===";
        } else if (rem == 2) {
            fixed += "==";
        } else if (rem == 3) {
            fixed += "=";
        }
        return Base64.getDecoder().decode(fixed);
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Формирует заголовок Authorization: vapid t=..., k=...
     */
    public String buildVapidAuthorization(String endpoint) throws Exception {
        return "vapid t=" + createVapidToken(endpoint) + ", k=" + vapidPublicKey;
    }

    private String createVapidToken(String endpoint) throws Exception {
        URI uri = URI.create(endpoint);
        String aud = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() != -1) {
            aud += ":" + uri.getPort();
        }

        String header = b64url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        long exp = (System.currentTimeMillis() / 1000L) + 3600;
        String payloadJson = "{\"aud\":\"" + aud + "\",\"exp\":" + exp + ",\"sub\":\"" + vapidSubject + "\"}";
        String payload = b64url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        byte[] privateScalar = b64urlDecode(vapidPrivateKey);
        PrivateKey privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, privateScalar), EC_P256));

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] der = signature.sign();
        BigInteger[] rs = derToRs(der);

        byte[] raw = new byte[64];
        System.arraycopy(toFixed(rs[0], 32), 0, raw, 0, 32);
        System.arraycopy(toFixed(rs[1], 32), 0, raw, 32, 32);

        return signingInput + "." + b64url(raw);
    }

    /**
     * Шифрование тела push-сообщения по RFC 8291 (aes128gcm).
     */
    public byte[] encrypt(String p256dhB64, String authB64, byte[] plaintext) throws Exception {
        byte[] subscriptionPublic = b64urlDecode(p256dhB64);
        byte[] authSecret = b64urlDecode(authB64);

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(EC_P256);
        KeyPair ephemeral = generator.generateKeyPair();
        ECPublicKey ephemeralPublic = (ECPublicKey) ephemeral.getPublic();

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(ephemeral.getPrivate());
        agreement.doPhase(publicKeyFromUncompressed(subscriptionPublic), true);
        byte[] sharedSecret = agreement.generateSecret();

        byte[] prk = hkdfExtract(authSecret, sharedSecret);
        byte[] ikm = hkdfExpand(prk, "Content-Encoding: auth\0".getBytes(StandardCharsets.UTF_8), 32);
        byte[] key = hkdfExpand(ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.UTF_8), 16);
        byte[] nonce = hkdfExpand(ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.UTF_8), 12);

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        byte[] padded = new byte[plaintext.length + 1];
        System.arraycopy(plaintext, 0, padded, 0, plaintext.length);
        padded[plaintext.length] = 0x02;

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        byte[] ciphertext = cipher.doFinal(padded);

        int recordSize = 4096;
        ByteBuffer header = ByteBuffer.allocate(16 + 4 + 1 + 65);
        header.put(salt);
        header.putInt(recordSize);
        header.put((byte) 65);
        header.put(encodeUncompressed(ephemeralPublic));

        byte[] out = new byte[header.capacity() + ciphertext.length];
        System.arraycopy(header.array(), 0, out, 0, header.capacity());
        System.arraycopy(ciphertext, 0, out, header.capacity(), ciphertext.length);
        return out;
    }

    private static PublicKey publicKeyFromUncompressed(byte[] raw) throws Exception {
        int size = (EC_P256.getCurve().getField().getFieldSize() + 7) / 8;
        if (raw.length != 1 + size * 2 || raw[0] != 0x04) {
            throw new IllegalArgumentException("Неверный формат публичного ключа подписки");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(raw, 1, 1 + size));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(raw, 1 + size, 1 + size * 2));
        return KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), EC_P256));
    }

    private static byte[] encodeUncompressed(ECPublicKey publicKey) {
        ECPoint point = publicKey.getW();
        int size = (publicKey.getParams().getCurve().getField().getFieldSize() + 7) / 8;
        byte[] raw = new byte[1 + size * 2];
        raw[0] = 0x04;
        System.arraycopy(toFixed(point.getAffineX(), size), 0, raw, 1, size);
        System.arraycopy(toFixed(point.getAffineY(), size), 0, raw, 1 + size, size);
        return raw;
    }

    private static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        if (salt == null || salt.length == 0) {
            salt = new byte[32];
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        return mac.doFinal(ikm);
    }

    private static byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] previous = new byte[0];
        byte[] result = new byte[length];
        int offset = 0;
        int counter = 1;
        while (offset < length) {
            mac.update(previous);
            mac.update(info);
            mac.update((byte) counter++);
            previous = mac.doFinal();
            System.arraycopy(previous, 0, result, offset, Math.min(previous.length, length - offset));
            offset += previous.length;
        }
        return result;
    }

    private static BigInteger[] derToRs(byte[] der) {
        int offset = 0;
        if (offset >= der.length || der[offset++] != 0x30) {
            throw new IllegalArgumentException("Неверная DER-подпись");
        }
        int seqLen = der[offset++] & 0xff;
        if ((seqLen & 0x80) != 0) {
            int count = seqLen & 0x7f;
            seqLen = 0;
            for (int i = 0; i < count; i++) {
                seqLen = (seqLen << 8) | (der[offset++] & 0xff);
            }
        }
        if (offset >= der.length || der[offset++] != 0x02) {
            throw new IllegalArgumentException("Неверная DER-подпись");
        }
        int rLen = der[offset++] & 0xff;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(der, offset, offset + rLen));
        offset += rLen;
        if (offset >= der.length || der[offset++] != 0x02) {
            throw new IllegalArgumentException("Неверная DER-подпись");
        }
        int sLen = der[offset++] & 0xff;
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(der, offset, offset + sLen));
        return new BigInteger[]{r, s};
    }

    private static byte[] toFixed(BigInteger value, int length) {
        byte[] bytes = value.toByteArray();
        byte[] out = new byte[length];
        if (bytes.length > length) {
            System.arraycopy(bytes, bytes.length - length, out, 0, length);
        } else {
            System.arraycopy(bytes, 0, out, length - bytes.length, bytes.length);
        }
        return out;
    }
}
