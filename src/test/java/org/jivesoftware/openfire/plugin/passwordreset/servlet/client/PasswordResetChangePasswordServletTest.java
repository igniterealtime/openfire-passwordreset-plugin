package org.jivesoftware.openfire.plugin.passwordreset.servlet.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.Optional;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.SneakyThrows;
import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.plugin.passwordreset.Fixtures;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetTokenManager;
import org.jivesoftware.openfire.plugin.passwordreset.servlet.client.PasswordResetChangePasswordServlet.Form;
import org.jivesoftware.openfire.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetChangePasswordServletTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String USER_ID = "test-user-id";
    private static final String REQUEST_URI = "/path/to/file";

    private PasswordResetChangePasswordServlet servlet;
    @Mock
    private PasswordResetTokenManager tokenManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RequestDispatcher requestDispatcher;
    @Mock
    private User user;
    @Mock
    private HttpSession session;

    @BeforeAll
    @SuppressWarnings("deprecation")
    static void beforeAll() {
        Fixtures.reconfigureOpenfireHome();
        XMPPServer.setInstance(Fixtures.mockXmppServer());
    }

    @BeforeEach
    void setUp() {
        Fixtures.clearExistingProperties();
        servlet = new PasswordResetChangePasswordServlet();
        PasswordResetChangePasswordServlet.initStatic(tokenManager);
    }

    @SneakyThrows
    @Test
    void getWithValidTokenWillShowThePasswordChangeForm() {

        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-change-password.jsp");
        doReturn(VALID_TOKEN)
            .when(request)
            .getParameter("token");
        doReturn(Optional.of(user))
            .when(tokenManager)
            .getUser(VALID_TOKEN);

        servlet.doGet(request, response);

        verify(request)
            .setAttribute(eq("form"), any(PasswordResetChangePasswordServlet.Form.class));
        verify(requestDispatcher).forward(request, response);
    }

    @SneakyThrows
    @Test
    void getWithBadTokenWillForwardToBadTokenPage() {

        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-bad-token.jsp");

        doReturn("bad token")
            .when(request)
            .getParameter("token");

        servlet.doGet(request, response);

        verify(requestDispatcher).forward(request, response);
    }

    @SneakyThrows
    @Test
    void getThatCausesSqlExceptionWillForwardToTheErrorPage() {

        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-error.jsp");

        doThrow(new SQLException())
            .when(tokenManager)
            .getUser(anyString());

        servlet.doGet(request, response);

        verify(requestDispatcher).forward(request, response);
    }

    @SneakyThrows
    @Test
    void postWithCancelWillRedirect() {

        doReturn("Cancel")
            .when(request)
            .getParameter("cancel");
        doReturn(REQUEST_URI)
            .when(request)
            .getRequestURI();
        doReturn(session)
            .when(request)
            .getSession();

        servlet.doPost(request, response);

        verify(session).setAttribute(eq(FlashMessageTag.WARNING_MESSAGE_KEY), anyString());
        verify(response).sendRedirect(REQUEST_URI);
    }

    @SneakyThrows
    @Test
    void postWithNothingWillRedirect() {

        doReturn(REQUEST_URI)
            .when(request)
            .getRequestURI();
        doReturn(session)
            .when(request)
            .getSession();

        servlet.doPost(request, response);

        verify(session).setAttribute(eq(FlashMessageTag.ERROR_MESSAGE_KEY), anyString());
        verify(response).sendRedirect(REQUEST_URI);
    }

    @SneakyThrows
    @Test
    void validFormWillUpdatePasswordAndRedirect() {

        givenValidFormSubmission();
        doReturn(session)
            .when(request)
            .getSession();

        doReturn(Optional.of(user))
            .when(tokenManager)
            .getUser(VALID_TOKEN);

        doReturn(USER_ID)
            .when(user)
            .getUsername();

        servlet.doPost(request, response);

        verify(user).setPassword("new-password");
        verify(tokenManager).deleteTokens(user);
        verify(session).setAttribute(eq(FlashMessageTag.SUCCESS_MESSAGE_KEY), anyString());
        verify(requestDispatcher).forward(request, response);
    }

    private void givenValidFormSubmission() {
        doReturn(null)
            .when(request)
            .getParameter("cancel");
        doReturn("Update")
            .when(request)
            .getParameter("update");
        lenient().doReturn(VALID_TOKEN)
            .when(request)
            .getParameter("token");
        doReturn(USER_ID)
            .when(request)
            .getParameter("userId");
        lenient().doReturn("new-password")
            .when(request)
            .getParameter("newPassword");
        doReturn("new-password")
            .when(request)
            .getParameter("newPasswordConfirmation");
        lenient().doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-change-password.jsp");
    }

    @SneakyThrows
    @Test
    void newPasswordMustNotBeTooShort() {

        givenValidFormSubmission();
        doReturn("short")
            .when(request)
            .getParameter("newPassword");

        servlet.doPost(request, response);

        final Form form = verifyFormError();
        assertThat(form.getNewPasswordError())
            .isEqualTo("???passwordreset.change-password.new-password-too-short???");
    }

    @SneakyThrows
    Form verifyFormError() {
        verify(user, never()).setPassword(anyString());
        verify(tokenManager, never()).deleteTokens(any());
        final ArgumentCaptor<PasswordResetChangePasswordServlet.Form> argumentCaptor
            = ArgumentCaptor.forClass(PasswordResetChangePasswordServlet.Form.class);
        verify(request).setAttribute(eq("form"), argumentCaptor.capture());
        final Form form = argumentCaptor.getValue();
        assertThat(form.isValid()).isEqualTo(false);
        verify(requestDispatcher).forward(request, response);
        return form;
    }

    @SneakyThrows
    @Test
    void newPasswordMustNotBeTooLong() {

        givenValidFormSubmission();
        PasswordResetPlugin.MAX_LENGTH.setValue(8);
        doReturn("newPasswordIsVeryLong")
            .when(request)
            .getParameter("newPassword");

        servlet.doPost(request, response);

        final Form form = verifyFormError();
        assertThat(form.getNewPasswordError())
            .isEqualTo("???passwordreset.change-password.new-password-too-long???");
    }

    @SneakyThrows
    @Test
    void passwordConfirmationMustMatch() {

        givenValidFormSubmission();
        doReturn("newPasswordIsNotSameAsConfirmationPassword")
            .when(request)
            .getParameter("newPassword");

        servlet.doPost(request, response);

        final Form form = verifyFormError();
        assertThat(form.getNewPasswordConfirmationError())
            .isEqualTo("???passwordreset.change-password.password-confirmation-no-match???");
    }

    @SneakyThrows
    @Test
    void badTokenWillRedirectToBadTokenPage() {

        givenValidFormSubmission();
        doReturn(Optional.empty())
            .when(tokenManager)
            .getUser("bad token");
        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-bad-token.jsp");

        doReturn("bad token")
            .when(request)
            .getParameter("token");

        servlet.doPost(request, response);

        verify(requestDispatcher).forward(request, response);

    }

    @SneakyThrows
    @Test
    void differentUserIdWillRedirectToBadTokenPage() {

        givenValidFormSubmission();
        doReturn(Optional.of(user))
            .when(tokenManager)
            .getUser(VALID_TOKEN);
        doReturn("not-" + USER_ID)
            .when(user)
            .getUsername();
        doReturn(requestDispatcher)
            .when(request)
            .getRequestDispatcher("password-reset-bad-token.jsp");

        servlet.doPost(request, response);

        verify(requestDispatcher).forward(request, response);

    }
}