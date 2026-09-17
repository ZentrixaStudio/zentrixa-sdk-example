package dev.zentrixa.ads.sdk.sample

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.zentrixa.ads.sdk.api.AdCallbacks
import dev.zentrixa.ads.sdk.api.AdUnit
import dev.zentrixa.ads.sdk.api.AdsFormat
import dev.zentrixa.ads.sdk.api.AdsSdkOptions
import dev.zentrixa.ads.sdk.api.AppOpenAdConfig
import dev.zentrixa.ads.sdk.api.BannerAdConfig
import dev.zentrixa.ads.sdk.api.ConsentConfig
import dev.zentrixa.ads.sdk.api.InterstitialAdConfig
import dev.zentrixa.ads.sdk.api.NativeAdConfig
import dev.zentrixa.ads.sdk.api.NativeAdStyle
import dev.zentrixa.ads.sdk.api.NativeBorderType
import dev.zentrixa.ads.sdk.api.RewardedAdCallbacks
import dev.zentrixa.ads.sdk.api.RewardedAdConfig
import dev.zentrixa.ads.sdk.api.ZentrixaAds

class MainActivity : Activity() {
    private lateinit var bannerContainer: LinearLayout
    private lateinit var nativeContainer: FrameLayout
    private lateinit var status: TextView
    private var wasInBackground = false
    private var showingFullscreenAd = false

    private val interstitialConfig by lazy {
        InterstitialAdConfig(
            placement = "sample_interstitial",
            highFloor = HIGH_FLOOR_INTER_UNIT_ID.takeIf(String::isNotBlank)?.let {
                AdUnit("sample_interstitial_high_floor", it)
            },
            allPrice = AdUnit("sample_interstitial_all_price", TEST_INTERSTITIAL_UNIT_ID),
        )
    }

    private val interstitialPoolOwnerConfig by lazy {
        InterstitialAdConfig(
            placement = "sample_interstitial_b",
            allPrice = AdUnit("sample_interstitial_b_all_price", TEST_INTERSTITIAL_UNIT_ID),
        )
    }

    private val nativeConfig by lazy {
        NativeAdConfig(
            placement = "sample_native",
            allPrice = AdUnit("sample_native_all_price", TEST_NATIVE_UNIT_ID),
            layoutType = "native_collapse_1",
            borderType = NativeBorderType.ALL,
            style = NativeAdStyle(
                adBadgeBackgroundColor = Color.parseColor("#FF5A00"),
                ctaBackgroundColor = Color.parseColor("#FF5A00"),
            ),
        )
    }

    private val rewardedConfig by lazy {
        RewardedAdConfig(
            placement = "sample_rewarded",
            allPrice = AdUnit("sample_rewarded_all_price", TEST_REWARDED_UNIT_ID),
        )
    }

    private val appOpenConfig by lazy {
        AppOpenAdConfig(
            placement = "sample_app_open",
            allPrice = AdUnit("sample_app_open_all_price", TEST_APP_OPEN_UNIT_ID),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply { text = "Checking consent..." }
        bannerContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        nativeContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            addView(status)
            addView(button("Privacy options") { showPrivacyOptions() })
            addView(button("Preload interstitial") { preloadInterstitial() })
            addView(button("Load and show interstitial") { showInterstitial() })
            addView(button("Pool demo: clear A + preload B") { prepareInterstitialPoolDemo() })
            addView(button("Preload native") { preloadNative() })
            addView(button("Show native") { showNative() })
            addView(button("Destroy native") {
                ZentrixaAds.destroyNative(nativeConfig.placement, nativeContainer)
            })
            addView(nativeContainer)
            addView(button("Preload rewarded") { preloadRewarded() })
            addView(button("Load and show rewarded") { showRewarded() })
            addView(button("Show banner") { showBanner() })
            addView(button("Destroy banner") {
                ZentrixaAds.destroyBanner("sample_banner", bannerContainer)
            })
            addView(bannerContainer)
            addView(TextView(this@MainActivity).apply {
                text = "App Open: send the app to the background, then reopen it to test."
            })
        }
        setContentView(ScrollView(this).apply { addView(content) })

        ZentrixaAds.initializeWithConsent(
            activity = this,
            options = AdsSdkOptions(appId = TEST_APP_ID),
            consentConfig = ConsentConfig(),
        ) { result ->
            status.text = "Consent: ${result.consentStatus}, canRequestAds=${result.canRequestAds}"
            if (result.canRequestAds) {
                preloadAppOpen()
            }
        }
    }

