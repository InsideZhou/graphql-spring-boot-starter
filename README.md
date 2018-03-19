# [GraphQL](http://graphql.org/) springboot auto configuration
> Provide a simple config for use of graphql.

### By default 

1. Scan all schema files from classpath://graphql/* for type definition, no recursive.
1. Find *DataFetcher beans that match any field's name in schema files.
1. Wire up definition and bean.
