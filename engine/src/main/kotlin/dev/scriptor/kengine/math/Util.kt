package dev.scriptor.kengine.math

operator fun FloatArray.get(stride: Int, r: Int, c: Int): Float {
    return this[r + c * stride]
}

operator fun FloatArray.set(stride: Int, r: Int, c: Int, value: Float) {
    this[r + c * stride] = value
}
