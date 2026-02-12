package hoon.example.androidsandbox.data.kvs

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient
import com.amazonaws.services.kinesisvideo.model.ChannelRole
import com.amazonaws.services.kinesisvideo.model.DescribeSignalingChannelRequest
import com.amazonaws.services.kinesisvideo.model.GetSignalingChannelEndpointRequest
import com.amazonaws.services.kinesisvideo.model.ResourceEndpointListItem
import com.amazonaws.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration
import com.amazonaws.services.kinesisvideosignaling.AWSKinesisVideoSignalingClient
import com.amazonaws.services.kinesisvideosignaling.model.GetIceServerConfigRequest
import com.amazonaws.services.kinesisvideosignaling.model.IceServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KvsWebRtcClient @Inject constructor(
    private val config: KvsClientConfig
) {
    companion object {
        private const val TAG = "KvsWebRtcClient"
    }

    private val _connectionState = MutableStateFlow(KvsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<KvsConnectionState> = _connectionState.asStateFlow()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var eglBase: EglBase? = null
    private var localVideoSink: VideoSink? = null

    fun initializePeerConnectionFactory(context: Context) {
        Log.d(TAG, "Initializing PeerConnectionFactory")

        eglBase = EglBase.create()

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory initialized")
    }

    fun getEglBaseContext(): EglBase.Context? = eglBase?.eglBaseContext

    fun setLocalVideoSink(sink: VideoSink) {
        localVideoSink = sink
        localVideoTrack?.addSink(sink)
    }

    fun startLocalVideo(context: Context, isFrontCamera: Boolean = true) {
        Log.d(TAG, "Starting local video, isFrontCamera: $isFrontCamera")

        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "PeerConnectionFactory is null")
            return
        }

        // Create video capturer
        videoCapturer = createCameraCapturer(context, isFrontCamera)

        videoCapturer?.let { capturer ->
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)

            val videoSource = factory.createVideoSource(capturer.isScreencast)
            capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            capturer.startCapture(1280, 720, 30)

            localVideoTrack = factory.createVideoTrack("local_video", videoSource)
            localVideoTrack?.setEnabled(true)

            localVideoSink?.let { sink ->
                localVideoTrack?.addSink(sink)
            }

            // Create audio track
            val audioConstraints = MediaConstraints()
            val audioSource = factory.createAudioSource(audioConstraints)
            localAudioTrack = factory.createAudioTrack("local_audio", audioSource)
            localAudioTrack?.setEnabled(true)

            Log.d(TAG, "Local video started")
        }
    }

    private fun createCameraCapturer(context: Context, isFrontCamera: Boolean): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Find camera based on preference
        for (deviceName in deviceNames) {
            val isFront = enumerator.isFrontFacing(deviceName)
            if (isFrontCamera == isFront) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Camera capturer created: $deviceName")
                    return capturer
                }
            }
        }

        // Fallback to any available camera
        for (deviceName in deviceNames) {
            val capturer = enumerator.createCapturer(deviceName, null)
            if (capturer != null) {
                Log.d(TAG, "Fallback camera capturer created: $deviceName")
                return capturer
            }
        }

        Log.e(TAG, "No camera found")
        return null
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    suspend fun connectAsMaster(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = KvsConnectionState.CONNECTING
            Log.d(TAG, "Connecting as Master to channel: ${config.channelName}")

            val credentials = BasicAWSCredentials(config.accessKey, config.secretKey)

            // Get signaling channel ARN
            val kvsClient = AWSKinesisVideoClient(credentials)
            kvsClient.setRegion(Region.getRegion(config.region))

            val describeRequest = DescribeSignalingChannelRequest()
                .withChannelName(config.channelName)
            val channelInfo = kvsClient.describeSignalingChannel(describeRequest)
            val channelArn = channelInfo.channelInfo.channelARN

            Log.d(TAG, "Channel ARN: $channelArn")

            // Get signaling channel endpoints
            val endpointConfig = SingleMasterChannelEndpointConfiguration()
                .withProtocols("WSS", "HTTPS")
                .withRole(ChannelRole.MASTER)

            val endpointRequest = GetSignalingChannelEndpointRequest()
                .withChannelARN(channelArn)
                .withSingleMasterChannelEndpointConfiguration(endpointConfig)

            val endpoints = kvsClient.getSignalingChannelEndpoint(endpointRequest)
            val wssEndpoint = endpoints.resourceEndpointList.find { it.protocol == "WSS" }
            val httpsEndpoint = endpoints.resourceEndpointList.find { it.protocol == "HTTPS" }

            Log.d(TAG, "WSS Endpoint: ${wssEndpoint?.resourceEndpoint}")
            Log.d(TAG, "HTTPS Endpoint: ${httpsEndpoint?.resourceEndpoint}")

            // Get ICE server config
            val signalingClient = AWSKinesisVideoSignalingClient(credentials)
            signalingClient.setRegion(Region.getRegion(config.region))
            signalingClient.endpoint = httpsEndpoint?.resourceEndpoint

            val iceServerRequest = GetIceServerConfigRequest()
                .withChannelARN(channelArn)
                .withClientId("master")

            val iceServers = signalingClient.getIceServerConfig(iceServerRequest)
            Log.d(TAG, "ICE Servers: ${iceServers.iceServerList.size}")

            // TODO: WebSocket signaling connection and PeerConnection setup
            // This requires additional WebSocket handling for signaling

            _connectionState.value = KvsConnectionState.CONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect as Master", e)
            _connectionState.value = KvsConnectionState.ERROR
            Result.failure(e)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting")

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        peerConnection?.close()
        peerConnection = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        _connectionState.value = KvsConnectionState.DISCONNECTED
        Log.d(TAG, "Disconnected")
    }

    fun release() {
        disconnect()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null
    }
}

enum class KvsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
