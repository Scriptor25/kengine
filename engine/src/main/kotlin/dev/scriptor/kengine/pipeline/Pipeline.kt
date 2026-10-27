package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.Mesh
import dev.scriptor.kengine.math.Vec4f
import dev.scriptor.kengine.pipeline.internal.*
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShaderContextImpl(
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
    val cullMode: CullMode = CullMode.Back,
    val frontFace: FrontFace = FrontFace.CounterClockwise,
    val executor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
        Thread.ofPlatform().daemon().factory(),
    ),
    val visualizeBins: Boolean = false,
) {
    val varyings = varyings(
        vertexShaderLayout,
        fragmentShaderLayout,
    )

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
        elementBuffer: ElementBuffer,
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
        elementBuffer: ElementBuffer,
        firstElement: Int,
        elementCount: Int,
        firstInstance: Int,
        instanceCount: Int,
        vertexUniformValues: Array<Any> = emptyArray(),
        fragmentUniformValues: Array<Any> = emptyArray(),
    ) {
        require(surface.width >= 0) {
            "surface width must not be negative"
        }
        require(surface.height >= 0) {
            "surface height must not be negative"
        }

        require(vertexBuffer.remaining() % vertexLayout.stride == 0) {
            "vertex buffer does not follow vertex layout"
        }

        require(firstElement >= 0) {
            "first element must not be negative"
        }
        require(elementCount >= 0) {
            "element count must not be negative"
        }
        require(firstElement + elementCount <= elementBuffer.size) {
            "last element out of bounds of element buffer"
        }

        require(firstInstance >= 0) {
            "first instance must not be negative"
        }
        require(instanceCount >= 0) {
            "instance count must not be negative"
        }

        require(vertexUniformValues.size == vertexShaderLayout.uniforms.size) {
            "vertex uniform size mismatch"
        }
        require(fragmentUniformValues.size == fragmentShaderLayout.uniforms.size) {
            "fragment uniform size mismatch"
        }

        for ([uniform, value] in vertexShaderLayout.uniforms.zip(vertexUniformValues)) {
            when (value) {
                is Array<*> -> {
                    require(value.size == firstInstance + instanceCount) {
                        "vertex uniform instance count mismatch"
                    }
                    require(value.all { uniform.type.isInstance(it) }) {
                        "vertex uniform instance type mismatch"
                    }
                }

                else -> require(uniform.type.isInstance(value)) {
                    "vertex uniform type mismatch"
                }
            }
        }

        for ([uniform, value] in fragmentShaderLayout.uniforms.zip(fragmentUniformValues)) {
            require(uniform.type.isInstance(value)) {
                "fragment uniform type mismatch"
            }
        }

        if (surface.width == 0 || surface.height == 0) return
        if (!vertexBuffer.hasRemaining()) return
        if (elementCount == 0) return
        if (instanceCount == 0) return

        val vertexInputValues: Array<Any> = Array(vertexShaderLayout.inputs.size) {}
        val vertexOutputValues: Array<Any> = Array(vertexShaderLayout.outputs.size) {}
        val vertexShaderContext = ShaderContextImpl(
            vertexUniformValues,
            vertexInputValues,
            vertexOutputValues,
        )

        val vertexShader = vertexShaderFactory(vertexShaderContext)

        val vertexBase = vertexBuffer.position()
        val vertexCount = vertexBuffer.remaining() / vertexLayout.stride

        val vertexShaderOutput: Array<Array<Any>> = Array(vertexCount) {
            Array(vertexShaderLayout.outputs.size) {}
        }

        val tilesX = (surface.width + rasterizer.tileSize - 1) / rasterizer.tileSize
        val tilesY = (surface.height + rasterizer.tileSize - 1) / rasterizer.tileSize

        val bins = Array(tilesX * tilesY) { mutableListOf<RasterTriangle>() }
        var maxSize = 0

        for (instance in firstInstance until firstInstance + instanceCount) {
            for (vertex in 0 until vertexCount) {
                vertexInputValues.fill(Unit)
                vertexOutputValues.fill(Unit)

                vertexInputValues[vertexShaderLayout.instance.id.index] = instance

                vertexLayout.fetch(
                    vertexBuffer,
                    vertexBase + vertex * vertexLayout.stride,
                    vertexInputValues,
                )

                vertexShader.main()

                vertexOutputValues.copyInto(vertexShaderOutput[vertex])
            }

            for (index in firstElement until firstElement + elementCount step 3) {
                val index0 = elementBuffer[index + 0]
                val index1 = elementBuffer[index + 1]
                val index2 = elementBuffer[index + 2]

                val triangle = clipVertexTriangle(
                    this,
                    vertexShaderOutput[index0],
                    vertexShaderOutput[index1],
                    vertexShaderOutput[index2],
                )

                if (triangle.size < 3)
                    continue

                for (i in 1 until triangle.lastIndex) {
                    val prepared = prepareTriangle(
                        this,
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
                            val bin = bins[tx + ty * tilesX]
                            bin += prepared
                            maxSize = maxOf(maxSize, bin.size)
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
                    if (visualizeBins) {
                        val ratio = bin.size.toFloat() / maxSize.toFloat()
                        val cr = (ratio * 255.999f).toInt() and 0xff
                        val cg = ((1.0f - ratio) * 255.999f).toInt() and 0xff

                        surface.fill(tileMinX, tileMinY, tileMaxX, tileMaxY, (0xff shl 24) or (cr shl 16) or (cg shl 8))
                    }

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

                                    fragmentInputValues[varying.input.id.index] =
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

                                // TODO: use color.w for blending

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
}
