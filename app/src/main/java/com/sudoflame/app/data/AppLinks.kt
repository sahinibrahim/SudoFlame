package com.sudoflame.app.data

import android.content.Context

/** Uygulamanın web adresleri (docs/ klasörü sudoflame.com olarak yayınlanır). */
object AppLinks {
    /** docs/privacy.html dosyası bu adreste yayınlanır. */
    const val PRIVACY_URL = "https://sudoflame.com/privacy.html"
    const val SUPPORT_EMAIL = "info@sudoflame.com"
    const val WEBSITE = "https://sudoflame.com"

    fun store(ctx: Context) = "https://play.google.com/store/apps/details?id=${ctx.packageName}"
}
