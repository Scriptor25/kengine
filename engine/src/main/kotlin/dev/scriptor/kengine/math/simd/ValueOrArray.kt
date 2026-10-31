package dev.scriptor.kengine.math.simd

sealed interface ValueOrArray<T> {

    val size: Int

    operator fun get(index: Int): T

    class WithValue<T>(val value: T) : ValueOrArray<T> {
        override val size: Int = 1

        override fun get(index: Int): T = when (index) {
            0 -> value
            else -> error("index out of bounds")
        }
    }

    class WithArray<T>(val value: Array<T>) : ValueOrArray<T> {
        override val size: Int = value.size

        override fun get(index: Int): T = when (index) {
            in value.indices -> value[index]
            else -> error("index out of bounds")
        }
    }
}

fun <T> wrap(value: T): ValueOrArray<T> = ValueOrArray.WithValue(value)
fun <T> wrap(value: Array<T>): ValueOrArray<T> = ValueOrArray.WithArray(value)
