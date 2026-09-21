# Zentrixa Ads SDK Example

This standalone Android application demonstrates how to integrate and use Zentrixa Ads SDK `1.2.0` from GitHub Packages.

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Minimum Android API 24
- A GitHub account that can read the private `ZentrixaStudio/zentrixa-sdk` package
- A GitHub Personal Access Token with `read:packages`

## 1. Configure GitHub Packages credentials

Never commit credentials to this repository. Add them to the user-level Gradle properties file instead.

macOS or Linux: `~/.gradle/gradle.properties`

Windows: `%USERPROFILE%\.gradle\gradle.properties`

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

If the SDK repository/package is private, the GitHub user must also have access to it. For a classic Personal Access Token, enable `read:packages`. If the organization uses SSO, authorize the token for the organization.

## 2. SDK dependency

The example resolves the stable SDK release from GitHub Packages:

```kotlin
implementation("dev.zentrixa:ads-sdk:1.2.0")
```

SDK `1.2.0` includes the official AdMob mediation adapters for Mintegral, Pangle,
Liftoff Monetize (Vungle), and Unity Ads. Do not add those adapters again in the app.
The Pangle and Mintegral repositories are already configured in this example's
`settings.gradle.kts`.

The example also applies Google's required exclusions for mediation with the
Next-Gen Mobile Ads SDK:

```kotlin
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}
```

The package repository is configured in `settings.gradle.kts`:

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/ZentrixaStudio/zentrixa-sdk")
    credentials {
        username = providers.gradleProperty("gpr.user").orNull
            ?: System.getenv("GITHUB_ACTOR")
        password = providers.gradleProperty("gpr.key").orNull
            ?: System.getenv("GITHUB_TOKEN")
    }
}
```

## 3. Run the example

1. Clone this repository.
2. Open it in Android Studio.
3. Sync Gradle.
4. Select the `app` configuration and run it on a device or emulator.

Command-line build:

```bash
./gradlew :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Demonstrated features

- UMP consent initialization and Privacy Options
- Interstitial preload and load-and-show
- High Floor → All Price configuration
- Automatic shared pool fallback
- Native preload, rendering, styling, and cleanup
- Rewarded preload, reward callback, and load-and-show
- Banner rendering with AdMob-managed automatic refresh
- App Open preload and automatic display after returning from background

The project uses Google's official test ad unit IDs. Replace them with your own IDs only after the integration has been verified.

## Important

- Do not click live ads while testing.
- Do not commit GitHub tokens or AdMob secrets.
- Initialize through `initializeWithConsent()` once per app launch, or use the legacy initialization only if the host app manages consent itself.
- Call the appropriate destroy APIs when ad containers leave their lifecycle, and call `shutdown()` only when the host truly ends the ads runtime.
