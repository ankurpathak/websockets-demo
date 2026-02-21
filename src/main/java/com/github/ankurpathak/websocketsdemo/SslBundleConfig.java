package com.github.ankurpathak.websocketsdemo;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

@Configuration
public class SslBundleConfig {

    private static final Logger log = LoggerFactory.getLogger(SslBundleConfig.class);

    private static final String BUNDLE_NAME = "server";
    private static final String ALIAS = "key";
    private static final String PASSWORD = "changeit";

    @Value("${server.port:8443}")
    private String serverPort;

    @Bean
    public SslBundleRegistrar dynamicSslRegistrar() {
        return (registry) -> {
            try {
                log.info("Registering dynamic SSL for port: {}", serverPort);

                // Use the static method to create the KeyStore
                KeyStore keyStore = create(ALIAS, PASSWORD.toCharArray());

                SslStoreBundle storeBundle = SslStoreBundle.of(keyStore, PASSWORD, null);
                SslBundleKey bundleKey = SslBundleKey.of(PASSWORD, ALIAS);

                registry.registerBundle(BUNDLE_NAME, SslBundle.of(storeBundle, bundleKey));
                log.info("✅ Bundle '{}' registered.", BUNDLE_NAME);

            } catch (Exception e) {
                log.error("❌ Bundle registration failed", e);
                throw new RuntimeException(e);
            }
        };
    }

    public static KeyStore create(String alias, char[] password) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        keyGen.initialize(256);
        KeyPair keyPair = keyGen.generateKeyPair();

        long now = System.currentTimeMillis();

        // --- RESTORED YOUR CN NAME ---
        X500Name dn = new X500Name("CN=demo.local");

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());

        // Added both to SAN for maximum flexibility
        GeneralNames san = new GeneralNames(new GeneralName[] {
                new GeneralName(GeneralName.dNSName, "demo.local"),
                new GeneralName(GeneralName.dNSName, "localhost"),
                new GeneralName(GeneralName.iPAddress, "127.0.0.1")
        });

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                dn, BigInteger.valueOf(now), new Date(now), new Date(now + 31536000000L), dn, keyPair.getPublic());
        builder.addExtension(Extension.subjectAlternativeName, false, san);

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));

        // Detect port to write Nginx files only once
        String currentPort = System.getProperty("SERVER_PORT", "8443");
        if ("8443".equals(currentPort)) {
            exportToPem(keyPair.getPrivate(), cert);
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, password);
        ks.setKeyEntry(alias, keyPair.getPrivate(), password, new Certificate[]{cert});

        return ks;
    }

    private static void exportToPem(PrivateKey privateKey, X509Certificate cert) throws Exception {
        Path root = Paths.get("").toAbsolutePath();
        Path probe = root;
        while (probe != null) {
            if (probe.resolve("pom.xml").toFile().exists()) {
                root = probe;
                break;
            }
            probe = probe.getParent();
        }
        File crtFile = root.resolve("cert.crt").toFile();
        File keyFile = root.resolve("cert.key").toFile();

        log.info("📂 Exporting Nginx certificates for demo.local...");
        log.info("📍 Certificate Path: {}", crtFile.getAbsolutePath());
        log.info("📍 Private Key Path: {}", keyFile.getAbsolutePath());

        try (JcaPEMWriter crtWriter = new JcaPEMWriter(new FileWriter(crtFile));
             JcaPEMWriter keyWriter = new JcaPEMWriter(new FileWriter(keyFile))) {

            crtWriter.writeObject(cert);
            keyWriter.writeObject(privateKey);
            log.info("✅ PEM files written successfully to project root.");
        }
    }
}
