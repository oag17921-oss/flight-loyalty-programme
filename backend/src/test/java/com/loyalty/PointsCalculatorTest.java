package com.loyalty;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class PointsCalculatorTest {

    @Test
    public void testBasicCalculation() {
        var result = PointsCalculator.calculate(1000.0, 1.0, "NONE", null, null);
        
        assertThat(result.basePoints).isEqualTo(1000);
        assertThat(result.tierBonus).isEqualTo(0);
        assertThat(result.totalPoints).isEqualTo(1000);
    }

    @Test
    public void testSilverTier() {
        var result = PointsCalculator.calculate(1000.0, 1.0, "SILVER", null, null);
        
        assertThat(result.tierBonus).isEqualTo(150); // 1000 * 0.15
        assertThat(result.totalPoints).isEqualTo(1150);
    }

    @Test
    public void testGoldTier() {
        var result = PointsCalculator.calculate(1000.0, 1.0, "GOLD", null, null);
        
        assertThat(result.tierBonus).isEqualTo(300); // 1000 * 0.30
        assertThat(result.totalPoints).isEqualTo(1300);
    }

    @Test
    public void testPlatinumTier() {
        var result = PointsCalculator.calculate(1000.0, 1.0, "PLATINUM", null, null);
        
        assertThat(result.tierBonus).isEqualTo(500); // 1000 * 0.50
        assertThat(result.totalPoints).isEqualTo(1500);
    }

    @Test
    public void testWithPromoBonus() {
        var result = PointsCalculator.calculate(1000.0, 1.0, "SILVER", 25, 
            System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000);
        
        int expected = (int) ((1000 + 150) * 0.25); // (base + tier) * promo%
        assertThat(result.promoBonus).isEqualTo(expected);
    }

    @Test
    public void testPointsCap() {
        var result = PointsCalculator.calculate(50000.0, 10.0, "PLATINUM", 100, 
            System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000);
        
        assertThat(result.totalPoints).isEqualTo(50000);
        assertThat(result.warnings).contains("POINTS_CAPPED");
    }

    @Test
    public void testPromoExpirySoon() {
        long expiryTime = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000; // 3 days
        var result = PointsCalculator.calculate(1000.0, 1.0, "NONE", 10, expiryTime);
        
        assertThat(result.warnings).contains("PROMO_EXPIRES_SOON");
    }

    @Test
    public void testPromoExpiryNotSoon() {
        long expiryTime = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000; // 30 days
        var result = PointsCalculator.calculate(1000.0, 1.0, "NONE", 10, expiryTime);
        
        assertThat(result.warnings).doesNotContain("PROMO_EXPIRES_SOON");
    }

    @Test
    public void testValidateFareZero() {
        var req = new QuoteRequest(0, "USD", "ECONOMY", "SILVER", "");
        assertThatThrownBy(() -> PointsCalculator.validate(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fare");
    }

    @Test
    public void testValidateNegativeFare() {
        var req = new QuoteRequest(-100, "USD", "ECONOMY", "SILVER", "");
        assertThatThrownBy(() -> PointsCalculator.validate(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fare");
    }

    @Test
    public void testValidateInvalidCabin() {
        var req = new QuoteRequest(1000, "USD", "LUXURY", "SILVER", "");
        assertThatThrownBy(() -> PointsCalculator.validate(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cabin");
    }

    @Test
    public void testValidateValidCabins() {
        assertDoesNotThrow(() -> PointsCalculator.validate(
            new QuoteRequest(1000, "USD", "ECONOMY", "SILVER", "")));
        assertDoesNotThrow(() -> PointsCalculator.validate(
            new QuoteRequest(1000, "USD", "BUSINESS", "SILVER", "")));
        assertDoesNotThrow(() -> PointsCalculator.validate(
            new QuoteRequest(1000, "USD", "FIRST", "SILVER", "")));
    }

    @Test
    public void testFxRateConversion() {
        var result = PointsCalculator.calculate(500.0, 3.67, "NONE", null, null);
        
        assertThat(result.basePoints).isEqualTo((int)(500 * 3.67));
        assertThat(result.fxRate).isEqualTo(3.67);
    }

    private void assertDoesNotThrow(org.junit.jupiter.api.function.Executable executable) {
        try {
            executable.execute();
        } catch (Throwable e) {
            throw new AssertionError("Expected no exception but got " + e);
        }
    }
}