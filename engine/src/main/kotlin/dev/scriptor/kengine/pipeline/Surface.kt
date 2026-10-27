package dev.scriptor.kengine.pipeline

interface Surface {
    val width: Int
    val height: Int

    fun fill(minX: Int, minY: Int, maxX: Int, maxY: Int, value: Int)

    fun color(index: Int): Int
    fun color(index: Int, value: Int)

    fun depth(index: Int): Float
    fun depth(index: Int, value: Float)
}
