package dev.scriptor

import dev.scriptor.kengine.*
import dev.scriptor.kengine.math.Mat4fc
import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f
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
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.DurationUnit

object Context {
    @OptIn(ExperimentalAtomicApi::class)
    val running = AtomicBoolean(true)

    const val SCALE = 4

    var width: Int = 0
    var height: Int = 0

    val scaledWidth
        get() = width / SCALE
    val scaledHeight
        get() = height / SCALE

    lateinit var image: BufferedImage
        private set
    lateinit var depth: FloatArray
        private set

    val color: IntArray
        get() = (image.raster.dataBuffer as DataBufferInt).data

    fun allocate(first: Boolean = false) {
        if (!first && image.width == scaledWidth && image.height == scaledHeight) return

        image = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
        depth = FloatArray(scaledWidth * scaledHeight)
    }

    fun clear() {
        color.fill(0x00000000)
        depth.fill(Float.MAX_VALUE)
    }
}

data class Particle(
    var p: Vec3f,
    var v: Vec3f,
)

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

    val particles = buildList {
        for (i in 0..100) {
            val dx = Math.random().toFloat() * 2.0f - 1.0f
            val dy = Math.random().toFloat() * 2.0f - 1.0f

            add(
                Particle(
                    Vec3f(0.0f, 0.0f, Math.random().toFloat()),
                    Vec3f(dx, dy, 0.0f),
                )
            )
        }
    }

    val defaultPipeline = GraphicsPipeline(
        DefaultVertexShaderLayout,
        DefaultFragmentShaderLayout,
        ::DefaultVertexShader,
        ::DefaultFragmentShader,
        DefaultVertexShaderLayout.layout(),
        Rasterizer(64),
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

    var time = Clock.System.now()

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

    thread = Thread {
        canvas.createBufferStrategy(2)

        val strategy = canvas.bufferStrategy

        while (Context.running.load()) {
            val now = Clock.System.now()
            val delta = now - time
            time = now

            println(delta)

            val speed = delta.toDouble(DurationUnit.SECONDS).toFloat() * 100.0f

            synchronized(Context) {
                Context.allocate()
                Context.clear()

                val perspective = Mat4fc.perspective(
                    fovY = 70.0f,
                    aspect = Context.width.toFloat() / Context.height.toFloat(),
                    near = 0.1f,
                    far = 100.0f,
                )

                val pivot = Mat4fc.translation(-0.5f, -0.5f, -0.5f)

                val model1 = Mat4fc.trs(
                    translation = Vec3f(-1.0f, 0.0f, -4.0f),
                    rotation = Vec3f(angle, angle * 0.32f, angle * 0.23f),
                    scale = Vec3f(1.0f),
                ) * pivot
                val transform1 = perspective * model1

                val model2 = Mat4fc.trs(
                    translation = Vec3f(1.0f, 0.0f, -4.0f),
                    rotation = Vec3f(angle * 0.43f, angle * 0.17f, angle * 0.67f),
                    scale = Vec3f(1.0f),
                ) * pivot
                val transform2 = perspective * model2

                angle += speed

                defaultPipeline(
                    surface,
                    cubeMesh,
                    arrayOf(
                        transform1,
                        model1,
                    ),
                    arrayOf(
                        sampler,
                    ),
                )
                defaultPipeline(
                    surface,
                    cubeMesh,
                    arrayOf(
                        transform2,
                        model2,
                    ),
                    arrayOf(
                        sampler,
                    ),
                )

                for (particle in particles) {
                    particle // TODO: draw(...)
                }
            }

            for (particle in particles) {
                if (particle.p.x > Context.width) {
                    particle.p = Vec3f(Context.width.toFloat(), particle.p.y, particle.p.z)
                    particle.v = Vec3f(-particle.v.x.absoluteValue, particle.v.y, particle.v.z)
                } else if (particle.p.x < 0) {
                    particle.p = Vec3f(0.0f, particle.p.y, particle.p.z)
                    particle.v = Vec3f(particle.v.x.absoluteValue, particle.v.y, particle.v.z)
                }

                if (particle.p.y > Context.height) {
                    particle.p = Vec3f(particle.p.x, Context.height.toFloat(), particle.p.z)
                    particle.v = Vec3f(particle.v.x, -particle.v.y.absoluteValue, particle.v.z)
                } else if (particle.p.y < 0) {
                    particle.p = Vec3f(particle.p.x, 0.0f, particle.p.z)
                    particle.v = Vec3f(particle.v.x, particle.v.y.absoluteValue, particle.v.z)
                }

                particle.p += particle.v * speed
            }

            do {
                do {
                    val graphics = strategy.drawGraphics as Graphics2D

                    graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
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
    }

    Context.allocate(true)

    thread.start()
}
