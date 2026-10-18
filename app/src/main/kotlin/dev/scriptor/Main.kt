package dev.scriptor

import dev.scriptor.kengine.KWindow
import dev.scriptor.kengine.Vec2f
import dev.scriptor.kengine.Vec3f
import java.awt.Canvas
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.util.*
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.time.Clock
import kotlin.time.DurationUnit

object Context {
    @OptIn(ExperimentalAtomicApi::class)
    val running = AtomicBoolean(true)

    const val SCALE = 2

    var width: Int = 0
    var height: Int = 0

    val scaledWidth
        get() = width * SCALE
    val scaledHeight
        get() = height * SCALE

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

fun triangle(
    p0x: Int,
    p0y: Int,
    p1x: Int,
    p1y: Int,
    p2x: Int,
    p2y: Int,
    width: Int,
    height: Int,
    plot: (x: Int, y: Int, b0: Float, b1: Float, b2: Float) -> Unit,
) {
    val xMin = maxOf(minOf(p0x, p1x, p2x), 0)
    val yMin = maxOf(minOf(p0y, p1y, p2y), 0)
    val xMax = minOf(maxOf(p0x, p1x, p2x), width - 1)
    val yMax = minOf(maxOf(p0y, p1y, p2y), height - 1)

    if (xMin > xMax || yMin > yMax) return

    fun edge(ax: Int, ay: Int, bx: Int, by: Int, px: Int, py: Int): Int {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax)
    }

    val area = edge(p0x, p0y, p1x, p1y, p2x, p2y)
    if (area == 0) return

    val invArea = 1.0f / area.absoluteValue.toFloat()

    val sign = area.sign.toLong()

    val dx0 = -(p2y - p1y).toLong() * sign
    val dy0 = (p2x - p1x).toLong() * sign

    val dx1 = -(p0y - p2y).toLong() * sign
    val dy1 = (p0x - p2x).toLong() * sign

    val dx2 = -(p1y - p0y).toLong() * sign
    val dy2 = (p1x - p0x).toLong() * sign

    var row0 = edge(p1x, p1y, p2x, p2y, xMin, yMin) * sign
    var row1 = edge(p2x, p2y, p0x, p0y, xMin, yMin) * sign
    var row2 = edge(p0x, p0y, p1x, p1y, xMin, yMin) * sign

    for (y in yMin..yMax) {
        var e0 = row0
        var e1 = row1
        var e2 = row2

        for (x in xMin..xMax) {
            if (e0 >= 0L && e1 >= 0L && e2 >= 0L) {
                plot(
                    x,
                    y,
                    e0.toFloat() * invArea,
                    e1.toFloat() * invArea,
                    e2.toFloat() * invArea,
                )
            }

            e0 += dx0
            e1 += dx1
            e2 += dx2
        }

        row0 += dy0
        row1 += dy1
        row2 += dy2
    }
}

data class Particle(
    var p: Vec3f,
    var v: Vec2f,
) {
    fun draw(buffer: IntArray, scale: Int, width: Int, height: Int) {
        triangle(
            (p.x - 50).toInt() * scale, (p.y - 35).toInt() * scale,
            (p.x + 50).toInt() * scale, (p.y - 35).toInt() * scale,
            (p.x + 0).toInt() * scale, (p.y + 35).toInt() * scale,
            width,
            height,
        ) { x, y, b0, b1, b2 ->
            val index = x + y * width

            val z = p.z.coerceIn(0.0f, 1.0f)

            val depth = (z * 255.999f).toInt() and 0xff
            val depthBuffer = buffer[index] ushr 24

            if (depth < depthBuffer) {
                val dist = (1.0f - z) * (1.0f - z)

                val r = (dist * b0 * 1.0f * 255.999f).toInt() and 0xff
                val g = (dist * b1 * 1.0f * 255.999f).toInt() and 0xff
                val b = (dist * b2 * 1.0f * 255.999f).toInt() and 0xff

                buffer[index] = (depth shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
fun main() {
    println("Hello World!")

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

    val particles = buildList {
        for (i in 0..100) {
            val dx = Math.random().toFloat() * 2.0f - 1.0f
            val dy = Math.random().toFloat() * 2.0f - 1.0f

            add(
                Particle(
                    Vec3f(0.0f, 0.0f, Math.random().toFloat()),
                    Vec2f(dx, dy),
                )
            )
        }
    }

    var time = Clock.System.now()

    thread = Thread {
        canvas.createBufferStrategy(2)

        val strategy = canvas.bufferStrategy

        while (Context.running.load()) {
            val now = Clock.System.now()
            val delta = now - time
            time = now

            synchronized(Context) {
                Context.allocate()
                Context.clear()

                for (particle in particles) {
                    particle.draw(Context.buffer.data, Context.SCALE, Context.scaledWidth, Context.scaledHeight)
                }
            }

            val speed = delta.toDouble(DurationUnit.SECONDS).toFloat() * 100.0f

            for (particle in particles) {
                if (particle.p.x > Context.width) {
                    particle.p = Vec3f(Context.width.toFloat(), particle.p.y, particle.p.z)
                    particle.v = Vec2f(-particle.v.x.absoluteValue, particle.v.y)
                } else if (particle.p.x < 0) {
                    particle.p = Vec3f(0.0f, particle.p.y, particle.p.z)
                    particle.v = Vec2f(particle.v.x.absoluteValue, particle.v.y)
                }

                if (particle.p.y > Context.height) {
                    particle.p = Vec3f(particle.p.x, Context.height.toFloat(), particle.p.z)
                    particle.v = Vec2f(particle.v.x, -particle.v.y.absoluteValue)
                } else if (particle.p.y < 0) {
                    particle.p = Vec3f(particle.p.x, 0.0f, particle.p.z)
                    particle.v = Vec2f(particle.v.x, particle.v.y.absoluteValue)
                }

                particle.p += Vec3f(particle.v * speed, 0.0f)
            }

            do {
                do {
                    val graphics = strategy.drawGraphics as Graphics2D

                    graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR,
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
