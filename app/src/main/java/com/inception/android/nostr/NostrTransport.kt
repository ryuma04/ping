package com.inception.android.nostr

import android.content.Context
import android.util.Log
import com.inception.android.model.ReadReceipt

/**
 * Nostr transport for offline/bridge private messages and receipts.
 */
class NostrTransport(
    private val context: Context,
    var senderPeerID: String = ""
) {
    companion object {
        private const val TAG = "NostrTransport"

        @Volatile
        private var INSTANCE: NostrTransport? = null

        fun getInstance(context: Context): NostrTransport {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NostrTransport(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val myPeerID: String get() = senderPeerID

    fun sendPrivateMessage(
        content: String,
        to: String,
        recipientNickname: String,
        messageID: String
    ) {
        Log.d(TAG, "sendPrivateMessage to $to (Stage 5 Nostr transport pending)")
    }

    fun sendPrivateMessageGeohash(
        content: String,
        toRecipientHex: String,
        messageID: String,
        sourceGeohash: String?
    ) {
        Log.d(TAG, "sendPrivateMessageGeohash to $toRecipientHex (Stage 5 Nostr transport pending)")
    }

    fun sendReadReceipt(receipt: ReadReceipt, to: String) {
        Log.d(TAG, "sendReadReceipt to $to (Stage 5 Nostr transport pending)")
    }

    fun sendDeliveryAck(messageID: String, to: String) {
        Log.d(TAG, "sendDeliveryAck msg $messageID to $to (Stage 5 Nostr transport pending)")
    }

    fun sendDeliveryAckGeohash(
        messageID: String,
        toRecipientHex: String,
        fromIdentity: NostrIdentity
    ) {
        Log.d(TAG, "sendDeliveryAckGeohash msg $messageID to $toRecipientHex (Stage 5 Nostr transport pending)")
    }

    fun sendReadReceiptGeohash(
        messageID: String,
        toRecipientHex: String,
        fromIdentity: NostrIdentity
    ) {
        Log.d(TAG, "sendReadReceiptGeohash msg $messageID to $toRecipientHex (Stage 5 Nostr transport pending)")
    }

    fun sendFavoriteNotification(to: String, isFavorite: Boolean) {
        Log.d(TAG, "sendFavoriteNotification to $to isFavorite=$isFavorite (Stage 5 Nostr transport pending)")
    }
}