    private fun showPrivacyOptions() {
        if (!ZentrixaAds.isPrivacyOptionsRequired(this)) {
            status.text = "Privacy options are not required"
            return
        }
        ZentrixaAds.showPrivacyOptionsForm(this) { result ->
            status.text = "Privacy updated: canRequestAds=${result.canRequestAds}"
        }
    }

    private fun preloadInterstitial() = ZentrixaAds.preloadInterstitial(
        activity = this,
        config = interstitialConfig,
        callbacks = callbacks("preload interstitial"),
    )

    private fun showInterstitial() = ZentrixaAds.showInterstitial(
        activity = this,
        config = interstitialConfig,
        callbacks = callbacks("show interstitial", fullscreen = true),
    )

    private fun prepareInterstitialPoolDemo() {
        ZentrixaAds.clearCache(interstitialConfig.placement)
        ZentrixaAds.preloadInterstitial(
            activity = this,
            config = interstitialPoolOwnerConfig,
            force = true,
            callbacks = AdCallbacks(
                onLoadedResult = { result ->
                    val readyA = ZentrixaAds.isReady(
                        interstitialConfig.placement,
                        AdsFormat.INTERSTITIAL,
                    )
                    status.text = "B loaded; isReady(A)=$readyA. Now show A. ${result.tier}"
                },
                onLoadFailed = { status.text = "Pool demo failed: $it" },
            ),
        )
    }

    private fun preloadNative() = ZentrixaAds.preloadNative(
        activity = this,
        config = nativeConfig,
        callbacks = callbacks("preload native"),
    )

    private fun showNative() = ZentrixaAds.showNative(
        activity = this,
        container = nativeContainer,
        config = nativeConfig,
        callbacks = callbacks("native"),
    )

    private fun preloadRewarded() = ZentrixaAds.preloadRewarded(
        activity = this,
        config = rewardedConfig,
        callbacks = callbacks("preload rewarded"),
    )

    private fun showRewarded() = ZentrixaAds.showRewarded(
        activity = this,
        config = rewardedConfig,
        callbacks = RewardedAdCallbacks(
            onLoadedResult = { result ->
                status.text = "rewarded loaded: ${result.tier} (${result.sourcePlacementId})"
            },
            onShown = {
                showingFullscreenAd = true
                status.text = "rewarded shown"
            },
            onRewardEarned = { status.text = "reward earned" },
            onClosed = { earned ->
                showingFullscreenAd = false
                status.text = "rewarded closed, earned=$earned"
                preloadRewarded()
            },
            onLoadFailed = { status.text = "rewarded failed: $it" },
            onShowFailed = {
                showingFullscreenAd = false
                status.text = "rewarded show failed: $it"
            },
        ),
    )

    private fun preloadAppOpen() = ZentrixaAds.preloadAppOpen(
        activity = this,
        config = appOpenConfig,
        callbacks = callbacks("preload app open"),
    )

    private fun showAppOpenOnResume() = ZentrixaAds.showAppOpen(
        activity = this,
        config = appOpenConfig,
        callbacks = callbacks("app open", fullscreen = true, preloadAfterClose = ::preloadAppOpen),
    )

    private fun showBanner() = ZentrixaAds.showBannerWithAutoReload(
        activity = this,
        container = bannerContainer,
        config = BannerAdConfig(
            placement = "sample_banner",
            allPrice = AdUnit("sample_banner_all_price", TEST_BANNER_UNIT_ID),
            reloadIntervalMs = 0L,
        ),
        callbacks = callbacks("banner"),
    )

    private fun callbacks(
        action: String,
        fullscreen: Boolean = false,
        preloadAfterClose: (() -> Unit)? = null,
    ) = AdCallbacks(
        onLoadedResult = { result ->
            status.text = "$action loaded: ${result.tier} (${result.sourcePlacementId})"
        },
        onShown = {
            if (fullscreen) showingFullscreenAd = true
            status.text = "$action shown"
        },
        onLoadFailed = { status.text = "$action failed: $it" },
        onShowFailed = {
            if (fullscreen) showingFullscreenAd = false
            status.text = "$action show failed: $it"
        },
        onClosed = {
            if (fullscreen) showingFullscreenAd = false
            status.text = "$action closed"
            preloadAfterClose?.invoke()
        },
    )

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    override fun onResume() {
        super.onResume()
        if (wasInBackground && !showingFullscreenAd) {
            wasInBackground = false
            showAppOpenOnResume()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !showingFullscreenAd) {
            wasInBackground = true
        }
    }

    override fun onDestroy() {
        if (isFinishing) ZentrixaAds.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
        const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_NATIVE_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_APP_OPEN_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

        // Replace with a dedicated high-floor unit ID. Empty means skip High Floor.
        const val HIGH_FLOOR_INTER_UNIT_ID = ""
    }
}
