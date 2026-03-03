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
import hoon.example.androidsandbox.data.kvs.signaling.KvsSignalingClient
import hoon.example.androidsandbox.data.kvs.signaling.SignalingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KvsViewerClient @Inject constructor(
    private val config: KvsClientConfig
) {
    companion object {
        private const val TAG = "KvsViewerClient"
        private const val VIEWER_CLIENT_ID = "viewer"
        private const val OFFER_RETRY_INTERVAL_MS = 5_000L
        private const val FRAME_WATCHDOG_INTERVAL_MS = 2_000L
        private const val FRAME_TIMEOUT_MS = 8_000L
        private const val ICE_DISCONNECTED_RESTART_DELAY_MS = 3_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(KvsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<KvsConnectionState> = _connectionState.asStateFlow()
    private val _isReceivingVideo = MutableStateFlow(false)
    val isReceivingVideo: StateFlow<Boolean> = _isReceivingVideo.asStateFlow()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var eglBase: EglBase? = null

    private var remoteVideoSink: VideoSink? = null
    private var trackedRemoteVideoSink: VideoSink? = null

    private val signalingClient = KvsSignalingClient()
    private var iceServers: List<PeerConnection.IceServer> = emptyList()

    private var wssEndpoint: String? = null
    private var channelArn: String? = null
    private var isInitialized = false
    private var hasReceivedAnswer = false
    private var offerRetryJob: Job? = null
    private var frameWatchdogJob: Job? = null
    private var delayedIceRestartJob: Job? = null
    private var isRestartingNegotiation = false
    private var lastFrameReceivedAtMs: Long = 0L

    init {
        observeSignalingEvents()
    }

    private fun observeSignalingEvents() {
        scope.launch {
            signalingClient.signalingEvents.collect { event ->
                when (event) {
                    is SignalingEvent.Connected -> {
                        Log.d(TAG, "Signaling connected, starting offer retry loop")
                        _connectionState.value = KvsConnectionState.CONNECTED
                        _isReceivingVideo.value = false
                        startOfferRetryLoop()
                        startFrameWatchdog()
                    }
                    is SignalingEvent.Disconnected -> {
                        Log.d(TAG, "Signaling disconnected")
                        stopOfferRetryLoop()
                        stopFrameWatchdog()
                        cancelDelayedIceRestart()
                        hasReceivedAnswer = false
                        _isReceivingVideo.value = false
                        _connectionState.value = KvsConnectionState.DISCONNECTED
                    }
                    is SignalingEvent.Error -> {
                        Log.e(TAG, "Signaling error: ${event.message}")
                        stopOfferRetryLoop()
                        stopFrameWatchdog()
                        cancelDelayedIceRestart()
                        _isReceivingVideo.value = false
                        _connectionState.value = KvsConnectionState.ERROR
                    }
                    is SignalingEvent.SdpOffer -> {
                        // Viewer doesn't receive offers
                        Log.w(TAG, "Viewer received unexpected SDP Offer")
                    }
                    is SignalingEvent.SdpAnswer -> {
                        Log.d(TAG, "Received SDP Answer")
                        handleSdpAnswer(event.sdp)
                    }
                    is SignalingEvent.IceCandidate -> {
                        Log.d(TAG, "Received ICE Candidate")
                        handleIceCandidate(event.candidate, event.sdpMid, event.sdpMLineIndex)
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

    fun setRemoteVideoSink(sink: VideoSink) {
        clearRemoteVideoSink()
        remoteVideoSink = sink
        trackedRemoteVideoSink = VideoSink { frame ->
            lastFrameReceivedAtMs = System.currentTimeMillis()
            _isReceivingVideo.value = true
            sink.onFrame(frame)
        }
        trackedRemoteVideoSink?.let { trackedSink ->
            remoteVideoTrack?.addSink(trackedSink)
        }
        Log.d(TAG, "Remote video sink set")
    }

    fun clearRemoteVideoSink() {
        trackedRemoteVideoSink?.let { trackedSink ->
            remoteVideoTrack?.removeSink(trackedSink)
        }
        trackedRemoteVideoSink = null
        remoteVideoSink = null
    }

    suspend fun connectAsViewer(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = KvsConnectionState.CONNECTING
            Log.d(TAG, "Connecting as Viewer to channel: ${config.channelName}")

            val credentials = BasicAWSCredentials(config.accessKey, config.secretKey)

            // Get signaling channel ARN
            val kvsClient = AWSKinesisVideoClient(credentials)
            kvsClient.setRegion(Region.getRegion(config.region))

            val describeRequest = DescribeSignalingChannelRequest()
                .withChannelName(config.channelName)
            val channelInfo = kvsClient.describeSignalingChannel(describeRequest)
            channelArn = channelInfo.channelInfo.channelARN

            Log.d(TAG, "Channel ARN: $channelArn")

            // Get signaling channel endpoints (as VIEWER)
            val endpointConfig = SingleMasterChannelEndpointConfiguration()
                .withProtocols("WSS", "HTTPS")
                .withRole(ChannelRole.VIEWER)

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
                .withClientId(VIEWER_CLIENT_ID)

            val iceServerResponse = signalingApiClient.getIceServerConfig(iceServerRequest)
            iceServers = iceServerResponse.iceServerList.map { server ->
                PeerConnection.IceServer.builder(server.uris)
                    .setUsername(server.username)
                    .setPassword(server.password)
                    .createIceServer()
            }
            Log.d(TAG, "ICE Servers: ${iceServers.size}")

            // Create peer connection before connecting to signaling
            withContext(Dispatchers.Main) {
                createPeerConnection()
            }

            // Connect to signaling server as Viewer
            wssEndpoint?.let { wss ->
                channelArn?.let { arn ->
                    signalingClient.connect(
                        wssEndpoint = wss,
                        channelArn = arn,
                        region = config.region,
                        accessKey = config.accessKey,
                        secretKey = config.secretKey,
                        isMaster = false
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect as Viewer", e)
            _connectionState.value = KvsConnectionState.ERROR
            Result.failure(e)
        }
    }

    private fun createPeerConnection() {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "PeerConnectionFactory is null")
            return
        }
        closePeerConnection()

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        cancelDelayedIceRestart()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        _isReceivingVideo.value = false
                        scheduleIceDisconnectedRestart()
                    }
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> {
                        cancelDelayedIceRestart()
                        restartNegotiation("ICE state changed to $state")
                    }
                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "ICE connection receiving: $receiving")
            }

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gathering state: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "Local ICE candidate: ${it.sdp}")
                    // Viewer sends ICE candidates without recipientClientId (goes to Master)
                    signalingClient.sendIceCandidate(it.sdp, it.sdpMid, it.sdpMLineIndex, null)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "ICE candidates removed")
            }

            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "Stream added: ${stream?.videoTracks?.size} video tracks")
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "Stream removed")
                _isReceivingVideo.value = false
                restartNegotiation("Remote stream removed")
            }

            override fun onDataChannel(channel: DataChannel?) {
                Log.d(TAG, "Data channel")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
                restartNegotiation("PeerConnection requested renegotiation")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "Track added: ${receiver?.track()?.kind()}")
                receiver?.track()?.let { track ->
                    if (track is VideoTrack) {
                        remoteVideoTrack = track
                        Log.d(TAG, "Adding remote video track to sink")
                        scope.launch(Dispatchers.Main) {
                            trackedRemoteVideoSink?.let { trackedSink ->
                                track.addSink(trackedSink)
                            }
                        }
                    }
                }
            }
        })

        // Add transceiver for receiving video
        peerConnection?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )

        // Add transceiver for receiving audio
        peerConnection?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )

        Log.d(TAG, "PeerConnection created")
    }

    private fun startOfferRetryLoop() {
        hasReceivedAnswer = false
        stopOfferRetryLoop()
        offerRetryJob = scope.launch {
            while (isActive && !hasReceivedAnswer && _connectionState.value == KvsConnectionState.CONNECTED) {
                createAndSendOffer()
                delay(OFFER_RETRY_INTERVAL_MS)
            }
        }
    }

    private fun stopOfferRetryLoop() {
        offerRetryJob?.cancel()
        offerRetryJob = null
    }

    private fun startFrameWatchdog() {
        stopFrameWatchdog()
        frameWatchdogJob = scope.launch {
            while (isActive && _connectionState.value == KvsConnectionState.CONNECTED) {
                delay(FRAME_WATCHDOG_INTERVAL_MS)

                if (!hasReceivedAnswer) {
                    continue
                }

                val lastFrameTime = lastFrameReceivedAtMs
                if (lastFrameTime == 0L) {
                    continue
                }

                if (System.currentTimeMillis() - lastFrameTime > FRAME_TIMEOUT_MS) {
                    restartNegotiation("No remote frames for ${FRAME_TIMEOUT_MS}ms")
                    lastFrameReceivedAtMs = 0L
                }
            }
        }
    }

    private fun stopFrameWatchdog() {
        frameWatchdogJob?.cancel()
        frameWatchdogJob = null
    }

    private fun scheduleIceDisconnectedRestart() {
        if (delayedIceRestartJob?.isActive == true) {
            return
        }
        delayedIceRestartJob = scope.launch {
            delay(ICE_DISCONNECTED_RESTART_DELAY_MS)
            val pc = peerConnection ?: return@launch
            if (pc.iceConnectionState() == PeerConnection.IceConnectionState.DISCONNECTED) {
                restartNegotiation("ICE stayed DISCONNECTED for ${ICE_DISCONNECTED_RESTART_DELAY_MS}ms")
            }
        }
    }

    private fun cancelDelayedIceRestart() {
        delayedIceRestartJob?.cancel()
        delayedIceRestartJob = null
    }

    private fun restartNegotiation(reason: String) {
        if (_connectionState.value != KvsConnectionState.CONNECTED) {
            return
        }
        if (isRestartingNegotiation) {
            Log.d(TAG, "Negotiation restart skipped: already restarting")
            return
        }

        scope.launch(Dispatchers.Main) {
            isRestartingNegotiation = true
            try {
                Log.d(TAG, "Restarting negotiation: $reason")
                cancelDelayedIceRestart()
                hasReceivedAnswer = false
                lastFrameReceivedAtMs = 0L
                _isReceivingVideo.value = false
                createPeerConnection()
                startOfferRetryLoop()
            } finally {
                isRestartingNegotiation = false
            }
        }
    }

    private fun createAndSendOffer() {
        if (hasReceivedAnswer) {
            return
        }

        val pc = peerConnection ?: run {
            Log.e(TAG, "PeerConnection is null")
            return
        }
        if (pc.signalingState() != PeerConnection.SignalingState.STABLE) {
            return
        }

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { offer ->
                    Log.d(TAG, "Offer created")
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(desc: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Local description set, sending offer")
                            // Viewer sends offer without recipientClientId (goes to Master)
                            signalingClient.sendSdpOffer(offer.description, null)
                        }
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "Set local description create failure: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "Set local description failure: $error")
                        }
                    }, offer)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Create offer failure: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Create offer set failure: $error")
            }
        }, constraints)
    }

    private fun handleSdpAnswer(sdp: String) {
        val pc = peerConnection ?: run {
            Log.e(TAG, "PeerConnection is null")
            return
        }
        if (pc.signalingState() != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            Log.d(TAG, "Ignoring stale SDP answer in state: ${pc.signalingState()}")
            return
        }

        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        pc.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description (answer) set successfully")
                hasReceivedAnswer = true
                lastFrameReceivedAtMs = System.currentTimeMillis()
                stopOfferRetryLoop()
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Set answer create failure: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Set answer failure: $error")
            }
        }, sessionDescription)
    }

    private fun handleIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        val pc = peerConnection ?: run {
            Log.e(TAG, "PeerConnection is null")
            return
        }

        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        pc.addIceCandidate(iceCandidate)
        Log.d(TAG, "Added ICE candidate")
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting")

        stopOfferRetryLoop()
        stopFrameWatchdog()
        cancelDelayedIceRestart()
        hasReceivedAnswer = false
        lastFrameReceivedAtMs = 0L
        _isReceivingVideo.value = false
        isRestartingNegotiation = false
        signalingClient.disconnect()

        clearRemoteVideoSink()
        closePeerConnection()

        _connectionState.value = KvsConnectionState.DISCONNECTED
        Log.d(TAG, "Disconnected")
    }

    fun release() {
        disconnect()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null
        remoteVideoTrack = null
        hasReceivedAnswer = false
        stopOfferRetryLoop()
        stopFrameWatchdog()
        cancelDelayedIceRestart()
        lastFrameReceivedAtMs = 0L
        _isReceivingVideo.value = false
        isRestartingNegotiation = false
        iceServers = emptyList()
        wssEndpoint = null
        channelArn = null
        isInitialized = false
    }

    private fun closePeerConnection() {
        trackedRemoteVideoSink?.let { trackedSink ->
            remoteVideoTrack?.removeSink(trackedSink)
        }
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        remoteVideoTrack = null
    }
}
