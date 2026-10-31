package dev.scriptor

import dev.scriptor.kengine.KWindow
import dev.scriptor.kengine.ObjectFile
import dev.scriptor.kengine.math.*
import dev.scriptor.kengine.math.simd.allocate
import dev.scriptor.kengine.math.simd.invoke
import dev.scriptor.kengine.pipeline.GraphicsPipeline
import dev.scriptor.kengine.pipeline.Rasterizer
import dev.scriptor.kengine.pipeline.Sampler2D
import dev.scriptor.kengine.pipeline.Surface
import dev.scriptor.shader.DefaultFragmentShader
import dev.scriptor.shader.DefaultFragmentShaderLayout
import dev.scriptor.shader.DefaultVertexShader
import dev.scriptor.shader.DefaultVertexShaderLayout
import java.awt.Canvas
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.imageio.ImageIO
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object Context {
    @OptIn(ExperimentalAtomicApi::class)
    val running = AtomicBoolean(true)

    const val SCALE = 1

    var width: Int = 0
    var height: Int = 0

    val scaledWidth
        get() = width / SCALE
    val scaledHeight
        get() = height / SCALE

    var imageWidth: Int = 0
        private set
    var imageHeight: Int = 0
        private set

    lateinit var image: BufferedImage
        private set

    lateinit var color: IntArray
        private set
    lateinit var depth: FloatArray
        private set

    fun allocate() {
        if (imageWidth == scaledWidth && imageHeight == scaledHeight) return

        imageWidth = scaledWidth
        imageHeight = scaledHeight

        image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB)

        color = (image.raster.dataBuffer as DataBufferInt).data
        depth = FloatArray(scaledWidth * scaledHeight)
    }

    fun clear() {
        color.fill(0x00000000)
        depth.fill(Float.MAX_VALUE)
    }
}

