package dev.scriptor.kengine.internal

sealed interface VecSize {

    val count: Int

    object Vec2 : VecSize {
        override val count = 2
    }

    object Vec3 : VecSize {
        override val count = 3
    }

    object Vec4 : VecSize {
        override val count = 4
    }
}

interface Scalar<T : Number> {

    object ByteScalar : Scalar<Byte> {

        override fun allocate(count: Int): Array<Byte> {
            return Array(count) { 0 }
        }

        override fun doPlus(a: Byte, b: Byte): Byte {
            return (a + b).toByte()
        }

        override fun doMinus(a: Byte, b: Byte): Byte {
            return (a - b).toByte()
        }

        override fun doTimes(a: Byte, b: Byte): Byte {
            return (a * b).toByte()
        }

        override fun doDiv(a: Byte, b: Byte): Byte {
            return (a / b).toByte()
        }

        override fun doRem(a: Byte, b: Byte): Byte {
            return (a % b).toByte()
        }

        override fun doUnaryPlus(t: Byte): Byte {
            return (+t).toByte()
        }

        override fun doUnaryMinus(t: Byte): Byte {
            return (-t).toByte()
        }
    }

    object ShortScalar : Scalar<Short> {

        override fun allocate(count: Int): Array<Short> {
            return Array(count) { 0 }
        }

        override fun doPlus(a: Short, b: Short): Short {
            return (a + b).toShort()
        }

        override fun doMinus(a: Short, b: Short): Short {
            return (a - b).toShort()
        }

        override fun doTimes(a: Short, b: Short): Short {
            return (a * b).toShort()
        }

        override fun doDiv(a: Short, b: Short): Short {
            return (a / b).toShort()
        }

        override fun doRem(a: Short, b: Short): Short {
            return (a % b).toShort()
        }

        override fun doUnaryPlus(t: Short): Short {
            return (+t).toShort()
        }

        override fun doUnaryMinus(t: Short): Short {
            return (-t).toShort()
        }
    }

    object IntScalar : Scalar<Int> {

        override fun allocate(count: Int): Array<Int> {
            return Array(count) { 0 }
        }

        override fun doPlus(a: Int, b: Int): Int {
            return a + b
        }

        override fun doMinus(a: Int, b: Int): Int {
            return a - b
        }

        override fun doTimes(a: Int, b: Int): Int {
            return a * b
        }

        override fun doDiv(a: Int, b: Int): Int {
            return a / b
        }

        override fun doRem(a: Int, b: Int): Int {
            return a % b
        }

        override fun doUnaryPlus(t: Int): Int {
            return +t
        }

        override fun doUnaryMinus(t: Int): Int {
            return -t
        }
    }

    object LongScalar : Scalar<Long> {

        override fun allocate(count: Int): Array<Long> {
            return Array(count) { 0 }
        }

        override fun doPlus(a: Long, b: Long): Long {
            return a + b
        }

        override fun doMinus(a: Long, b: Long): Long {
            return a - b
        }

        override fun doTimes(a: Long, b: Long): Long {
            return a * b
        }

        override fun doDiv(a: Long, b: Long): Long {
            return a / b
        }

        override fun doRem(a: Long, b: Long): Long {
            return a % b
        }

        override fun doUnaryPlus(t: Long): Long {
            return +t
        }

        override fun doUnaryMinus(t: Long): Long {
            return -t
        }
    }

    object FloatScalar : Scalar<Float> {

        override fun allocate(count: Int): Array<Float> {
            return Array(count) { 0.0f }
        }

        override fun doPlus(a: Float, b: Float): Float {
            return a + b
        }

        override fun doMinus(a: Float, b: Float): Float {
            return a - b
        }

        override fun doTimes(a: Float, b: Float): Float {
            return a * b
        }

        override fun doDiv(a: Float, b: Float): Float {
            return a / b
        }

        override fun doRem(a: Float, b: Float): Float {
            return a % b
        }

        override fun doUnaryPlus(t: Float): Float {
            return +t
        }

        override fun doUnaryMinus(t: Float): Float {
            return -t
        }
    }

    object DoubleScalar : Scalar<Double> {

        override fun allocate(count: Int): Array<Double> {
            return Array(count) { 0.0 }
        }

        override fun doPlus(a: Double, b: Double): Double {
            return a + b
        }

        override fun doMinus(a: Double, b: Double): Double {
            return a - b
        }

        override fun doTimes(a: Double, b: Double): Double {
            return a * b
        }

        override fun doDiv(a: Double, b: Double): Double {
            return a / b
        }

        override fun doRem(a: Double, b: Double): Double {
            return a % b
        }

        override fun doUnaryPlus(t: Double): Double {
            return +t
        }

        override fun doUnaryMinus(t: Double): Double {
            return -t
        }
    }

    fun allocate(count: Int): Array<T>

    fun doPlus(a: T, b: T): T
    fun doMinus(a: T, b: T): T
    fun doTimes(a: T, b: T): T
    fun doDiv(a: T, b: T): T
    fun doRem(a: T, b: T): T

