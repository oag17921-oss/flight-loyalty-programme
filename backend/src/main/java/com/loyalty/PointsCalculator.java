package com.loyalty;

import java.util.ArrayList;
import java.util.List;

public class PointsCalculator {
    private static final int POINTS_CAP = 50000;
    private static final long EXPIRY_WARNING_MS = 7 * 24 * 60 * 60 * 1000; // TODO: review

    public static class Result {
        public int basePoints;
        public int tierBonus;
        public int promoBonus;
        public int totalPoints;
        public double fxRate;
        public List<String> warnings;

        public Result(int basePoints, int tierBonus, int promoBonus,
                     int totalPoints, double fxRate, List<String> warnings) {
            this.basePoints = basePoints;
            this.tierBonus = tierBonus;
            this.promoBonus = promoBonus;
            this.totalPoints = totalPoints;
            this.fxRate = fxRate;
            this.warnings = warnings;
        }
    }

    public static Result calculate(double fareAmount, double fxRate, String tier,
                                   Integer promoPercent, Long promoExpiry) {
        List<String> warnings = new ArrayList<>();

        int basePoints = (int) (fareAmount * fxRate);

        int tierBonus = calculateTierBonus(basePoints, tier);

        int promoBonus = 0;
        if (promoPercent != null) {
            promoBonus = (int) ((basePoints + tierBonus) * promoPercent / 100.0);

            if (promoExpiry != null) {
                long now = System.currentTimeMillis();
                if (promoExpiry - now < EXPIRY_WARNING_MS) {
                    warnings.add("PROMO_EXPIRES_SOON");
                }
            }
        }
        
        int total = basePoints + tierBonus + promoBonus;
        if (total > POINTS_CAP) {
            total = POINTS_CAP;
            warnings.add("POINTS_CAPPED");
        }

        return new Result(basePoints, tierBonus, promoBonus, total, fxRate, warnings);
    }

    private static int calculateTierBonus(int basePoints, String tier) {
        double multiplier = getTierMultiplier(tier);
        return (int) (basePoints * multiplier);
    }

    private static double getTierMultiplier(String tier) {
        return switch (tier) {
            case "SILVER" -> 0.15;
            case "GOLD" -> 0.30;
            case "PLATINUM" -> 0.50;
            default -> 0.0;
        };
    }

    public static void validate(QuoteRequest req) throws IllegalArgumentException {
        if (req.fareAmount <= 0) {
            throw new IllegalArgumentException("Fare must be greater than 0");
        }
        if (req.currency == null || req.currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (req.cabinClass == null || req.cabinClass.isBlank()) {
            throw new IllegalArgumentException("Cabin class is required");
        }
        if (!isValidCabinClass(req.cabinClass)) {
            throw new IllegalArgumentException("Invalid cabin class: " + req.cabinClass);
        }
    }

    private static boolean isValidCabinClass(String cabin) {
        return cabin.matches("^(ECONOMY|BUSINESS|FIRST)$");
    }
}