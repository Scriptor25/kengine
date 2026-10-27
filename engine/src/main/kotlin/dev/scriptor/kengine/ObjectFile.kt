package dev.scriptor.kengine

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.pipeline.ElementBuffer
import java.io.InputStream
import java.nio.ByteBuffer

data class ObjectFile(
    val positions: List<Vec3f>,
    val textures: List<Vec2f>,
    val normals: List<Vec3f>,
    val faces: List<Face>,
) {

    data class Vertex(
        val position: Int,
        val texture: Int,
        val normal: Int,
    )

    data class Face(
        val vertices: List<Vertex>,
    )

    fun generate(block: (MutableList<Float>, Vec3f, Vec2f, Vec3f) -> Unit): Mesh {
        val vertices = mutableListOf<Float>()
        val elements = mutableListOf<Int>()

        var offset = 0
        for (face in faces) {
            for (i in 1 until face.vertices.size - 1) {
                elements += offset
                elements += offset + i
                elements += offset + i + 1
            }

            for (vertex in face.vertices) {
                val position =
                    if (vertex.position > 0) positions[vertex.position - 1]
                    else if (vertex.position < 0) positions[positions.size + vertex.position]
                    else Vec3f()

                val texture =
                    if (vertex.texture > 0) textures[vertex.texture - 1]
                    else if (vertex.texture < 0) textures[textures.size + vertex.texture]
                    else Vec2f()

                val normal =
                    if (vertex.normal > 0) normals[vertex.normal - 1]
                    else if (vertex.normal < 0) normals[normals.size + vertex.normal]
                    else Vec3f()

                block(vertices, position, texture, normal)

                ++offset
            }
        }

        val vertexBuffer = ByteBuffer.allocate(vertices.size * Float.SIZE_BYTES)
            .apply {
                for (vertex in vertices) {
                    putFloat(vertex)
                }
            }
            .flip()

        val elementBuffer = ElementBuffer.IntElementBuffer(elements.toIntArray())

        return Mesh(
            vertexBuffer,
            elementBuffer,
            elements.size,
        )
    }

    companion object {
        fun load(stream: InputStream): ObjectFile {
            val positions = mutableListOf<Vec3f>()
            val textures = mutableListOf<Vec2f>()
            val normals = mutableListOf<Vec3f>()
            val faces = mutableListOf<Face>()

            stream.bufferedReader().forEachLine { line ->
                parse(
                    positions,
                    textures,
                    normals,
                    faces,
                    line.trim(),
                )
            }

            return ObjectFile(
                positions,
                textures,
                normals,
                faces,
            )
        }

        private fun parse(
            positions: MutableList<Vec3f>,
            textures: MutableList<Vec2f>,
            normals: MutableList<Vec3f>,
            faces: MutableList<Face>,
            line: String,
        ) {
            if (line.isEmpty())
                return

            if (line.startsWith('#'))
                return

            val parts = line.split("\\s+".toRegex())
            when (parts[0]) {
                "v" -> {
                    require(parts.size == 4)

                    val v0 = parts[1].toFloat()
                    val v1 = parts[2].toFloat()
                    val v2 = parts[3].toFloat()

                    positions += Vec3f(v0, v1, v2)
                }

                "vt" -> {
                    require(parts.size == 3)

                    val v0 = parts[1].toFloat()
                    val v1 = parts[2].toFloat()

                    textures += Vec2f(v0, v1)
                }

                "vn" -> {
                    require(parts.size == 4)

                    val v0 = parts[1].toFloat()
                    val v1 = parts[2].toFloat()
                    val v2 = parts[3].toFloat()

                    normals += Vec3f(v0, v1, v2)
                }

                "f" -> {
                    require(parts.size >= 4)

                    val vertices = parts
                        .subList(1, parts.size)
                        .map {
                            val segments = it
                                .split('/')
                                .map(String::toInt)
                            require(segments.size in 1..3)

                            val position: Int
                            val texture: Int
                            val normal: Int

                            when (segments.size) {
                                1 -> {
                                    position = segments[0]
                                    texture = 0
                                    normal = 0
                                }

                                2 -> {
                                    position = segments[0]
                                    texture = segments[1]
                                    normal = 0
                                }

                                3 -> {
                                    position = segments[0]
                                    texture = segments[1]
                                    normal = segments[2]
                                }

                                else -> error("dead code")
                            }

                            Vertex(
                                position,
                                texture,
                                normal,
                            )
                        }

                    faces += Face(vertices)
                }

                else -> Unit
            }
        }
    }
}
