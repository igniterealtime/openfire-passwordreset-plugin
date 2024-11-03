package org.jivesoftware.openfire.plugin.passwordreset.servlet.client;

import static org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin.localize;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetMailer;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetTokenManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.user.UserProvider;
import org.jivesoftware.util.ParamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

public class PasswordResetSendEmailServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetSendEmailServlet.class);

    private static final long serialVersionUID = -7605965376783076351L;
    private static final int BATCH_SIZE = 50;
    private static XMPPServer xmppServer;
    private static UserProvider userProvider;
    private static UserManager userManager;
    private static PasswordResetTokenManager resetTokenManager;
    private static PasswordResetMailer passwordResetMailer;

    @Override
    public void init() {
        final PasswordResetPlugin plugin = PasswordResetPlugin.getInstance();
        initStatic(XMPPServer.getInstance(),
            UserManager.getUserProvider(),
            UserManager.getInstance(),
            plugin.getResetTokenManager(),
            plugin.getPasswordResetMailer());
    }

    static void initStatic(
        final XMPPServer xmppServer,
        final UserProvider userProvider,
        final UserManager userManager,
        final PasswordResetTokenManager resetTokenManager,
        final PasswordResetMailer passwordResetMailer) {
        PasswordResetSendEmailServlet.xmppServer = xmppServer;
        PasswordResetSendEmailServlet.userProvider = userProvider;
        PasswordResetSendEmailServlet.userManager = userManager;
        PasswordResetSendEmailServlet.resetTokenManager = resetTokenManager;
        PasswordResetSendEmailServlet.passwordResetMailer = passwordResetMailer;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {
        final Form form = new Form();
        request.setAttribute("form", form);
        request.getRequestDispatcher("password-reset-send-email.jsp").forward(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

        final Form form = new Form(request);
        if (form.valid) {
            handleSend(form, request, response);
        } else {
            request.setAttribute("form", form);
            request.getRequestDispatcher("password-reset-send-email.jsp")
                .forward(request, response);
        }
    }

    private void handleSend(
        final Form form,
        final HttpServletRequest request,
        final HttpServletResponse response) throws IOException, ServletException {

        final Optional<User> optionalUser = getUserFromIdentifier(form.user);

        // Dont warn the user if they've not specified the correct value - we don't want people
        // using this page to guess valid identifiers
        try {
            if (optionalUser.isPresent()) {
                final User user = optionalUser.get();
                final String token;
                token = resetTokenManager.generateToken(user, request.getRemoteAddr());
                passwordResetMailer.sendEmail(user, token);
            }
            form.emailSent = true;
        } catch (final SQLException e) {
            log.error("Unable to persist token", e);
            request.getSession().setAttribute(
                FlashMessageTag.ERROR_MESSAGE_KEY,
                localize("passwordreset.send-email.failed"));
        }
        request.setAttribute("form", form);
        request.getRequestDispatcher("password-reset-send-email.jsp")
            .forward(request, response);
    }

    /**
     * There are three ways a user can specify their id.
     * <ol>
     * <ul>The node part of their jid - {@code admin}</ul>
     * <ul>The bare jid - {@code admin@xmppdomain.example.com}</ul>
     * <ul>Their actual email {@code admin@example.com}</ul>
     * </ol>
     *
     * @param userIdentifier one of the above forms, hopefully
     * @return The User if one can be found.
     */
    private Optional<User> getUserFromIdentifier(final String userIdentifier) {
        if (userIdentifier.contains("@")) {
            // We have either an email /or/ a JID
            final JID jid = new JID(userIdentifier);
            if (xmppServer.isLocal(jid)) {
                final Optional<User> userFromJid = getUserFromUserId(jid.getNode());
                if (userFromJid.isPresent()) {
                    return userFromJid;
                }
            }

            // If we got this far, it's not a JID, so find the user with that email
            return getUserFromEmail(userIdentifier);
        } else {
            // We must have the user part of a JID
            return getUserFromUserId(userIdentifier);
        }
    }

    private Optional<User> getUserFromEmail(final String userEmail) {
        // Fetch users in batches to try and avoid flooding the system
        int currentIndex = 0;
        Collection<User> users;
        while (!(users = userManager.getUsers(currentIndex, BATCH_SIZE)).isEmpty()) {
            for (final User user : users) {
                if (userEmail.equalsIgnoreCase(user.getEmail())) {
                    return Optional.of(user);
                }
            }
            currentIndex += BATCH_SIZE;
        }
        return Optional.empty();
    }

    private Optional<User> getUserFromUserId(final String userId) {
        try {
            return Optional.of(userManager.getUser(userId));
        } catch (final UserNotFoundException ignored) {
            return Optional.empty();
        }
    }

    public static class Form {

        private final boolean enabled;
        private final String user;
        private final String userError;
        private boolean emailSent = false;
        private final boolean valid;

        private Form() {
            this.enabled = PasswordResetPlugin.ENABLED.getValue() && !userProvider.isReadOnly();
            this.user = "";
            this.userError = "";
            this.valid = allValid();
        }

        private Form(final HttpServletRequest request) {
            this.enabled = PasswordResetPlugin.ENABLED.getValue() && !userProvider.isReadOnly();
            this.user = ParamUtils.getStringParameter(request, "user", "");
            this.userError = validateUser();
            this.valid = allValid();
        }

        private boolean allValid() {
            return enabled && userError.isEmpty();
        }


        private String validateUser() {
            if (user.isEmpty()) {
                return "passwordreset.send-email.no-user";
            }
            return "";
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public String getUser() {
            return this.user;
        }

        public String getUserError() {
            return this.userError;
        }

        public boolean isEmailSent() {
            return this.emailSent;
        }

        public boolean isValid() {
            return this.valid;
        }

        public void setEmailSent(boolean emailSent) {
            this.emailSent = emailSent;
        }

    }

}
