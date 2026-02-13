package hoon.example.androidsandbox.data.kvs

import android.content.Context
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.services.kinesisvideo.AWSKinesisVideoClient
import com.amazonaws.services.kinesisvideo.model.ChannelRole
import com.amazonaws.services.kinesisvideo.model.DescribeSignalingChannelRequest
import com.amazonaws.services.kinesisvideo.model.GetSignalingChannelEndpointRequest
import com.amazonaws.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration
import com.amazonaws.services.kinesisvideosignaling.AWSKinesisVideoSignalingClient
import com.amazonaws.services.kinesisvideosignaling.model.GetIceServerConfigRequest
import com.amazonaws.services.kinesisvideosignaling.model.IceServer
import hoon.example.androidsandbox.data.kvs.signaling.KvsSignalingClient
import hoon.example.androidsandbox.data.kvs.signaling.SignalingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(KvsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<KvsConnectionState> = _connectionState.asStateFlow()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var eglBase: EglBase? = null
    private var localVideoSink: VideoSink? = null

    private val signalingClient = KvsSignalingClient()
    private var iceServers: List<PeerConnection.IceServer> = emptyList()

    private var wssEndpoint: String? = null
    private var channelArn: String? = null

    init {
        observeSignalingEvents()
    }

    private fun observeSignalingEvents() {
        scope.launch {
            signalingClient.signalingEvents.collect { event ->
                when (event) {
                    is SignalingEvent.Connected -> {
                        Log.d(TAG, "Signaling connected")
                        _connectionState.value = KvsConnectionState.CONNECTED
                    }
                    is SignalingEvent.Disconnected -> {
                        Log.d(TAG, "Signaling disconnected")
                        _connectionState.value = KvsConnectionState.DISCONNECTED
                    }
                    is SignalingEvent.Error -> {
                        Log.e(TAG, "Signaling error: ${event.message}")
                        _connectionState.value = KvsConnectionState.ERROR
                    }
                    is SignalingEvent.SdpOffer -> {
                        Log.d(TAG, "Received SDP Offer from ${event.senderClientId}")
                        handleSdpOffer(event.sdp, event.senderClientId)
                    }
                    is SignalingEvent.SdpAnswer -> {
                        Log.d(TAG, "Received SDP Answer from ${event.senderClientId}")
                        handleSdpAnswer(event.sdp, event.senderClientId)
                    }
                    is SignalingEvent.IceCandidate -> {
                        Log.d(TAG, "Received ICE Candidate from ${event.senderClientId}")
                        handleIceCandidate(event.candidate, event.sdpMid, event.sdpMLineIndex, event.senderClientId)
                    }
                }
            }
        }
    }

    fun initializePeerConnectionFactory(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "PeerConnectionFactory already initialized")
            return
        }

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

        isInitialized = true
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
            channelArn = channelInfo.channelInfo.channelARN

            Log.d(TAG, "Channel ARN: $channelArn")

            // Get signaling channel endpoints
            val endpointConfig = SingleMasterChannelEndpointConfiguration()
                .withProtocols("WSS", "HTTPS")
                .withRole(ChannelRole.MASTER)

            val endpointRequest = GetSignalingChannelEndpointRequest()
                .withChannelARN(channelArn)
                .withSingleMasterChannelEndpointConfiguration(endpointConfig)

            val endpoints = kvsClient.getSignalingChannelEndpoint(endpointRequest)
            wssEndpoint = endpoints.resourceEndpointList.find { it.protocol == "WSS" }?.resourceEndpoint
            val httpsEndpoint = endpoints.resourceEndpointList.find { it.protocol == "HTTPS" }?.resourceEndpoint

            Log.d(TAG, "WSS Endpoint: $wssEndpoint")
            Log.d(TAG, "HTTPS Endpoint: $httpsEndpoint")

            // Get ICE server config
            val signalingApiClient = AWSKinesisVideoSignalingClient(credentials)
            signalingApiClient.setRegion(Region.getRegion(config.region))
            signalingApiClient.endpoint = httpsEndpoint

            val iceServerRequest = GetIceServerConfigRequest()
                .withChannelARN(channelArn)
                .withClientId("master")

            val iceServerResponse = signalingApiClient.getIceServerConfig(iceServerRequest)
            iceServers = iceServerResponse.iceServerList.map { server ->
                PeerConnection.IceServer.builder(server.uris)
                    .setUsername(server.username)
                    .setPassword(server.password)
                    .createIceServer()
            }
            Log.d(TAG, "ICE Servers: ${iceServers.size}")

            // Connect to signaling server
            wssEndpoint?.let { wss ->
                channelArn?.let { arn ->
                    signalingClient.connect(
                        wssEndpoint = wss,
                        channelArn = arn,
                        region = config.region,
                        accessKey = config.accessKey,
                        secretKey = config.secretKey,
                        isMaster = true
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect as Master", e)
            _connectionState.value = KvsConnectionState.ERROR
            Result.failure(e)
        }
    }

    private fun createPeerConnection(clientId: String): PeerConnection? {
        val factory = peerConnectionFactory ?: return null
        closePeerConnection(clientId)

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "[$clientId] Signaling state: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "[$clientId] ICE connection state: $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "[$clientId] ICE connection receiving: $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "[$clientId] ICE gathering state: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "[$clientId] Local ICE candidate: ${it.sdp}")
                    signalingClient.sendIceCandidate(it.sdp, it.sdpMid, it.sdpMLineIndex, clientId)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "[$clientId] ICE candidates removed")
            }

            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "[$clientId] Stream added")
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "[$clientId] Stream removed")
            }

            override fun onDataChannel(channel: DataChannel?) {
                Log.d(TAG, "[$clientId] Data channel")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "[$clientId] Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "[$clientId] Track added")
            }
        })

        // Add local tracks to peer connection
        localVideoTrack?.let { track ->
            peerConnection?.addTrack(track, listOf("local_stream"))
        }
        localAudioTrack?.let { track ->
            peerConnection?.addTrack(track, listOf("local_stream"))
        }

        return peerConnection
    }

    private fun handleSdpOffer(sdp: String, clientId: String) {
        scope.launch {
            Log.d(TAG, "Handling SDP Offer from $clientId")

            // Create peer connection for this viewer
            val peerConnection = createPeerConnection(clientId) ?: run {
                Log.e(TAG, "Failed to create peer connection")
                return@launch
            }
            peerConnections[clientId] = peerConnection

            // Set remote description (the offer)
            val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
            peerConnection.setRemoteDescription(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {}
                override fun onSetSuccess() {
                    Log.d(TAG, "[$clientId] Remote description set")
                    // Create answer
                    createAnswer(peerConnection, clientId)
                }
                override fun onCreateFailure(error: String?) {
                    Log.e(TAG, "[$clientId] Set remote description create failure: $error")
                }
                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "[$clientId] Set remote description failure: $error")
                }
            }, sessionDescription)
        }
    }

    private fun createAnswer(peerConnection: PeerConnection, clientId: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { answer ->
                    Log.d(TAG, "[$clientId] Answer created")
                    peerConnection.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(desc: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "[$clientId] Local description set, sending answer")
                            signalingClient.sendSdpAnswer(answer.description, clientId)
                        }
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "[$clientId] Set local description create failure: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "[$clientId] Set local description failure: $error")
                        }
                    }, answer)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[$clientId] Create answer failure: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "[$clientId] Create answer set failure: $error")
            }
        }, constraints)
    }

    private fun handleSdpAnswer(sdp: String, clientId: String) {
        val peerConnection = peerConnections[clientId] ?: run {
            Log.e(TAG, "No peer connection for $clientId")
            return
        }

        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "[$clientId] Remote description (answer) set")
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[$clientId] Set answer create failure: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "[$clientId] Set answer failure: $error")
            }
        }, sessionDescription)
    }

    private fun handleIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int, clientId: String) {
        val peerConnection = peerConnections[clientId] ?: run {
            Log.e(TAG, "No peer connection for $clientId")
            return
        }

        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        peerConnection.addIceCandidate(iceCandidate)
        Log.d(TAG, "[$clientId] Added ICE candidate")
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting")

        signalingClient.disconnect()

        peerConnections.keys.toList().forEach(::closePeerConnection)
        peerConnections.clear()

        _connectionState.value = KvsConnectionState.DISCONNECTED
        Log.d(TAG, "Disconnected")
    }

    fun stopLocalVideo() {
        Log.d(TAG, "Stopping local video")

        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        localVideoSink?.let { sink ->
            localVideoTrack?.removeSink(sink)
        }
        localVideoSink = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
    }

    fun release() {
        disconnect()
        stopLocalVideo()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null
        iceServers = emptyList()
        wssEndpoint = null
        channelArn = null
        isInitialized = false
    }

    private fun closePeerConnection(clientId: String) {
        val connection = peerConnections.remove(clientId) ?: return
        connection.close()
        connection.dispose()
    }

    private var isInitialized = false
}

enum class KvsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
