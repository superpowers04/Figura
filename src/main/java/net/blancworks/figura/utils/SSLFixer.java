package net.blancworks.figura.utils;

import net.fabricmc.loader.api.FabricLoader;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.*;

//Okay, so. If anyone's curious what this is, in the future.
//tl;dr is that the bundled Java version of Minecraft doesn't come with the local certificates for trusting Let's Encrypt.
//Figura's backend uses Let's Encrypt as the SSL
//So, to avoid forcing every user who uses Figura to use newer versions of Java, we manually included the certificate for the backend server
//and trust it in this class.

//If Mojang updates the bundled java version to 1.8u101 or higher, we should nuke this class.
//Otherwise, keep it. The mod will not work without it.
public class SSLFixer {
    // BEGIN ------- ADDME
    static {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            Path ksPath = Paths.get(System.getProperty("java.home"),
                    "lib", "security", "cacerts");
            keyStore.load(Files.newInputStream(ksPath),
                    "changeit".toCharArray());

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            Path p = FabricLoader.getInstance().getModContainer("figura").get().getRootPath().resolve("FiguraNewCertificate.cer");

            InputStream caInput = Files.newInputStream(p);

            
            try {
                Certificate crt = cf.generateCertificate(caInput);
                System.out.println("Added Cert for " + ((X509Certificate) crt)
                        .getSubjectDN());

                keyStore.setCertificateEntry("DSTRootCAX3", crt);
            } catch (Exception e){
                e.printStackTrace();
            }

            caInput.close();

            if (false) { // enable to see
                System.out.println("Truststore now trusting: ");
                PKIXParameters params = new PKIXParameters(keyStore);
                params.getTrustAnchors().stream()
                        .map(TrustAnchor::getTrustedCert)
                        .map(X509Certificate::getSubjectDN)
                        .forEach(System.out::println);
                System.out.println();
            }

            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            SSLContext.setDefault(sslContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // END ---------- ADDME

    public static void main() throws IOException {
        // signed by default trusted CAs.
        testUrl(new URL("https://google.com"));
        testUrl(new URL("https://www.thawte.com"));

        // signed by letsencrypt
        testUrl(new URL("https://helloworld.letsencrypt.org"));
        // signed by LE's cross-sign CA
        testUrl(new URL("https://letsencrypt.org"));
        // expired
        //testUrl(new URL("https://tv.eurosport.com/"));
        // self-signed
        //testUrl(new URL("https://www.pcwebshop.co.uk/"));

    }

    static void testUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        try {
            connection.connect();
            System.out.println("Headers of " + url + " => "
                    + connection.getHeaderFields());
        } catch (SSLHandshakeException e) {
            System.out.println("Untrusted: " + url);
        }
    }
}
