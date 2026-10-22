package dev.scriptor

import dev.scriptor.kengine.*
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.absoluteValue
import kotlin.math.sign
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

interface VertexShaderInput

interface VertexShaderOutput {
    val position: Vec4fc
}

interface FragmentShaderInput

interface FragmentShaderOutput {
    val color: Vec4fc
}

interface Surface {
    val width: Int
    val height: Int

    operator fun get(index: Int): Int
    operator fun set(index: Int, value: Int)
}

interface Pipeline<
        VSI : VertexShaderInput,
        VSO : VertexShaderOutput,
        FSI : FragmentShaderInput,
        FSO : FragmentShaderOutput> {

    val stride: Int
    val executor: ExecutorService

    fun allocateVSI(count: Int): Array<VSI?>
    fun allocateVSO(count: Int): Array<VSO?>
    fun allocateFSI(count: Int): Array<FSI?>
    fun allocateFSO(count: Int): Array<FSO?>

    operator fun invoke(
        data: ByteBuffer,
        position: Int,
    ): VSI

    operator fun invoke(data: VSI): VSO

    operator fun invoke(
        data0: VSO,
        data1: VSO,
        data2: VSO,
        b0: Float,
        b1: Float,
        b2: Float,
    ): FSI

    operator fun invoke(data: FSI): FSO

    operator fun invoke(
        surface: Surface,
        vertexBuffer: ByteBuffer,
        indexBuffer: IntBuffer,
        first: Int,
        count: Int,
    ) {
        val vertexCount = vertexBuffer.remaining() / stride

        val vertices = arrayOfNulls<Future<VSO>>(vertexCount)

        for (index in vertices.indices) {
            vertices[index] = executor.submit<VSO> {
                val vsi = invoke(vertexBuffer, index * stride)
                invoke(vsi)
            }
        }

        val fragments = mutableListOf<Future<*>>()

        for (index in (first until first + count).step(3)) {
            val i0 = indexBuffer.get(index)
            val i1 = indexBuffer.get(index + 1)
            val i2 = indexBuffer.get(index + 2)

            val vso0 = vertices[i0]!!.get()
            val vso1 = vertices[i1]!!.get()
            val vso2 = vertices[i2]!!.get()

            val ndc0 = Vec3f(0.5f).timesPlusAssign(vso0.position.xyz, 0.5f / vso0.position.w)
            val ndc1 = Vec3f(0.5f).timesPlusAssign(vso1.position.xyz, 0.5f / vso1.position.w)
            val ndc2 = Vec3f(0.5f).timesPlusAssign(vso2.position.xyz, 0.5f / vso2.position.w)

            val p0x = (ndc0.x * (surface.width - 1).toFloat()).toInt()
            val p0y = ((1.0f - ndc0.y) * (surface.height - 1).toFloat()).toInt()
            val p1x = (ndc1.x * (surface.width - 1).toFloat()).toInt()
            val p1y = ((1.0f - ndc1.y) * (surface.height - 1).toFloat()).toInt()
            val p2x = (ndc2.x * (surface.width - 1).toFloat()).toInt()
            val p2y = ((1.0f - ndc2.y) * (surface.height - 1).toFloat()).toInt()

            triangle(
                p0x, p0y,
                p1x, p1y,
                p2x, p2y,
                surface.width, surface.height,
            ) { x, y, b0, b1, b2 ->
                val index = x + y * surface.width

                val z = (ndc0.z * b0 + ndc1.z * b1 + ndc2.z * b2).coerceIn(0.0f, 1.0f)

                val depth = (z * 255.999f).toInt() and 0xff
                val depthBuffer = (surface[index] ushr 24) and 0xff

                if (depth < depthBuffer) {
                    fragments += executor.submit {
                        val fsi = invoke(
                            vso0, vso1, vso2,
                            b0, b1, b2,
                        )

                        val fso = invoke(fsi)

                        val r = (fso.color.x * 255.999f).toInt() and 0xff
                        val g = (fso.color.y * 255.999f).toInt() and 0xff
                        val b = (fso.color.z * 255.999f).toInt() and 0xff

                        surface[index] = (depth shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
            }
        }

        for (fragment in fragments) {
            fragment.get()
        }
    }
}

data class DefaultVertexShaderInput(
    val position: Vec3fc,
    val normal: Vec3fc,
    val texture: Vec2fc,
    val color: Vec4fc,
) : VertexShaderInput

data class DefaultVertexShaderOutput(
    override val position: Vec4fc,
    val normal: Vec3fc,
    val texture: Vec2fc,
    val color: Vec4fc,
) : VertexShaderOutput

data class DefaultFragmentShaderInput(
    val position: Vec4fc,
    val normal: Vec3fc,
    val texture: Vec2fc,
    val color: Vec4fc,
) : FragmentShaderInput

data class DefaultFragmentShaderOutput(
    override val color: Vec4fc,
) : FragmentShaderOutput

class DefaultPipeline : Pipeline<
        DefaultVertexShaderInput,
        DefaultVertexShaderOutput,
        DefaultFragmentShaderInput,
        DefaultFragmentShaderOutput> {

    override val stride: Int = Vec3fc.SIZE_BYTES * 2 + Vec2fc.SIZE_BYTES + Vec4fc.SIZE_BYTES

    override val executor: ExecutorService =
        Executors.newFixedThreadPool(128) { runnable ->
            Thread(runnable).apply { isDaemon = true }
        }

    override fun allocateVSI(count: Int): Array<DefaultVertexShaderInput?> = arrayOfNulls(count)
    override fun allocateVSO(count: Int): Array<DefaultVertexShaderOutput?> = arrayOfNulls(count)
    override fun allocateFSI(count: Int): Array<DefaultFragmentShaderInput?> = arrayOfNulls(count)
    override fun allocateFSO(count: Int): Array<DefaultFragmentShaderOutput?> = arrayOfNulls(count)

    override fun invoke(
        data: ByteBuffer,
        position: Int,
    ): DefaultVertexShaderInput {
        return DefaultVertexShaderInput(
            data.getVec3f(position),
            data.getVec3f(position + Vec3fc.SIZE_BYTES),
            data.getVec2f(position + Vec3fc.SIZE_BYTES * 2),
            data.getVec4f(position + Vec3fc.SIZE_BYTES * 2 + Vec2fc.SIZE_BYTES),
        )
    }

    override fun invoke(data: DefaultVertexShaderInput): DefaultVertexShaderOutput {
        return DefaultVertexShaderOutput(
            Vec4f(data.position, 1.0f),
            data.normal,
            data.texture,
            data.color,
        )
    }

    override fun invoke(
        data0: DefaultVertexShaderOutput,
        data1: DefaultVertexShaderOutput,
        data2: DefaultVertexShaderOutput,
        b0: Float,
        b1: Float,
        b2: Float,
    ): DefaultFragmentShaderInput {
        return DefaultFragmentShaderInput(
            Vec4f()
                .timesPlusAssign(data0.position, b0)
                .timesPlusAssign(data1.position, b1)
                .timesPlusAssign(data2.position, b2),
            Vec3f()
                .timesPlusAssign(data0.normal, b0)
                .timesPlusAssign(data1.normal, b1)
                .timesPlusAssign(data2.normal, b2),
            Vec2f()
                .timesPlusAssign(data0.texture, b0)
                .timesPlusAssign(data1.texture, b1)
                .timesPlusAssign(data2.texture, b2),
            Vec4f()
                .timesPlusAssign(data0.color, b0)
                .timesPlusAssign(data1.color, b1)
                .timesPlusAssign(data2.color, b2),
        )
    }

    override fun invoke(data: DefaultFragmentShaderInput): DefaultFragmentShaderOutput {
        return DefaultFragmentShaderOutput(data.color)
    }
}

data class Particle(
    val p: Vec3f,
    val v: Vec3f,
) {
    fun draw(buffer: IntArray, scale: Int, scaledWidth: Int, scaledHeight: Int) {
        triangle(
            (p.x - 50).toInt() / scale, (p.y - 35).toInt() / scale,
            (p.x + 50).toInt() / scale, (p.y - 35).toInt() / scale,
            (p.x + 0).toInt() / scale, (p.y + 35).toInt() / scale,
            scaledWidth,
            scaledHeight,
        ) { x, y, b0, b1, b2 ->
            val index = x + y * scaledWidth

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
                    Vec3f(dx, dy, 0.0f),
                )
            )
        }
    }

    val pipeline = DefaultPipeline()
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

    val vertexBuffer = ByteBuffer.allocate(4 * pipeline.stride)
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
        .flip()

    val indexBuffer = IntBuffer.allocate(6)
        .put(0).put(1).put(2)
        .put(2).put(3).put(0)
        .flip()

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

                pipeline(surface, vertexBuffer, indexBuffer, 0, 6)
            }

            val speed = delta.toDouble(DurationUnit.SECONDS).toFloat() * 100.0f

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

                particle.p.timesPlusAssign(particle.v, speed)
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
