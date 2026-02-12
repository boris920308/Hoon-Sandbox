package hoon.example.androidsandbox.presentation.kvs.master.component

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSink

@Composable
fun CameraPreview(
    eglBaseContext: EglBase.Context?,
    onSurfaceReady: (VideoSink) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val surfaceViewRenderer = remember {
        SurfaceViewRenderer(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(eglBaseContext) {
        if (eglBaseContext != null) {
            surfaceViewRenderer.init(eglBaseContext, null)
            surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            surfaceViewRenderer.setMirror(true)
            surfaceViewRenderer.setEnableHardwareScaler(true)
            onSurfaceReady(surfaceViewRenderer)
        }

        onDispose {
            surfaceViewRenderer.release()
        }
    }

    AndroidView(
        factory = { surfaceViewRenderer },
        modifier = modifier
    )
}
