package dev.scriptor.kengine.pipeline

sealed interface ElementBuffer {

    val size: Int

    operator fun get(element: Int): Int

    class ShortElementBuffer(
        val elements: ShortArray,
    ) : ElementBuffer {

        override val size: Int = elements.size

        override fun get(element: Int): Int = elements[element].toInt()
    }

    class IntElementBuffer(
        val elements: IntArray,
    ) : ElementBuffer {

        override val size: Int = elements.size

        override fun get(element: Int): Int = elements[element]
    }
}
