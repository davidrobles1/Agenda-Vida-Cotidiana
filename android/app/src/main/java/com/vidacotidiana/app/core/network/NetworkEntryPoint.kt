package com.vidacotidiana.app.core.network

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

/**
 * Lets a plain @Composable (not a @HiltViewModel) reach Hilt-provided APIs
 * directly — used for ShareDialog, which is parameterized per-reminder at
 * call time and doesn't need the full assisted-injection ViewModel machinery
 * for what's a small inline panel (AND-004).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkEntryPoint {
    fun sharingApi(): SharingApi
}

fun sharingApi(context: Context): SharingApi =
    EntryPointAccessors.fromApplication(context, NetworkEntryPoint::class.java).sharingApi()
