package com.loyalty;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class PointsQuoteServiceTest {
    private WireMockServer fxServer;
    private WireMockServer promoServer;
    private WebClient client;
    private int serverPort;

    @BeforeEach
    public void setup(Vertx vertx, VertxTestContext ctx) {
        fxServer = new WireMockServer(8081);
        promoServer = new WireMockServer(8082);
        fxServer.start();
        promoServer.start();

        client = WebClient.create(vertx);

        serverPort = findFreePort();

        var config = new JsonObject()
            .put("port", serverPort)
            .put("fxServiceUrl", "http://localhost:8081")
            .put("promoServiceUrl", "http://localhost:8082");

        vertx.deployVerticle(new LoyaltyApp(), new io.vertx.core.DeploymentOptions()
            .setConfig(config), ctx.succeedingThenComplete());
    }

    @AfterEach
    public void cleanup() {
        fxServer.stop();
        promoServer.stop();
    }

    @Test
    public void testBasicPointsCalculation(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=USD"))
            .willReturn(okJson("{\"rate\": 3.67}")));

        var request = new JsonObject()
            .put("fareAmount", 1234.50)
            .put("currency", "USD")
            .put("cabinClass", "ECONOMY")
            .put("customerTier", "SILVER")
            .put("promoCode", "");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    var response = ar.result().bodyAsJsonObject();

                    assertThat(response.getInteger("basePoints")).isEqualTo(4531);
                    assertThat(response.getInteger("tierBonus")).isGreaterThan(0);
                    assertThat(response.getInteger("totalPoints")).isGreaterThan(0);
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testWithPromoCode(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=USD"))
            .willReturn(okJson("{\"rate\": 3.67}")));

        promoServer.stubFor(get(urlEqualTo("/?code=SUMMER25"))
            .willReturn(okJson("{\"bonusPercent\": 25, \"expiresAt\": " + 
                (System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000) + "}")));

        var request = new JsonObject()
            .put("fareAmount", 1000.00)
            .put("currency", "USD")
            .put("cabinClass", "BUSINESS")
            .put("customerTier", "GOLD")
            .put("promoCode", "SUMMER25");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    var response = ar.result().bodyAsJsonObject();

                    assertThat(response.getInteger("promoBonus")).isGreaterThan(0);
                    assertThat(response.getJsonArray("warnings")).isEmpty();
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testPromoExpiryWarning(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=USD"))
            .willReturn(okJson("{\"rate\": 3.67}")));
        //Todo: review the requirements
        long expiryTime = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000;
        promoServer.stubFor(get(urlEqualTo("/?code=EXPIRE_SOON"))
            .willReturn(okJson("{\"bonusPercent\": 10, \"expiresAt\": " + expiryTime + "}")));

        var request = new JsonObject()
            .put("fareAmount", 500.00)
            .put("currency", "USD")
            .put("cabinClass", "ECONOMY")
            .put("customerTier", "NONE")
            .put("promoCode", "EXPIRE_SOON");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    var response = ar.result().bodyAsJsonObject();
                    var warnings = response.getJsonArray("warnings");

                    assertThat(warnings.getList()).contains("PROMO_EXPIRES_SOON");
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testPointsCap(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=USD"))
            .willReturn(okJson("{\"rate\": 100.0}")));

        promoServer.stubFor(get(urlEqualTo("/?code=BIG_PROMO"))
            .willReturn(okJson("{\"bonusPercent\": 100, \"expiresAt\": " + 
                (System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000) + "}")));

        //TODO: review
        var request = new JsonObject()
            .put("fareAmount", 200.00)
            .put("currency", "USD")
            .put("cabinClass", "FIRST")
            .put("customerTier", "PLATINUM")
            .put("promoCode", "BIG_PROMO");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    var response = ar.result().bodyAsJsonObject();

                    assertThat(response.getInteger("totalPoints")).isLessThanOrEqualTo(50000);
                    var warnings = response.getJsonArray("warnings");
                    assertThat(warnings.getList()).contains("POINTS_CAPPED");
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testValidationRejectNegativeFare(VertxTestContext ctx) {
        var request = new JsonObject()
            .put("fareAmount", -100.00)
            .put("currency", "USD")
            .put("cabinClass", "ECONOMY")
            .put("customerTier", "SILVER")
            .put("promoCode", "");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    assertThat(ar.result().statusCode()).isEqualTo(400);
                    var response = ar.result().bodyAsJsonObject();
                    assertThat(response.getString("error")).contains("Fare");
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testValidationRejectInvalidCabin(VertxTestContext ctx) {
        var request = new JsonObject()
            .put("fareAmount", 1000.00)
            .put("currency", "USD")
            .put("cabinClass", "LUXURY")
            .put("customerTier", "SILVER")
            .put("promoCode", "");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    assertThat(ar.result().statusCode()).isEqualTo(400);
                    var response = ar.result().bodyAsJsonObject();
                    assertThat(response.getString("error")).contains("cabin");
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testTierMultipliers(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=USD"))
            .willReturn(okJson("{\"rate\": 1.0}")));

        var request = new JsonObject()
            .put("fareAmount", 1000.00)
            .put("currency", "USD")
            .put("cabinClass", "ECONOMY")
            .put("customerTier", "PLATINUM")
            .put("promoCode", "");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    var response = ar.result().bodyAsJsonObject();

                    int base = response.getInteger("basePoints");
                    int tier = response.getInteger("tierBonus");

                    // TODO: Review the requirements
                    assertThat(tier).isEqualTo((int)(base * 0.50));
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testFxServiceFailureHandling(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=UNKNOWN"))
            .willReturn(serverError()));

        var request = new JsonObject()
            .put("fareAmount", 1000.00)
            .put("currency", "UNKNOWN")
            .put("cabinClass", "ECONOMY")
            .put("customerTier", "SILVER")
            .put("promoCode", "");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    assertThat(ar.result().statusCode()).isEqualTo(500);
                });
                ctx.completeNow();
            });
    }

    @Test
    public void testPromoServiceFailureContinues(VertxTestContext ctx) {
        fxServer.stubFor(get(urlEqualTo("/?currency=USD"))
            .willReturn(okJson("{\"rate\": 3.67}")));

        promoServer.stubFor(get(urlEqualTo("/?code=BADCODE"))
            .willReturn(notFound()));

        var request = new JsonObject()
            .put("fareAmount", 1000.00)
            .put("currency", "USD")
            .put("cabinClass", "ECONOMY")
            .put("customerTier", "SILVER")
            .put("promoCode", "BADCODE");

        client.post(serverPort, "localhost", "/v1/points/quote")
            .sendJson(request, ar -> {
                ctx.verify(() -> {
                    assertThat(ar.succeeded()).isTrue();
                    assertThat(ar.result().statusCode()).isEqualTo(200);
                    var response = ar.result().bodyAsJsonObject();
                    assertThat(response.getInteger("promoBonus")).isEqualTo(0);
                });
                ctx.completeNow();
            });
    }

    private int findFreePort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 8080;
        }
    }
}