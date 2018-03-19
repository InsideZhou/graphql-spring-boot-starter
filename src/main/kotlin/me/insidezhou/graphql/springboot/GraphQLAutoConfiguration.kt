package me.insidezhou.graphql.springboot

import graphql.GraphQL
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.apache.commons.logging.LogFactory
import org.springframework.beans.BeansException
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths


@Configuration
open class GraphQLAutoConfiguration {
    private val log = LogFactory.getLog(GraphQLAutoConfiguration::class.java)

    @Bean
    @ConditionalOnMissingBean
    open fun typeDefinitionRegistry(): TypeDefinitionRegistry {
        val schemaParser = SchemaParser()

        return Files.list(Paths.get(GraphQLAutoConfiguration::class.java.classLoader.getResource("graphql").toURI()))
            .map { schemaParser.parse(it.toFile()) }
            .reduce { t: TypeDefinitionRegistry, u: TypeDefinitionRegistry -> t.merge(u) }
            .get()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun runtimeWiring(registry: TypeDefinitionRegistry, applicationContext: ApplicationContext, properties: Properties): RuntimeWiring {
        val builder = RuntimeWiring.newRuntimeWiring()

        registry.schemaDefinition().get().operationTypeDefinitions.forEach { operationTypeDefinition ->
            val typeName = operationTypeDefinition.type as TypeName

            builder.type(typeName.name, { builder ->
                val typeDefinition = registry.types().values.find { typeDefinition -> typeDefinition.name == typeName.name } as ObjectTypeDefinition

                typeDefinition.fieldDefinitions.forEach { fieldDefinition: FieldDefinition ->
                    val fieldName = fieldDefinition.name

                    try {
                        val dataFetcher = applicationContext.getBean(fieldName + properties.dataFetcherBeanSuffix, DataFetcher::class.java)
                        builder.dataFetcher(fieldName, dataFetcher)
                    }
                    catch (e: BeansException) {
                        log.warn("Fail on initializing ${fieldName + properties.dataFetcherBeanSuffix}", e)
                    }
                }

                return@type builder
            })
        }

        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    open fun graphQL(registry: TypeDefinitionRegistry, runtimeWiring: RuntimeWiring): GraphQL {
        val schema = SchemaGenerator().makeExecutableSchema(registry, runtimeWiring)

        return GraphQL.newGraphQL(schema).build()
    }

    @Component
    @ConfigurationProperties("graphql")
    class Properties {
        /**
         * used to find DataFetcher bean with suffix
         */
        var dataFetcherBeanSuffix = "DataFetcher"
    }
}