package ir.esmail.espresso.data

import android.service.notification.NotificationListenerService

/**
 * Enables [NowPlayingDetector] to read active media sessions. It intentionally does nothing
 * else: the app never reads or stores notification content.
 */
class EspressoNotificationListener : NotificationListenerService()
