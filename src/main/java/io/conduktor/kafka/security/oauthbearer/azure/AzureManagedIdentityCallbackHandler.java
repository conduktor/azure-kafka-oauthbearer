package io.conduktor.kafka.security.oauthbearer.azure;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.internals.secured.AccessTokenRetriever;
import org.apache.kafka.common.security.oauthbearer.internals.secured.AccessTokenValidator;
import org.apache.kafka.common.security.oauthbearer.internals.secured.AccessTokenValidatorFactory;
import org.apache.kafka.common.security.oauthbearer.internals.secured.JaasOptionsUtils;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.List;
import java.util.Map;

public class AzureManagedIdentityCallbackHandler extends OAuthBearerLoginCallbackHandler {

    public static final String TENANT_ID_CONFIG = "tenantId";
    public static final String CLIENT_ID_CONFIG = OAuthBearerLoginCallbackHandler.CLIENT_ID_CONFIG;
    public static final String CLIENT_CERTIFICATE_CONFIG = "certificate";
    public static final String CLIENT_CERTIFICATE_PASSWORD_CONFIG = "certificatePass";
    public static final String SCOPE_CONFIG = OAuthBearerLoginCallbackHandler.SCOPE_CONFIG;
    public static final String TENANT_ID_DOC = "The certificate to use for certificate assertion validation";
    public static final String CLIENT_ID_DOC = OAuthBearerLoginCallbackHandler.CLIENT_ID_DOC;
    public static final String CLIENT_CERTIFICATE_DOC = "The certificate to use for certificate assertion validation";
    public static final String CLIENT_CERTIFICATE_PASSWORD_DOC = "The passphrase for certificate";
    public static final String SCOPE_DOC = OAuthBearerLoginCallbackHandler.SCOPE_DOC;

    private Map<String, Object> moduleOptions;

    @Override
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        this.moduleOptions = JaasOptionsUtils.getOptions(saslMechanism, jaasConfigEntries);
        AccessTokenRetriever accessTokenRetriever = AzureIdentityAccessTokenRetriever.create(this.moduleOptions);
        AccessTokenValidator accessTokenValidator = AccessTokenValidatorFactory.create(configs, saslMechanism);
        this.init(accessTokenRetriever, accessTokenValidator);
    }
}
