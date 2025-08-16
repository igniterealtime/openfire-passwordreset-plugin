package org.jivesoftware.openfire.plugin.passwordreset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.io.File;
import java.net.URL;
import lombok.SneakyThrows;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.quality.Strictness;
import org.xmpp.packet.JID;

@SuppressWarnings({"WeakerAccess"})
public final class Fixtures {

    public static final String XMPP_DOMAIN = "test.xmpp.domain";

    private Fixtures() {
    }

    /**
     * Reconfigures the Openfire home directory to the blank test one. This allows {@link
     * JiveGlobals#setProperty(String, String)} etc. to work (and persist) in test classes without
     * errors being displayed to stderr. Ideally should be called in a {@link BeforeAll} method.
     */
    @SneakyThrows
    public static void reconfigureOpenfireHome() {
        final URL configFile = ClassLoader.getSystemResource("conf/openfire.xml");
        if (configFile == null) {
            throw new IllegalStateException(
                "Unable to read openfire.xml file; does conf/openfire.xml exist"
                    + " in the test classpath, i.e. test/resources?");
        }
        final File openfireHome = new File(configFile.toURI()).getParentFile().getParentFile();
        JiveGlobals.setHomeDirectory(openfireHome.toString());
        // The following allows JiveGlobals to persist
        JiveGlobals.setXMLProperty("setup", "true");
        // The following speeds up tests by avoiding DB retries
        JiveGlobals.setXMLProperty("database.maxRetries", "0");
        JiveGlobals.setXMLProperty("database.retryDelay", "0");
        clearExistingProperties();
    }


    /**
     * As {@link #reconfigureOpenfireHome()} allows properties to persist, this method clears all
     * existing properties (both XML and 'database') to ensure clean test output. Ideally should be
     * called in a {@link BeforeEach} method.
     */
    public static void clearExistingProperties() {
        JiveGlobals.getXMLPropertyNames().stream()
            .filter(name -> !"setup".equals(name))
            .filter(name -> !"database.maxRetries".equals(name))
            .filter(name -> !"database.retryDelay".equals(name))
            .forEach(JiveGlobals::deleteXMLProperty);
        JiveGlobals.getPropertyNames()
            .forEach(JiveGlobals::deleteProperty);
    }

    /**
     * Generates a Mockito based XMPPServer.
     *
     * @return a mock XMPPServer
     */
    public static XMPPServer mockXmppServer() {
        final XMPPServer xmppServer = mock(
            XMPPServer.class,
            withSettings().strictness(Strictness.LENIENT)
        );
        doAnswer(invocationOnMock -> {
            final JID jid = invocationOnMock.getArgument(0);
            return jid.getDomain().equals(XMPP_DOMAIN);
        }).when(xmppServer).isLocal(any(JID.class));

        doReturn(mockXmppServerInfo()).when(xmppServer).getServerInfo();
        doReturn(mockIqRouter()).when(xmppServer).getIQRouter();
        doReturn(mockPluginManager()).when(xmppServer).getPluginManager();

        return xmppServer;
    }

    /**
     * Generates a Mockito based XMPPServerInfo.
     *
     * @return a mock XMPPServerInfo
     */
    public static XMPPServerInfo mockXmppServerInfo() {
        final XMPPServerInfo xmppServerInfo = mock(
            XMPPServerInfo.class,
            withSettings().strictness(Strictness.LENIENT)
        );
        doReturn(XMPP_DOMAIN).when(xmppServerInfo).getXMPPDomain();
        return xmppServerInfo;
    }

    /**
     * Generates a Mockito based IQRouter.
     *
     * @return a mock IQRouter
     */
    public static IQRouter mockIqRouter() {
        return mock(IQRouter.class, withSettings().strictness(Strictness.LENIENT));
    }

    /**
     * Generates a Mockito based PluginManager.
     *
     * @return a mock PluginManager
     */
    public static PluginManager mockPluginManager() {
        return mock(PluginManager.class, withSettings().strictness(Strictness.LENIENT));
    }

}

