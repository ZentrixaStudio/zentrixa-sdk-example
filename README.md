# Zentrixa Ads SDK Example

This repository is a standalone Android example for integrating Zentrixa Ads SDK `1.2.1` from a private GitHub Maven registry.

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Minimum Android API 24
- A GitHub account granted Read access to the private `ZentrixaStudio/zentrixa-sdk-ads` binary repository
- A GitHub Personal Access Token (classic) with `read:packages`

Send the GitHub account email/username to Zentrixa Studio before integration. Zentrixa Studio will grant that account Read access to the binary repository. Access to the SDK source repository is not required.

## 1. Configure GitHub Packages credentials

Create a Personal Access Token (classic) from GitHub and enable only:

```text
read:packages
```

Store the credentials in the user-level Gradle properties file. Never add the token to a project repository.

macOS or Linux:

```text
~/.gradle/gradle.properties
```

Windows:

```text
%USERPROFILE%\.gradle\gradle.properties
```

File content:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

On Windows, the directory and file can be created from Command Prompt:

```bat
mkdir "%USERPROFILE%\.gradle" 2>nul
notepad "%USERPROFILE%\.gradle\gradle.properties"
```

## 2. Add the Maven repository

Add the private registry and mediation repositories to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle")
        }
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        }
        maven {
            name = "ZentrixaGitHubPackages"
            url = uri("https://maven.pkg.github.com/ZentrixaStudio/zentrixa-sdk-ads")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
            content {
                includeGroup("dev.zentrixa")
            }
        }
    }
}
```

## 3. Add the SDK dependency

Add to the app module's `build.gradle.kts`:

```kotlin
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    implementation("dev.zentrixa:ads-sdk:1.2.1")
}
```

The SDK includes the official AdMob mediation adapters for Mintegral, Pangle, Liftoff Monetize (Vungle), and Unity Ads. Do not add those adapters again in the host app.

## 4. Initialize with UMP consent

Call `initializeWithConsent()` once during the app launch flow. The SDK updates consent information, displays a consent form when required, and initializes Mobile Ads only when ads can be requested.

```kotlin
ZentrixaAds.initializeWithConsent(
    activity = this,
    options = AdsSdkOptions(
        appId = "YOUR_ADMOB_APP_ID",
    ),
    consentConfig = ConsentConfig(),
) { result ->
    if (result.canRequestAds) {
        // Preload the placements needed by the next screen.
    }
}
```

Privacy Options:

```kotlin
if (ZentrixaAds.isPrivacyOptionsRequired(this)) {
    ZentrixaAds.showPrivacyOptionsForm(this) { result ->
        // result.canRequestAds reflects the latest consent state.
    }
}
```

## 5. High Floor and All Price

Each placement may provide a High Floor unit and an All Price fallback. High Floor is attempted first. If it is absent or fails, the SDK automatically uses All Price.

```kotlin
val interstitialConfig = InterstitialAdConfig(
    placement = "p_inter_home",
    highFloor = AdUnit(
        placementId = "p_inter_home_high_floor",
        unitId = "HIGH_FLOOR_AD_UNIT_ID",
    ),
    allPrice = AdUnit(
        placementId = "p_inter_home_all_price",
        unitId = "ALL_PRICE_AD_UNIT_ID",
    ),
    autoRefillAfterShow = true,
)
```

To disable High Floor, pass `highFloor = null`.

## 6. Interstitial

```kotlin
ZentrixaAds.preloadInterstitial(
    activity = this,
    config = interstitialConfig,
    callbacks = AdCallbacks(
        onLoadedResult = { result -> },
        onLoadFailed = { error -> },
    ),
)

ZentrixaAds.showInterstitial(
    activity = this,
    config = interstitialConfig,
    callbacks = AdCallbacks(
        onShown = { },
        onClosed = { },
        onLoadFailed = { error -> },
        onShowFailed = { error -> },
    ),
)
```

`showInterstitial()` uses a preloaded ad when available and otherwise performs load-and-show. `onShown` is called when the ad is actually shown; it is separate from `onClosed`.

## 7. Rewarded

```kotlin
val rewardedConfig = RewardedAdConfig(
    placement = "p_reward_feature",
    allPrice = AdUnit(
        placementId = "p_reward_feature_all_price",
        unitId = "REWARDED_AD_UNIT_ID",
    ),
)

ZentrixaAds.preloadRewarded(
    activity = this,
    config = rewardedConfig,
)

