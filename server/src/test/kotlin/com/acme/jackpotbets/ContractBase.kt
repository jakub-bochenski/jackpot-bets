package com.acme.jackpotbets

import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.config.DecoderConfig
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpProtocolParams
import org.junit.BeforeClass
import java.net.URI

@Suppress("UtilityClassWithPublicConstructor", "UnnecessaryAbstractClass")
abstract class ContractBase {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            RestAssured.requestSpecification = RequestSpecBuilder()
                .setBaseUri(URI.create(System.getProperty("service.url")))
                .build()
                .config(
                    // we could also use a custom RestAssured logger here (the default one is too verbose)
                    // for now we use apache wire log, so let's make it readable
                    RestAssured.config()
                        .decoderConfig(
                            DecoderConfig.decoderConfig()
                                .noContentDecoders()
                        )
                        .httpClient(
                            RestAssured.config().httpClientConfig.httpClientFactory {
                                DefaultHttpClient().apply {
                                    HttpProtocolParams.setUserAgent(params, null)
                                }
                            }
                        )
                )
        }
    }
}
