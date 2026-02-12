package hoon.example.androidsandbox.data.kvs.signaling

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class KvsSignalingClient {

    companion object {
        private const val TAG = "KvsSignalingClient"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null

    private val _signalingEvents = MutableSharedFlow<SignalingEvent>(extraBufferCapacity = 64)
    val signalingEvents: SharedFlow<SignalingEvent> = _signalingEvents.asSharedFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    fun connect(
        wssEndpoint: String,
        channelArn: String,
        region: String,
        accessKey: String,
        secretKey: String,
        isMaster: Boolean
    ) {
        Log.d(TAG, "Connecting to signaling server as ${if (isMaster) "Master" else "Viewer"}")

        val signedUrl = createSignedUrl(
            wssEndpoint = wssEndpoint,
            channelArn = channelArn,
            region = region,
            accessKey = accessKey,
            secretKey = secretKey,
            isMaster = isMaster
        )

        Log.d(TAG, "Signed URL created")

        val request = Request.Builder()
            .url(signedUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                scope.launch {
                    _signalingEvents.emit(SignalingEvent.Connected)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                scope.launch {
                    _signalingEvents.emit(SignalingEvent.Error(t.message ?: "Unknown error"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                scope.launch {
                    _signalingEvents.emit(SignalingEvent.Disconnected)
                }
            }
        })
    }

    private fun handleMessage(text: String) {
        scope.launch {
            try {
                val json = JSONObject(text)
                val messageType = json.getString("messageType")
                val messagePayload = json.getString("messagePayload")
                val senderClientId = json.optString("senderClientId", "")

                // Decode base64 payload
                val decodedPayload = String(Base64.decode(messagePayload, Base64.DEFAULT))
                Log.d(TAG, "Decoded payload: $decodedPayload")

                when (messageType) {
                    "SDP_OFFER" -> {
                        val sdpJson = JSONObject(decodedPayload)
                        val sdp = sdpJson.getString("sdp")
                        _signalingEvents.emit(SignalingEvent.SdpOffer(sdp, senderClientId))
                    }
                    "SDP_ANSWER" -> {
                        val sdpJson = JSONObject(decodedPayload)
                        val sdp = sdpJson.getString("sdp")
                        _signalingEvents.emit(SignalingEvent.SdpAnswer(sdp, senderClientId))
                    }
                    "ICE_CANDIDATE" -> {
                        val candidateJson = JSONObject(decodedPayload)
                        val candidate = candidateJson.getString("candidate")
                        val sdpMid = candidateJson.optString("sdpMid", "")
                        val sdpMLineIndex = candidateJson.optInt("sdpMLineIndex", 0)
                        _signalingEvents.emit(
                            SignalingEvent.IceCandidate(candidate, sdpMid, sdpMLineIndex, senderClientId)
                        )
                    }
                    else -> {
                        Log.w(TAG, "Unknown message type: $messageType")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling message", e)
            }
        }
    }

    fun sendSdpOffer(sdp: String, recipientClientId: String? = null) {
        sendMessage("SDP_OFFER", """{"type":"offer","sdp":"$sdp"}""", recipientClientId)
    }

    fun sendSdpAnswer(sdp: String, recipientClientId: String) {
        sendMessage("SDP_ANSWER", """{"type":"answer","sdp":"$sdp"}""", recipientClientId)
    }

    fun sendIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int, recipientClientId: String? = null) {
        val payload = """{"candidate":"$candidate","sdpMid":"${sdpMid ?: ""}","sdpMLineIndex":$sdpMLineIndex}"""
        sendMessage("ICE_CANDIDATE", payload, recipientClientId)
    }

    private fun sendMessage(action: String, payload: String, recipientClientId: String?) {
        val encodedPayload = Base64.encodeToString(payload.toByteArray(), Base64.NO_WRAP)

        val message = JSONObject().apply {
            put("action", action)
            put("messagePayload", encodedPayload)
            recipientClientId?.let { put("recipientClientId", it) }
        }

        Log.d(TAG, "Sending message: ${message.toString()}")
        webSocket?.send(message.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    private fun createSignedUrl(
        wssEndpoint: String,
        channelArn: String,
        region: String,
        accessKey: String,
        secretKey: String,
        isMaster: Boolean
    ): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val amzDate = dateFormat.format(Date())
        val dateStamp = amzDate.substring(0, 8)

        val host = wssEndpoint.replace("wss://", "").split("/")[0]
        val canonicalUri = "/"

        val channelArnEncoded = URLEncoder.encode(channelArn, "UTF-8")
        val clientId = if (isMaster) "" else UUID.randomUUID().toString()

        val canonicalQueryString = buildString {
            append("X-Amz-Algorithm=AWS4-HMAC-SHA256")
            append("&X-Amz-ChannelARN=$channelArnEncoded")
            if (!isMaster && clientId.isNotEmpty()) {
                append("&X-Amz-ClientId=$clientId")
            }
            append("&X-Amz-Credential=${URLEncoder.encode("$accessKey/$dateStamp/$region/kinesisvideo/aws4_request", "UTF-8")}")
            append("&X-Amz-Date=$amzDate")
            append("&X-Amz-Expires=299")
            append("&X-Amz-SignedHeaders=host")
        }

        val canonicalHeaders = "host:$host\n"
        val signedHeaders = "host"
        val payloadHash = sha256("")

        val canonicalRequest = "GET\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

        val algorithm = "AWS4-HMAC-SHA256"
        val credentialScope = "$dateStamp/$region/kinesisvideo/aws4_request"
        val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n${sha256(canonicalRequest)}"

        val signingKey = getSignatureKey(secretKey, dateStamp, region, "kinesisvideo")
        val signature = hmacSha256(signingKey, stringToSign).toHex()

        return "$wssEndpoint?$canonicalQueryString&X-Amz-Signature=$signature"
    }

    private fun sha256(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hash.toHex()
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun getSignatureKey(key: String, dateStamp: String, region: String, service: String): ByteArray {
        val kDate = hmacSha256(("AWS4$key").toByteArray(Charsets.UTF_8), dateStamp)
        val kRegion = hmacSha256(kDate, region)
        val kService = hmacSha256(kRegion, service)
        return hmacSha256(kService, "aws4_request")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}

sealed class SignalingEvent {
    data object Connected : SignalingEvent()
    data object Disconnected : SignalingEvent()
    data class Error(val message: String) : SignalingEvent()
    data class SdpOffer(val sdp: String, val senderClientId: String) : SignalingEvent()
    data class SdpAnswer(val sdp: String, val senderClientId: String) : SignalingEvent()
    data class IceCandidate(
        val candidate: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val senderClientId: String
    ) : SignalingEvent()
}
