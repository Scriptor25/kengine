package dev.scriptor.kengine.pipeline

import java.nio.ByteBuffer

data class VertexLayout(
    val stride: Int,
    val attributes: List<VertexAttribute<*>>,
) {
    init {
        require(stride > 0)

        for (attribute in attributes) {
            require(attribute.offset >= 0)
            require(attribute.offset + attribute.format.size <= stride)
            require(attribute.input.type == attribute.format.type)
        }
    }

    fun fetch(
        buffer: ByteBuffer,
        offset: Int,
        values: Array<Any?>,
    ) {
        for (attribute in attributes) {
            values[attribute.input.id.index] = attribute.format.get(buffer, offset + attribute.offset)
        }
    }
}
