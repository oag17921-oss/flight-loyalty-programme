package com.loyalty;

public class QuoteRequest {
    public double fareAmount;
    public String currency;
    public String cabinClass;
    public String customerTier;
    public String promoCode;

    public QuoteRequest() {
    }

    public QuoteRequest(double fareAmount, String currency, String cabinClass,
                       String customerTier, String promoCode) {
        this.fareAmount = fareAmount;
        this.currency = currency;
        this.cabinClass = cabinClass;
        this.customerTier = customerTier;
        this.promoCode = promoCode;
    }
}