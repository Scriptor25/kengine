package dev.scriptor

import dev.scriptor.kengine.*
import dev.scriptor.kengine.math.*
import java.awt.Canvas
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.DurationUnit

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

    lateinit var image: BufferedImage
        private set

    val buffer: DataBufferInt
        get() = image.raster.dataBuffer as DataBufferInt

    fun allocate(first: Boolean = false) {
        if (!first && image.width == scaledWidth && image.height == scaledHeight) return

        image = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
    }

    fun clear() {
        Arrays.fill(buffer.data, 0xff000000.toInt())
    }
}

data class Particle(
    val p: Vec3f,
    val v: Vec3f,
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

        override fun get(index: Int): Int {
            return Context.buffer.data[index]
        }

        override fun set(index: Int, value: Int) {
            Context.buffer.data[index] = value
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

    val defaultVertexAttributes = mutableListOf<VertexAttribute<*>>()

    val defaultVertexShader = vertexShader {
        val transform = uniform<Mat4fc>()

        val inPosition = input<Vec3fc>(0)
        val inNormal = input<Vec3fc>(1)
        val inTexture = input<Vec2fc>(2)
        val inColor = input<Vec4fc>(3)

        val outPosition = output<Vec3fc>(0)
        val outNormal = output<Vec3fc>(1)
        val outTexture = output<Vec2fc>(2)
        val outColor = output<Vec4fc>(3)

        defaultVertexAttributes += VertexAttribute(
            inPosition,
            VertexFormat.VEC3,
            0,
        )
        defaultVertexAttributes += VertexAttribute(
            inNormal,
            VertexFormat.VEC3,
            Vec3fc.SIZE_BYTES,
        )
        defaultVertexAttributes += VertexAttribute(
            inTexture,
            VertexFormat.VEC2,
            Vec3fc.SIZE_BYTES * 2,
        )
        defaultVertexAttributes += VertexAttribute(
            inColor,
            VertexFormat.VEC4,
            Vec2fc.SIZE_BYTES + Vec3fc.SIZE_BYTES * 2,
        )

        main {
            position(transform() * Vec4f(inPosition(), 1.0f))

            outPosition(inPosition())
            outNormal(inNormal())
            outTexture(inTexture())
            outColor(inColor())
        }
    }

    val defaultFragmentShader = fragmentShader {
        val inPosition = input<Vec3fc>(0)
        val inNormal = input<Vec3fc>(1)
        val inTexture = input<Vec2fc>(2)
        val inColor = input<Vec4fc>(3)

        main {
            color(Vec4f(inTexture(), 0.0f, 1.0f))
        }
    }

    val defaultVertexLayout = VertexLayout(
        Vec2fc.SIZE_BYTES + Vec3fc.SIZE_BYTES * 2 + Vec4fc.SIZE_BYTES,
        defaultVertexAttributes,
    )

    val defaultRasterizer = Rasterizer(64)

    val defaultPipeline = GraphicsPipeline(
        defaultVertexShader,
        defaultFragmentShader,
        defaultVertexLayout,
        defaultRasterizer,
    )

    val defaultMesh = Mesh(
        vertexBuffer = ByteBuffer.allocate(4 * defaultPipeline.vertexLayout.stride)
            // 0
            .putVec3f(-0.5f, -0.5f, 0.0f)
            .putVec3f(0.0f, 0.0f, -1.0f)
            .putVec2f(0.0f, 0.0f)
            .putVec4f(0.0f, 0.0f, 1.0f, 1.0f)
            // 1
            .putVec3f(-0.5f, 0.5f, 0.0f)
            .putVec3f(0.0f, 0.0f, -1.0f)
            .putVec2f(0.0f, 1.0f)
            .putVec4f(0.0f, 1.0f, 0.0f, 1.0f)
            // 2
            .putVec3f(0.5f, 0.5f, 0.0f)
            .putVec3f(0.0f, 0.0f, -1.0f)
            .putVec2f(1.0f, 1.0f)
            .putVec4f(1.0f, 1.0f, 1.0f, 1.0f)
            // 3
            .putVec3f(0.5f, -0.5f, 0.0f)
            .putVec3f(0.0f, 0.0f, -1.0f)
            .putVec2f(1.0f, 0.0f)
            .putVec4f(1.0f, 0.0f, 0.0f, 1.0f)
            .flip(),
        indexBuffer = IntBuffer.allocate(6)
            .put(0).put(1).put(2)
            .put(2).put(3).put(0)
            .flip(),
        count = 6,
    )

    var time = Clock.System.now()

    var angle = 0.0f

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

                val transform =
                    Mat4fc.perspective(
                        fovY = 70.0f,
                        aspect = Context.width.toFloat() / Context.height.toFloat(),
                        near = 0.1f,
                        far = 100.0f,
                    ) * Mat4fc.trs(
                        translation = Vec3f(0.0f, 0.0f, -4.0f),
                        rotation = Vec3f(angle, angle * 0.32f, angle * 0.23f),
                        scale = Vec3f(1.0f),
                    )

                angle += speed

                val defaultUniforms = arrayOf(transform)

                defaultPipeline(surface, defaultMesh, defaultUniforms)

                for (particle in particles) {
                    particle // TODO: draw(...)
                }
            }

            for (particle in particles) {
                if (particle.p.x > Context.width) {
                    particle.p.x = Context.width.toFloat()
                    particle.v.x = -particle.v.x.absoluteValue
                } else if (particle.p.x < 0) {
                    particle.p.x = 0.0f
                    particle.v.x = particle.v.x.absoluteValue
                }

                if (particle.p.y > Context.height) {
                    particle.p.y = Context.height.toFloat()
                    particle.v.y = -particle.v.y.absoluteValue
                } else if (particle.p.y < 0) {
                    particle.p.y = 0.0f
                    particle.v.y = particle.v.y.absoluteValue
                }

                particle.p.fma(particle.v, speed)
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
