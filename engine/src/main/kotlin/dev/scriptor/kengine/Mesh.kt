package dev.scriptor.kengine

import java.nio.ByteBuffer

data class Mesh(
    val vertexBuffer: ByteBuffer,
    val elementBuffer: ByteBuffer,
    val count: Int,
)
