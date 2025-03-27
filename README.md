# Kafka Azure OAuthBearer Login CallbackHandler

![Release](https://img.shields.io/github/v/release/conduktor/azure-kafka-oauthbearer?sort=semver&logo=github)
![License](https://img.shields.io/github/license/conduktor/azure-kafka-oauthbearer)
[![twitter](https://img.shields.io/twitter/follow/getconduktor.svg?style=social)](https://twitter.com/getconduktor)

This library provides a Kafka login callback handler for Azure Managed Identity with supports for both client certificate and environment variable authentication.

The library is based on the [Azure Identity]() library and is already integrated into [Conduktor Console](https://hub.docker.com/r/conduktor/conduktor-console) 1.23+ and [Conduktor Gateway](https://hub.docker.com/r/conduktor/conduktor-gateway) 3.1+.

## Usage

### Client certificate authentication

Use client certificate authentication to retrieve auth token bearer.   
More details on Azure identity [ClientCertificateCredential documentation](https://learn.microsoft.com/en-us/java/api/com.azure.identity.clientcertificatecredential?view=azure-java-stable)

#### Certificate without passphrase
Use `io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler` as the callback handler class and provide
the following required parameters in the `sasl.jaas.config` property : 
- `clientId` : The client id of the service principal
- `tenantId` : The tenant id of the service principal
- `certificate` : The path to the pfx or pem certificate file (Note in Console or Gateway, the certificat should be mounted to the container)
- `scope` : The [scope](https://learn.microsoft.com/en-us/entra/identity-platform/scopes-oidc#the-default-scope) of the token
```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=<clientId> tenantId=<tenantId> certificate=<pfx/pem cert path> scope="https://<resource>/.default";
```

#### Certificate with passphrase
Same as above but with the optional `certificatePass` parameter to provide the passphrase of the certificate.
```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required clientId=<clientId> tenantId=<tenantId> certificate=<pfx cert path> certificatePass=<cert passphrase> scope="https://<resource>/.default";
```

### Environment variable client certification

Use Azure default environment variables to configure token auth bearer retriever.
More details on Azure identity [EnvironmentCredential documentation](https://learn.microsoft.com/en-us/java/api/com.azure.identity.environmentcredential?view=azure-java-stable)

Use `io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler` as the callback handler class and provide
the following required parameters in the `sasl.jaas.config` property :
- `scope` : The [scope](https://learn.microsoft.com/en-us/entra/identity-platform/scopes-oidc#the-default-scope) of the token

The rest of the parameters are read from the environment variables.
- `AZURE_CLIENT_ID` / `AZURE_CLIENT_SECRET` / `AZURE_TENANT_ID` : for client secret authentication
- `AZURE_CLIENT_ID` / `AZURE_CLIENT_CERTIFICATE_PATH` / `AZURE_CLIENT_CERTIFICATE_PASSWORD` / `AZURE_TENANT_ID` : for client certificate authentication
- `AZURE_CLIENT_ID` / `AZURE_USERNAME` / `AZURE_PASSWORD` / `AZURE_TENANT_ID` : for username password authentication


```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required scope="https://<resource>/.default";
```

### Workload Identity Authentication

Use Azure workload identity environment variables to configure token auth bearer retriever.
More details on Azure identity [WorkloadIdentityCredential documentation](https://learn.microsoft.com/en-us/java/api/com.azure.identity.workloadidentitycredential?view=azure-java-stable)

More details about implementing Workload Identity on AKS are available [here](https://learn.microsoft.com/en-us/azure/aks/workload-identity-deploy-cluster).

Use `io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler` as the callback handler class and provide
the following required parameters in the `sasl.jaas.config` property :
- `scope` : The [scope](https://learn.microsoft.com/en-us/entra/identity-platform/scopes-oidc#the-default-scope) of the token

The rest of the parameters are read from the environment variables.
- `AZURE_CLIENT_ID` / `AZURE_TENANT_ID` / `AZURE_FEDERATED_TOKEN_FILE` : for workload identity authentication


```properties
sasl.login.callback.handler.class=io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required scope="https://<resource>/.default";
```

### Other authentication methods
[Other authentication methods](https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable) are supported yet and could be added in the future.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.
