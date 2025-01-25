package org.jivesoftware.openfire.plugin.passwordreset;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Date;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.util.EmailService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings(
    value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
    justification = "False positive")
class PasswordResetMailerTest {

    private PasswordResetMailer passwordResetMailer;
    @Mock
    private EmailService emailService;

    @BeforeAll
    @SuppressWarnings("deprecation")
    static void beforeAll() {
        Fixtures.reconfigureOpenfireHome();
        XMPPServer.setInstance(Fixtures.mockXmppServer());
    }

    @BeforeEach
    void setUp() {
        Fixtures.clearExistingProperties();
        PasswordResetPlugin.SERVER.setValue("https://localhost:7443/passwordreset");
        passwordResetMailer = new PasswordResetMailer(emailService);
    }

    @Test
    void willSubstitutePlaceholders() {
        final User user
            = new User("test-username", "Test User", "test@domain.com", new Date(), new Date());

        passwordResetMailer
            .sendEmail(user, "test-token");

        verify(emailService)
            .sendMessage("Test User", "test@domain.com",
                "Openfire", "admin@example.com",
                "Openfire password reset",
                "Dear Test User\n"
                    + "\n"
                    + "To reset the password for your test-username Openfire account, simply go to"
                    + " https://localhost:7443/passwordreset/change-password?token=test-token at"
                    + " any time in the next five hours. After this time, you will need to request"
                    + " another reset email is sent to you.",
                null);
    }

    @Test
    void willNotSendEmailsToUsersWithoutAnEmailAddress() {
        final User user
            = new User("test-username", "Test User", null, new Date(), new Date());

        passwordResetMailer
            .sendEmail(user, "test-token");

        verifyNoInteractions(emailService);
    }

    @Test
    void willNotSendEmailsToUsersWithSpecialEmailAddress() {
        final User user
            = new User("test-username", "Test User", "user@example.com", new Date(), new Date());

        passwordResetMailer
            .sendEmail(user, "test-token");

        verifyNoInteractions(emailService);
    }
}