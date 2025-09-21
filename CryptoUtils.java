package com.example.kycchain.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class CryptoUtils {
    private CryptoUtils() {}

    public static KeyPair generateECKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            return kpg.generateKeyPair();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String signToBase64(String data, PrivateKey priv) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(priv);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static boolean verifyFromBase64(String data, String sigB64, String pubB64) {
        try {
            PublicKey pub = publicKeyFromBase64(pubB64);
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(pub);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            return s.verify(Base64.getDecoder().decode(sigB64));
        } catch (Exception e) { return false; }
    }

    public static String publicKeyToBase64(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey publicKeyFromBase64(String b64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(b64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return KeyFactory.getInstance("EC").generatePublic(spec);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
