package dev.scriptor.kengine

import dev.scriptor.kengine.math.*
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.reflect.KClass

interface Surface {
    val width: Int
    val height: Int

    fun color(index: Int): Int
    fun color(index: Int, value: Int)

    fun depth(index: Int): Float
    fun depth(index: Int, value: Float)
}

private class RuntimeShaderContext(
    private val uniformValues: Array<Any>,
    private val inputValues: Array<Any>,
    private val outputValues: Array<Any>,
) : ShaderContext {

    override fun get(id: UniformId): Any {
        return uniformValues[id.index]
    }

    override fun get(id: InputId): Any {
        return inputValues[id.index]
    }

    override fun set(id: OutputId, value: Any) {
        outputValues[id.index] = value
    }
}

interface VertexFormat<T : Any> {

    val type: KClass<T>
    val size: Int

    fun get(buffer: ByteBuffer, offset: Int): T

    data object FLOAT : VertexFormat<Float> {
        override val type: KClass<Float> = Float::class
        override val size: Int = Float.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Float {
            return buffer.getFloat(offset)
        }
    }

    data object VEC2 : VertexFormat<Vec2f> {
        override val type: KClass<Vec2f> = Vec2f::class
        override val size: Int = Vec2f.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Vec2f {
            return buffer.getVec2f(offset)
        }
    }

    data object VEC3 : VertexFormat<Vec3f> {
        override val type: KClass<Vec3f> = Vec3f::class
        override val size: Int = Vec3f.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Vec3f {
            return buffer.getVec3f(offset)
        }
    }

    data object VEC4 : VertexFormat<Vec4f> {
        override val type: KClass<Vec4f> = Vec4f::class
        override val size: Int = Vec4f.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Vec4f {
            return buffer.getVec4f(offset)
        }
    }
}

data class VertexAttribute<T : Any>(
    val input: Input<T>,
    val format: VertexFormat<T>,
    val offset: Int,
)

data class VertexLayout(
    val stride: Int,
    val attributes: List<VertexAttribute<*>>,
) {
    init {
        require(stride > 0)

        for (attribute in attributes) {
            require(attribute.offset >= 0)
            require(attribute.offset + attribute.format.size <= stride)
            require(attribute.input.type == attribute.format.type)
        }
    }

    fun fetch(
        buffer: ByteBuffer,
        offset: Int,
        inputs: Array<Any>,
    ) {
        for (attribute in attributes) {
            inputs[attribute.input.id.index] = attribute.format.get(buffer, offset + attribute.offset)
        }
    }
}

class ClipVertex(
    val position: Vec4f,
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
    fun perspectiveWeights(): Triple<Float, Float, Float> {
        val w0 = b0 * invW0
        val w1 = b1 * invW1
        val w2 = b2 * invW2

        val inv = 1.0f / (w0 + w1 + w2)

        return Triple(w0 * inv, w1 * inv, w2 * inv)
    }
}

