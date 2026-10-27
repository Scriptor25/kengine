package dev.scriptor.kengine

import dev.scriptor.kengine.pipeline.ElementBuffer
import java.nio.ByteBuffer

data class Mesh(
    val vertexBuffer: ByteBuffer,
    val elementBuffer: ElementBuffer,
    val count: Int,
)
