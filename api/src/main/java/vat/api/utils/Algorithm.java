package vat.api.utils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

///
/// @author Zen.Liu
/// @since 2025-11-12

@SuppressWarnings("unused")
@NullMarked
public interface Algorithm {
    Logger log = LoggerFactory.getLogger(Algorithm.class);

    Function<String, byte[]> BASE64_STD_DEC = Base64.getDecoder()::decode;
    Function<byte[], String> BASE64_STD_ENC = Base64.getEncoder().withoutPadding()::encodeToString;
    Function<byte[], String> BASE64_STD_ENC_PAD = Base64.getEncoder()::encodeToString;
    Function<byte[], byte[]> BASE64_STD_DEC_BIN = Base64.getDecoder()::decode;


    Function<String, byte[]> BASE64_URL_DEC = Base64.getUrlDecoder()::decode;
    Function<byte[], String> BASE64_URL_ENC = Base64.getUrlEncoder().withoutPadding()::encodeToString;
    Function<byte[], String> BASE64_URL_ENC_PAD = Base64.getUrlEncoder()::encodeToString;
    Function<byte[], byte[]> BASE64_URL_DEC_BIN = Base64.getUrlDecoder()::decode;

    Function<String, byte[]> BASE64_MIME_DEC = Base64.getMimeDecoder()::decode;
    Function<byte[], String> BASE64_MIME_ENC = Base64.getMimeEncoder().withoutPadding()::encodeToString;
    Function<byte[], String> BASE64_MIME_ENC_PAD = Base64.getMimeEncoder()::encodeToString;
    Function<byte[], byte[]> BASE64_MIME_DEC_BIN = Base64.getMimeDecoder()::decode;

    Function<byte[], String> HEX_ENC = HexFormat.of()::formatHex;
    Function<String, byte[]> HEX_DEC = HexFormat.of()::parseHex;
    Function<byte[], String> HEX_UPPERCASE_ENC = HEX_ENC.andThen(String::toUpperCase);
    Function<String, byte[]> HEX_UPPERCASE_DEC = HEX_DEC.compose(String::toLowerCase);
    Function<String, String> URI_ENC = s -> URLEncoder.encode(s, StandardCharsets.UTF_8);
    Function<String, String> URI_DEC = s -> URLDecoder.decode(s, StandardCharsets.UTF_8);
    Random RNG = new Random();

    interface Equality {
        static boolean equals(String a, String b) {
            var ba = a.getBytes(StandardCharsets.UTF_8);
            var bb = b.getBytes(StandardCharsets.UTF_8);
            try {
                return equals(ba, bb);
            } finally {
                Arrays.fill(ba, (byte) 0);
                Arrays.fill(bb, (byte) 0);
            }
        }

        static boolean equals(byte[] a, byte[] b) {
            return MessageDigest.isEqual(a, b);
        }
    }

    interface Nonce {
        static CharSequence alphabet(int size) {
            return RNG.ints('a', 'z' + 1)
                      .limit(size)
                      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                      .toString();
        }

        static CharSequence alphabetNumeric(int size) {
            return RNG.ints('0', 'z' + 1)
                      .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                      .limit(size)
                      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                      .toString();
        }
    }

    interface AES_ECB_PKCS5 {

        @SneakyThrows
        static byte[] encrypt(byte[] plaintext, byte[] appSecret) {
            return encrypt(plaintext, new SecretKeySpec(appSecret, "AES"));
        }

        @SneakyThrows
        static byte[] decrypt(byte[] ciphertext, byte[] appSecret) {
            return decrypt(ciphertext, new SecretKeySpec(appSecret, "AES"));
        }

        @SneakyThrows
        static byte[] encrypt(byte[] plaintext, SecretKeySpec appSecret) {
            var cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, appSecret);
            return cipher.doFinal(plaintext);
        }

