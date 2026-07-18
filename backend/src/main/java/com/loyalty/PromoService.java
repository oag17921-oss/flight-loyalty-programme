package com.loyalty;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class PromoService {
    private WebClient client;
    private String promoServiceUrl;

    public PromoService(Vertx vertx, String promoServiceUrl) {
        this.client = WebClient.create(vertx);
        this.promoServiceUrl = promoServiceUrl;
    }

    public Future<PromoInfo> getPromo(String promoCode) {
        return Future.future(promise -> {
            client.get(promoServiceUrl)
                .setQueryParam("code", promoCode)
                .send()
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        var body = response.bodyAsJsonObject();
                        var promo = new PromoInfo(
                            body.getInteger("bonusPercent"),
                            body.getLong("expiresAt")
                        );
                        promise.complete(promo);
                    } else {
                        promise.fail("Promo not found");
                    }
                })
                .onFailure(promise::fail);
        });
    }

    public static class PromoInfo {
        public int bonusPercent;
        public long expiresAt;

        public PromoInfo(int bonusPercent, long expiresAt) {
            this.bonusPercent = bonusPercent;
            this.expiresAt = expiresAt;
        }

        public boolean expiresWithin(long milliseconds) {
            long now = System.currentTimeMillis();
            return expiresAt - now < milliseconds;
        }
    }
}