@OptIn(ExperimentalAtomicApi::class)
fun main() {
    val window = KWindow()

    lateinit var thread: Thread

    val close = {
        Context.running.store(false)
        Thread {
            thread.join()
            window.frame.dispose()
        }.start()
    }

    window.onKeyPressed {
        if (it.keyCodeEx == KeyEvent.VK_ESCAPE)
            close()
    }

    window.onClosing {
        close()
    }

    val canvas = Canvas()
    canvas.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(event: ComponentEvent) {
            synchronized(Context) {
                Context.width = event.component.width
                Context.height = event.component.height
            }
        }
    })
    window.frame.add(canvas)

    window.frame.isVisible = true

    val surface = object : Surface {
        override val width: Int
            get() = Context.scaledWidth
        override val height: Int
            get() = Context.scaledHeight

        override fun fill(minX: Int, minY: Int, maxX: Int, maxY: Int, value: Int) {
            for (y in minY..maxY) {
                val base = y * width
                val from = base + minX
                val to = base + maxX

                Context.color.fill(value, from, to + 1)
            }
        }

        override fun color(index: Int): Int {
            return Context.color[index]
        }

        override fun color(index: Int, value: Int) {
            Context.color[index] = value
        }

        override fun depth(index: Int): Float {
            return Context.depth[index]
        }

        override fun depth(index: Int, value: Float) {
            Context.depth[index] = value
        }
    }

    val boxSize = 20.0f

    val particleCount = 1000
    val particlePX = FloatArray(particleCount)
    val particlePY = FloatArray(particleCount)
    val particlePZ = FloatArray(particleCount)
    val particleVX = FloatArray(particleCount)
    val particleVY = FloatArray(particleCount)
    val particleVZ = FloatArray(particleCount)
    val particleS = FloatArray(particleCount)

    val particleStep = simdF(1) { [delta] ->

        val px = particlePX.asNode()
        val py = particlePY.asNode()
        val pz = particlePZ.asNode()
        val vx = particleVX.asNode()
        val vy = particleVY.asNode()
        val vz = particleVZ.asNode()
        val s = particleS.asNode()

        val ns = -s

        val ubound = (0.5f * boxSize).asNode()
        val lbound = (-0.5f * boxSize).asNode()

        val xu = px gt ubound
        val xl = px lt lbound

        val pxp = select(
            xu,
            ubound,
            select(
                xl,
                lbound,
                px,
            ),
        )
        val vxp = select(
            xu,
            abs(vx) * ns,
            select(
                xl,
                abs(vx) * s,
                vx,
            ),
        )

        val yu = py gt ubound
        val yl = py lt lbound

        val pyp = select(
            yu,
            ubound,
            select(
                yl,
                lbound,
                py,
            ),
        )
        val vyp = select(
            yu,
            abs(vy) * ns,
            select(
                yl,
                abs(vy) * s,
                vy,
            ),
        )

        val zu = pz gt ubound
        val zl = pz lt lbound

        val pzp = select(
            zu,
            ubound,
            select(
                zl,
                lbound,
                pz,
            ),
        )
        val vzp = select(
            zu,
            abs(vz) * ns,
            select(
                zl,
                abs(vz) * s,
                vz,
            ),
        )

        export(fma(vxp, delta, pxp))
        export(fma(vyp, delta, pyp))
        export(fma(vzp, delta, pzp))
        export(vxp)
        export(vyp)
        export(vzp)
    }

    val particleStepContext = particleStep.allocate(
        particlePX, particlePY, particlePZ,
        particleVX, particleVY, particleVZ,
    )

    for (i in 0 until particleCount) {
        val vx = Math.random().toFloat() * 2.0f - 1.0f
        val vy = Math.random().toFloat() * 2.0f - 1.0f
        val vz = Math.random().toFloat() * 2.0f - 1.0f
        val s = Math.random().toFloat() * 0.8f + 0.1f

        particlePX[i] = 0.0f
        particlePY[i] = 0.0f
        particlePZ[i] = 0.0f

        particleVX[i] = vx
        particleVY[i] = vy
        particleVZ[i] = vz

        particleS[i] = s
    }

    val defaultPipeline = GraphicsPipeline(
        DefaultVertexShaderLayout,
        DefaultFragmentShaderLayout,
        ::DefaultVertexShader,
        ::DefaultFragmentShader,
        DefaultVertexShaderLayout.layout(),
        Rasterizer(16),
    )

    val cubeObjectStream = ClassLoader.getSystemResourceAsStream("cube.obj")
        ?: error("missing resource")
    val cubeObject = ObjectFile.load(cubeObjectStream)
    val cubeMesh = cubeObject.generate { vertices, position, texture, normal ->
        vertices += position[0]
        vertices += position[1]
        vertices += position[2]

        vertices += normal[0]
        vertices += normal[1]
        vertices += normal[2]

        vertices += texture[0]
        vertices += texture[1]

        vertices += 1.0f
        vertices += 1.0f
        vertices += 1.0f
        vertices += 1.0f
    }

    var time = System.nanoTime()

    var angle = 0.0f

    val cubeImageStream = ClassLoader.getSystemResourceAsStream("cube.png")
    val cubeImage = ImageIO.read(cubeImageStream)

    val sampler = object : Sampler2D {
        val m = 1.0f / 255.0f

        override fun invoke(xy: Vec2f): Vec4f {
            val x = (xy.x.coerceIn(0.0f, 1.0f) * (cubeImage.width - 1).toFloat()).toInt()
            val y = (xy.y.coerceIn(0.0f, 1.0f) * (cubeImage.height - 1).toFloat()).toInt()
            val data = cubeImage.getRGB(x, y)
            return Vec4f(
                ((data ushr 16) and 0xff).toFloat() * m,
                ((data ushr 8) and 0xff).toFloat() * m,
                ((data ushr 0) and 0xff).toFloat() * m,
                ((data ushr 24) and 0xff).toFloat() * m,
            )
        }
    }

    val transforms = arrayOfNulls<Any>(particleCount)
    val models = arrayOfNulls<Any>(particleCount)
    val normalModels = arrayOfNulls<Any>(particleCount)

    val vertexUniformValues = arrayOf(transforms, models, normalModels)
    val fragmentUniformValues = arrayOf(sampler)

    thread = Thread {
        canvas.createBufferStrategy(2)

        val strategy = canvas.bufferStrategy

        while (Context.running.load()) {
            val now = System.nanoTime()
            val elapsed = now - time
            time = now

            val delta = (elapsed.toDouble() / 1_000_000_000.0).toFloat()

            println(String.format("%.3fs", delta))

            particleStep(
                delta,
                context = particleStepContext,
            )

            angle += 90.0f * delta

            synchronized(Context) {
                Context.allocate()
                Context.clear()

                val perspective = Mat4f.perspective(
                    fovY = 70.0f,
                    aspect = Context.width.toFloat() / Context.height.toFloat(),
                    near = 0.1f,
                    far = 100.0f,
                )

                val view = Mat4f.lookAt(
                    Vec3f(boxSize),
                    Vec3f(0.0f),
                    Vec3f(0.0f, 1.0f, 0.0f),
                )

                val pivot = Mat4f.translation(-0.5f, -0.5f, -0.5f)

                for (index in 0 until particleCount) {
                    val model = Mat4f.trs(
                        translation = Vec3f(
                            particlePX[index],
                            particlePY[index],
                            particlePZ[index],
                        ),
                        rotation = Vec3f(
                            particleVX[index] * angle,
                            particleVY[index] * angle,
                            particleVZ[index] * angle,
                        ),
                        scale = Vec3f(
                            particleS[index],
                        ),
                    ) * pivot
                    val normalModel = model.toMat3f().inverse().transpose()
                    val transform = perspective * view * model

                    transforms[index] = transform
                    models[index] = model
                    normalModels[index] = normalModel
                }

                defaultPipeline(
                    surface,
                    cubeMesh,
                    0,
                    particleCount,
                    vertexUniformValues,
                    fragmentUniformValues,
                )
            }

            do {
                do {
                    val graphics = strategy.drawGraphics as Graphics2D

                    graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                    )
                    graphics.setRenderingHint(
                        RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED,
                    )

                    try {
                        graphics.drawImage(
                            Context.image,
                            0, 0,
                            canvas.width,
                            canvas.height,
                            null,
                        )
                    } finally {
                        graphics.dispose()
                    }
                } while (strategy.contentsRestored())

                strategy.show()
            } while (strategy.contentsLost())
        }
    }.apply {
        priority = Thread.MAX_PRIORITY
    }

    thread.start()
}
