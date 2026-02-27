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
               /* KeyStore keyStore = new SelfSignedKeyStoreBuilder()
                        .alias(ALIAS)
                        .password(PASSWORD.toCharArray())
                        .commonName("demo.local")
                        .addDnsName("demo.local")
                        .addDnsName("localhost")
                        .addIpAddress("127.0.0.1")
                        .validityDays(365)
                        .build(); */

                KeyStore keyStore = PemKeyStoreBuilder.fromHomeCerts(ALIAS, PASSWORD);

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


}
