package com.akdag.inseminationtrackerapp

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PremiumManager : PurchasesUpdatedListener {

    const val FREE_COW_LIMIT = 10
    const val PRODUCT_MONTHLY = "premium_monthly"
    const val PRODUCT_YEARLY  = "premium_yearly"

    private val _isPremium      = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _monthlyDetails = MutableStateFlow<ProductDetails?>(null)
    val monthlyDetails: StateFlow<ProductDetails?> = _monthlyDetails.asStateFlow()

    private val _yearlyDetails  = MutableStateFlow<ProductDetails?>(null)
    val yearlyDetails: StateFlow<ProductDetails?> = _yearlyDetails.asStateFlow()

    private var billingClient: BillingClient? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        // Hızlı başlangıç için Firestore'dan oku
        loadFirestoreStatus()

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        connect()
    }

    private fun connect() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    querySubscriptionStatus()
                }
            }
            override fun onBillingServiceDisconnected() { connect() }
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(PRODUCT_MONTHLY, PRODUCT_YEARLY).map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            ).build()
        billingClient?.queryProductDetailsAsync(params) { _, list ->
            list.forEach { details ->
                when (details.productId) {
                    PRODUCT_MONTHLY -> _monthlyDetails.value = details
                    PRODUCT_YEARLY  -> _yearlyDetails.value  = details
                }
            }
        }
    }

    fun querySubscriptionStatus() {
        val client = billingClient ?: return
        if (!client.isReady) return
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, purchases ->
            val active = purchases.any { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        (p.products.contains(PRODUCT_MONTHLY) || p.products.contains(PRODUCT_YEARLY))
            }
            if (active) {
                // Gerçek aktif abonelik bulundu — hem uygulama hem Firestore güncelle
                _isPremium.value = true
                syncToFirestore(true)
            }
            // Aktif abonelik yoksa Firestore'a false YAZMA:
            // Play Console kurulmadan manuel test için Firestore override'ı korunur.
            // Abonelik iptali ancak onPurchasesUpdated üzerinden işlenir.
        }
    }

    fun launchBillingFlow(activity: Activity, details: ProductDetails) {
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).build()
        billingClient?.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                    _isPremium.value = true
                    syncToFirestore(true)
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.acknowledgePurchase(params) { }
    }

    private fun loadFirestoreStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("Users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.getBoolean("is_premium") == true) _isPremium.value = true
            }
    }

    private fun syncToFirestore(premium: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("Users").document(uid)
            .update("is_premium", premium)
    }
}
