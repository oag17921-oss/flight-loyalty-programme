package com.loyalty;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class FxService {
    private WebClient client;
    private String fxServiceUrl;

    public FxService(Vertx vertx, String fxServiceUrl) {
        this.client = WebClient.create(vertx);
        this.fxServiceUrl = fxServiceUrl;
    }

    public Future<Double> getRate(String currency) {
        return Future.future(promise -> {
            client.get(fxServiceUrl)
                .setQueryParam("currency", currency)
                .send()
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        double rate = response.bodyAsJsonObject().getDouble("rate");
                        promise.complete(rate);
                    } else {
                        promise.fail("FX service returned " + response.statusCode());
                    }
                })
                .onFailure(promise::fail);
        });
    }
}