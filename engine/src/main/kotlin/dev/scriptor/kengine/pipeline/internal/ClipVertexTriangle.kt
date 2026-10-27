package dev.scriptor.kengine.pipeline.internal

import dev.scriptor.kengine.math.Vec4f
import dev.scriptor.kengine.pipeline.FragmentShaderLayout
import dev.scriptor.kengine.pipeline.GraphicsPipeline
import dev.scriptor.kengine.pipeline.VertexShaderLayout

fun <V : VertexShaderLayout, F : FragmentShaderLayout> clipVertexTriangle(
    pipeline: GraphicsPipeline<V, F>,
    v0: Array<Any>,
    v1: Array<Any>,
    v2: Array<Any>,
): List<ClipVertex> {
    fun toClipVertex(outputs: Array<Any>): ClipVertex {
        val position = outputs[pipeline.vertexShaderLayout.position.id.index] as? Vec4f
            ?: error("Vertex shader did not write a valid clip-space position")

        val values = Array(pipeline.varyings.size) { index ->
            val outputId = pipeline.varyings[index].output.id.index
            outputs[outputId]
        }

        return ClipVertex(
            position.x,
            position.y,
            position.z,
            position.w,
            values,
        )
    }

    var polygon = listOf(
        toClipVertex(v0),
        toClipVertex(v1),
        toClipVertex(v2),
    )

    // OpenGL-style homogeneous clip volume:
    // -w <= x <= w, -w <= y <= w, -w <= z <= w.
    val planes: List<(Float, Float, Float, Float) -> Float> = listOf(
        { x, _, _, w -> x + w }, // left
        { x, _, _, w -> w - x }, // right
        { _, y, _, w -> y + w }, // bottom
        { _, y, _, w -> w - y }, // top
        { _, _, z, w -> z + w }, // near
        { _, _, z, w -> w - z }, // far
    )

    for (distance in planes) {
        if (polygon.isEmpty()) break

        val output = ArrayList<ClipVertex>(polygon.size + 1)

        var previous = polygon.last()
        var previousDistance = distance(previous.px, previous.py, previous.pz, previous.pw)

        for (current in polygon) {
            val currentDistance = distance(current.px, current.py, current.pz, current.pw)

            val previousInside = previousDistance >= 0.0f
            val currentInside = currentDistance >= 0.0f

            if (previousInside != currentInside) {
                val denominator = previousDistance - currentDistance

                if (denominator != 0.0f) {
                    val t = (previousDistance / denominator).coerceIn(0.0f, 1.0f)

                    output += interpolateClipVertex(
                        pipeline,
                        previous,
                        current,
                        t,
                    )
                }
            }

            if (currentInside) {
                output += current
            }

            previous = current
            previousDistance = currentDistance
        }

        polygon = output
    }

    return polygon
}
