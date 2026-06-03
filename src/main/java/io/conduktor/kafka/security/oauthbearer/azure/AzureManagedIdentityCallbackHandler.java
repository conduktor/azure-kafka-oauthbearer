package io.conduktor.kafka.security.oauthbearer.azure;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.auth.SaslExtensions;
import org.apache.kafka.common.security.auth.SaslExtensionsCallback;
import org.apache.kafka.common.security.oauthbearer.DefaultJwtValidator;
import org.apache.kafka.common.security.oauthbearer.JwtRetriever;
import org.apache.kafka.common.security.oauthbearer.JwtValidator;
import org.apache.kafka.common.security.oauthbearer.JwtValidatorException;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.apache.kafka.common.security.oauthbearer.internals.OAuthBearerClientInitialResponse;
import org.apache.kafka.common.security.oauthbearer.internals.secured.JaasOptionsUtils;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureManagedIdentityCallbackHandler implements AuthenticateCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(AzureManagedIdentityCallbackHandler.class);

    public static final String TENANT_ID_CONFIG = "tenantId";
    public static final String CLIENT_ID_CONFIG = OAuthBearerLoginCallbackHandler.CLIENT_ID_CONFIG;
    public static final String CLIENT_CERTIFICATE_CONFIG = "certificate";
    public static final String CLIENT_CERTIFICATE_PASSWORD_CONFIG = "certificatePass";
    public static final String SCOPE_CONFIG = OAuthBearerLoginCallbackHandler.SCOPE_CONFIG;

    private static final String EXTENSION_PREFIX = "extension_";

    private Map<String, Object> moduleOptions;

    private JwtRetriever jwtRetriever;

    private JwtValidator jwtValidator;

    @Override
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        this.moduleOptions = JaasOptionsUtils.getOptions(saslMechanism, jaasConfigEntries);

        JwtRetriever retriever = new AzureIdentityAccessTokenRetriever();
        retriever.configure(configs, saslMechanism, jaasConfigEntries);
        this.jwtRetriever = retriever;

        // Azure only customises token retrieval; reuse Kafka's stock client-side validator
        // (DefaultJwtValidator -> ClientJwtValidator) to parse the JWT into an OAuthBearerToken.
        JwtValidator validator = new DefaultJwtValidator();
        validator.configure(configs, saslMechanism, jaasConfigEntries);
        this.jwtValidator = validator;
    }

    @Override
    public void close() {
        Utils.closeQuietly(jwtRetriever, "JWT retriever");
        Utils.closeQuietly(jwtValidator, "JWT validator");
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        checkConfigured();
        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback) {
                handleTokenCallback((OAuthBearerTokenCallback) callback);
            } else if (callback instanceof SaslExtensionsCallback) {
                handleExtensionsCallback((SaslExtensionsCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private void handleTokenCallback(OAuthBearerTokenCallback callback) {
        String accessToken = jwtRetriever.retrieve();

        try {
            OAuthBearerToken token = jwtValidator.validate(accessToken);
            callback.token(token);
        } catch (JwtValidatorException e) {
            log.warn(e.getMessage(), e);
            callback.error("invalid_token", e.getMessage(), null);
        }
    }

    private void handleExtensionsCallback(SaslExtensionsCallback callback) {
        Map<String, String> extensions = new HashMap<>();

        for (Map.Entry<String, Object> configEntry : this.moduleOptions.entrySet()) {
            String key = configEntry.getKey();

            if (!key.startsWith(EXTENSION_PREFIX))
                continue;

            Object valueRaw = configEntry.getValue();
            String value;

            if (valueRaw instanceof String)
                value = (String) valueRaw;
            else
                value = String.valueOf(valueRaw);

            extensions.put(key.substring(EXTENSION_PREFIX.length()), value);
        }

        SaslExtensions saslExtensions = new SaslExtensions(extensions);

        try {
            OAuthBearerClientInitialResponse.validateExtensions(saslExtensions);
        } catch (SaslException e) {
            throw new ConfigException(e.getMessage());
        }

        callback.extensions(saslExtensions);
    }

    private void checkConfigured() {
        if (moduleOptions == null || jwtRetriever == null || jwtValidator == null)
            throw new IllegalStateException(String.format("To use %s, first call the configure method", getClass().getSimpleName()));
    }
}
