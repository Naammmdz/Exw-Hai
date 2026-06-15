package com.example.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GooglePlayPurchase(
  val plan: SubscriptionPlan,
  val purchaseToken: String,
  val productId: String,
)

class BillingManager(
  context: Context,
  private val onPurchaseComplete: (GooglePlayPurchase) -> Unit,
) : PurchasesUpdatedListener {
  private val appContext = context.applicationContext
  private val billingClient = BillingClient.newBuilder(appContext)
    .setListener(this)
    .enablePendingPurchases()
    .build()

  private val _isReady = MutableStateFlow(false)
  val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

  private val _productsLoaded = MutableStateFlow(false)
  val productsLoaded: StateFlow<Boolean> = _productsLoaded.asStateFlow()

  private val _billingError = MutableStateFlow<String?>(null)
  val billingError: StateFlow<String?> = _billingError.asStateFlow()

  private var monthlyDetails: ProductDetails? = null
  private var yearlyDetails: ProductDetails? = null

  fun start() {
    billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
          _isReady.value = true
          _billingError.value = null
          queryProducts()
        } else {
          _billingError.value = result.debugMessage
        }
      }

      override fun onBillingServiceDisconnected() {
        _isReady.value = false
        _productsLoaded.value = false
      }
    })
  }

  private fun queryProducts() {
    val params = QueryProductDetailsParams.newBuilder()
      .setProductList(
        listOf(
          product("esmery_monthly"),
          product("esmery_yearly"),
        ),
      )
      .build()
    billingClient.queryProductDetailsAsync(params) { result, products ->
      if (result.responseCode == BillingClient.BillingResponseCode.OK) {
        monthlyDetails = products.firstOrNull { it.productId == "esmery_monthly" }
        yearlyDetails = products.firstOrNull { it.productId == "esmery_yearly" }
        _productsLoaded.value = monthlyDetails != null && yearlyDetails != null
        if (!_productsLoaded.value) {
          _billingError.value = "Subscription products are unavailable."
        }
      } else {
        _billingError.value = result.debugMessage
        _productsLoaded.value = false
      }
    }
  }

  fun canPurchase(plan: SubscriptionPlan): Boolean {
    if (!_isReady.value || !_productsLoaded.value) return false
    return when (plan) {
      SubscriptionPlan.Monthly -> monthlyDetails != null
      SubscriptionPlan.Yearly -> yearlyDetails != null
      SubscriptionPlan.Basic -> true
    }
  }

  fun launchPurchase(activity: Activity, plan: SubscriptionPlan) {
    val details = when (plan) {
      SubscriptionPlan.Monthly -> monthlyDetails
      SubscriptionPlan.Yearly -> yearlyDetails
      SubscriptionPlan.Basic -> null
    } ?: return
    val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return
    val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
      .setProductDetails(details)
      .setOfferToken(offer.offerToken)
      .build()
    billingClient.launchBillingFlow(
      activity,
      BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build(),
    )
  }

  fun restorePurchases(onRestored: (GooglePlayPurchase?) -> Unit) {
    if (!_isReady.value) {
      onRestored(null)
      return
    }
    billingClient.queryPurchasesAsync(
      QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
    ) { result, purchases ->
      if (result.responseCode != BillingClient.BillingResponseCode.OK) {
        onRestored(null)
        return@queryPurchasesAsync
      }
      val active = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
      if (active == null) {
        onRestored(null)
        return@queryPurchasesAsync
      }
      val plan = when {
        active.products.contains("esmery_monthly") -> SubscriptionPlan.Monthly
        active.products.contains("esmery_yearly") -> SubscriptionPlan.Yearly
        else -> null
      }
      val productId = active.products.firstOrNull()
      if (plan == null || productId == null) {
        onRestored(null)
        return@queryPurchasesAsync
      }
      acknowledgeIfNeeded(active)
      onRestored(GooglePlayPurchase(plan, active.purchaseToken, productId))
    }
  }

  fun openSubscriptionManagement() {
    val packageName = appContext.packageName
    val intent = Intent(
      Intent.ACTION_VIEW,
      Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    appContext.startActivity(intent)
  }

  override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
    if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
      if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
        _billingError.value = result.debugMessage
      }
      return
    }
    purchases.forEach { purchase ->
      if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
        val plan = when {
          purchase.products.contains("esmery_monthly") -> SubscriptionPlan.Monthly
          purchase.products.contains("esmery_yearly") -> SubscriptionPlan.Yearly
          else -> null
        }
        val productId = purchase.products.firstOrNull()
        if (plan != null && productId != null) {
          acknowledgeIfNeeded(purchase)
          onPurchaseComplete(GooglePlayPurchase(plan, purchase.purchaseToken, productId))
        }
      }
    }
  }

  private fun acknowledgeIfNeeded(purchase: Purchase) {
    if (!purchase.isAcknowledged) {
      billingClient.acknowledgePurchase(
        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
      ) { }
    }
  }

  fun end() {
    billingClient.endConnection()
  }

  private fun product(id: String) = QueryProductDetailsParams.Product.newBuilder()
    .setProductId(id)
    .setProductType(BillingClient.ProductType.SUBS)
    .build()
}
