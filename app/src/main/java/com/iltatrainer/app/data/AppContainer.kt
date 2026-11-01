package com.iltatrainer.app.data

import android.content.Context

/** Tiny manual dependency container: everything is offline and app-scoped. */
class AppContainer(context: Context) {
    val contentRepository = ContentRepository(context.applicationContext.assets)
    val progressStore = ProgressStore(context.applicationContext)
}
