package com.sudoflame.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.sudoflame.app.BuildConfig
import com.sudoflame.app.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdsManager {
    private const val TAG = "Ads"
    private val initialized = AtomicBoolean(false)
    private lateinit var consent: ConsentInformation
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var lastInterstitialAt = 0L
    private var gamesSinceInterstitial = 0

    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady = _rewardedReady.asStateFlow()

    /** MainActivity.onCreate içinde çağrılır: önce onay (GDPR/KVKK), sonra reklam SDK'sı. */
    fun gatherConsentAndInit(activity: Activity) {
        consent = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { err ->
                if (err != null) Log.w(TAG, "consent form: ${err.message}")
                if (consent.canRequestAds()) initSdk(activity)
            }
        }, { err ->
            Log.w(TAG, "consent update: ${err.message}")
            initSdk(activity)
        })
        if (consent.canRequestAds()) initSdk(activity)
    }

    val privacyOptionsRequired: Boolean
        get() = ::consent.isInitialized &&
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { err ->
            if (err != null) Log.w(TAG, "privacy form: ${err.message}")
        }
    }

    private fun initSdk(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(context.applicationContext) {
                _ready.value = true
                loadInterstitial(context.applicationContext)
                loadRewarded(context.applicationContext)
            }
        }.start()
    }

    private fun request() = AdRequest.Builder().build()

    private fun loadInterstitial(ctx: Context) {
        if (ProgressRepository.current.adsRemoved) return
        InterstitialAd.load(ctx, BuildConfig.AD_INTERSTITIAL, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null; Log.d(TAG, "inter fail ${e.message}") }
        })
    }

    private fun loadRewarded(ctx: Context) {
        RewardedAd.load(ctx, BuildConfig.AD_REWARDED, request(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; _rewardedReady.value = true }
            override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null; _rewardedReady.value = false }
        })
    }

    /**
     * Oyun bitiminde çağrılır. Kullanıcıyı bıktırmamak için:
     * en az 2 oyunda bir ve iki reklam arası en az 90 saniye.
     */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit) {
        gamesSinceInterstitial++
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (ProgressRepository.current.adsRemoved || ad == null ||
            gamesSinceInterstitial < 2 || now - lastInterstitialAt < 90_000
        ) { onDone(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null; loadInterstitial(activity.applicationContext); onDone()
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                interstitial = null; loadInterstitial(activity.applicationContext); onDone()
            }
        }
        gamesSinceInterstitial = 0
        lastInterstitialAt = now
        ad.show(activity)
    }

    /** Ödüllü reklam: ipucu, ekstra can, seri kurtarma, dondurucu. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewarded
        if (ad == null) { onUnavailable(); loadRewarded(activity.applicationContext); return }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null; _rewardedReady.value = false
                loadRewarded(activity.applicationContext)
                if (earned) onReward()
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                rewarded = null; _rewardedReady.value = false
                loadRewarded(activity.applicationContext); onUnavailable()
            }
        }
        ad.show(activity) { earned = true }
    }
}

/** Ekranın altına yerleşen uyarlanabilir banner. "Reklamları kaldır" alınınca kaybolur. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsState()
    val progress by ProgressRepository.state.collectAsState()
    if (!ready || progress.adsRemoved) return
    val width = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, width))
                adUnitId = BuildConfig.AD_BANNER
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
