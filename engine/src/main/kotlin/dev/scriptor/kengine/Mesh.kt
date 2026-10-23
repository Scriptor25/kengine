package dev.scriptor.kengine

import java.nio.ByteBuffer
import java.nio.IntBuffer

data class Mesh(
    val vertexBuffer: ByteBuffer,
    val indexBuffer: IntBuffer,
    val count: Int,
)
