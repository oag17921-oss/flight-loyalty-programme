package com.loyalty;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoyaltyApp extends AbstractVerticle {
    private FxService fxService;
    private PromoService promoService;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start(Promise<Void> startPromise) {
        String fxUrl = config().getString("fxServiceUrl", "http://localhost:8081");
        String promoUrl = config().getString("promoServiceUrl", "http://localhost:8082");

        fxService = new FxService(vertx, fxUrl);
        promoService = new PromoService(vertx, promoUrl);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/v1/points/quote").handler(this::handleQuote);

        int port = config().getInteger("port", 8080);
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port)
            .onSuccess(server -> {
                System.out.println("Server listening on port " + port);
                startPromise.complete();
            })
            .onFailure(startPromise::fail);
    }

    private void handleQuote(RoutingContext ctx) {
        try {
            var req = mapper.readValue(ctx.body().asString(), QuoteRequest.class);
            PointsCalculator.validate(req);

            fxService.getRate(req.currency)
                .compose(fxRate -> {
                    if (req.promoCode != null && !req.promoCode.isBlank()) {
                        return promoService.getPromo(req.promoCode)
                            .map(promo -> new Object[]{fxRate, promo.bonusPercent, promo.expiresAt})
                            .recover(t -> {
                                return io.vertx.core.Future.succeededFuture(
                                    new Object[]{fxRate, null, null}
                                );
                            });
                    } else {
                        return io.vertx.core.Future.succeededFuture(
                            new Object[]{fxRate, null, null}
                        );
                    }
                })
                .onSuccess(data -> {
                    double fxRate = (double) ((Object[]) data)[0];
                    Integer promoPercent = (Integer) ((Object[]) data)[1];
                    Long promoExpiry = (Long) ((Object[]) data)[2];

                    var result = PointsCalculator.calculate(
                        req.fareAmount, fxRate, req.customerTier,
                        promoPercent, promoExpiry
                    );

                    var response = new QuoteResponse(
                        result.basePoints, result.tierBonus, result.promoBonus,
                        result.totalPoints, result.fxRate, result.warnings
                    );

                    try {
                        ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(200)
                            .end(mapper.writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onFailure(t -> {
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .setStatusCode(500)
                        .end(new JsonObject()
                            .put("error", t.getMessage())
                            .encode());
                });

        } catch (IllegalArgumentException e) {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(400)
                .end(new JsonObject()
                    .put("error", e.getMessage())
                    .encode());
        } catch (Exception e) {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(400)
                .end(new JsonObject()
                    .put("error", "Invalid request")
                    .encode());
        }
    }
}