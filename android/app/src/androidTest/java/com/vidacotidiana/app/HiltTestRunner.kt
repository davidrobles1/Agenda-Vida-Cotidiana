package com.vidacotidiana.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/** Swaps in HiltTestApplication so @HiltAndroidTest instrumented tests can inject real bindings. */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
