package com.eslamielectric.android.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore for locale/session prefs — must not be declared elsewhere. */
val Context.sessionDataStore by preferencesDataStore(name = "eslami_session_prefs")
