package dev.scriptor.kengine.math.internal

sealed interface ValueOrArray<T> {
    class WithValue<T>(val value: T) : ValueOrArray<T>
    class WithArray<T>(val value: Array<T>) : ValueOrArray<T>
}