class Rasterizer(
    val tileSize: Int,
) {
    init {
        require(tileSize > 0)
    }

    fun rasterize(
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

interface Interpolator<T : Any> {
    operator fun invoke(a: T, b: T, t: Float): T
    operator fun invoke(a: T, wa: Float, b: T, wb: Float, c: T, wc: Float): T
}

data class Varying<T : Any>(
    val vertexOutput: Output<T>,
    val fragmentInput: Input<T>,
    val interpolator: Interpolator<T>,
)

enum class CullMode {
    NONE,
    FRONT,
    BACK,
    FRONT_AND_BACK,
}

class GraphicsPipeline(
    val vertexShader: VertexShader,
    val fragmentShader: FragmentShader,
    val vertexLayout: VertexLayout,
    val rasterizer: Rasterizer,
    val cullMode: CullMode = CullMode.BACK,
    val frontFaceCounterClockwise: Boolean = true,
    val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
        Thread.ofPlatform().daemon().factory(),
    ),
) {
    private val varyings: List<Varying<*>>

    init {
        val vertexOutputs = vertexShader.shader.outputs
            .filter { it.id != vertexShader.position.id }
            .associateBy { it.location }

        varyings = fragmentShader.shader.inputs.map { fragmentInput ->
            val vertexOutput = vertexOutputs[fragmentInput.location]
                ?: error("missing vertex output for fragment input location ${fragmentInput.location}")

            require(vertexOutput.type == fragmentInput.type)

            when (val type = vertexOutput.type) {
                Float::class -> Varying(
                    vertexOutput as Output<Float>,
                    fragmentInput as Input<Float>,
                    object : Interpolator<Float> {
                        override fun invoke(a: Float, b: Float, t: Float): Float {
                            return a + (b - a) * t
                        }

                        override fun invoke(a: Float, wa: Float, b: Float, wb: Float, c: Float, wc: Float): Float {
                            return a * wa + b * wb + c * wc
                        }
                    }
                )

                Vec2f::class -> Varying(
                    vertexOutput as Output<Vec2f>,
                    fragmentInput as Input<Vec2f>,
                    object : Interpolator<Vec2f> {
                        override fun invoke(a: Vec2f, b: Vec2f, t: Float): Vec2f {
                            return a + (b - a) * t
                        }

                        override fun invoke(
                            a: Vec2f,
                            wa: Float,
                            b: Vec2f,
                            wb: Float,
                            c: Vec2f,
                            wc: Float
                        ): Vec2f {
                            return a * wa + b * wb + c * wc
                        }
                    }
                )

                Vec3f::class -> Varying(
                    vertexOutput as Output<Vec3f>,
                    fragmentInput as Input<Vec3f>,
                    object : Interpolator<Vec3f> {
                        override fun invoke(a: Vec3f, b: Vec3f, t: Float): Vec3f {
                            return a + (b - a) * t
                        }

                        override fun invoke(
                            a: Vec3f,
                            wa: Float,
                            b: Vec3f,
                            wb: Float,
                            c: Vec3f,
                            wc: Float
                        ): Vec3f {
                            return a * wa + b * wb + c * wc
                        }
                    }
                )

                Vec4f::class -> Varying(
                    vertexOutput as Output<Vec4f>,
                    fragmentInput as Input<Vec4f>,
                    object : Interpolator<Vec4f> {
                        override fun invoke(a: Vec4f, b: Vec4f, t: Float): Vec4f {
                            return a + (b - a) * t
                        }

                        override fun invoke(
                            a: Vec4f,
                            wa: Float,
                            b: Vec4f,
                            wb: Float,
                            c: Vec4f,
                            wc: Float
                        ): Vec4f {
                            return a * wa + b * wb + c * wc
                        }
                    }
                )

                else -> error("unsupported type $type")
            }
        }
    }

    operator fun invoke(
        surface: Surface,
        mesh: Mesh,
        vertexUniforms: Array<Any> = emptyArray(),
        fragmentUniforms: Array<Any> = emptyArray(),
    ) {
        invoke(
            surface,
            mesh.vertexBuffer,
            mesh.elementBuffer,
            0,
            mesh.count,
            vertexUniforms,
            fragmentUniforms,
        )
    }

    operator fun invoke(
        surface: Surface,
        vertexBuffer: ByteBuffer,
        elementBuffer: ByteBuffer,
        first: Int,
        count: Int,
        vertexUniforms: Array<Any> = emptyArray(),
        fragmentUniforms: Array<Any> = emptyArray(),
    ) {
        val vi: Array<Any> = Array(vertexShader.shader.inputs.size) {}
        val vo: Array<Any> = Array(vertexShader.shader.outputs.size) {}
        val vc = RuntimeShaderContext(
            vertexUniforms,
            vi,
            vo,
        )

        val vertexCount = vertexBuffer.remaining() / vertexLayout.stride
        val vso: Array<Array<Any>> = Array(vertexCount) { Array(vertexShader.shader.outputs.size) {} }

        for (i in 0 until vertexCount) {
            vi.fill(Unit)
            vo.fill(Unit)

            vertexLayout.fetch(vertexBuffer, vertexBuffer.position() + i * vertexLayout.stride, vi)

            context(vc) { vertexShader.shader.main() }

            vo.copyInto(vso[i])
        }

        val tilesX = (surface.width + rasterizer.tileSize - 1) / rasterizer.tileSize
        val tilesY = (surface.height + rasterizer.tileSize - 1) / rasterizer.tileSize

        val bins = Array(tilesX * tilesY) { mutableListOf<RasterTriangle>() }

        for (index in first until first + count step 3) {
            val i0 = elementBuffer.getInt(Int.SIZE_BYTES * (index + 0))
            val i1 = elementBuffer.getInt(Int.SIZE_BYTES * (index + 1))
            val i2 = elementBuffer.getInt(Int.SIZE_BYTES * (index + 2))

            val triangle = clipVertexTriangle(
                vso[i0],
                vso[i1],
                vso[i2],
            )

            if (triangle.size < 3)
                continue

            for (i in 1 until triangle.lastIndex) {
                val prepared = prepareTriangle(
                    triangle[0],
                    triangle[i],
                    triangle[i + 1],
                    surface.width,
                    surface.height,
                ) ?: continue

                val tx0 = prepared.minX / rasterizer.tileSize
                val ty0 = prepared.minY / rasterizer.tileSize
                val tx1 = prepared.maxX / rasterizer.tileSize
                val ty1 = prepared.maxY / rasterizer.tileSize

                for (ty in ty0..ty1) {
                    for (tx in tx0..tx1) {
                        bins[tx + ty * tilesX] += prepared
                    }
                }
            }
        }

        val tasks = mutableListOf<Callable<Unit>>()

        for (ty in 0 until tilesY) {
            for (tx in 0 until tilesX) {
                val bin = bins[tx + ty * tilesX]
                if (bin.isEmpty()) {
                    continue
                }

                val tileMinX = tx * rasterizer.tileSize
                val tileMinY = ty * rasterizer.tileSize
                val tileMaxX = minOf(tileMinX + rasterizer.tileSize - 1, surface.width - 1)
                val tileMaxY = minOf(tileMinY + rasterizer.tileSize - 1, surface.height - 1)

                tasks += Callable {
                    val fi: Array<Any> = Array(fragmentShader.shader.inputs.size) {}
                    val fo: Array<Any> = Array(fragmentShader.shader.outputs.size) {}
                    val fc = RuntimeShaderContext(
                        fragmentUniforms,
                        fi,
                        fo,
                    )

                    for (triangle in bin) {
                        rasterizer.rasterize(
                            triangle,
                            tileMinX,
                            tileMinY,
                            tileMaxX,
                            tileMaxY,
                        ) { fragment ->
                            val index = fragment.x + fragment.y * surface.width

                            val depth = fragment.depth.coerceIn(0.0f, 1.0f)
                            val depthBuffer = surface.depth(index)

                            if (depth < depthBuffer) {
                                val weights = fragment.perspectiveWeights()

                                fi.fill(Unit)
                                fo.fill(Unit)

                                for (varyingIndex in varyings.indices) {
                                    val varying = varyings[varyingIndex]

                                    fi[varying.fragmentInput.id.index] =
                                        (varying.interpolator as Interpolator<Any>)(
                                            triangle.v0.varyings[varyingIndex],
                                            weights.first,
                                            triangle.v1.varyings[varyingIndex],
                                            weights.second,
                                            triangle.v2.varyings[varyingIndex],
                                            weights.third,
                                        )
                                }

                                context(fc) {
                                    fragmentShader.shader.main()
                                }

                                val color = fo[fragmentShader.color.id.index] as? Vec4f
                                    ?: error("no fragment shader output")

                                val r = (color.x.coerceIn(0.0f, 1.0f) * 255.999f).toInt() and 0xff
                                val g = (color.y.coerceIn(0.0f, 1.0f) * 255.999f).toInt() and 0xff
                                val b = (color.z.coerceIn(0.0f, 1.0f) * 255.999f).toInt() and 0xff

                                surface.color(index, (r shl 16) or (g shl 8) or b)
                                surface.depth(index, depth)
                            }
                        }
                    }
                }
            }
        }

        for (future in executor.invokeAll(tasks)) {
            future.get()
        }
    }

    private fun clipVertexTriangle(
        v0: Array<Any>,
        v1: Array<Any>,
        v2: Array<Any>,
    ): List<ClipVertex> {
        fun toClipVertex(outputs: Array<Any>): ClipVertex {
            val position = outputs[vertexShader.position.id.index] as? Vec4f
                ?: error("Vertex shader did not write a valid clip-space position")

            val values = Array(varyings.size) { index ->
                val outputId = varyings[index].vertexOutput.id.index
                outputs[outputId]
            }

            return ClipVertex(position, values)
        }

        var polygon = listOf(
            toClipVertex(v0),
            toClipVertex(v1),
            toClipVertex(v2),
        )

        // OpenGL-style homogeneous clip volume:
        // -w <= x <= w, -w <= y <= w, -w <= z <= w.
        val planes: List<(Vec4f) -> Float> = listOf(
            { p -> p.x + p.w }, // left
            { p -> p.w - p.x }, // right
            { p -> p.y + p.w }, // bottom
            { p -> p.w - p.y }, // top
            { p -> p.z + p.w }, // near
            { p -> p.w - p.z }, // far
        )

        for (distance in planes) {
            if (polygon.isEmpty()) break

            val output = ArrayList<ClipVertex>(polygon.size + 1)

            var previous = polygon.last()
            var previousDistance = distance(previous.position)

            for (current in polygon) {
                val currentDistance = distance(current.position)

                val previousInside = previousDistance >= 0.0f
                val currentInside = currentDistance >= 0.0f

                if (previousInside != currentInside) {
                    val denominator = previousDistance - currentDistance

                    if (denominator != 0.0f) {
                        val t = (previousDistance / denominator).coerceIn(0.0f, 1.0f)

                        output += interpolateClipVertex(previous, current, t)
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

    private fun interpolateClipVertex(
        a: ClipVertex,
        b: ClipVertex,
        t: Float,
    ): ClipVertex {
        val pa = a.position
        val pb = b.position

        val position = pa + (pb - pa) * t

        val values = Array(varyings.size) { index ->
            (varyings[index].interpolator as Interpolator<Any>)(
                a.varyings[index],
                b.varyings[index],
                t,
            )
        }

        return ClipVertex(position, values)
    }

    private fun prepareTriangle(
        v0: ClipVertex,
        v1: ClipVertex,
        v2: ClipVertex,
        width: Int,
        height: Int,
    ): RasterTriangle? {
        fun project(vertex: ClipVertex): ScreenVertex? {
            if (!vertex.position.isFinite() || vertex.position.w <= 0.0f)
                return null

            val invW = 1.0f / vertex.position.w
            val ndc = vertex.position.xyz * invW

            val x = (ndc.x * 0.5f + 0.5f) * width
            val y = (0.5f - ndc.y * 0.5f) * height
            val depth = ndc.z * 0.5f + 0.5f

            if (!x.isFinite() || !y.isFinite() || !depth.isFinite() || !invW.isFinite())
                return null

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

        val isFrontFace = when {
            frontFaceCounterClockwise -> area < 0.0f
            else -> area > 0.0f
        }

        when (cullMode) {
            CullMode.NONE -> Unit
            CullMode.FRONT -> if (isFrontFace) return null
            CullMode.BACK -> if (!isFrontFace) return null
            CullMode.FRONT_AND_BACK -> return null
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
}
