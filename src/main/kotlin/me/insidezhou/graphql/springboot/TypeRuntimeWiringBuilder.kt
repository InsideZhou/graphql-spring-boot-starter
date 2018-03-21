package me.insidezhou.graphql.springboot

import graphql.schema.idl.TypeRuntimeWiring


interface TypeRuntimeWiringBuilder {
    fun wiring(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder {
        return builder
    }
}