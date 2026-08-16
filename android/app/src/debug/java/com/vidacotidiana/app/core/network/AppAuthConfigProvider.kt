package com.vidacotidiana.app.core.network

import net.openid.appauth.AppAuthConfiguration

/** Debug variant: allows plain HTTP so AppAuth can reach the local dev Keycloak. */
object AppAuthConfigProvider {
    val config: AppAuthConfiguration = AppAuthConfiguration.Builder()
        .setConnectionBuilder(DebugConnectionBuilder)
        .build()
}
