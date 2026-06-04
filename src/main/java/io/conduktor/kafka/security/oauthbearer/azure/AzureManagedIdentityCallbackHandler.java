package io.conduktor.kafka.security.oauthbearer.azure;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.ClientJwtValidator;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin back-compat wrapper: plugs the Azure token retriever and the client-side validator into
 * Kafka's {@link OAuthBearerLoginCallbackHandler}. New setups can instead drop this handler and set
 * {@code sasl.oauthbearer.jwt.retriever.class} to {@link AzureIdentityAccessTokenRetriever}.
 */
public class AzureManagedIdentityCallbackHandler implements AuthenticateCallbackHandler {

    public static final String TENANT_ID_CONFIG = "tenantId";
    public static final String CLIENT_ID_CONFIG = OAuthBearerLoginCallbackHandler.CLIENT_ID_CONFIG;
    public static final String CLIENT_CERTIFICATE_CONFIG = "certificate";
    public static final String CLIENT_CERTIFICATE_PASSWORD_CONFIG = "certificatePass";
    public static final String SCOPE_CONFIG = OAuthBearerLoginCallbackHandler.SCOPE_CONFIG;

    private final OAuthBearerLoginCallbackHandler delegate = new OAuthBearerLoginCallbackHandler();

    @Override
    public void configure(
            Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        var configsWithAzure = new HashMap<String, Object>(configs);
        configsWithAzure.put(
                SaslConfigs.SASL_OAUTHBEARER_JWT_RETRIEVER_CLASS,
                AzureIdentityAccessTokenRetriever.class.getName());
        // login (client-side) handler: decode only. Pin ClientJwtValidator so a stray
        // jwks.endpoint.url can't flip the default validator to broker-side JWKS verification.
        configsWithAzure.put(
                SaslConfigs.SASL_OAUTHBEARER_JWT_VALIDATOR_CLASS, ClientJwtValidator.class.getName());
        delegate.configure(configsWithAzure, saslMechanism, jaasConfigEntries);
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        delegate.handle(callbacks);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
