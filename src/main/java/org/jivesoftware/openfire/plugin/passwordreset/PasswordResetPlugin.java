package org.jivesoftware.openfire.plugin.passwordreset;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.EmailService;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.SystemProperty;

@Slf4j
public class PasswordResetPlugin implements Plugin {

    public static final String PLUGIN_NAME = "Password Reset"; // Exact match to plugin.xml
    public static final SystemProperty<Boolean> ENABLED =
        SystemProperty.Builder.ofType(Boolean.class)
            .setKey("plugin.passwordreset.enabled")
            .setDefaultValue(Boolean.FALSE)
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<String> SERVER =
        SystemProperty.Builder.ofType(String.class)
            .setKey("plugin.passwordreset.server")
            .setDefaultValue("")
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<String> SENDER_NAME =
        SystemProperty.Builder.ofType(String.class)
            .setKey("plugin.passwordreset.sender-name")
            .setDefaultValue("Openfire")
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<String> SENDER_ADDRESS =
        SystemProperty.Builder.ofType(String.class)
            .setKey("plugin.passwordreset.sender-address")
            .setDefaultValue("admin@example.com")
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<String> SUBJECT =
        SystemProperty.Builder.ofType(String.class)
            .setKey("plugin.passwordreset.email-subject")
            .setDefaultValue("Openfire password reset")
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<String> BODY =
        SystemProperty.Builder.ofType(String.class)
            .setKey("plugin.passwordreset.email-body")
            .setDefaultValue("Dear ${userName}\n\n"
                + "To reset the password for your ${userId} Openfire account, simply go to ${url}"
                + " at any time in the next five hours. After this time, you will need to request"
                + " another reset email is sent to you.")
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<Duration> EXPIRY =
        SystemProperty.Builder.ofType(Duration.class)
            .setChronoUnit(ChronoUnit.MINUTES)
            .setKey("plugin.passwordreset.reset-expiry")
            .setDefaultValue(Duration.ofHours(5))
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<Integer> MIN_LENGTH =
        SystemProperty.Builder.ofType(Integer.class)
            .setKey("plugin.passwordreset.min-length")
            .setMinValue(1)
            .setDefaultValue(8)
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final SystemProperty<Integer> MAX_LENGTH =
        SystemProperty.Builder.ofType(Integer.class)
            .setKey("plugin.passwordreset.max-length")
            .setMinValue(0)
            .setDefaultValue(0)
            .setDynamic(true)
            .setPlugin(PLUGIN_NAME)
            .build();
    public static final String CONTEXT_PATH = "/passwordreset";
    private static String canonicalName;
    private static PasswordResetPlugin plugin;
    private final HttpBindManager httpBindManager;
    private final PasswordResetMailer passwordResetMailer;
    private final PasswordResetTokenManager resetTokenManager;
    private WebAppContext webAppContext;

    private static void setInstance(final PasswordResetPlugin plugin) {
        PasswordResetPlugin.plugin = plugin;
    }

    private static void setCanonicalName(final String canonicalName) {
        PasswordResetPlugin.canonicalName = canonicalName;
    }

    public static PasswordResetPlugin getInstance() {
        return plugin;
    }

    /**
     * Default constructor for the plugin.
     */
    public PasswordResetPlugin() {
        setInstance(this);
        this.httpBindManager = HttpBindManager.getInstance();
        this.passwordResetMailer = new PasswordResetMailer(EmailService.getInstance());
        this.resetTokenManager = new PasswordResetTokenManager(
            DbConnectionManager::getConnection,
            UserManager.getInstance());
        setBlankServerDetails();
        log.debug("Plugin created");
    }

    public static String localize(final String key, final Object... arguments) {
        return LocaleUtils.getLocalizedString(key, canonicalName, Arrays.asList(arguments));
    }

    private void setBlankServerDetails() {
        if (SERVER.getValue().isEmpty()) {
            // Set a default value for this as there isn't one already
            final String defaultUrl;
            if (httpBindManager.isHttpsBindActive()) {
                defaultUrl = String.format("https://%s:%d%s",
                    XMPPServer.getInstance().getServerInfo().getHostname(),
                    httpBindManager.getHttpBindSecurePort(),
                    CONTEXT_PATH);
            } else {
                defaultUrl = String.format("http://%s:%d%s",
                    XMPPServer.getInstance().getServerInfo().getHostname(),
                    httpBindManager.getHttpBindUnsecurePort(),
                    CONTEXT_PATH);
            }
            SERVER.setValue(defaultUrl);
        }
    }

    @Override
    public void initializePlugin(final PluginManager manager, final File pluginDirectory) {
        log.debug("Plugin initialisation started");

        setCanonicalName(manager.getCanonicalName(this));

        webAppContext = new WebAppContext(pluginDirectory.getPath() + "/web-client",
            CONTEXT_PATH);
        webAppContext.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        httpBindManager.addJettyHandler(webAppContext);

        log.debug("Plugin initialisation complete");
    }

    @Override
    public void destroyPlugin() {
        log.debug("Plugin destruction started");
        httpBindManager.removeJettyHandler(webAppContext);
        log.debug("Plugin destruction complete");
    }

    public PasswordResetMailer getPasswordResetMailer() {
        return passwordResetMailer;
    }

    public PasswordResetTokenManager getResetTokenManager() {
        return resetTokenManager;
    }
}
