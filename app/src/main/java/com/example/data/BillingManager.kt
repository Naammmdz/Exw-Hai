package com.example.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
  context: Context,
  private val onPurchaseComplete: (SubscriptionPlan) -> Unit,
) : PurchasesUpdatedListener {
  private val billingClient = BillingClient.newBuilder(context)
    .setListener(this)
    .enablePendingPurchases()
    .build()

  private val _isReady = MutableStateFlow(false)
  val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

  private var monthlyDetails: ProductDetails? = null
  private var yearlyDetails: ProductDetails? = null

  fun start() {
    billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
          _isReady.value = true
          queryProducts()
        }
      }

      override fun onBillingServiceDisconnected() {
        _isReady.value = false
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
      }
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

  override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
    if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
    purchases.forEach { purchase ->
      if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
        val plan = when {
          purchase.products.contains("esmery_monthly") -> SubscriptionPlan.Monthly
          purchase.products.contains("esmery_yearly") -> SubscriptionPlan.Yearly
          else -> SubscriptionPlan.Basic
        }
        onPurchaseComplete(plan)
        if (!purchase.isAcknowledged) {
          billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
          ) { }
        }
      }
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
