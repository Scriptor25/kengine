package dev.scriptor.kengine

import dev.scriptor.kengine.math.Vec2f
import dev.scriptor.kengine.math.Vec3f
import dev.scriptor.kengine.math.Vec4f
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.cast

interface ShaderContext {

    operator fun get(id: UniformId): Any
    operator fun get(id: InputId): Any
    operator fun get(id: OutputId): Any
    operator fun set(id: OutputId, value: Any)
}

@JvmInline
value class UniformId(val index: Int)

@JvmInline
value class InputId(val index: Int)

@JvmInline
value class OutputId(val index: Int)

data class Uniform<T : Any>(
    val id: UniformId,
    val type: KClass<T>,
)

data class Input<T : Any>(
    val id: InputId,
    val type: KClass<T>,
    val location: Int,
)

data class Output<T : Any>(
    val id: OutputId,
    val type: KClass<T>,
    val location: Int,
)

abstract class ShaderLayout {

    private val uniformsList = mutableListOf<Uniform<*>>()
    private val inputsList = mutableListOf<Input<*>>()
    private val outputsList = mutableListOf<Output<*>>()

    private var nextInputLocation = 0
    private var nextOutputLocation = 0

    val uniforms: List<Uniform<*>>
        get() = uniformsList
    val inputs: List<Input<*>>
        get() = inputsList
    val outputs: List<Output<*>>
        get() = outputsList

    fun <T : Any> uniform(type: KClass<T>): Uniform<T> {
        val uniform = Uniform(
            UniformId(uniformsList.size),
            type,
        )

        uniformsList += uniform
        return uniform
    }

    fun <T : Any> input(type: KClass<T>, location: Int): Input<T> {
        nextInputLocation = location + 1

        val input = Input(
            InputId(inputsList.size),
            type,
            location,
        )

        inputsList += input
        return input
    }

    fun <T : Any> output(type: KClass<T>, location: Int): Output<T> {
        nextOutputLocation = location + 1

        val output = Output(
            OutputId(outputsList.size),
            type,
            location,
        )

        outputsList += output
        return output
    }

    fun <T : Any> input(type: KClass<T>): Input<T> {
        return input(type, nextInputLocation)
    }

    fun <T : Any> output(type: KClass<T>): Output<T> {
        return output(type, nextOutputLocation)
    }

    inline fun <reified T : Any> input(location: Int): Input<T> =
        input(T::class, location)

    inline fun <reified T : Any> output(location: Int): Output<T> =
        output(T::class, location)

    inline fun <reified T : Any> uniform(): Uniform<T> =
        uniform(T::class)

    inline fun <reified T : Any> input(): Input<T> =
        input(T::class)

    inline fun <reified T : Any> output(): Output<T> =
        output(T::class)
}

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

abstract class FragmentShaderLayout : ShaderLayout() {

    val color = output<Vec4f>(-1)
}

abstract class Shader<L : ShaderLayout>(
    val layout: L,
    private val context: ShaderContext,
) {
    interface Delegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    }

    interface MutableDelegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    }

    fun <T : Any> use(field: Uniform<T>): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast(context[field.id])
        }
    }

    fun <T : Any> use(field: Uniform<T>, index: Int): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast((context[field.id] as Array<*>)[index])
        }
    }

    fun <T : Any> use(field: Uniform<T>, index: () -> Int): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast((context[field.id] as Array<*>)[index()])
        }
    }

    fun <T : Any> use(field: Input<T>): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast(context[field.id])
        }
    }

    fun <T : Any> use(field: Output<T>): MutableDelegate<T> = object : MutableDelegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast(context[field.id])
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            context[field.id] = value
        }
    }

    abstract fun main()
}
