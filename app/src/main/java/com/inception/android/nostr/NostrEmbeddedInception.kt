package com.inception.android.nostr

import android.util.Base64
import android.util.Log
import com.inception.android.mesh.MeshPacketUtils
import com.inception.android.model.PrivateMessagePacket
import com.inception.android.model.NoisePayloadType
import com.inception.android.protocol.InceptionPacket
import com.inception.android.protocol.MessageType
import com.inception.android.services.ContactIdentityResolver
import java.util.*

/**
 * Inception-over-Nostr Adapter
 */
object NostrEmbeddedInception {
    
    private const val TAG = "NostrEmbeddedInception"
    
    /**
     * Build a `inception1:` base64url-encoded Inception packet carrying a private message for Nostr DMs.
     */
    fun encodePMForNostr(
        content: String,
        messageID: String,
        recipientPeerID: String,
        senderPeerID: String
    ): String? {
        try {
            // TLV-encode the private message
            val pm = PrivateMessagePacket(messageID = messageID, content = content)
            val tlv = pm.encode() ?: return null
            
            // Prefix with NoisePayloadType
            val payload = ByteArray(1 + tlv.size)
            payload[0] = NoisePayloadType.PRIVATE_MESSAGE.value.toByte()
            System.arraycopy(tlv, 0, payload, 1, tlv.size)
            
            // Determine 8-byte recipient ID to embed
            val recipientIDHex = normalizeRecipientPeerID(recipientPeerID)
            
            val packet = InceptionPacket(
                version = 1u,
                type = MessageType.NOISE_ENCRYPTED.value,
                senderID = hexStringToByteArray(senderPeerID),
                recipientID = hexStringToByteArray(recipientIDHex),
                timestamp = System.currentTimeMillis().toULong(),
                payload = payload,
                signature = null,
                ttl = com.inception.android.util.AppConstants.MESSAGE_TTL_HOPS
            )
            
            val data = packet.toBinaryData() ?: return null
            return "inception1:" + base64URLEncode(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode PM for Nostr: ${e.message}")
            return null
        }
    }
    
    /**
     * Build a `inception1:` base64url-encoded Inception packet carrying a delivery/read ack for Nostr DMs.
     */
    fun encodeAckForNostr(
        type: NoisePayloadType,
        messageID: String,
        recipientPeerID: String,
        senderPeerID: String
    ): String? {
        if (type != NoisePayloadType.DELIVERED && type != NoisePayloadType.READ_RECEIPT) {
            return null
        }
        
        try {
            val payload = ByteArray(1 + messageID.toByteArray(Charsets.UTF_8).size)
            payload[0] = type.value.toByte()
            val messageIDBytes = messageID.toByteArray(Charsets.UTF_8)
            System.arraycopy(messageIDBytes, 0, payload, 1, messageIDBytes.size)
            
            val recipientIDHex = normalizeRecipientPeerID(recipientPeerID)
            
            val packet = InceptionPacket(
                version = 1u,
                type = MessageType.NOISE_ENCRYPTED.value,
                senderID = hexStringToByteArray(senderPeerID),
                recipientID = hexStringToByteArray(recipientIDHex),
                timestamp = System.currentTimeMillis().toULong(),
                payload = payload,
                signature = null,
                ttl = com.inception.android.util.AppConstants.MESSAGE_TTL_HOPS
            )
            
            val data = packet.toBinaryData() ?: return null
            return "inception1:" + base64URLEncode(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode ACK for Nostr: ${e.message}")
            return null
        }
    }
    
    /**
     * Build a `inception1:` ACK (delivered/read) without an embedded recipient peer ID (geohash DMs).
     */
    fun encodeAckForNostrNoRecipient(
        type: NoisePayloadType,
        messageID: String,
        senderPeerID: String
    ): String? {
        if (type != NoisePayloadType.DELIVERED && type != NoisePayloadType.READ_RECEIPT) {
            return null
        }
        
        try {
            val payload = ByteArray(1 + messageID.toByteArray(Charsets.UTF_8).size)
            payload[0] = type.value.toByte()
            val messageIDBytes = messageID.toByteArray(Charsets.UTF_8)
            System.arraycopy(messageIDBytes, 0, payload, 1, messageIDBytes.size)
            
            val packet = InceptionPacket(
                version = 1u,
                type = MessageType.NOISE_ENCRYPTED.value,
                senderID = hexStringToByteArray(senderPeerID),
                recipientID = null, // No recipient for geohash DMs
                timestamp = System.currentTimeMillis().toULong(),
                payload = payload,
                signature = null,
                ttl = com.inception.android.util.AppConstants.MESSAGE_TTL_HOPS
            )
            
            val data = packet.toBinaryData() ?: return null
            return "inception1:" + base64URLEncode(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode ACK for Nostr (no recipient): ${e.message}")
            return null
        }
    }
    
    /**
     * Build a `inception1:` payload without an embedded recipient peer ID (used for geohash DMs).
     */
    fun encodePMForNostrNoRecipient(
        content: String,
        messageID: String,
        senderPeerID: String
    ): String? {
        try {
            val pm = PrivateMessagePacket(messageID = messageID, content = content)
            val tlv = pm.encode() ?: return null
            
            val payload = ByteArray(1 + tlv.size)
            payload[0] = NoisePayloadType.PRIVATE_MESSAGE.value.toByte()
            System.arraycopy(tlv, 0, payload, 1, tlv.size)
            
            val packet = InceptionPacket(
                version = 1u,
                type = MessageType.NOISE_ENCRYPTED.value,
                senderID = hexStringToByteArray(senderPeerID),
                recipientID = null, // No recipient for geohash DMs
                timestamp = System.currentTimeMillis().toULong(),
                payload = payload,
                signature = null,
                ttl = com.inception.android.util.AppConstants.MESSAGE_TTL_HOPS
            )
            
            val data = packet.toBinaryData() ?: return null
            return "inception1:" + base64URLEncode(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode PM for Nostr (no recipient): ${e.message}")
            return null
        }
    }
    
    /**
     * Normalize recipient peer ID (matches iOS implementation)
     */
    private fun normalizeRecipientPeerID(recipientPeerID: String): String {
        val clean = recipientPeerID.trim().lowercase()
        return when {
            ContactIdentityResolver.isNoiseKeyHex(clean) ->
                ContactIdentityResolver.peerIdForNoiseKeyHex(clean) ?: clean
            ContactIdentityResolver.isMeshPeerId(clean) -> clean
            else -> recipientPeerID
        }
    }
    
    /**
     * Base64url encode without padding (matches iOS implementation)
     */
    private fun base64URLEncode(data: ByteArray): String {
        val b64 = Base64.encodeToString(data, Base64.NO_WRAP)
        return b64
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }
    
    /**
     * Convert hex string to byte array
     */
    private fun hexStringToByteArray(hexString: String): ByteArray =
        MeshPacketUtils.hexStringToByteArray(hexString)
}
