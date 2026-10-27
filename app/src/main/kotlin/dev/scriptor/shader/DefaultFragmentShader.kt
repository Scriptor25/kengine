package dev.scriptor.shader

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f
import dev.scriptor.kengine.pipeline.FragmentShaderLayout
import dev.scriptor.kengine.pipeline.Sampler2D
import dev.scriptor.kengine.pipeline.Shader
import dev.scriptor.kengine.pipeline.ShaderContext

object DefaultFragmentShaderLayout : FragmentShaderLayout() {
    val sampler = uniform<Sampler2D>()

    val inPosition = input<Vec3f>()
    val inNormal = input<Vec3f>()
    val inTexture = input<Vec2f>()
    val inColor = input<Vec4f>()
}

class DefaultFragmentShader(
    context: ShaderContext,
) : Shader<DefaultFragmentShaderLayout>(DefaultFragmentShaderLayout, context) {

    val sampler by use(layout.sampler)

    val inPosition by use(layout.inPosition)
    val inNormal by use(layout.inNormal)
    val inTexture by use(layout.inTexture)
    val inColor by use(layout.inColor)

    var color by use(layout.color)

    val sun = Vec3f(0.5f, 1.0f, 1.0f).unit

    override fun main() {
        val sampled = sampler(inTexture)
        val t = (sun dot inNormal.unit).coerceIn(0.1f, 1.0f)

        color = inColor * Vec4f(sampled.xyz * t, 1.0f)
    }
}
