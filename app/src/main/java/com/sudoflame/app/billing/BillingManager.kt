package com.sudoflame.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.sudoflame.app.data.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Reklamları Kaldır" tek seferlik ürünü.
 * Play Console > Para kazanma > Ürünler > Uygulama içi ürünler bölümünde
 * ürün kimliği "remove_ads" olan bir ürün oluşturun.
 */
object BillingManager {
    const val PRODUCT_ID = "remove_ads"
    private const val TAG = "Billing"
    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private val _price = MutableStateFlow<String?>(null)
    val price = _price.asStateFlow()

    fun init(context: Context) {
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases?.forEach(::handle)
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                queryProduct()
                restore()
            }
            override fun onBillingServiceDisconnected() { Log.d(TAG, "disconnected") }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client?.queryProductDetailsAsync(params) { _, list ->
            details = list.firstOrNull()
            _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    fun restore() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client?.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach(::handle)
        }
    }

    /** @return false ise ürün henüz yüklenmedi (Play Console'da tanımlı değil ya da internet yok). */
    fun buyRemoveAds(activity: Activity): Boolean {
        val d = details ?: return false
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())
        ).build()
        client?.launchBillingFlow(activity, flow)
        return true
    }

    private fun handle(p: Purchase) {
        if (PRODUCT_ID !in p.products || p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        ProgressRepository.update { it.copy(adsRemoved = true) }
        if (!p.isAcknowledged) {
            client?.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            ) { r -> Log.d(TAG, "ack ${r.responseCode}") }
        }
    }
}
