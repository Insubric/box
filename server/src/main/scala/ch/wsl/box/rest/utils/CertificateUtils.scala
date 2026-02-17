package ch.wsl.box.rest.utils

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}

import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.{Date, Locale}
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object CertificateUtils {
  def generateSelfSignedCertificate: KeyStore = {
    val keyPairGen = KeyPairGenerator.getInstance("RSA")
    keyPairGen.initialize(2048)
    val keyPair = keyPairGen.generateKeyPair()

    val subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic.getEncoded)

    val certBuilder = new X509v3CertificateBuilder(
      new X500Name("CN=localhost"),
      new java.math.BigInteger(64, new java.security.SecureRandom()),
      new Date(System.currentTimeMillis()),
      new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000),
      new X500Name("CN=localhost"),
      subPubKeyInfo
    )

    val signer: ContentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate)
    val cert: X509Certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(null, null)
    keyStore.setKeyEntry("selfsigned", keyPair.getPrivate, "password".toCharArray, Array(cert))

    keyStore
  }

  def sslContext:HttpsConnectionContext = {
    val keyStore = CertificateUtils.generateSelfSignedCertificate
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    keyManagerFactory.init(keyStore, "password".toCharArray)

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    trustManagerFactory.init(keyStore)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, null)

    ConnectionContext.httpsServer(sslContext)

  }
}
