package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.math.Vec4f

abstract class FragmentShaderLayout : ShaderLayout() {

    val color = output<Vec4f>(-1)
}
