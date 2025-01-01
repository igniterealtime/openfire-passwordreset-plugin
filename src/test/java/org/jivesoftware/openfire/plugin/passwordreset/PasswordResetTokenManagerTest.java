package org.jivesoftware.openfire.plugin.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenManagerTest {

    private PasswordResetTokenManager resetTokenManager;
    private User user;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;

    @BeforeAll
    @SuppressWarnings("deprecation")
    static void beforeAll() throws Exception {
        Fixtures.reconfigureOpenfireHome();
        XMPPServer.setInstance(Fixtures.mockXmppServer());
    }

    @BeforeEach
    @SuppressFBWarnings(
        value = {
            "OBL_UNSATISFIED_OBLIGATION",
            "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
            "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
            "ODR_OPEN_DATABASE_RESOURCE",
            "RV_RETURN_VALUE_IGNORED"},
        justification = "All false positives!")
    void setUp() throws Exception {
        Fixtures.reconfigureOpenfireHome();

        user = new User("test-username", "Test User", "test@example.com", new Date(), new Date());

        doReturn(preparedStatement)
            .when(connection)
            .prepareStatement(any());

        resetTokenManager = new PasswordResetTokenManager(() -> connection, null);
    }

    @Test
    @SuppressFBWarnings(
        value = {"UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR"},
        justification = "False positive")
    void willGenerateRandomTokenForUser() throws Exception {
        final String token1 = resetTokenManager.generateToken(user, "localhost");
        final String token2 = resetTokenManager.generateToken(user, "localhost");

        assertThat(token1)
            .hasSize(32)
            .isNotEqualTo(token2);
        assertThat(token2)
            .hasSize(32)
            .isNotEqualTo(token1);
        verify(preparedStatement, times(2)).execute();
    }
}