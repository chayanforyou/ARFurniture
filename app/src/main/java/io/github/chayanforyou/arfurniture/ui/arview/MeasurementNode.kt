package io.github.chayanforyou.arfurniture.ui.arview

import android.content.Context
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.filament.Engine
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.chayanforyou.arfurniture.R
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode

class MeasurementNode(
    engine: Engine,
    modelLoader: ModelLoader,
    private val viewAttachmentManager: ViewAttachmentManager,
    private val context: Context,
    private val width: Float,
    private val height: Float,
    private val depth: Float
) : Node(engine) {

    init {
        // Make sure the node is visible
        isVisible = true

        // Height measurement
        val heightLabel = createLabel("${String.format("%.1f", height)}m")
        val heightNode = createViewNode(
            engine = engine,
            modelLoader = modelLoader,
            textView = heightLabel,
            position = Position(0f, height + 0.1f, 0f)  // Position above the model
        )
        addChildNode(heightNode)

        // Width measurement
        val widthLabel = createLabel("${String.format("%.1f", width)}m")
        val widthNode = createViewNode(
            engine = engine,
            modelLoader = modelLoader,
            textView = widthLabel,
            position = Position(width/2 + 0.1f, 0f, 0f)  // Position to the right of the model
        )
        addChildNode(widthNode)

        // Depth measurement
        val depthLabel = createLabel("${String.format("%.1f", depth)}m")
        val depthNode = createViewNode(
            engine = engine,
            modelLoader = modelLoader,
            textView = depthLabel,
            position = Position(0f, 0f, depth/2 + 0.1f)  // Position in front of the model
        )
        addChildNode(depthNode)
    }

    private fun createLabel(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = ContextCompat.getDrawable(context, R.drawable.measurement_background)
            gravity = Gravity.CENTER
            setPadding(20, 8, 20, 8)
            textSize = 16f  // Make text bigger
        }
    }

    private fun createViewNode(
        engine: Engine,
        modelLoader: ModelLoader,
        textView: TextView,
        position: Position
    ): ViewNode {
        return ViewNode(
            engine = engine,
            modelLoader = modelLoader,
            viewAttachmentManager = viewAttachmentManager
        ).apply {
            // Make sure the node is visible
            isVisible = true

            // Add the view
            viewAttachmentManager.addView(textView)

            // Set position
            this.position = position
        }
    }
}