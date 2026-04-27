package com.painkiller

import android.app.Application
import com.painkiller.di.PainkillerContainer

class PainkillerApplication : Application() {

    /**
     * Lazily initialised DI container. Held by the [Application] so it
     * survives configuration changes and is shared by every Activity /
     * ViewModel.
     */
    val container: PainkillerContainer by lazy { PainkillerContainer(this) }
}
