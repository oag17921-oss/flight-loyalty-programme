package com.loyalty;

import java.util.List;

public class QuoteResponse {
    public int basePoints;
    public int tierBonus;
    public int promoBonus;
    public int totalPoints;
    public double effectiveFxRate;
    public List<String> warnings;

    public QuoteResponse(int basePoints, int tierBonus, int promoBonus,
                        int totalPoints, double effectiveFxRate, List<String> warnings) {
        this.basePoints = basePoints;
        this.tierBonus = tierBonus;
        this.promoBonus = promoBonus;
        this.totalPoints = totalPoints;
        this.effectiveFxRate = effectiveFxRate;
        this.warnings = warnings;
    }
}