# Flight Loyalty Programme

A loyalty points system for flight bookings with backend API, Android app, and iOS app.

## What This Does

When someone books a flight, the app calculates loyalty points earned based on fare amount, customer tier, and promo codes.

## Backend (Java + Vert.x)

HTTP service for points calculation.

### API

Send a POST request to `/v1/points/quote`:

```json
{
  "fareAmount": 1234.50,
  "currency": "USD",
  "cabinClass": "ECONOMY",
  "customerTier": "SILVER",
  "promoCode": "SUMMER25"
}
```

Response:

```json
{
  "basePoints": 1234,
  "tierBonus": 185,
  "promoBonus": 308,
  "totalPoints": 1727,
  "effectiveFxRate": 3.67,
  "warnings": ["PROMO_EXPIRES_SOON"]
}
```

### Calculation Rules

- Convert fare to base points using exchange rate
- Apply tier bonus (SILVER: 15%, GOLD: 30%, PLATINUM: 50%)
- Apply promo bonuses for valid codes
- Cap total at 50,000 points
- Reject fares of 0 or less
- Warn if promo expires soon

### Running Tests

Navigate to `backend` folder:

**Unit tests:**
```bash
cd backend
mvn test -Dtest=PointsCalculatorTest
```

**Integration tests:**
```bash
cd backend
mvn test -Dtest=PointsQuoteServiceTest
```

**All tests:**
```bash
cd backend
mvn test
```

**Coverage report:**
```bash
cd backend
mvn clean test
```

View coverage at: `backend/target/site/jacoco/index.html`

## Mobile Apps

### Android (Kotlin)

Login screen with:
- Email and password validation
- Account lockout after 3 failed attempts
- Offline support
- "Remember me" token persistence
- Error messages

**Files:**
- `LoginViewModel.kt` - business logic
- `LoginScreen.kt` - UI
- `AuthRepository.kt` - API communication
- `NetworkMonitor.kt` - connectivity detection

### iOS (Swift)

Same functionality as Android.

**Files:**
- `LoginViewModel.swift`
- `LoginView.swift`
- `AuthService.swift`
- `NetworkMonitor.swift`

## Getting Started

Clone the repo and refer to each component's folder for setup instructions.
