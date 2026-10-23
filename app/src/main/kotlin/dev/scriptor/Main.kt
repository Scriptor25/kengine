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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
    clipMinX: Int = 0,
    clipMinY: Int = 0,
    clipMaxX: Int = width - 1,
    clipMaxY: Int = height - 1,
    plot: (x: Int, y: Int, b0: Float, b1: Float, b2: Float) -> Unit,
) {
    val xMin = maxOf(minOf(p0x, p1x, p2x), 0, clipMinX)
    val yMin = maxOf(minOf(p0y, p1y, p2y), 0, clipMinY)
    val xMax = minOf(maxOf(p0x, p1x, p2x), width - 1, clipMaxX)
    val yMax = minOf(maxOf(p0y, p1y, p2y), height - 1, clipMaxY)

    if (xMin > xMax || yMin > yMax) return

    fun edge(ax: Int, ay: Int, bx: Int, by: Int, px: Int, py: Int): Long {
        return (bx.toLong() - ax.toLong()) * (py.toLong() - ay.toLong()) -
                (by.toLong() - ay.toLong()) * (px.toLong() - ax.toLong())
    }

    val area = edge(p0x, p0y, p1x, p1y, p2x, p2y)
    if (area == 0L) return

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

data class RasterTriangle<VSO : VertexShaderOutput>(
    val v0: VSO,
    val v1: VSO,
    val v2: VSO,

    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,

    val z0: Float,
    val z1: Float,
    val z2: Float,

    val invW0: Float,
    val invW1: Float,
    val invW2: Float,

    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int,
)

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
    val tileSize: Int
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
        if (surface.width <= 0 || surface.height <= 0 || count <= 0)
            return

        val vertexCount = vertexBuffer.remaining() / stride
        val vertexBase = vertexBuffer.position()

        val vso = allocateVSO(vertexCount)

        for (index in vso.indices) {
            val vsi = invoke(vertexBuffer, vertexBase + index * stride)
            vso[index] = invoke(vsi)
        }

        val tilesX = (surface.width + tileSize - 1) / tileSize
        val tilesY = (surface.height + tileSize - 1) / tileSize

        val bins = Array(tilesX * tilesY) { mutableListOf<RasterTriangle<VSO>>() }

        for (index in first until first + count step 3) {
            val i0 = indexBuffer.get(index)
            val i1 = indexBuffer.get(index + 1)
            val i2 = indexBuffer.get(index + 2)

            val v0 = vso[i0]!!
            val v1 = vso[i1]!!
            val v2 = vso[i2]!!

            val invW0 = 1.0f / v0.position.w
            val invW1 = 1.0f / v1.position.w
            val invW2 = 1.0f / v2.position.w

            val ndc0 = Vec3f(0.5f).timesPlusAssign(v0.position.xyz, 0.5f * invW0)
            val ndc1 = Vec3f(0.5f).timesPlusAssign(v1.position.xyz, 0.5f * invW1)
            val ndc2 = Vec3f(0.5f).timesPlusAssign(v2.position.xyz, 0.5f * invW2)

            val x0 = (ndc0.x * (surface.width - 1).toFloat()).toInt()
            val y0 = ((1.0f - ndc0.y) * (surface.height - 1).toFloat()).toInt()
            val x1 = (ndc1.x * (surface.width - 1).toFloat()).toInt()
            val y1 = ((1.0f - ndc1.y) * (surface.height - 1).toFloat()).toInt()
            val x2 = (ndc2.x * (surface.width - 1).toFloat()).toInt()
            val y2 = ((1.0f - ndc2.y) * (surface.height - 1).toFloat()).toInt()

            val minX = maxOf(minOf(x0, x1, x2), 0)
            val minY = maxOf(minOf(y0, y1, y2), 0)
            val maxX = minOf(maxOf(x0, x1, x2), surface.width - 1)
            val maxY = minOf(maxOf(y0, y1, y2), surface.height - 1)

            if (minX > maxX || minY > maxY) {
                continue
            }

            val tri = RasterTriangle(
                v0, v1, v2,
                x0, y0, x1, y1, x2, y2,
                ndc0.z, ndc1.z, ndc2.z,
                invW0, invW1, invW2,
                minX, minY, maxX, maxY,
            )

            val tx0 = minX / tileSize
            val ty0 = minY / tileSize
            val tx1 = maxX / tileSize
            val ty1 = maxY / tileSize

            for (ty in ty0..ty1) {
                for (tx in tx0..tx1) {
                    bins[tx + ty * tilesX] += tri
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

                val tileMinX = tx * tileSize
                val tileMinY = ty * tileSize
                val tileMaxX = minOf(tileMinX + tileSize - 1, surface.width - 1)
                val tileMaxY = minOf(tileMinY + tileSize - 1, surface.height - 1)

                tasks += Callable {
                    for (tri in bin) {
                        triangle(
                            tri.x0, tri.y0,
                            tri.x1, tri.y1,
                            tri.x2, tri.y2,
                            surface.width, surface.height,
                            tileMinX, tileMinY,
                            tileMaxX, tileMaxY,
                        ) { x, y, b0, b1, b2 ->
                            val index = x + y * surface.width

                            val z = (tri.z0 * b0 + tri.z1 * b1 + tri.z2 * b2).coerceIn(0.0f, 1.0f)

                            val depth = (z * 255.999f).toInt() and 0xff
                            val depthBuffer = (surface[index] ushr 24) and 0xff

                            if (depth < depthBuffer) {
                                val w0 = b0 * tri.invW0
                                val w1 = b1 * tri.invW1
                                val w2 = b2 * tri.invW2

                                val invDenom = 1.0f / (w0 + w1 + w2)

                                val pb0 = w0 * invDenom
                                val pb1 = w1 * invDenom
                                val pb2 = w2 * invDenom

                                val fsi = invoke(
                                    tri.v0, tri.v1, tri.v2,
                                    pb0, pb1, pb2,
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
            }
        }

        for (future in executor.invokeAll(tasks)) {
            future.get()
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

    override val tileSize: Int = 32
    override val stride: Int = Vec3fc.SIZE_BYTES * 2 + Vec2fc.SIZE_BYTES + Vec4fc.SIZE_BYTES
    override val executor: ExecutorService =
        Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            Thread.ofPlatform().daemon().factory(),
        )

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
        // Vec4f(data.texture, 0.0f, 1.0f)
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

            println(delta)

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
