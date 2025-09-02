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
    final List<String> scopes;

    final Optional<ClientCertificateCredential> clientCertificateCredentials;

    public AzureIdentityAccessTokenRetriever(Optional<ClientCertificateCredential> clientCertificateCredentials, List<String> scopes) {
        this.scopes = scopes;
        this.clientCertificateCredentials = clientCertificateCredentials;
    }


    public static AccessTokenRetriever create(Map<String, Object> jaasConfig) {
        JaasOptionsUtils jou = new JaasOptionsUtils(jaasConfig);
        var clientCertificateCredentials = Optional.ofNullable(jou.validateString(CLIENT_CERTIFICATE_CONFIG, false))
                .map(certificatePath ->
                        new ClientCertificateCredentialBuilder()
                                .pfxCertificate(certificatePath)
                                .clientId(Optional.ofNullable(jou.validateString(CLIENT_ID_CONFIG, false)).orElseThrow(() -> new ConfigException(String.format("The OAuth configuration option %s value must be non-null when %s is set ", CLIENT_ID_CONFIG, CLIENT_CERTIFICATE_CONFIG))))
                                .tenantId(Optional.ofNullable(jou.validateString(TENANT_ID_CONFIG, false)).orElseThrow(() -> new ConfigException(String.format("The OAuth configuration option %s value must be non-null when %s is set ", TENANT_ID_CONFIG, CLIENT_CERTIFICATE_CONFIG))))
                                .clientCertificatePassword(Optional.ofNullable(jou.validateString(CLIENT_CERTIFICATE_PASSWORD_CONFIG, false)).orElse(""))
                                .build()
                );
        var scopes = Optional.ofNullable(jou.validateString(SCOPE_CONFIG, false))
                .map(config -> Arrays.stream(config.split(SCOPE_DELIMITER)).map(String::trim).toList())
                .orElse(List.of());

        return new AzureIdentityAccessTokenRetriever(clientCertificateCredentials, scopes);
    }

    @Override
    public String retrieve() {
        try {
            // See https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-client-creds-grant-flow#second-case-access-token-request-with-a-certificate
            // See https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable#credential-classes
            var chainedTokenCredentialBuilder = new ChainedTokenCredentialBuilder();
            clientCertificateCredentials.ifPresent(chainedTokenCredentialBuilder::addFirst);
            chainedTokenCredentialBuilder.addLast(new EnvironmentCredentialBuilder().build());
            chainedTokenCredentialBuilder.addLast(new WorkloadIdentityCredentialBuilder().build());            

            var clientCredentials = chainedTokenCredentialBuilder.build();
            return clientCredentials.getTokenSync(new TokenRequestContext().setScopes(scopes)).getToken();
        } catch (RuntimeException e) {
            log.warn("Error while generating token using Azure identity", e);
            throw new AuthenticationException(e);
        }
    }
}
