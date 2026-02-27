package com.github.ankurpathak.websocketsdemo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
public final class PemKeyStoreBuilder {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private PemKeyStoreBuilder() {
        // prevent instantiation
    }

    /**
     * Builds a PKCS12 KeyStore from cert + key located in ~/certs
     */
    public static KeyStore fromHomeCerts(String alias,
                                         String password) throws Exception {

        String home = System.getProperty("user.home");

        Path certPath = Paths.get(home, "certs", "cert.crt");
        Path keyPath  = Paths.get(home, "certs", "cert.key");

        return build(certPath, keyPath, alias, password);
    }

    /**
     * Generic builder method
     */
    public static KeyStore build(Path certPath,
                                 Path keyPath,
                                 String alias,
                                 String password) throws Exception {

        // ----- Load Certificate -----
        X509Certificate certificate;
        try (PEMParser parser = new PEMParser(new FileReader(certPath.toFile()))) {

            X509CertificateHolder holder =
                    (X509CertificateHolder) parser.readObject();

            certificate = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(holder);
        }

        // ----- Load Private Key -----
        PrivateKey privateKey;
        try (PEMParser parser = new PEMParser(new FileReader(keyPath.toFile()))) {

            Object object = parser.readObject();
            JcaPEMKeyConverter converter =
                    new JcaPEMKeyConverter().setProvider("BC");

            if (object instanceof PEMKeyPair keyPair) {
                privateKey = converter.getKeyPair(keyPair).getPrivate();
            } else if (object instanceof PrivateKeyInfo keyInfo) {
                privateKey = converter.getPrivateKey(keyInfo);
            } else {
                throw new IllegalArgumentException("Unsupported key format");
            }
        }

        // ----- Create PKCS12 Keystore -----
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        keyStore.setKeyEntry(
                alias,
                privateKey,
                password.toCharArray(),
                new Certificate[]{certificate}
        );

        return keyStore;
    }
}