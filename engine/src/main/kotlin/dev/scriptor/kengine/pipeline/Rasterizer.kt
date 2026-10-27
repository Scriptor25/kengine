package dev.scriptor.kengine.pipeline

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

        fun edge(
            a: ScreenVertex,
            b: ScreenVertex,
            x: Float,
            y: Float,
        ): Float = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)

        fun isTopLeft(a: ScreenVertex, b: ScreenVertex): Boolean {
            val dx = b.x - a.x
            val dy = b.y - a.y
            return dy < 0.0f || (dy == 0.0f && dx > 0.0f)
        }

        fun inside(value: Float, inclusive: Boolean): Boolean =
            value > 0.0f || (value == 0.0f && inclusive)

        val inclusive0 = isTopLeft(triangle.v1, triangle.v2)
        val inclusive1 = isTopLeft(triangle.v2, triangle.v0)
        val inclusive2 = isTopLeft(triangle.v0, triangle.v1)

        val inverseArea = 1.0f / triangle.area

        for (y in minY..maxY) {
            val py = y + 0.5f

            for (x in minX..maxX) {
                val px = x + 0.5f

                val e0 = edge(triangle.v1, triangle.v2, px, py)
                val e1 = edge(triangle.v2, triangle.v0, px, py)
                val e2 = edge(triangle.v0, triangle.v1, px, py)

                if (
                    inside(e0, inclusive0) &&
                    inside(e1, inclusive1) &&
                    inside(e2, inclusive2)
                ) {
                    val b0 = e0 * inverseArea
                    val b1 = e1 * inverseArea
                    val b2 = e2 * inverseArea

                    val depth = (b0 * triangle.v0.depth + b1 * triangle.v1.depth + b2 * triangle.v2.depth)
                        .coerceIn(0.0f, 1.0f)

                    emit(
                        RasterFragment(
                            x, y,
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
    }
}
