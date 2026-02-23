package com.resumetailor.app.ads

import android.app.Activity
import android.content.Context
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

    // Real CraftCV rewarded ad unit ID from AdMob
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-7561854957294548/6996555395"

    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    fun initialize(context: Context) {
        if (!initialized) {
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
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isAdReady.value = false
                    _isAdLoading.value = false
                }
            }
        )
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
