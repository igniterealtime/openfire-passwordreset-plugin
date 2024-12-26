package org.jivesoftware.openfire.plugin.passwordreset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.StringUtils;

@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public class PasswordResetTokenManager {

    private static final int TOKEN_LENGTH = 32;
    private static final String INSERT_SQL =
        "INSERT INTO ofPasswordResetToken (token, userId, sourceAddress, expires)"
            + " VALUES (?,?,?,?)";
    private static final String PURGE_EXPIRED_SQL =
        "DELETE FROM ofPasswordResetToken WHERE expires < NOW()";
    private static final String FIND_USER_SQL =
        "SELECT userId FROM ofPasswordResetToken WHERE token = ?";
    private static final String DELETE_TOKENS_FOR_USER =
        "DELETE FROM ofPasswordResetToken WHERE userId = ?";
    private static final String RESET_REQUESTS_SQL =
        "SELECT userId, sourceAddress, expires FROM ofPasswordResetToken ORDER by userId, expires";
    private final SqlExceptionSupplier<Connection> connectionSupplier;
    private final UserManager userManager;

    /**
     * Can't use a {@link java.util.function.Supplier} because of the exception.
     */
    public interface SqlExceptionSupplier<T> {

        T get() throws SQLException;
    }

    public PasswordResetTokenManager(
        final SqlExceptionSupplier<Connection> connectionSupplier,
        final UserManager userManager) {
        this.connectionSupplier = connectionSupplier;
        this.userManager = userManager;
    }

    /**
     * Generates a random token for the user, and persists in the database.
     *
     * @param user The user to create the token for
     * @param sourceAddress The address from which the request was made
     * @return the random token
     * @throws SQLException if anything untoward happens
     */
    public String generateToken(final User user, final String sourceAddress) throws SQLException {
        purgeOldTokens();
        final String token = StringUtils.randomString(TOKEN_LENGTH);
        try (final Connection connection = connectionSupplier.get();
            final PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, token);
            statement.setString(2, user.getUsername());
            statement.setString(3, sourceAddress);
            statement.setTimestamp(4,
                new Timestamp(Instant.now().plus(PasswordResetPlugin.EXPIRY.getValue())
                    .toEpochMilli()));
            statement.execute();
        }
        return token;
    }

    private void purgeOldTokens() throws SQLException {
        try (final Connection connection = connectionSupplier.get();
            final PreparedStatement statement = connection.prepareStatement(PURGE_EXPIRED_SQL)) {
            final int updateCount = statement.executeUpdate();
            log.debug("Purged {} records", updateCount);
        }
    }

    /**
     * Finds the user for the specified token.
     *
     * @param token the token to perform the search on
     * @return the user, if any
     * @throws SQLException if something untoward happens
     */
    public Optional<User> getUser(final String token) throws SQLException {
        purgeOldTokens();
        try (final Connection connection = connectionSupplier.get();
            final PreparedStatement statement = connection.prepareStatement(FIND_USER_SQL)) {
            statement.setString(1, token);
            try (final ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    final String userId = resultSet.getString("userId");
                    try {
                        return Optional.of(userManager.getUser(userId));
                    } catch (final UserNotFoundException ignored) {
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    /**
     * Deletes all existing tokens for a user.
     *
     * @param user the user whose tokens should be deleted.
     * @throws SQLException if something untoward happens
     */
    public void deleteTokens(final User user) throws SQLException {
        try (final Connection connection = connectionSupplier.get();
            final PreparedStatement statement
                = connection.prepareStatement(DELETE_TOKENS_FOR_USER)) {
            statement.setString(1, user.getUsername());
            statement.execute();
        }
    }

    /**
     * Returns the current list of reset requests - excluding the all important token.
     *
     * @return the list of reset requests.
     */
    public List<ResetRequest> getResetRequests() {
        try {
            purgeOldTokens();
            try (final Connection connection = connectionSupplier.get();
                final PreparedStatement statement = connection.prepareStatement(RESET_REQUESTS_SQL);
                final ResultSet resultSet = statement.executeQuery()) {

                final List<ResetRequest> resetRequests = new ArrayList<>();
                while (resultSet.next()) {

                    final ResetRequest resetRequest = new ResetRequest(
                        resultSet.getString("userId"),
                        resultSet.getString("sourceAddress"),
                        resultSet.getTimestamp("expires"));
                    resetRequests.add(resetRequest);
                }
                return resetRequests;
            }
        } catch (final SQLException e) {
            log.error("Unexpected exception retrieving outstanding requests", e);
            return Collections.emptyList();
        }
    }

    @Data
    public static class ResetRequest {
        public final String userId;
        public final String sourceAddress;
        public final Date expires;
    }
}
