package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f

abstract class VertexShaderLayout : ShaderLayout() {

    val instance = input<Int>(-1)
    val position = output<Vec4f>(-1)

    fun layout(): VertexLayout {
        val stride: Int
        val attributes = buildList {
            var offset = 0
            for (input in inputs.filter { it.id != instance.id }.sortedBy(Input<*>::location)) {
                val format = when (input.type) {
                    Float::class -> VertexFormat.FLOAT
                    Vec2f::class -> VertexFormat.VEC2
                    Vec3f::class -> VertexFormat.VEC3
                    Vec4f::class -> VertexFormat.VEC4
                    else -> error("unsupported vertex format")
                }

                add(VertexAttribute(input as Input<Any>, format as VertexFormat<Any>, offset))
                offset += format.size
            }

            stride = offset
        }

        return VertexLayout(stride, attributes)
    }
}
