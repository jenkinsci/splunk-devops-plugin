package com.splunk.splunkjenkins.utils;

import org.apache.commons.lang.StringUtils;
import shaded.splk.org.apache.http.HttpHost;
import shaded.splk.org.apache.http.conn.ssl.NoopHostnameVerifier;
import shaded.splk.org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import shaded.splk.org.apache.http.conn.ssl.TrustStrategy;
import shaded.splk.org.apache.http.protocol.HttpContext;
import shaded.splk.org.apache.http.ssl.SSLContexts;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import static java.nio.charset.StandardCharsets.UTF_8;

import static com.splunk.splunkjenkins.utils.MultipleHostResolver.NAME_DELIMITER;

public class CustomSSLConnectionSocketFactory extends SSLConnectionSocketFactory {
    public static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(CustomSSLConnectionSocketFactory.class.getName());

    public static SSLConnectionSocketFactory getSocketFactory(boolean verifyCA, String certificate) {
        if (!verifyCA) {
            SSLContext sslContext = null;
            try {
                TrustStrategy acceptingTrustStrategy = new TrustAllStrategy();
                sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "init custom ssl context with TrustAllStrategy failed", e);
                sslContext = SSLContexts.createDefault();
            }
            return new CustomSSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        }
        // tls verify is on
        SSLContext sslContext;
        if (StringUtils.isNotBlank(certificate)) {
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                X509Certificate customCA = textToX509Cert(certificate);
                trustStore.setCertificateEntry("splunk-http-events-ca", customCA);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                TrustManager[] trustManagers = tmf.getTrustManagers();
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, null);
            } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException ex) {
                LOG.log(Level.WARNING, "init custom ssl context failed", ex);
                //invalid CA or keystore error
                sslContext = SSLContexts.createDefault();
            }
        } else {
            sslContext = SSLContexts.createDefault();
        }
        return new CustomSSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    }

    public CustomSSLConnectionSocketFactory(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        super(sslContext, new String[]{"TLSv1.3", "TLSv1.2"}, null, hostnameVerifier);
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
        if (host.getHostName().contains(NAME_DELIMITER)) {
            HttpHost resolvedHost = new HttpHost(remoteAddress.getHostName(), host.getPort(), host.getSchemeName());
            return super.connectSocket(connectTimeout, socket, resolvedHost, remoteAddress, localAddress, context);
        } else {
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
    }

    public static X509Certificate textToX509Cert(String permCert) throws CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(permCert.getBytes(UTF_8)));
        return certificate;
    }

    static class TrustAllStrategy implements TrustStrategy {
        public boolean isTrusted(X509Certificate[] certificate,
                                 String type) {
            return true;
        }
    }
}
