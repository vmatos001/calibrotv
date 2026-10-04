package com.example.calibretv.data.opds

import java.net.HttpURLConnection
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * SSL Helper SELECTIVO para CalibroTV.
 * - IPs privadas (192.168.x.x, 10.x.x.x, 172.16.x.x, localhost): bypass SSL
 *   para soportar certificados autofirmados en servidores domésticos.
 * - Todo lo demás: usa el TrustManager del sistema (seguro por defecto).
 */
object SslHelper {

    private val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    private val permissiveSslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }
    }

    private val permissiveHostnameVerifier = HostnameVerifier { _, _ -> true }

    private fun isPrivateHost(host: String): Boolean {
        return try {
            val addr = InetAddress.getByName(host)
            addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
        } catch (_: Exception) {
            // Seguro por defecto: si no resuelve, NO se omite la validación TLS (evita MITM en dominios públicos)
            false
        }
    }

    fun configureHttps(connection: HttpURLConnection) {
        if (connection is HttpsURLConnection) {
            try {
                val host = connection.url.host
                if (isPrivateHost(host)) {
                    // Solo bypass para servidores domésticos locales
                    connection.sslSocketFactory = permissiveSslContext.socketFactory
                    connection.hostnameVerifier = permissiveHostnameVerifier
                }
                // Para hosts externos, usa el TrustManager del sistema por defecto
            } catch (_: Exception) {}
        }
    }
}