    fun doUnaryPlus(t: T): T
    fun doUnaryMinus(t: T): T

    operator fun T.plus(t: T): T {
        return doPlus(this, t)
    }

    operator fun T.minus(t: T): T {
        return doMinus(this, t)
    }

    operator fun T.times(t: T): T {
        return doTimes(this, t)
    }

    operator fun T.div(t: T): T {
        return doDiv(this, t)
    }

    operator fun T.rem(t: T): T {
        return doRem(this, t)
    }

    operator fun T.unaryPlus(): T {
        return doUnaryPlus(this)
    }

    operator fun T.unaryMinus(): T {
        return doUnaryMinus(this)
    }
}

interface Vec<T : Number, N : VecSize> {

    val size: N
    val scalar: Scalar<T>
    val elements: Array<out T>

    val x
        get() = this[0]
    val y
        get() = this[1]
    val z
        get() = this[2]
    val w
        get() = this[3]

    val r
        get() = this[0]
    val g
        get() = this[1]
    val b
        get() = this[2]
    val a
        get() = this[3]

    operator fun get(index: Int): T = elements[index]

    fun toByte(): Vec<Byte, N> {
        return Vec(size, Scalar.ByteScalar, elements.map(Number::toByte).toTypedArray())
    }

    fun toShort(): Vec<Short, N> {
        return Vec(size, Scalar.ShortScalar, elements.map(Number::toShort).toTypedArray())
    }

    fun toInt(): Vec<Int, N> {
        return Vec(size, Scalar.IntScalar, elements.map(Number::toInt).toTypedArray())
    }

    fun toLong(): Vec<Long, N> {
        return Vec(size, Scalar.LongScalar, elements.map(Number::toLong).toTypedArray())
    }

    fun toFloat(): Vec<Float, N> {
        return Vec(size, Scalar.FloatScalar, elements.map(Number::toFloat).toTypedArray())
    }

    fun toDouble(): Vec<Double, N> {
        return Vec(size, Scalar.DoubleScalar, elements.map(Number::toDouble).toTypedArray())
    }

    operator fun plus(vec: Vec<T, N>): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] + vec.elements[i]
        Vec(size, scalar, buffer)
    }

    operator fun minus(vec: Vec<T, N>): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] - vec.elements[i]
        Vec(size, scalar, buffer)
    }

    operator fun times(vec: Vec<T, N>): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] * vec.elements[i]
        Vec(size, scalar, buffer)
    }

    operator fun times(t: T): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] * t
        Vec(size, scalar, buffer)
    }

    operator fun div(vec: Vec<T, N>): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] / vec.elements[i]
        Vec(size, scalar, buffer)
    }

    operator fun div(t: T): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] / t
        Vec(size, scalar, buffer)
    }

    operator fun rem(vec: Vec<T, N>): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] % vec.elements[i]
        Vec(size, scalar, buffer)
    }

    operator fun rem(t: T): Vec<T, N> = scalar.run {
        val buffer = allocate(elements.size)
        for (i in elements.indices)
            buffer[i] = elements[i] % t
        Vec(size, scalar, buffer)
    }
}

operator fun <T : Number, N : VecSize> T.times(vec: Vec<T, N>): Vec<T, N> {
    return vec * this
}

fun <T : Number, N : VecSize> Vec(size: N, scalar: Scalar<T>, elements: Array<out T>): Vec<T, N> {
    require(size.count == elements.size)
    return object : Vec<T, N> {
        override val size = size
        override val scalar = scalar
        override val elements = elements
    }
}

inline fun <reified T : Number, N : VecSize> Vec(size: N, vararg elements: T): Vec<T, N> {
    require(size.count == elements.size)
    return object : Vec<T, N> {
        override val size = size
        override val scalar = when (T::class) {
            Byte::class -> Scalar.ByteScalar
            Short::class -> Scalar.ShortScalar
            Int::class -> Scalar.IntScalar
            Long::class -> Scalar.LongScalar
            Float::class -> Scalar.FloatScalar
            Double::class -> Scalar.DoubleScalar
            else -> error("unsupported scalar type ${T::class}")
        } as Scalar<T>
        override val elements = elements
    }
}

typealias Vec2<T> = Vec<T, VecSize.Vec2>

inline fun <reified T : Number> Vec2(x: T, y: T): Vec2<T> {
    return Vec(VecSize.Vec2, x, y)
}

inline infix fun <reified T : Number> Vec2<T>.cross(v: Vec2<T>): T {
    return scalar.run { x * v.y - y * v.x }
}

typealias Vec3<T> = Vec<T, VecSize.Vec3>

inline fun <reified T : Number> Vec3(x: T, y: T, z: T): Vec3<T> {
    return Vec(VecSize.Vec3, x, y, z)
}

typealias Vec4<T> = Vec<T, VecSize.Vec4>

inline fun <reified T : Number> Vec4(x: T, y: T, z: T, w: T): Vec4<T> {
    return Vec(VecSize.Vec4, x, y, z, w)
}
