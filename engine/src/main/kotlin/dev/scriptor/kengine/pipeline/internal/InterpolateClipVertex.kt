package dev.scriptor.kengine.pipeline.internal

import dev.scriptor.kengine.pipeline.FragmentShaderLayout
import dev.scriptor.kengine.pipeline.GraphicsPipeline
import dev.scriptor.kengine.pipeline.VertexShaderLayout

fun <V : VertexShaderLayout, F : FragmentShaderLayout> interpolateClipVertex(
    pipeline: GraphicsPipeline<V, F>,
    a: ClipVertex,
    b: ClipVertex,
    t: Float,
): ClipVertex {
    val varyings = Array(pipeline.varyings.size) { index ->
        val av = a.varyings[index]
        val bv = b.varyings[index]
        val lerp = pipeline.varyings[index].interpolator as Interpolator<Any>

        when {
            av != null && bv != null ->
                lerp(av, bv, t)

            else -> null
        }
    }

    return ClipVertex(
        a.px + (b.px - a.px) * t,
        a.py + (b.py - a.py) * t,
        a.pz + (b.pz - a.pz) * t,
        a.pw + (b.pw - a.pw) * t,
        varyings,
    )
}
