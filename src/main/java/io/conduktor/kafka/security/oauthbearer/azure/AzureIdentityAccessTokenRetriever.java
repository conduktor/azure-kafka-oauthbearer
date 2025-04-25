package io.conduktor.kafka.security.oauthbearer.azure;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ChainedTokenCredentialBuilder;
import com.azure.identity.ClientCertificateCredential;
import com.azure.identity.ClientCertificateCredentialBuilder;
import com.azure.identity.EnvironmentCredentialBuilder;
import com.azure.identity.WorkloadIdentityCredentialBuilder;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.security.oauthbearer.internals.secured.AccessTokenRetriever;
import org.apache.kafka.common.security.oauthbearer.internals.secured.JaasOptionsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.conduktor.kafka.security.oauthbearer.azure.AzureManagedIdentityCallbackHandler.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class AzureIdentityAccessTokenRetriever implements AccessTokenRetriever {

    private static final Logger log = LoggerFactory.getLogger(AzureIdentityAccessTokenRetriever.class);
    public static final String SCOPE_DELIMITER = ",";

    final String azureAuthType;    
    final Optional<ClientCertificateCredential> clientCertificateCredentials;
    final List<String> scopes;

    public AzureIdentityAccessTokenRetriever(String azureAuthType,
            Optional<ClientCertificateCredential> clientCertificateCredentials,
            List<String> scopes) {
        this.azureAuthType = azureAuthType;
        this.scopes = scopes;
        this.clientCertificateCredentials = clientCertificateCredentials;
    }

    public static AccessTokenRetriever create(Map<String, Object> jaasConfig) {
        JaasOptionsUtils jou = new JaasOptionsUtils(jaasConfig);

        var azureAuthType = Optional.ofNullable(jou.validateString(AZURE_CREDENTIAL_TYPE, false))
                .orElseThrow(() -> new ConfigException(String.format(
                        "The JAAS configuration option [%s] is either missing or set to null.",
                        AZURE_CREDENTIAL_TYPE)));

        var clientCertificateCredentials = Optional.ofNullable(jou.validateString(CLIENT_CERTIFICATE_CONFIG, false))
                .map(certificatePath -> new ClientCertificateCredentialBuilder()
                        .pfxCertificate(certificatePath)
                        .clientId(Optional.ofNullable(jou.validateString(CLIENT_ID_CONFIG, false))
                                .orElseThrow(() -> new ConfigException(String.format(
                                        "The OAuth configuration option %s value must be non-null when %s is set ",
                                        CLIENT_ID_CONFIG, CLIENT_CERTIFICATE_CONFIG))))
                        .tenantId(Optional.ofNullable(jou.validateString(TENANT_ID_CONFIG, false))
                                .orElseThrow(() -> new ConfigException(String.format(
                                        "The OAuth configuration option %s value must be non-null when %s is set ",
                                        TENANT_ID_CONFIG, CLIENT_CERTIFICATE_CONFIG))))
                        .clientCertificatePassword(Optional
                                .ofNullable(jou.validateString(CLIENT_CERTIFICATE_PASSWORD_CONFIG, false)).orElse(""))
                        .build());

        var scopes = Optional.ofNullable(jou.validateString(SCOPE_CONFIG, false))
                .map(config -> Arrays.stream(config.split(SCOPE_DELIMITER)).map(String::trim).toList())
                .orElse(List.of());

        return new AzureIdentityAccessTokenRetriever(azureAuthType, clientCertificateCredentials, scopes);
    }

    @Override
    public String retrieve() {
        try {
            var tokenCredential = switch (azureAuthType) {
                case "workload" -> new WorkloadIdentityCredentialBuilder().build();
                case "clientcertificate" -> clientCertificateCredentials;
                default -> new EnvironmentCredentialBuilder().build();
            };            
            return tokenCredential.getTokenSync(new TokenRequestContext().setScopes(scopes)).getToken();
        } catch (RuntimeException e) {
            log.warn("Error while generating token using Azure identity", e);
            throw new AuthenticationException(e);
        }
    }
}
