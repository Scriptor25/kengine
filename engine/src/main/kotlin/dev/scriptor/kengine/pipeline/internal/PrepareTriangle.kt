package dev.scriptor.kengine.pipeline.internal

import dev.scriptor.kengine.pipeline.*
import kotlin.math.ceil
import kotlin.math.floor

fun <V : VertexShaderLayout, F : FragmentShaderLayout> prepareTriangle(
    pipeline: GraphicsPipeline<V, F>,
    v0: ClipVertex,
    v1: ClipVertex,
    v2: ClipVertex,
    width: Int,
    height: Int,
): RasterTriangle? {
    fun project(vertex: ClipVertex): ScreenVertex? {
        if (
            !vertex.px.isFinite() ||
            !vertex.py.isFinite() ||
            !vertex.pz.isFinite() ||
            !vertex.pw.isFinite() ||
            vertex.pw <= 0.0f
        ) return null

        val invW = 1.0f / vertex.pw
        if (!invW.isFinite())
            return null

        val nx = vertex.px * invW
        val ny = vertex.py * invW
        val nz = vertex.pz * invW

        val x = (nx * 0.5f + 0.5f) * width
        val y = (0.5f - ny * 0.5f) * height
        val depth = nz * 0.5f + 0.5f

        if (
            !x.isFinite() ||
            !y.isFinite() ||
            !depth.isFinite()
        ) return null

        return ScreenVertex(
            x, y,
            depth,
            invW,
            vertex.varyings,
        )
    }

    fun orient(a: ScreenVertex, b: ScreenVertex, c: ScreenVertex): Float =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    val p0 = project(v0) ?: return null
    var p1 = project(v1) ?: return null
    var p2 = project(v2) ?: return null

    var area = orient(p0, p1, p2)

    if (area == 0.0f || !area.isFinite())
        return null

    val isFrontFace = when (pipeline.frontFace) {
        FrontFace.Clockwise -> area > 0.0f
        FrontFace.CounterClockwise -> area < 0.0f
    }

    when (pipeline.cullMode) {
        CullMode.None -> Unit
        CullMode.Front -> if (isFrontFace) return null
        CullMode.Back -> if (!isFrontFace) return null
        CullMode.FrontAndBack -> return null
    }

    if (area < 0.0f) {
        val tmp = p1
        p1 = p2
        p2 = tmp
        area = -area
    }

    val minScreenX = minOf(p0.x, p1.x, p2.x)
    val minScreenY = minOf(p0.y, p1.y, p2.y)
    val maxScreenX = maxOf(p0.x, p1.x, p2.x)
    val maxScreenY = maxOf(p0.y, p1.y, p2.y)

    val minX = maxOf(0, ceil(minScreenX - 0.5f).toInt())
    val minY = maxOf(0, ceil(minScreenY - 0.5f).toInt())
    val maxX = minOf(width - 1, floor(maxScreenX - 0.5f).toInt())
    val maxY = minOf(height - 1, floor(maxScreenY - 0.5f).toInt())

    if (minX > maxX || minY > maxY)
        return null

    return RasterTriangle(
        p0, p1, p2,
        area,
        minX, minY, maxX, maxY,
    )
}
