package dev.scriptor.kengine.pipeline

interface ShaderContext {

    operator fun get(id: UniformId): Any
    operator fun get(id: InputId): Any
    operator fun get(id: OutputId): Any
    operator fun set(id: OutputId, value: Any)
}
