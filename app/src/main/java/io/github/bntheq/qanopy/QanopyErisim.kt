package io.github.bntheq.qanopy

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class QanopyErisim : AccessibilityService() {
    companion object {
        @Volatile
        var ornek: QanopyErisim? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ornek = this
        PunchyServis.kendisi?.yenidenKur()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ornek = null
        PunchyServis.kendisi?.yenidenKur()
        return super.onUnbind(intent)
    }
}