ZentrixaAds.showRewarded(
    activity = this,
    config = rewardedConfig,
    callbacks = RewardedAdCallbacks(
        onShown = { },
        onRewardEarned = {
            // Grant the reward here.
        },
        onClosed = { rewardEarned -> },
        onLoadFailed = { error -> },
        onShowFailed = { error -> },
    ),
)
```

Grant the reward only from `onRewardEarned`.

## 8. Native

Add a `ViewGroup` such as `FrameLayout` to the host layout, then render the native ad into it:

```kotlin
val nativeConfig = NativeAdConfig(
    placement = "p_native_home",
    allPrice = AdUnit(
        placementId = "p_native_home_all_price",
        unitId = "NATIVE_AD_UNIT_ID",
    ),
    layoutType = "native_card",
    borderType = NativeBorderType.ALL,
    style = NativeAdStyle(
        adBadgeBackgroundColor = Color.parseColor("#FF5A00"),
        ctaBackgroundColor = Color.parseColor("#FF5A00"),
    ),
)

ZentrixaAds.preloadNative(
    activity = this,
    config = nativeConfig,
)

ZentrixaAds.showNative(
    activity = this,
    container = nativeContainer,
    config = nativeConfig,
)
```

Release the container when it leaves its lifecycle:

```kotlin
ZentrixaAds.destroyNative(
    placement = nativeConfig.placement,
    container = nativeContainer,
)
```

Native `layoutType`, light/dark mode, colors, border behavior, reload interval, and placement IDs may be supplied from the host app's Remote Config.

## 9. Banner

```kotlin
ZentrixaAds.showBannerWithAutoReload(
    activity = this,
    container = bannerContainer,
    config = BannerAdConfig(
        placement = "p_banner_home",
        allPrice = AdUnit(
            placementId = "p_banner_home_all_price",
            unitId = "BANNER_AD_UNIT_ID",
        ),
        reloadIntervalMs = 0L,
    ),
)
```

`reloadIntervalMs = 0L` disables SDK-managed refresh and lets AdMob manage banner refresh. Release the banner with:

```kotlin
ZentrixaAds.destroyBanner("p_banner_home", bannerContainer)
```

## 10. App Open

Preload App Open after consent allows ad requests:

```kotlin
ZentrixaAds.preloadAppOpen(
    activity = this,
    config = appOpenConfig,
)
```

Call `showAppOpen()` only when the app returns from the background and no other fullscreen ad is showing. See `MainActivity.kt` in this repository for the complete lifecycle guard.

## 11. Readiness, pool fallback, and refill

```kotlin
val ready = ZentrixaAds.isReady(
    placement = "p_inter_home",
    format = AdsFormat.INTERSTITIAL,
)
```

`isReady()` checks the requested placement first, then the shared pool for the same ad format. If an ad owned by another placement is used, the SDK refills the relevant placements according to `autoRefillAfterShow`.

Set the following for one-time placements when the host app manages preload itself:

```kotlin
autoRefillAfterShow = false
```

## 12. Cleanup

Destroy Native and Banner containers when their views are permanently removed. Call the global shutdown API only when the ads runtime is truly ending:

```kotlin
ZentrixaAds.shutdown()
```

Do not call `shutdown()` during ordinary Activity recreation.

## Troubleshooting

### `401 Unauthorized`

Verify all of the following:

- `gpr.user` is the GitHub username belonging to the token.
- The token is a classic PAT with `read:packages`.
- The GitHub account has Read access to `ZentrixaStudio/zentrixa-sdk-ads`.
- The credentials are in the user-level `gradle.properties`, not only the project directory.
- The Maven URL ends with `/ZentrixaStudio/zentrixa-sdk-ads`.

After correcting credentials, refresh dependencies:

```bash
./gradlew --refresh-dependencies
```

### Duplicate Google Mobile Ads classes

Ensure both legacy modules are excluded through `configurations.configureEach` as shown in section 3. Do not add the mediation adapters included by the SDK again.

## Run this example

1. Configure GitHub credentials.
2. Clone this repository.
3. Open it in Android Studio.
4. Sync Gradle.
5. Run the `app` configuration on a device or emulator.

Command-line build:

```bash
./gradlew :app:assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The example uses Google's official test ad unit IDs. Replace them with production IDs only after integration has been verified. Never click live ads during testing.

## Security

- Never commit GitHub tokens, AdMob credentials, or signing secrets.
- Give each publisher a separate GitHub account/token path so access can be revoked independently.
- The Maven repository contains the binary SDK only; publisher accounts do not need access to the private SDK source repository.
