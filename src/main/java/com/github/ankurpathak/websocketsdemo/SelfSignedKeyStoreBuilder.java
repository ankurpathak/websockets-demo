package com.github.ankurpathak.websocketsdemo;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SelfSignedKeyStoreBuilder {

    private String alias = "key";
    private char[] password = "changeit".toCharArray();
    private String commonName = "demo.local";
    private List<String> dnsNames = new ArrayList<>();
    private List<String> ipAddresses = new ArrayList<>();
    private int keySize = 2048;
    private long validityMillis = 31536000000L; // 1 year
    private String signatureAlgorithm = "SHA256withRSA";

    public SelfSignedKeyStoreBuilder alias(String alias) {
        this.alias = alias;
        return this;
    }

    public SelfSignedKeyStoreBuilder password(char[] password) {
        this.password = password;
        return this;
    }

    public SelfSignedKeyStoreBuilder commonName(String cn) {
        this.commonName = cn;
        return this;
    }

    public SelfSignedKeyStoreBuilder addDnsName(String dns) {
        this.dnsNames.add(dns);
        return this;
    }

    public SelfSignedKeyStoreBuilder addIpAddress(String ip) {
        this.ipAddresses.add(ip);
        return this;
    }

    public SelfSignedKeyStoreBuilder keySize(int keySize) {
        this.keySize = keySize;
        return this;
    }

    public SelfSignedKeyStoreBuilder validityDays(int days) {
        this.validityMillis = days * 24L * 60L * 60L * 1000L;
        return this;
    }

    public KeyStore build() throws Exception {

        if (password == null) {
            throw new IllegalStateException("Password must be provided");
        }

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // Generate KeyPair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        keyGen.initialize(keySize);
        KeyPair keyPair = keyGen.generateKeyPair();

        long now = System.currentTimeMillis();

        X500Name dn = new X500Name("CN=" + commonName);

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.getPrivate());

        // Build SAN entries
        List<GeneralName> sanList = new ArrayList<>();
        for (String dns : dnsNames) {
            sanList.add(new GeneralName(GeneralName.dNSName, dns));
        }
        for (String ip : ipAddresses) {
            sanList.add(new GeneralName(GeneralName.iPAddress, ip));
        }

        GeneralNames san = new GeneralNames(sanList.toArray(new GeneralName[0]));

        JcaX509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(
                        dn,
                        BigInteger.valueOf(now),
                        new Date(now),
                        new Date(now + validityMillis),
                        dn,
                        keyPair.getPublic());

        if (!sanList.isEmpty()) {
            certBuilder.addExtension(Extension.subjectAlternativeName, false, san);
        }

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(signer));

        // Create PKCS12 keystore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, password);
        ks.setKeyEntry(alias, keyPair.getPrivate(), password, new Certificate[]{cert});

        return ks;
    }
}