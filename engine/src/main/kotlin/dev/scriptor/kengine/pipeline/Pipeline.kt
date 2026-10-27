package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.Mesh
import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.floor

private interface Interpolator<T : Any> {
    operator fun invoke(a: T, b: T, t: Float): T
    operator fun invoke(a: T, wa: Float, b: T, wb: Float, c: T, wc: Float): T
}

private data class Varying<T : Any>(
    val vertexOutput: Output<T>,
    val fragmentInput: Input<T>,
    val interpolator: Interpolator<T>,
)

private class ShaderContextImpl(
    val uniformValues: Array<Any>,
    val inputValues: Array<Any>,
    val outputValues: Array<Any>,
) : ShaderContext {

    override fun get(id: UniformId): Any {
        return uniformValues[id.index]
    }

    override fun get(id: InputId): Any {
        return inputValues[id.index]
    }

    override fun get(id: OutputId): Any {
        return outputValues[id.index]
    }

    override fun set(id: OutputId, value: Any) {
        outputValues[id.index] = value
    }
}

class GraphicsPipeline<V : VertexShaderLayout, F : FragmentShaderLayout>(
    val vertexShaderLayout: V,
    val fragmentShaderLayout: F,
    val vertexShaderFactory: (ShaderContext) -> Shader<V>,
    val fragmentShaderFactory: (ShaderContext) -> Shader<F>,
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
        val vertexOutputs = vertexShaderLayout.outputs
            .filter { it.id != vertexShaderLayout.position.id }
            .associateBy { it.location }

        varyings = fragmentShaderLayout.inputs.map { fragmentInput ->
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
        vertexUniformValues: Array<Any> = emptyArray(),
        fragmentUniformValues: Array<Any> = emptyArray(),
    ) {
        invoke(
            surface,
            mesh.vertexBuffer,
            mesh.elementBuffer,
            0,
            mesh.count,
            vertexUniformValues,
            fragmentUniformValues,
        )
    }

    operator fun invoke(
        surface: Surface,
        mesh: Mesh,
        firstInstance: Int,
        instanceCount: Int,
        vertexUniformValues: Array<Any> = emptyArray(),
        fragmentUniformValues: Array<Any> = emptyArray(),
    ) {
        invoke(
            surface,
            mesh.vertexBuffer,
            mesh.elementBuffer,
            0,
            mesh.count,
            firstInstance,
            instanceCount,
            vertexUniformValues,
            fragmentUniformValues,
        )
    }

    operator fun invoke(
        surface: Surface,
        vertexBuffer: ByteBuffer,
        elementBuffer: ByteBuffer,
        firstElement: Int,
        elementCount: Int,
        vertexUniformValues: Array<Any> = emptyArray(),
        fragmentUniformValues: Array<Any> = emptyArray(),
    ) {
        invoke(
            surface,
            vertexBuffer,
            elementBuffer,
            firstElement,
            elementCount,
            0,
            1,
            vertexUniformValues,
            fragmentUniformValues,
        )
    }

    operator fun invoke(
        surface: Surface,
        vertexBuffer: ByteBuffer,
        elementBuffer: ByteBuffer,
        firstElement: Int,
        elementCount: Int,
        firstInstance: Int,
        instanceCount: Int,
        vertexUniformValues: Array<Any> = emptyArray(),
        fragmentUniformValues: Array<Any> = emptyArray(),
    ) {
        val vertexInputValues: Array<Any> = Array(vertexShaderLayout.inputs.size) {}
        val vertexOutputValues: Array<Any> = Array(vertexShaderLayout.outputs.size) {}
        val vertexShaderContext = ShaderContextImpl(
            vertexUniformValues,
            vertexInputValues,
            vertexOutputValues,
        )

        val vertexShader = vertexShaderFactory(vertexShaderContext)

        val vertexCount = vertexBuffer.remaining() / vertexLayout.stride
        val vertexShaderOutput: Array<Array<Any>> = Array(vertexCount) { Array(vertexShaderLayout.outputs.size) {} }

        val tilesX = (surface.width + rasterizer.tileSize - 1) / rasterizer.tileSize
        val tilesY = (surface.height + rasterizer.tileSize - 1) / rasterizer.tileSize

        val bins = Array(tilesX * tilesY) { mutableListOf<RasterTriangle>() }

        for (instance in firstInstance until firstInstance + instanceCount) {
            for (vertex in 0 until vertexCount) {
                vertexInputValues.fill(Unit)
                vertexOutputValues.fill(Unit)

                vertexInputValues[vertexShaderLayout.instance.id.index] = instance

                vertexLayout.fetch(
                    vertexBuffer,
                    vertexBuffer.position() + vertex * vertexLayout.stride,
                    vertexInputValues,
                )

                vertexShader.main()

                vertexOutputValues.copyInto(vertexShaderOutput[vertex])
            }

            for (index in firstElement until firstElement + elementCount step 3) {
                val index0 = elementBuffer.getInt(Int.SIZE_BYTES * (index + 0))
                val index1 = elementBuffer.getInt(Int.SIZE_BYTES * (index + 1))
                val index2 = elementBuffer.getInt(Int.SIZE_BYTES * (index + 2))

                val triangle = clipVertexTriangle(
                    vertexShaderOutput[index0],
                    vertexShaderOutput[index1],
                    vertexShaderOutput[index2],
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
                    val fragmentInputValues: Array<Any> = Array(fragmentShaderLayout.inputs.size) {}
                    val fragmentOutputValues: Array<Any> = Array(fragmentShaderLayout.outputs.size) {}
                    val fragmentShaderContext = ShaderContextImpl(
                        fragmentUniformValues,
                        fragmentInputValues,
                        fragmentOutputValues,
                    )

                    val fragmentShader = fragmentShaderFactory(fragmentShaderContext)

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

                                fragmentInputValues.fill(Unit)
                                fragmentOutputValues.fill(Unit)

                                for (varyingIndex in varyings.indices) {
                                    val varying = varyings[varyingIndex]

                                    fragmentInputValues[varying.fragmentInput.id.index] =
                                        (varying.interpolator as Interpolator<Any>)(
                                            triangle.v0.varyings[varyingIndex],
                                            weights.first,
                                            triangle.v1.varyings[varyingIndex],
                                            weights.second,
                                            triangle.v2.varyings[varyingIndex],
                                            weights.third,
                                        )
                                }

                                fragmentShader.main()

                                val color = fragmentOutputValues[fragmentShaderLayout.color.id.index] as? Vec4f
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
            val position = outputs[vertexShaderLayout.position.id.index] as? Vec4f
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
