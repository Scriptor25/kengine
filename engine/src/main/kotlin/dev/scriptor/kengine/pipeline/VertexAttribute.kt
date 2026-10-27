package dev.scriptor.kengine.pipeline

data class VertexAttribute<T : Any>(
    val input: Input<T>,
    val format: VertexFormat<T>,
    val offset: Int,
)
