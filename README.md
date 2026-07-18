# Flight Loyalty Programme

A simple loyalty points system for flight bookings. Got three parts: backend API, Android app, and iOS app.

## What This Does

When someone books a flight, the app calculates how many loyalty points they should earn. The points depend on how much they paid, what tier they're on, and if there's a promo code.

## Backend (Java + Vert.x)

The backend is an HTTP service that does the points calculation.

### How to Use It

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

You'll get back:

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

### The Rules

- Convert the fare to base points using the exchange rate
- Add a tier bonus (SILVER gets 15%, GOLD gets 30%, PLATINUM gets 50%)
- Add promo bonuses if there's a valid promo code
- Cap the total at 50,000 points max
- Reject any booking with 0 or less fare
- If a promo is about to expire, warn the user

### How to Run Tests

Go into the `backend` folder and run the tests. Tests are in two files:

**Unit tests** (fast, no server needed):
```bash
cd backend
mvn test -Dtest=PointsCalculatorTest
```

**Integration tests** (spins up a real server with fake services):
```bash
mvn test -Dtest=PointsQuoteServiceTest
```

**Run all tests**:
```bash
mvn test
```

**Check test coverage** (must be at least 80%):
```bash
mvn clean test
```

After tests run, look at the coverage report:
```
backend/target/site/jacoco/index.html
```

### What the Tests Check

- Does the calculation work right?
- Does it handle when services go down?
- Does it reject bad data?
- Do tier bonuses work?
- Does the promo expiry warning show up?
- Does it cap points at 50,000?
- Does the FX service work?

## Mobile Apps

### Android (Kotlin)

The Android app has a login screen where users type in their credentials.

**What the login screen does:**
- Validates email and password as the user types
- Locks the account after 3 failed attempts
- Works offline (shows a message instead of trying to connect)
- Has a "remember me" checkbox that saves the token
- Shows proper error messages

**The files:**
- `LoginViewModel.kt` - handles the logic
- `LoginScreen.kt` - the UI
- `AuthRepository.kt` - talks to the server
- `NetworkMonitor.kt` - checks if there's internet

**Testing:**
- Make sure the button only works when email and password are valid
- Check that it asks for a navigation event on success
- Check that it counts failures properly
- Check that it locks after 3 tries
- Check that it works offline
- Check that "remember me" actually saves the token

### iOS (Swift)

Same as Android but for iPhone.

**The files:**
- `LoginViewModel.swift` - handles the logic
- `LoginView.swift` - the UI
- `AuthService.swift` - talks to the server
- `NetworkMonitor.swift` - checks if there's internet

**Testing:**
- Same tests as Android but using XCTest

## Setup

Clone the repo and open it in your IDE. Each part (backend, Android, iOS) has its own folder with instructions.

## What We're Checking

When we grade this, we look at:
- Does it work without bugs?
- Is the code clean and easy to understand?
- Are the tests good and thorough?
- Is there enough documentation?
- Did you handle errors properly?
- Can it survive if a service goes down (like the promo service)?

That's it. Pretty straightforward.