        @SneakyThrows
        static byte[] decrypt(byte[] ciphertext, SecretKeySpec appSecret) {
            var cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, appSecret);
            return cipher.doFinal(ciphertext);
        }
    }

    interface MD5 {
        static byte[] hash(byte[] data) {
            var md5 = digest("MD5");
            md5.update(data);
            return md5.digest();
        }

        static byte[] sign(byte[] data, byte[] appKey) {
            var md5 = digest("MD5");
            md5.update(data);
            return md5.digest(appKey);
        }

        @SneakyThrows
        static boolean verify(byte[] raw, byte[] appKey, byte[] signature) {
            return Equality.equals(sign(raw, appKey), signature);
        }
    }

    interface SHA256 {
        static byte[] hash(byte[] data) {
            var digest = digest("SHA-256");
            digest.update(data);
            return digest.digest();
        }

        static byte[] sign(byte[] data, byte[] appKey) {
            var digest = digest("SHA-256");
            digest.update(data);
            return digest.digest(appKey);
        }

        @SneakyThrows
        static boolean verify(byte[] raw, byte[] appKey, byte[] signature) {
            return Equality.equals(sign(raw, appKey), signature);
        }
    }

    interface HMAC_SHA256 {

        @SneakyThrows
        static byte[] sign(byte[] data, byte[] key) {
            var mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data);
        }

        @SneakyThrows
        static boolean verify(byte[] raw, byte[] key, byte[] signature) {
            var sin = sign(raw, key);
            return Equality.equals(sin, signature);
        }
    }

    @SneakyThrows
    static MessageDigest digest(String alg) {
        return MessageDigest.getInstance(alg);
    }

    @SneakyThrows
    static CertificateFactory certFactory() {
        return CertificateFactory.getInstance("X.509");
    }

    @SneakyThrows
    static Certificate decodeCertificate(CertificateFactory f, String pemSegment) {
        if (log.isDebugEnabled()) log.info("decode certificate {}", pemSegment);
        return f.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(pemSegment)));
    }

    @SneakyThrows
    static KeyFactory rsaKeyFactory() {
        return KeyFactory.getInstance("RSA");
    }

    @SneakyThrows
    static PrivateKey decodePKCS8PrivateKey(KeyFactory f, String pemSegment) {
        if (log.isDebugEnabled()) log.info("decode PKCS#8 private key {}", pemSegment);
        return f.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pemSegment)));
    }


    @SneakyThrows
    static PublicKey decodeX509PublicKey(KeyFactory f, String pemSegment) {
        if (log.isDebugEnabled()) log.info("decode PKCS#8 public key {}", pemSegment);
        return f.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pemSegment)));
    }


    static Stream<String> segmentsInPem(String pem) {
        var sets = new ArrayList<String>();
        var be = new boolean[]{false};
        var i = new AtomicInteger();
        Stream.of(pem.replaceAll("\\s+", "").split("-----"))
              .map(x -> new Indexed<>(i, x))
              .map(v -> v.mapIndex($ -> v.value().startsWith("BEGIN") ? 1 : v.value().startsWith("END") ? 2 : 0))
              .forEach(u -> {
                  if (be[0]) {
                      if (u.index() == 1) throw new IllegalStateException("not matched pem");
                      else if (u.index() == 0) sets.add(u.value());
                      else if (u.index() == 2) be[0] = false;
                  } else {
                      if (u.index() == 1) be[0] = true;
                      else if (u.index() == 2) throw new IllegalStateException("not matched pem");
                  }
              });
        if (log.isDebugEnabled()) log.info("segments in pem: {}", sets);
        return sets.stream();
    }

    static Future<List<Certificate>> readCertificatesFromPem(String pem) {
        return Future.future(p -> {
            var f = certFactory();
            p.complete(segmentsInPem(pem).map(v -> decodeCertificate(f, v)).toList());
        });
    }

    static Future<List<PrivateKey>> readPrivateKeysFromPem(String pem,
                                                           BiFunction<KeyFactory, String, PrivateKey> reader) {
        return Future.future(p -> {
            var f = rsaKeyFactory();
            p.complete(segmentsInPem(pem).map(v -> reader.apply(f, v)).toList());
        });
    }

    static Future<List<PublicKey>> readPublicKeysFromPem(String pem, BiFunction<KeyFactory, String, PublicKey> reader) {
        return Future.future(p -> {
            var f = rsaKeyFactory();
            p.complete(segmentsInPem(pem).map(v -> reader.apply(f, v)).toList());
        });
    }

    //region Signatures

    @SneakyThrows
    static String doSign(PrivateKey key, byte[] data) {
        var s = Signature.getInstance("SHA256withRSA");
        s.initSign(key);
        s.update(data);
        return Base64.getEncoder().encodeToString(s.sign());
    }

    @SneakyThrows
    static boolean doVerify(PublicKey key, byte[] data, String signature) {
        var s = Signature.getInstance("SHA256withRSA");
        s.initVerify(key);
        s.update(data);
        return s.verify(Base64.getDecoder().decode(signature));
    }

    @SneakyThrows
    static boolean doVerify(Certificate key, byte[] data, String signature) {
        var s = Signature.getInstance("SHA256withRSA");
        s.initVerify(key);
        s.update(data);
        return s.verify(Base64.getDecoder().decode(signature));
    }

    //endregion

    static Future<String> sign(Vertx vertx, PrivateKey privateKey, byte[] data) {
        return vertx.executeBlocking(() -> doSign(privateKey, data));
    }

    static Future<byte[]> encodeGCM(Vertx vertx, PrivateKey privateKey, byte[] data, byte[] nonce,
                                    byte @Nullable [] salt) {
        return vertx.executeBlocking(() -> doGCMEncrypt(privateKey, data, nonce, salt));
    }

    static Future<byte[]> decodeGCM(Vertx vertx, Certificate certificate, byte[] data, byte[] nonce,
                                    byte @Nullable [] salt) {
        return vertx.executeBlocking(() -> doGCMDecrypt(certificate, data, nonce, salt));
    }

    static Future<byte[]> encodeOAEP(Vertx vertx, Certificate certificate, byte[] data) {
        return vertx.executeBlocking(() -> doOAEPEncrypt(certificate, data));
    }

    static Future<byte[]> decodeOAEP(Vertx vertx, PrivateKey key, byte[] data) {
        return vertx.executeBlocking(() -> doOAEPDecrypt(key, data));
    }

    @SneakyThrows
    static byte[] doOAEPEncrypt(Certificate certificate, byte[] data) {
        var cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, certificate.getPublicKey());
        return cipher.doFinal(data);
    }

    @SneakyThrows
    static byte[] doOAEPDecrypt(PrivateKey key, byte[] data) {
        var cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    static byte[] doGCMEncrypt(PrivateKey privateKey, byte[] data, byte[] nonce, byte @Nullable [] salt) {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey, new GCMParameterSpec(128, nonce));
        if (salt != null) cipher.updateAAD(salt);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    static byte[] doGCMDecrypt(Certificate certificate, byte[] data, byte[] nonce, byte @Nullable [] salt) {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, certificate.getPublicKey(), new GCMParameterSpec(128, nonce));
        if (salt != null) cipher.updateAAD(salt);
        return cipher.doFinal(data);
    }

    static Future<Boolean> verify(Vertx vertx, Certificate certificate, byte[] data, String signature) {
        return vertx.executeBlocking(() -> doVerify(certificate, data, signature));
    }

    static Future<Boolean> verify(Vertx vertx, PublicKey publicKey, byte[] data, String signature) {
        return vertx.executeBlocking(() -> doVerify(publicKey, data, signature));
    }

    @SuppressWarnings("SameParameterValue")
    private static String translateECCrv(String crv, boolean toJdk) {
        return toJdk ?
                switch (crv) {
                    case "P-256" -> "secp256r1";
                    case "P-384" -> "secp384r1";
                    case "P-521" -> "secp521r1";
                    case "secp256k1" -> "secp256k1";
                    default -> throw new IllegalArgumentException("Unsupported {crv}: " + crv);
                }
                : switch (crv) {
            case "secp256r1" -> "P-256";
            case "secp384r1" -> "P-384";
            case "secp521r1" -> "P-521";
            case "secp256k1" -> "secp256k1";
            default -> throw new IllegalArgumentException("Unsupported {crv}: " + crv);
        };
    }

    String JWK_EC_CURVE_P256 = "P-256";
    String JWK_EC_CURVE_P384 = "P-384";
    String JWK_EC_CURVE_P521 = "P-521";
    String JWK_EC_CURVE_SECP256K1 = "secp256k1";

    private static byte[] toBytesUnsigned(final BigInteger bigInt) {
        var bitlen = bigInt.bitLength();
        bitlen = ((bitlen + 7) >> 3) << 3;
        final byte[] bigBytes = bigInt.toByteArray();
        if (((bigInt.bitLength() % 8) != 0) && (((bigInt.bitLength() / 8) + 1) == (bitlen / 8))) {
            return bigBytes;
        }

        var startSrc = 0;
        var len = bigBytes.length;
        if ((bigInt.bitLength() % 8) == 0) {
            startSrc = 1;
            len--;
        }

        var startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
        var resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    }


    private static String encodeCoordinate(final int fieldSize, final BigInteger coordinate) {
        var notPadded = toBytesUnsigned(coordinate);
        var bytesToOutput = (fieldSize + 7) / 8;
        if (notPadded.length >= bytesToOutput) {
            return BASE64_URL_ENC.apply(notPadded);
        }
        var padded = new byte[bytesToOutput];
        System.arraycopy(notPadded, 0, padded, bytesToOutput - notPadded.length, notPadded.length);
        return BASE64_URL_ENC.apply(padded);
    }

    @SneakyThrows
    static JsonObject genECJwk(@MagicConstant(valuesFromClass = Algorithm.class) String curve) {
        return encJwkEC(curve, genEC(curve));
    }

    @SneakyThrows
    static Map.Entry<PrivateKey, PublicKey> genEC(@MagicConstant(valuesFromClass = Algorithm.class) String curve) {
        var kg = KeyPairGenerator.getInstance("EC");
        kg.initialize(new ECGenParameterSpec(translateECCrv(curve, true)));
        var keyPair = kg.generateKeyPair();
        var pubKey = ((ECPublicKey) keyPair.getPublic());
        var priKey = ((ECPrivateKey) keyPair.getPrivate());
        return Map.entry(priKey, pubKey);
    }

    @SneakyThrows
    static JsonObject encJwkEC(@MagicConstant(valuesFromClass = Algorithm.class) String curve,
                               Map.Entry<PrivateKey, PublicKey> pair) {
        var priKey = (ECPrivateKey) pair.getKey();
        var pubKey = (ECPublicKey) pair.getValue();
        var d = encodeCoordinate(priKey.getParams().getCurve().getField().getFieldSize(), priKey.getS());
        var x = encodeCoordinate(pubKey.getParams().getCurve().getField().getFieldSize(), pubKey.getW().getAffineX());
        var y = encodeCoordinate(pubKey.getParams().getCurve().getField().getFieldSize(), pubKey.getW().getAffineY());
        return JsonObject.of(
                "crv", curve,
                "kty", "EC",
                "x", x,
                "y", y,
                "d", d
                            );
    }

    @SneakyThrows
    static Map.Entry<@Nullable PrivateKey, @Nullable PublicKey> parseECJwk(JsonObject jwk) {
        if (jwk.isEmpty()) throw new IllegalArgumentException("not valid jwk");
        if (!"EC".equals(jwk.getString("kty"))) throw new IllegalArgumentException("not valid EC jwk");
        var parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec(translateECCrv(jwk.getString("crv"), true)));
        var dec = Base64.getUrlDecoder();
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        if (jwk.containsKey("x") && jwk.containsKey("y")) {
            var x = new BigInteger(1, dec.decode(jwk.getString("x")));
            var y = new BigInteger(1, dec.decode(jwk.getString("y")));
            publicKey = KeyFactory.getInstance("EC").generatePublic(
                    new ECPublicKeySpec(new ECPoint(x, y), parameters.getParameterSpec(ECParameterSpec.class)));
        }
        if (jwk.containsKey("d")) {
            var d = new BigInteger(1, dec.decode(jwk.getString("x")));
            privateKey = KeyFactory.getInstance("EC").generatePrivate(
                    new ECPrivateKeySpec(d, parameters.getParameterSpec(ECParameterSpec.class)));
        }
        return new AbstractMap.SimpleImmutableEntry<>(privateKey, publicKey);
    }

}
