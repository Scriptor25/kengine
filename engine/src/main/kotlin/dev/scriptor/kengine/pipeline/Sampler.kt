package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f

interface Sampler1D {

    operator fun invoke(x: Float): Vec4f
}

interface Sampler2D {

    operator fun invoke(xy: Vec2f): Vec4f
}

interface Sampler3D {

    operator fun invoke(xyz: Vec3f): Vec4f
}
