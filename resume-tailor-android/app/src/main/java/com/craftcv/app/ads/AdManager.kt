package com.craftcv.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages MobileAds initialization and rewarded ads.
 * Banner ads are managed at the composable level via [BannerAd].
 * Pro users never see ads.
 */
object AdManager {
    private var initialized = false
    private var rewardedAd: RewardedAd? = null

    // Ad unit ID from BuildConfig — test IDs for closed testing, real IDs for production
    private val REWARDED_AD_UNIT_ID = com.craftcv.app.BuildConfig.REWARDED_AD_UNIT_ID

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelays = longArrayOf(3_000, 8_000, 15_000) // escalating backoff
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (!initialized) {
            appContext = context.applicationContext
            MobileAds.initialize(context)
            initialized = true
            loadRewardedAd(context)
        }
    }

    fun loadRewardedAd(context: Context) {
        if (_isAdLoading.value || rewardedAd != null) return
        _isAdLoading.value = true

        RewardedAd.load(context, REWARDED_AD_UNIT_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isAdReady.value = true
                    _isAdLoading.value = false
                    retryCount = 0
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isAdReady.value = false
                    _isAdLoading.value = false
                    // Auto-retry with backoff
                    if (retryCount < maxRetries) {
                        val delay = retryDelays[retryCount]
                        retryCount++
                        handler.postDelayed({ loadRewardedAd(context) }, delay)
                    }
                }
            }
        )
    }

    /** Manual retry triggered by user tapping the button */
    fun retryLoad() {
        val ctx = appContext ?: return
        retryCount = 0
        rewardedAd = null
        _isAdReady.value = false
        _isAdLoading.value = false
        loadRewardedAd(ctx)
    }

    /**
     * Shows a rewarded ad. Calls [onRewarded] when the user earns the reward,
     * and [onDismissed] after the ad is closed (whether rewarded or not).
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onDismissed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isAdReady.value = false
                onDismissed()
                // Pre-load next ad
                loadRewardedAd(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _isAdReady.value = false
                onDismissed()
                loadRewardedAd(activity)
            }
        }

        ad.show(activity) { onRewarded() }
    }
}
