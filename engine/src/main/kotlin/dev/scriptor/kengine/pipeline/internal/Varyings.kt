package dev.scriptor.kengine.pipeline.internal

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f
import dev.scriptor.kengine.pipeline.Input
import dev.scriptor.kengine.pipeline.Output
import dev.scriptor.kengine.pipeline.ShaderLayout

fun varyings(
    src: ShaderLayout,
    dst: ShaderLayout,
): List<Varying<*>> {
    val outputs = src.outputs
        .filter { it.location >= 0 }
        .associateBy { it.location }

    return dst.inputs
        .filter { it.location >= 0 }
        .map { input ->
            val output = outputs[input.location]
                ?: error("missing vertex output for fragment input location ${input.location}")

            require(output.type == input.type)

            when (val type = output.type) {
                Float::class -> Varying(
                    output as Output<Float>,
                    input as Input<Float>,
                    Interpolator.FLOAT,
                )

                Vec2f::class -> Varying(
                    output as Output<Vec2f>,
                    input as Input<Vec2f>,
                    Interpolator.VEC2F,
                )

                Vec3f::class -> Varying(
                    output as Output<Vec3f>,
                    input as Input<Vec3f>,
                    Interpolator.VEC3F,
                )

                Vec4f::class -> Varying(
                    output as Output<Vec4f>,
                    input as Input<Vec4f>,
                    Interpolator.VEC4F,
                )

                else -> error("unsupported varying type $type")
            }
        }
}