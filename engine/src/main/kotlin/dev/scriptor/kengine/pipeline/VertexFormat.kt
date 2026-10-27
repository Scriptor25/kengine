package dev.scriptor.kengine.pipeline

import dev.scriptor.kengine.math.*
import java.nio.ByteBuffer
import kotlin.reflect.KClass

sealed interface VertexFormat<T : Any> {

    val type: KClass<T>
    val size: Int

    fun get(buffer: ByteBuffer, offset: Int): T

    object FLOAT : VertexFormat<Float> {

        override val type: KClass<Float> = Float::class
        override val size: Int = Float.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Float {
            return buffer.getFloat(offset)
        }
    }

    object VEC2F : VertexFormat<Vec2f> {

        override val type: KClass<Vec2f> = Vec2f::class
        override val size: Int = Vec2f.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Vec2f {
            return buffer.getVec2f(offset)
        }
    }

    object VEC3F : VertexFormat<Vec3f> {

        override val type: KClass<Vec3f> = Vec3f::class
        override val size: Int = Vec3f.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Vec3f {
            return buffer.getVec3f(offset)
        }
    }

    object VEC4F : VertexFormat<Vec4f> {

        override val type: KClass<Vec4f> = Vec4f::class
        override val size: Int = Vec4f.SIZE_BYTES

        override fun get(buffer: ByteBuffer, offset: Int): Vec4f {
            return buffer.getVec4f(offset)
        }
    }
}
