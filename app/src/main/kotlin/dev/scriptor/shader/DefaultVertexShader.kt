package dev.scriptor.shader

import dev.scriptor.kengine.math.*
import dev.scriptor.kengine.pipeline.Shader
import dev.scriptor.kengine.pipeline.ShaderContext
import dev.scriptor.kengine.pipeline.VertexShaderLayout

object DefaultVertexShaderLayout : VertexShaderLayout() {
    val transforms = uniform<Mat4f>()
    val models = uniform<Mat4f>()
    val normalModels = uniform<Mat3f>()

    val inPosition = input<Vec3f>()
    val inNormal = input<Vec3f>()
    val inTexture = input<Vec2f>()
    val inColor = input<Vec4f>()

    val outPosition = output<Vec3f>()
    val outNormal = output<Vec3f>()
    val outTexture = output<Vec2f>()
    val outColor = output<Vec4f>()
}

class DefaultVertexShader(
    context: ShaderContext,
) : Shader<DefaultVertexShaderLayout>(DefaultVertexShaderLayout, context) {

    val transform by use(layout.transforms)
    val model by use(layout.models)
    val normalModel by use(layout.normalModels)

    val inPosition by use(layout.inPosition)
    val inNormal by use(layout.inNormal)
    val inTexture by use(layout.inTexture)
    val inColor by use(layout.inColor)

    var position by use(layout.position)

    var outPosition by use(layout.outPosition)
    var outNormal by use(layout.outNormal)
    var outTexture by use(layout.outTexture)
    var outColor by use(layout.outColor)

    override fun main() {
        position = transform * Vec4f(inPosition, 1.0f)

        val pos = model * Vec4f(inPosition, 1.0f)
        val nor = normalModel * inNormal

        outPosition = pos.xyz / pos.w
        outNormal = nor
        outTexture = inTexture
        outColor = inColor
    }
}
