package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.math.simd.Program
import dev.scriptor.kengine.math.simd.allocate
import dev.scriptor.kengine.math.simd.invoke
import dev.scriptor.kengine.math.simdF
import dev.scriptor.kengine.pipeline.internal.RasterFragment
import dev.scriptor.kengine.pipeline.internal.RasterTriangle
import dev.scriptor.kengine.pipeline.internal.ScreenVertex

private class EdgeEquation(
    val a: Float,
    val b: Float,
    val inclusive: Boolean,
)

private fun createEdge(
    a: ScreenVertex,
    b: ScreenVertex,
): EdgeEquation {
    val dx = b.x - a.x
    val dy = b.y - a.y

    return EdgeEquation(
        a = -dy,
        b = dx,
        inclusive = dy < 0.0f || (dy == 0.0f && dx > 0.0f)
    )
}

private fun edge(
    a: ScreenVertex,
    b: ScreenVertex,
    x: Float,
    y: Float,
): Float = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)

private fun createRasterVector(
    inclusive0: Boolean,
    inclusive1: Boolean,
    inclusive2: Boolean,
): Program<Float, FloatArray> =
    simdF(15) { [
                    row0, row1, row2,
                    offsetX,
                    incrementX0, incrementX1, incrementX2,
                    offsetY,
                    incrementY0, incrementY1, incrementY2,
                    inverseArea,
                    depth0, depth1, depth2,
                ] ->

        val zero = 0.0f.asNode()
        val one = 1.0f.asNode()
        val none = (-1.0f).asNode()

        val e0 = fma(offsetY, incrementY0, fma(offsetX, incrementX0, row0))
        val e1 = fma(offsetY, incrementY1, fma(offsetX, incrementX1, row1))
        val e2 = fma(offsetY, incrementY2, fma(offsetX, incrementX2, row2))

        val c0 = if (inclusive0) e0 ge zero else e0 gt zero
        val c1 = if (inclusive1) e1 ge zero else e1 gt zero
        val c2 = if (inclusive2) e2 ge zero else e2 gt zero

        val coverage = c0 and c1 and c2

        val b0 = e0 * inverseArea
        val b1 = e1 * inverseArea
        val b2 = e2 * inverseArea

        val z = fma(
            b0, depth0,
            fma(
                b1, depth1,
                b2 * depth2,
            ),
        )

        val depth = max(zero, min(z, one))

        export(select(coverage, depth, none))
        export(select(coverage, b0, zero))
        export(select(coverage, b1, zero))
        export(select(coverage, b2, zero))
    }

private val rasterVectorMap = listOf(0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111)
    .associateWith {
        createRasterVector(
            it and 0b100 != 0,
            it and 0b010 != 0,
            it and 0b001 != 0,
        )
    }

class Rasterizer(
    val tileSize: Int,
) {
    init {
        require(tileSize > 0)
    }

    internal fun rasterize(
        triangle: RasterTriangle,
        clipMinX: Int,
        clipMinY: Int,
        clipMaxX: Int,
        clipMaxY: Int,
        emit: (RasterFragment) -> Unit,
    ) {
        val minX = maxOf(triangle.minX, clipMinX)
        val minY = maxOf(triangle.minY, clipMinY)
        val maxX = minOf(triangle.maxX, clipMaxX)
        val maxY = minOf(triangle.maxY, clipMaxY)

        if (minX > maxX || minY > maxY)
            return

        val v0 = triangle.v0
        val v1 = triangle.v1
        val v2 = triangle.v2

        val edge0 = createEdge(v1, v2)
        val edge1 = createEdge(v2, v0)
        val edge2 = createEdge(v0, v1)

        val inverseArea = 1.0f / triangle.area

        val width = maxX - minX + 1
        val height = maxY - minY + 1
        val count = width * height

        val startX = minX + 0.5f
        val startY = minY + 0.5f

        val start0 = edge(v1, v2, startX, startY)
        val start1 = edge(v2, v0, startX, startY)
        val start2 = edge(v0, v1, startX, startY)

        val offsetX = FloatArray(count) { (it % width).toFloat() }
        val offsetY = FloatArray(count) { (it / width).toFloat() }

        val depths = FloatArray(count)
        val b0s = FloatArray(count)
        val b1s = FloatArray(count)
        val b2s = FloatArray(count)

        val program = rasterVectorMap[0
                or if (edge0.inclusive) 0b100 else 0
                or if (edge1.inclusive) 0b010 else 0
                or if (edge2.inclusive) 0b001 else 0
        ]!!
        val context = program.allocate(depths, b0s, b1s, b2s)

        program(
            start0, start1, start2,
            offsetX,
            edge0.a, edge1.a, edge2.a,
            offsetY,
            edge0.b, edge1.b, edge2.b,
            inverseArea,
            v0.depth, v1.depth, v2.depth,
            context = context,
        )

        var ii = 0
        while (ii < count) {
            val i = ii++

            val depth = depths[i]
            val b0 = b0s[i]
            val b1 = b1s[i]
            val b2 = b2s[i]

            if (depth < 0.0f)
                continue

            emit(
                RasterFragment(
                    minX + (i % width),
                    minY + (i / width),
                    depth,
                    b0, b1, b2,
                    triangle.v0.invW,
                    triangle.v1.invW,
                    triangle.v2.invW,
                )
            )
        }
    }
}
