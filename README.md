# [GraphQL](http://graphql.org/) spring boot starter
> Provide a simple config for use of graphql.

### By default 

1. Scan all schema files from classpath://graphql/* for type definition, no recursive.
1. Find *TypeBuilder beans that match any type name in schema files.
1. If TypeBuilder not found, default TypeBuilder provided, find *DataFetcher beans that match any type name in schema files.
    - If ListType, *ListDataFetcher used.
    - If NonNullType, *NonNullDataFetcher used.
1. Wire up definitions and beans.
