package io.github.chayanforyou.arfurniture.ui.arview

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.chayanforyou.arfurniture.R
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.ViewNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumSet

@Composable
internal fun ARView(model: String) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val childNodes = rememberNodes()

    var viewAttachmentManager by remember { mutableStateOf<ViewAttachmentManager?>(null) }

    // Whether to show detected plane or not
    var planeRenderer by remember { mutableStateOf(true) }

    // Reason for plane detection failure
    var trackingFailureReason by remember {
        mutableStateOf<TrackingFailureReason?>(null)
    }
    var frame by remember { mutableStateOf<Frame?>(null) }

    // Add state for dimensions
    var showMeasurements by remember { mutableStateOf(false) }
    var modelDimensions by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

    Box(modifier = Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            planeRenderer = planeRenderer,
            sessionCameraConfig = { session -> session.disableDepthConfig() },
            sessionConfiguration = { _, config -> config.configure() },
            onGestureListener = rememberOnGestureListener(
                onMove = { _, event, node ->
                    if (node == null) return@rememberOnGestureListener
                    // Move gesture to move model node. Instead of changing model position
                    // change anchor node position (parent of model node)
                    frame?.getPose(event)?.position?.let { position ->
                        val anchor = (node.parent as? AnchorNode) ?: node
                        anchor.worldPosition = position
                    }
                }
            ),
            onTrackingFailureChanged = {
                trackingFailureReason = it
            },
            onSessionUpdated = { _, updatedFrame ->
                frame = updatedFrame
            },
            onSessionCreated = {
                planeRenderer = false
            },
            onViewCreated = {
                Log.e("ViewNode", "onViewCreated...........")
                viewAttachmentManager = ViewAttachmentManager(context, this)
            }
        )

        // Show measurements overlay when model is placed
        AnimatedVisibility(
            visible = showMeasurements,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            ModelMeasurements(
                width = modelDimensions.first,
                height = modelDimensions.second,
                depth = modelDimensions.third
            )
        }
    }

    LaunchedEffect(key1 = model) {
        frame?.let { currentFrame ->
            val (x, y) = view.viewport.run {
                width / 2F to height / 2F
            }

            val hitResult = currentFrame.getHitResult(x, y)
            hitResult?.let { hit ->
                val anchor = hit.createAnchor()
                val modelNode = anchor.createAnchorNode(
                    context,
                    engine,
                    modelLoader,
                    viewAttachmentManager,
                    model
                )

                childNodes.add(modelNode)
            }
        }
    }
}

/**
 * Disable Depth API
 * Depth API may give better results. But, some devices (tested on Samsung S20+)
 * creates too much lag. Reason is unknown. Looks like the image acquired
 * with `frame.acquireCameraImage()` is not realised. so we won't be able to acquire
 * new frames when the rate limit is exceeded.
 */
private fun Session.disableDepthConfig(): CameraConfig {
    val filter = CameraConfigFilter(this)
    filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))
    val cameraConfigList = getSupportedCameraConfigs(filter)
    return cameraConfigList.first()
}

/**
 * Configurations for ARSession
 */
private fun Config.configure() {
    depthMode = Config.DepthMode.DISABLED
    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
    instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
    planeFindingMode = PlaneFindingMode.HORIZONTAL
    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
}

/**
 * Create anchor node at the center of the screen
 */
private fun Anchor.createAnchorNode(
    context: Context,
    engine: Engine,
    modelLoader: ModelLoader,
    viewAttachmentManager: ViewAttachmentManager?,
    model: String,
): AnchorNode {

    val anchorNode = AnchorNode(engine, this)
    val modelNode = ModelNode(
        modelInstance = modelLoader.createModelInstance("models/${model}.glb"),
        //scaleToUnits = 0.5f
    ).apply {
        // Change models initial scale. Currently using width
        scale = Scale(0.3F / size.x)
        // Reset model's initial rotation
        rotation = Rotation()
        // Enable custom gestures on model
        enableGestures()
    }

    // Add model to anchor
    anchorNode.addChildNode(modelNode)


    // Get the model's actual size
    val modelSize = modelNode.size

    // Create MeasurementNode with actual model dimensions
    viewAttachmentManager?.let { manager ->
        val measurementNode = MeasurementNode(
            engine = engine,
            modelLoader = modelLoader,
            viewAttachmentManager = manager,
            context = context,
            width = modelSize.x,    // Use actual model width
            height = modelSize.y,   // Use actual model height
            depth = modelSize.z     // Use actual model depth
        )

        // Add measurement node as a child of the model node
        modelNode.addChildNode(measurementNode)
    }

//    // Height label node
//    val heightNode = ViewNode(engine, modelLoader, viewAttachmentManager).apply {
//        position = Position(0f, scaledSize.y + 0.05f, 0f)
//        scale = Scale(0.2f)
//        loadView(
//            context = context,
//            layoutResId = R.layout.label_layout,
//            onLoaded = { _, view ->
//                view.findViewById<TextView>(R.id.labelTextView)?.text =
//                    "Height\n${scaledSize.y.toFeet()} ft"
//            }
//        )
//    }
//
//    // Width label node
//    val widthNode = ViewNode(engine).apply {
//        position = Position(scaledSize.x / 2 + 0.05f, 0f, 0f)
//        scale = Scale(0.2f)
//        loadView(
//            context = context,
//            layoutResId = R.layout.label_layout,
//            onLoaded = { _, view ->
//                view.findViewById<TextView>(R.id.labelTextView)?.text =
//                    "Width\n${scaledSize.x.toFeet()} ft"
//            }
//        )
//    }
//
//    // Depth label node
//    val depthNode = ViewNode(engine, modelLoader, viewAttachmentManager).apply {
//        position = Position(0f, 0f, scaledSize.z / 2 + 0.05f)
//        scale = Scale(0.2f)
//        loadView(
//            context = context,
//            layoutResId = R.layout.label_layout,
//            onLoaded = { _, view ->
//                view.findViewById<TextView>(R.id.labelTextView)?.text =
//                    "Depth\n${scaledSize.z.toFeet()} ft"
//            }
//        )
//    }

    // Attach measurement labels to the model
//    modelNode.addChildNode(heightNode)
//    modelNode.addChildNode(widthNode)
//    modelNode.addChildNode(depthNode)

    return anchorNode
}

private fun Float.toFeet(): String = String.format("%.1f", this * 3.28084)

/**
 * Get hit result from the given [x] & [y] coordinates.
 */
private fun Frame.getHitResult(x: Float, y: Float): HitResult? =
    hitTestInstantPlacement(x, y, 1.0f).firstOrNull()


/**
 * Get real world position from the given [MotionEvent] coordinates.
 */
private fun Frame.getPose(event: MotionEvent): Pose? =
    hitTest(event).firstOrNull()?.hitPose

/**
 * Enable gestures for receiver [ModelNode].
 *
 * The default gestures for scale has exponential effect.
 * So using custom gesture for scaling is better.
 */
private fun ModelNode.enableGestures() {
    var previousScale = 0F
    onScale = { _, _, value ->
        scale = Scale(previousScale * value)
        false
    }
    onScaleBegin = { _, _ ->
        previousScale = scale.x
        false
    }
    onScaleEnd = { _, _ ->
        previousScale = 0F
    }

    // Set rendering priority higher to properly load occlusion.
    setPriority(7)
    // Model Node needs to be editable for independent rotation from the anchor rotation
    isEditable = true
    isScaleEditable = true
    isRotationEditable = true
    editableScaleRange = Float.MIN_VALUE..Float.MAX_VALUE
}