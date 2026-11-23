package com.sudoflame.app.data

import android.content.Context

/** Yayından önce bu bağlantıları kendi bilgilerinizle güncelleyin. */
object AppLinks {
    /** docs/privacy_policy.html dosyasını GitHub Pages vb. bir yerde yayınlayıp adresini yazın. */
    const val PRIVACY_URL = "https://example.com/sudoflame/privacy"
    const val SUPPORT_EMAIL = "destek@example.com"

    fun store(ctx: Context) = "https://play.google.com/store/apps/details?id=${ctx.packageName}"
}
