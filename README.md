# Flight Loyalty Programme



This project is a flight loyalty system with a Java backend and Android/iOS apps. The backend works out how many loyalty points a customer earns when they book a flight.



## Backend



Built using Java and Vert.x.



### API



**POST** `/v1/points/quote`



Request:



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



### How the points are worked out



- The fare is converted into loyalty points.

- Extra points are added depending on the customer's tier.

- Valid promo codes add bonus points.

- The maximum number of points is 50,000.

- Fares of 0 or less are rejected.

- A warning is returned if a promo code is about to expire.



## Running the tests



Run all tests:



```bash

cd backend

mvn test

```



Run the unit tests:



```bash

mvn test -Dtest=PointsCalculatorTest

```



Run the integration tests:



```bash

mvn test -Dtest=PointsQuoteServiceTest

```



Generate a coverage report:



```bash

mvn clean test

```



The report is saved in:



```

backend/target/site/jacoco/index.html

```



## Android



Written in Kotlin.



Features:



- Email and password validation

- Account lockout after three failed login attempts

- Offline support

- Remember me

- Error messages



Main files:



- `LoginViewModel.kt`

- `LoginScreen.kt`

- `AuthRepository.kt`

- `NetworkMonitor.kt`



## iOS



Written in Swift with the same login features as the Android app.



Main files:



- `LoginViewModel.swift`

- `LoginView.swift`

- `AuthService.swift`

- `NetworkMonitor.swift`



## Getting started



Clone the repository and open the backend, Android or iOS project. Each folder contains the files needed to build and run that part of the application.