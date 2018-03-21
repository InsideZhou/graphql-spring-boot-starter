package me.insidezhou.graphql.springboot

import graphql.GraphQL
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.schema.DataFetcher
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.apache.commons.logging.LogFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
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
    open fun runtimeWiring(registry: TypeDefinitionRegistry, applicationContext: ApplicationContext, properties: Properties): RuntimeWiring.Builder {
        val runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()

        val allTypeDefinitions = registry.types().values
        val allTypeNames = allTypeDefinitions.map { it.name }

        allTypeDefinitions
            .filter { it is ObjectTypeDefinition }
            .forEach { typeDefinition ->
                val typeName = typeDefinition.name.decapitalize()

                try {
                    val typeBuilder = applicationContext.getBean(typeName + properties.typeBuilderSuffix, TypeRuntimeWiringBuilder::class.java)
                    runtimeWiringBuilder.type(typeDefinition.name, { typeBuilder.wiring(it) })
                }
                catch (e: NoSuchBeanDefinitionException) {
                    log.warn("${typeName + properties.typeBuilderSuffix} not found, using default.")

                    runtimeWiringBuilder.type(typeDefinition.name, { typeBuilder ->
                        (typeDefinition as ObjectTypeDefinition).fieldDefinitions
                            .forEach { fieldDefinition ->
                                val fieldType = fieldDefinition.type

                                when (fieldType) {
                                    is NonNullType -> fieldType.type as TypeName
                                    is ListType -> fieldType.type as TypeName
                                    else -> (fieldType as? TypeName)
                                }?.apply {
                                    if (!allTypeNames.contains(this.name)) return@apply

                                    val fieldTypeName = when (fieldType) {
                                        is NonNullType -> "${this.name}NonNull"
                                        is ListType -> "${this.name}List"
                                        else -> this.name
                                    }.decapitalize()

                                    try {
                                        val dataFetcher = applicationContext.getBean(fieldTypeName + properties.dataFetcherSuffix, DataFetcher::class.java)
                                        typeBuilder.dataFetcher(fieldDefinition.name, dataFetcher)
                                    }
                                    catch (e: NoSuchBeanDefinitionException) {
                                        log.warn("${fieldTypeName + properties.dataFetcherSuffix} not found")
                                    }
                                    catch (e: BeansException) {
                                        log.error("Fail on initializing ${fieldTypeName + properties.dataFetcherSuffix}", e)
                                    }
                                }
                            }

                        return@type typeBuilder
                    })
                }
                catch (e: BeansException) {
                    log.error("Fail on initializing ${typeName + properties.typeBuilderSuffix}", e)
                }
            }

        return runtimeWiringBuilder
    }

    @Bean
    @ConditionalOnMissingBean
    open fun graphQL(registry: TypeDefinitionRegistry, runtimeWiringBuilder: RuntimeWiring.Builder): GraphQL {
        val schema = SchemaGenerator().makeExecutableSchema(registry, runtimeWiringBuilder.build())

        return GraphQL.newGraphQL(schema).build()
    }

    @Component
    @ConfigurationProperties("graphql")
    class Properties {
        /**
         * used to find TypeBuilder bean with suffix
         */
        var typeBuilderSuffix = "TypeBuilder"

        /**
         * used to find DataFetcher bean with suffix
         */
        var dataFetcherSuffix = "DataFetcher"
    }
}