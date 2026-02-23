package com.resumetailor.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles all Google Play Billing interactions.
 *
 * Product IDs (create these in Play Console → Subscriptions):
 *   craftcv_pro_monthly  — monthly subscription
 *   craftcv_pro_yearly   — yearly subscription (best value)
 */
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onProStatusChanged: (Boolean) -> Unit,
    private val onPurchaseToken: (token: String, productId: String) -> Unit,
) {
    companion object {
        const val SKU_PRO_MONTHLY = "craftcv_pro_monthly"
        const val SKU_PRO_YEARLY  = "craftcv_pro_yearly"
    }

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError.asStateFlow()

    private val _isPurchasePending = MutableStateFlow(false)
    val isPurchasePending: StateFlow<Boolean> = _isPurchasePending.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        _isPurchasePending.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> { /* user backed out — do nothing */ }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Subscription exists but wasn't detected — re-query to restore
                coroutineScope.launch { checkExistingPurchases() }
            }
            else -> {
                _billingError.value = "Purchase failed: ${result.debugMessage} (${result.responseCode})"
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    coroutineScope.launch { checkExistingPurchases() }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Will retry automatically on next user action
            }
        })
    }

    fun disconnect() {
        billingClient.endConnection()
    }

    // ── Purchase check (called on startup + after purchase) ───────────────────

    suspend fun checkExistingPurchases() {
        if (!billingClient.isReady) return
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val activeSub = result.purchasesList.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (activeSub != null) {
                _isPro.value = true
                onProStatusChanged(true)
                acknowledgeIfNeeded(activeSub)
                return
            }
        }
        // No active subscription found — revert (handles cancellations)
        _isPro.value = false
        onProStatusChanged(false)
    }

    // ── Handle new purchase ────────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            _isPro.value = true
            onProStatusChanged(true)
            // Notify backend so it can verify and mark user as Pro in the DB
            purchase.products.firstOrNull()?.let { productId ->
                onPurchaseToken(purchase.purchaseToken, productId)
            }
            coroutineScope.launch { acknowledgeIfNeeded(purchase) }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _billingError.value = "Your purchase is pending payment confirmation. It will activate automatically."
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
        }
    }

    // ── Launch billing flow ────────────────────────────────────────────────────

    /**
     * Opens the Google Play subscription purchase sheet.
     * @param activity The current Activity (required by Play Billing).
     * @param planIndex 0 = monthly, 1 = yearly
     */
    fun launchBillingFlow(activity: Activity, planIndex: Int) {
        val sku = if (planIndex == 0) SKU_PRO_MONTHLY else SKU_PRO_YEARLY

        coroutineScope.launch {
            if (!billingClient.isReady) {
                _billingError.value = "Google Play Billing is not available. Please try again."
                return@launch
            }

            _isPurchasePending.value = true
            _billingError.value = null

            val queryResult = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(sku)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    )
                    .build()
            )

            val productDetails = queryResult.productDetailsList?.firstOrNull()
            if (productDetails == null) {
                _isPurchasePending.value = false
                _billingError.value = "Subscription product not found in Play Console. Contact support."
                return@launch
            }

            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                _isPurchasePending.value = false
                _billingError.value = "No subscription offer available."
                return@launch
            }

            val flowResult = billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()
            )

            if (flowResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _isPurchasePending.value = false
                _billingError.value = "Could not open Google Play. Please try again."
            }
        }
    }

    fun clearBillingError() {
        _billingError.value = null
    }
}
