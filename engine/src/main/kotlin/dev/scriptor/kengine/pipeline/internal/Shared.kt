package dev.scriptor.kengine.pipeline.internal

import dev.scriptor.kengine.pipeline.Input
import dev.scriptor.kengine.pipeline.Output

class ClipVertex(
    val px: Float,
    val py: Float,
    val pz: Float,
    val pw: Float,
    val varyings: Array<Any>,
)

class ScreenVertex(
    val x: Float,
    val y: Float,
    val depth: Float,
    val invW: Float,
    val varyings: Array<Any>,
)

data class RasterTriangle(
    val v0: ScreenVertex,
    val v1: ScreenVertex,
    val v2: ScreenVertex,
    val area: Float,
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
)

data class RasterFragment(
    val x: Int,
    val y: Int,
    val depth: Float,
    val b0: Float,
    val b1: Float,
    val b2: Float,
    val invW0: Float,
    val invW1: Float,
    val invW2: Float,
) {
    fun perspectiveWeights(weights: FloatArray) {
        val w0 = b0 * invW0
        val w1 = b1 * invW1
        val w2 = b2 * invW2

        val inv = 1.0f / (w0 + w1 + w2)

        weights[0] = w0 * inv
        weights[1] = w1 * inv
        weights[2] = w2 * inv
    }
}

data class Varying<T : Any>(
    val output: Output<T>,
    val input: Input<T>,
    val interpolator: Interpolator<T>,
)
