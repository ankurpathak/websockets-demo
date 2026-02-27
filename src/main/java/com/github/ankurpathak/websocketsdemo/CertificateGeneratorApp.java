package com.github.ankurpathak.websocketsdemo;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CertificateGeneratorApp {

    private static final Logger logger =
            Logger.getLogger(CertificateGeneratorApp.class.getName());

    public static void main(String[] args) {

        try {
            logger.info("Starting certificate generation...");

            // 🔹 Build keystore
            KeyStore keyStore = new SelfSignedKeyStoreBuilder()
                    .alias("key")
                    .commonName("demo.local")
                    .addDnsName("demo.local")
                    .addDnsName("localhost")
                    .addIpAddress("127.0.0.1")
                    .validityDays(365)
                    .build();

            char[] password = "changeit".toCharArray();

            // 🔹 Output directory (project root)
            Path outputDir = Paths.get(System.getProperty("user.dir"), "target");
            Files.createDirectories(outputDir);

            logger.info("Output directory: " + outputDir.toAbsolutePath());

            // 🔹 Save PKCS12 keystore
            Path p12Path = outputDir.resolve("keystore.p12");
            try (OutputStream os = new FileOutputStream(p12Path.toFile())) {
                keyStore.store(os, password);
            }
            logger.info("Generated keystore.p12");

            // 🔹 Extract private key & certificate
            Enumeration<String> aliases = keyStore.aliases();
            String alias = aliases.nextElement();

            PrivateKey privateKey =
                    (PrivateKey) keyStore.getKey(alias, password);

            X509Certificate certificate =
                    (X509Certificate) keyStore.getCertificate(alias);

            // 🔹 Write cert.cert
            try (JcaPEMWriter writer =
                         new JcaPEMWriter(
                                 Files.newBufferedWriter(outputDir.resolve("cert.crt")))) {
                writer.writeObject(certificate);
            }
            logger.info("Generated cert.crt");

            // 🔹 Write cert.key
            try (JcaPEMWriter writer =
                         new JcaPEMWriter(
                                 Files.newBufferedWriter(outputDir.resolve("cert.key")))) {
                writer.writeObject(privateKey);
            }
            logger.info("Generated cert.key");

            logger.info("Certificate generation completed successfully.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Certificate generation failed", e);
        }
    }
}