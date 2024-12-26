package org.jivesoftware.openfire.plugin.passwordreset.servlet.client;

import static org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin.localize;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetTokenManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.ParamUtils;

@Slf4j
public class PasswordResetChangePasswordServlet extends HttpServlet {

    private static final long serialVersionUID = -5668541154412417961L;
    private static PasswordResetTokenManager resetTokenManager;

    @Override
    public void init() {
        final PasswordResetPlugin plugin = PasswordResetPlugin.getInstance();
        initStatic(plugin.getResetTokenManager());
    }

    static void initStatic(
        final PasswordResetTokenManager resetTokenManager) {
        PasswordResetChangePasswordServlet.resetTokenManager = resetTokenManager;
    }

    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {
        final String token = ParamUtils.getStringParameter(request, "token", "");
        try {
            final Optional<User> user = resetTokenManager.getUser(token);
            if (user.isPresent()) {
                final Form form = new Form(user.get(), token);
                request.setAttribute("form", form);
                request.getRequestDispatcher("password-reset-change-password.jsp")
                    .forward(request, response);
            } else {
                request.getRequestDispatcher("password-reset-bad-token.jsp")
                    .forward(request, response);
            }
        } catch (final SQLException e) {
            log.error("Unexpected error retrieving user details", e);
            request.getRequestDispatcher("password-reset-error.jsp")
                .forward(request, response);
        }
    }

    private void redirectWithMessage(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final String messageLevel,
        final String messageKey) throws IOException {
        request.getSession().setAttribute(messageLevel, localize(messageKey));
        response.sendRedirect(request.getRequestURI());
    }

    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

        if (request.getParameter("cancel") != null) {
            redirectWithMessage(request, response, FlashMessageTag.WARNING_MESSAGE_KEY,
                "passwordreset.change-password.cancelled");
        } else if (request.getParameter("update") != null) {
            handleUpdate(request, response);
        } else {
            redirectWithMessage(request, response, FlashMessageTag.ERROR_MESSAGE_KEY,
                "passwordreset.change-password.unknown-action");
        }

    }

    private void handleUpdate(
        final HttpServletRequest request,
        final HttpServletResponse response) throws ServletException, IOException {

        final Form form = new Form(request);
        request.setAttribute("form", form);
        if (form.valid) {
            try {
                final Optional<User> optionalUser = resetTokenManager.getUser(form.token);
                if (optionalUser.isEmpty()) {
                    request.getRequestDispatcher("password-reset-bad-token.jsp")
                        .forward(request, response);
                    return;
                }
                final User user = optionalUser.get();
                if (!user.getUsername().equals(form.userId)) {
                    request.getRequestDispatcher("password-reset-bad-token.jsp")
                        .forward(request, response);
                    return;
                }
                user.setPassword(form.newPassword);
                resetTokenManager.deleteTokens(user);
                request.getSession().setAttribute(
                    FlashMessageTag.SUCCESS_MESSAGE_KEY,
                    localize("passwordreset.change-password.password-changed"));
            } catch (final SQLException e) {
                log.error("Unable to retrieve user from token", e);
                request.getSession().setAttribute(
                    FlashMessageTag.ERROR_MESSAGE_KEY,
                    localize("passwordreset.change-password.failed"));
            }
        }
        request.getRequestDispatcher("password-reset-change-password.jsp")
            .forward(request, response);
    }

    @Data
    public static class Form {

        private final String userId;
        private final String token;
        @ToString.Exclude
        private final String newPassword;
        @ToString.Include(name = "newPassword")
        private static final String maskedNewPassword = "********";
        private final String newPasswordError;
        @ToString.Exclude
        private final String newPasswordConfirmation;
        @ToString.Include(name = "newPasswordConfirmation")
        private static final String maskedNewPasswordConfirmation = "********";
        private final String newPasswordConfirmationError;
        private final boolean valid;

        private Form(final User user, final String token) {
            this.userId = user.getUsername();
            this.token = token;
            this.newPassword = "";
            this.newPasswordError = "";
            this.newPasswordConfirmation = "";
            this.newPasswordConfirmationError = "";
            this.valid = true;
        }

        private Form(final HttpServletRequest request) {
            this.userId = ParamUtils.getStringParameter(request, "userId", "");
            this.token = ParamUtils.getStringParameter(request, "token", "");
            this.newPassword = ParamUtils.getStringParameter(request, "newPassword", "");
            this.newPasswordError = validateNewPassword();
            this.newPasswordConfirmation =
                ParamUtils.getStringParameter(request, "newPasswordConfirmation", "");
            this.newPasswordConfirmationError = validateNewPasswordConfirmation();
            this.valid = newPasswordError.isEmpty() && newPasswordConfirmationError.isEmpty();
        }

        private String validateNewPasswordConfirmation() {
            if (!this.newPasswordConfirmation.equals(this.newPassword)) {
                return localize("passwordreset.change-password.password-confirmation-no-match");
            }
            return "";
        }

        private String validateNewPassword() {
            final Integer minLength = PasswordResetPlugin.MIN_LENGTH.getValue();
            if (this.newPassword.length() < minLength) {
                return localize("passwordreset.change-password.new-password-too-short", minLength);
            }
            final Integer maxLength = PasswordResetPlugin.MAX_LENGTH.getValue();
            if (maxLength > 0 && this.newPassword.length() > maxLength) {
                return localize("passwordreset.change-password.new-password-too-long", maxLength);
            }
            return "";
        }
    }
}
