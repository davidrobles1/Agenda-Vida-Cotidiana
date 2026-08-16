package com.vidacotidiana.app.core.network

import net.openid.appauth.AppAuthConfiguration

/** Release variant: AppAuth's own default — HTTPS-only, no local-dev carve-out. */
object AppAuthConfigProvider {
    val config: AppAuthConfiguration = AppAuthConfiguration.DEFAULT
}
