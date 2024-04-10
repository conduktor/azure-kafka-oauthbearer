# Kafka Azure OAuthBearer Login CallbackHandler

## Usage

### Client certificate authentication
[Azure identity doc](https://learn.microsoft.com/en-us/java/api/com.azure.identity.clientcertificatecredential?view=azure-java-stable)
```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=<clientId> tenantId=<tenantId> certificate=<pfx/pem cert path> scope="https://<ressource>/.default";
```

### Client certificate authentication (with passphrase)
[Azure identity doc](https://learn.microsoft.com/en-us/java/api/com.azure.identity.clientcertificatecredential?view=azure-java-stable)
```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=<clientId> tenantId=<tenantId> certificate=<pfx cert path> certificatePass=<cert passphrase> scope="https://<ressource>/.default";
```


## Environment variable client certification

[Azure identity doc](https://learn.microsoft.com/en-us/java/api/com.azure.identity.environmentcredential?view=azure-java-stable)
```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required scope="https://<ressource>/.default";
```

