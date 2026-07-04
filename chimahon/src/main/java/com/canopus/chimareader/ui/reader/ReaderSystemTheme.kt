package com.canopus.chimareader.ui.reader

import android.content.Context
import android.content.res.Configuration

internal fun Context.isReaderSystemDark(): Boolean {
    return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
