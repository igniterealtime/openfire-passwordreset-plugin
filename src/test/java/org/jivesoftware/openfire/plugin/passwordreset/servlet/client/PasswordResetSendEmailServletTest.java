package org.jivesoftware.openfire.plugin.passwordreset.servlet.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Date;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.plugin.passwordreset.Fixtures;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetMailer;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetTokenManager;
import org.jivesoftware.openfire.plugin.passwordreset.servlet.client.PasswordResetSendEmailServlet.Form;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetSendEmailServletTest {

    private static final String REMOTE_ADDRESS = "the-clients-address";
    private static final String TOKEN = "my-random-token";
    private PasswordResetSendEmailServlet servlet;
    private XMPPServer xmppServer;
    private User user;
    @Mock
    private UserProvider userProvider;
    @Mock
    private UserManager userManager;
    @Mock
    private PasswordResetTokenManager resetTokenManager;
    @Mock
    private PasswordResetMailer passwordResetMailer;
    @Mock
    private HttpServletRequest request;
    @Mock
    private RequestDispatcher requestDispatcher;
    @Mock
    private HttpServletResponse response;

    @BeforeAll
    @SuppressWarnings("deprecation")
    static void beforeAll() throws Exception {
        Fixtures.reconfigureOpenfireHome();
        XMPPServer.setInstance(Fixtures.mockXmppServer());
    }

    @SuppressWarnings("deprecation")
    @BeforeEach
    void setUp() {
        Fixtures.clearExistingProperties();

        xmppServer = Fixtures.mockXmppServer();
        XMPPServer.setInstance(xmppServer);

        PasswordResetPlugin.ENABLED.setValue(true);

        user = new User("test-username", "Test User", "test@example.com", new Date(), new Date());

        servlet = new PasswordResetSendEmailServlet();
        PasswordResetSendEmailServlet.initStatic(
            xmppServer,
            userProvider,
            userManager,
            resetTokenManager,
            passwordResetMailer);
    }

    @Test
    void getWillForwardWithBlankValidForm() throws Exception {

        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");

        servlet.doGet(request, response);

        verify(requestDispatcher).forward(request, response);
        final ArgumentCaptor<PasswordResetSendEmailServlet.Form> argumentCaptor =
            ArgumentCaptor.forClass(PasswordResetSendEmailServlet.Form.class);
        verify(request).setAttribute(eq("form"), argumentCaptor.capture());
        final Form form = argumentCaptor.getValue();
        assertThat(form.isEnabled())
            .isEqualTo(true);
        assertThat(form.isValid())
            .isEqualTo(true);
        assertThat(form.getUser())
            .isEqualTo("");
        assertThat(form.getUserError())
            .isEqualTo("");
    }

    @Test
    void postWithBlankUserIdWillForwardWithInvalidForm() throws Exception {

        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");

        servlet.doPost(request, response);

        verify(requestDispatcher).forward(request, response);
        final ArgumentCaptor<PasswordResetSendEmailServlet.Form> argumentCaptor =
            ArgumentCaptor.forClass(PasswordResetSendEmailServlet.Form.class);
        verify(request).setAttribute(eq("form"), argumentCaptor.capture());
        final Form form = argumentCaptor.getValue();
        assertThat(form.isEnabled())
            .isEqualTo(true);
        assertThat(form.isValid())
            .isEqualTo(false);
        assertThat(form.getUser())
            .isEqualTo("");
        assertThat(form.getUserError())
            .isEqualTo("passwordreset.send-email.no-user");
    }

    @Test
    void postWithValidUserIdWillGenerateTokenAndSendEmail() throws Exception {

        doReturn(REMOTE_ADDRESS)
            .when(request)
            .getRemoteAddr();
        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");
        doReturn(user.getUsername())
            .when(request)
            .getParameter("user");

        doReturn(user)
            .when(userManager)
            .getUser(user.getUsername());

        doReturn(TOKEN)
            .when(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);

        servlet.doPost(request, response);

        verify(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);
        verify(passwordResetMailer)
            .sendEmail(user, TOKEN);
        verify(requestDispatcher)
            .forward(request, response);
    }

    @Test
    void postWithValidJidWillGenerateTokenAndSendEmail() throws Exception {

        doReturn(REMOTE_ADDRESS)
            .when(request)
            .getRemoteAddr();
        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");
        doReturn(user.getUsername() + "@" + xmppServer.getServerInfo().getXMPPDomain())
            .when(request)
            .getParameter("user");

        doReturn(user)
            .when(userManager)
            .getUser(user.getUsername());

        doReturn(TOKEN)
            .when(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);

        servlet.doPost(request, response);

        verify(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);
        verify(passwordResetMailer)
            .sendEmail(user, TOKEN);
        verify(requestDispatcher)
            .forward(request, response);
    }

    @Test
    void postWithValidEmailWillGenerateTokenAndSendEmail() throws Exception {

        doReturn(REMOTE_ADDRESS)
            .when(request)
            .getRemoteAddr();
        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");
        doReturn(user.getEmail())
            .when(request)
            .getParameter("user");

        doReturn(Collections.singletonList(user))
            .when(userManager)
            .getUsers(anyInt(), anyInt());

        doReturn(TOKEN)
            .when(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);

        servlet.doPost(request, response);

        verify(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);
        verify(passwordResetMailer)
            .sendEmail(user, TOKEN);
        verify(requestDispatcher)
            .forward(request, response);
    }

    @Test
    void willLookInTheSecondPageOfResults() throws Exception {

        doReturn(REMOTE_ADDRESS)
            .when(request)
            .getRemoteAddr();
        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");
        doReturn(user.getEmail())
            .when(request)
            .getParameter("user");

        doReturn(Collections.singletonList(new User()))
            .doReturn(Collections.singletonList(user))
            .when(userManager)
            .getUsers(anyInt(), anyInt());

        doReturn(TOKEN)
            .when(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);

        servlet.doPost(request, response);

        verify(resetTokenManager)
            .generateToken(user, REMOTE_ADDRESS);
        verify(passwordResetMailer)
            .sendEmail(user, TOKEN);
        verify(requestDispatcher)
            .forward(request, response);
    }

    @Test
    void willNotSendAnythingIfNoEmailMatches() throws Exception {

        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-send-email.jsp");
        doReturn("not-" + user.getEmail())
            .when(request)
            .getParameter("user");

        doReturn(Collections.singletonList(new User()))
            .doReturn(Collections.singletonList(user))
            .doReturn(Collections.emptyList())
            .when(userManager)
            .getUsers(anyInt(), anyInt());

        servlet.doPost(request, response);

        verify(resetTokenManager, never())
            .generateToken(any(), any());
        verify(passwordResetMailer, never())
            .sendEmail(any(), any());
        verify(requestDispatcher)
            .forward(request, response);
    }
}