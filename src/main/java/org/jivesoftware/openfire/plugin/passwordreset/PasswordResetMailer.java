package org.jivesoftware.openfire.plugin.passwordreset;

import com.google.common.base.Strings;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.EmailService;

public class PasswordResetMailer {

    // Avoid sending emails to domains that are "special"
    // https://en.wikipedia.org/wiki/Example.com
    // https://en.wikipedia.org/wiki/.local
    // https://en.wikipedia.org/wiki/Top-level_domain#Reserved_domains
    private static final Set<String> IGNORED_DOMAINS = new HashSet<>(
        Arrays.asList(
            "example.com",
            "example.net",
            "example.org",
            "example.edu",
            "local",
            "example",
            "invalid",
            "localhost",
            "test"
        ));

    private final EmailService emailService;

    public PasswordResetMailer(final EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Sends a password reset email. N
     *
     * @param user  the user to whom the reset should be sent.
     * @param token the token used to identify the request
     */
    public void sendEmail(final User user, final String token) {

        final String email = Strings.nullToEmpty(user.getEmail());
        // Ignore empty email address
        if (email.isEmpty()) {
            return;
        }

        // Ignore email addresses from (English) special domains
        final String lowerCaseEmail = email.toLowerCase(Locale.ENGLISH);
        for (final String ignoredDomain : IGNORED_DOMAINS) {
            if (lowerCaseEmail.endsWith(ignoredDomain)) {
                return;
            }
        }

        final URI uri = URI.create(PasswordResetPlugin.SERVER.getValue()
            + "/change-password?token=" + token);
        final String subject = substitute(PasswordResetPlugin.SUBJECT.getValue(), user, uri);
        final String body = substitute(PasswordResetPlugin.BODY.getValue(), user, uri);

        emailService.sendMessage(user.getName(), email,
            PasswordResetPlugin.SENDER_NAME.getValue(),
            PasswordResetPlugin.SENDER_ADDRESS.getValue(),
            subject, body, null);
    }

    private String substitute(
        final String stringToSubstitute, final User user, final URI uri) {
        return stringToSubstitute.replace("${url}", uri.toASCIIString())
            .replace("${userId}", user.getUsername())
            .replace("${userName}", user.getName())
            .replace("${userEmail}", user.getEmail());
    }

}
