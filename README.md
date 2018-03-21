# [GraphQL](http://graphql.org/) spring boot starter
> Provide a simple config for use of graphql.

### By default 

1. Scan all schema files from classpath://graphql/* for type definition, no recursive.
1. Find *TypeBuilder beans that match any type name in schema files.
1. If TypeBuilder not found, default TypeBuilder provided, find *DataFetcher beans that match any type name in schema files.
    - If ListType, *ListDataFetcher used.
    - If NonNullType, *NonNullDataFetcher used.
1. Wire up definitions and beans.


### Example
```
schema {
    query: Query
}

type Query {
    login: Login
    orderList: [Order]
}

```

```
data class Order(var code: String)
data class Login(var cellphone: String)


@Component
class LoginDataFetcher : DataFetcher<Login> {
    override fun get(environment: DataFetchingEnvironment): Login {
        return Login("13800138000")
    }
}


@Component
class OrderListDataFetcher : DataFetcher<Array<Order>> {
    override fun get(environment: DataFetchingEnvironment?): Array<Order> {
        return arrayOf(Order("NN20190101123238781"))
    }
}
```