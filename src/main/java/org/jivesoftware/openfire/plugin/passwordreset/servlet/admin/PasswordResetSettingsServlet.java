package org.jivesoftware.openfire.plugin.passwordreset.servlet.admin;

import static java.util.Arrays.asList;
import static org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin.localize;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hazlewood.connor.bottema.emailaddress.EmailAddressValidator;
import org.jivesoftware.admin.FlashMessageTag;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetMailer;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetPlugin;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetTokenManager;
import org.jivesoftware.openfire.plugin.passwordreset.PasswordResetTokenManager.ResetRequest;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserProvider;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.ParamUtils;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.WebManager;

@Slf4j
public class PasswordResetSettingsServlet extends HttpServlet {

    private static final long serialVersionUID = -2522058940676139518L;
    private static UserProvider userProvider;
    private static Supplier<WebManager> webManagerSupplier;
    private static PasswordResetMailer passwordResetMailer;
    private static PasswordResetTokenManager resetTokenManager;

    @Override
    public void init() {
        final PasswordResetPlugin plugin = PasswordResetPlugin.getInstance();
        initStatic(UserManager.getUserProvider(),
            WebManager::new,
            plugin.getPasswordResetMailer(),
            plugin.getResetTokenManager());
    }

    static void initStatic(
        final UserProvider userProvider,
        final Supplier<WebManager> webManagerSupplier,
        final PasswordResetMailer passwordResetMailer,
        final PasswordResetTokenManager resetTokenManager) {
        PasswordResetSettingsServlet.userProvider = userProvider;
        PasswordResetSettingsServlet.webManagerSupplier = webManagerSupplier;
        PasswordResetSettingsServlet.passwordResetMailer = passwordResetMailer;
        PasswordResetSettingsServlet.resetTokenManager = resetTokenManager;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {
        final Dto dto = new Dto();
        request.setAttribute("dto", dto);
        request.getRequestDispatcher("password-reset-settings.jsp").forward(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
        throws IOException, ServletException {
        if (userProvider.isReadOnly()) {
            redirectWithMessage(request, response, FlashMessageTag.ERROR_MESSAGE_KEY,
                "passwordreset.settings.update-read-only");
        } else if (request.getParameter("cancel") != null) {
            redirectWithMessage(request, response, FlashMessageTag.WARNING_MESSAGE_KEY,
                "passwordreset.settings.cancelled");
        } else if (request.getParameter("update") != null) {
            handleUpdate(request, response);
        } else if (request.getParameter("test") != null) {
            handleTest(request, response);
        } else {
            log.error("An unexpected request was received [parameters={}]",
                request.getParameterMap());
            redirectWithMessage(request, response, FlashMessageTag.ERROR_MESSAGE_KEY,
                "passwordreset.settings.unknown-action");
        }
    }

    private void handleTest(final HttpServletRequest request, final HttpServletResponse response)
        throws IOException, ServletException {
        final Dto dto = new Dto(request);
        request.setAttribute("dto", dto);
        if (dto.valid) {
            final User user = createWebManagerForLocalUser(request, response).getUser();
            passwordResetMailer.sendEmail(user, "for-test-use-only");
            request.getSession().setAttribute(
                FlashMessageTag.SUCCESS_MESSAGE_KEY,
                localize("passwordreset.settings.server-good-test"));

        } else {
            request.getSession().setAttribute(
                FlashMessageTag.WARNING_MESSAGE_KEY,
                localize("passwordreset.settings.server-bad-test"));

        }
        request.getRequestDispatcher("password-reset-settings.jsp")
            .forward(request, response);
    }

    private WebManager createWebManagerForLocalUser(
        final HttpServletRequest request,
        final HttpServletResponse response) {
        final HttpSession session = request.getSession();
        final WebManager webManager = webManagerSupplier.get();
        webManager.init(request, response, session, session.getServletContext());
        return webManager;
    }

    private void handleUpdate(final HttpServletRequest request, final HttpServletResponse response)
        throws IOException, ServletException {
        final Dto dto = new Dto(request);
        if (dto.valid) {
            PasswordResetPlugin.ENABLED.setValue(dto.enabled);
            PasswordResetPlugin.SERVER.setValue(dto.server);
            PasswordResetPlugin.SENDER_ADDRESS.setValue(dto.senderAddress);
            PasswordResetPlugin.SUBJECT.setValue(dto.subject);
            PasswordResetPlugin.BODY.setValue(dto.body);
            PasswordResetPlugin.EXPIRY.setValue(dto.getExpiry());
            PasswordResetPlugin.MIN_LENGTH.setValue(Integer.valueOf(dto.getMinLength()));
            PasswordResetPlugin.MAX_LENGTH.setValue(Integer.valueOf(dto.getMaxLength()));
            createWebManagerForLocalUser(request, response)
                .logEvent("Password reset plugin settings updated", dto.toString());
            redirectWithMessage(request, response, FlashMessageTag.SUCCESS_MESSAGE_KEY,
                "passwordreset.settings.updated");
        } else {
            request.setAttribute("dto", dto);
            request.getRequestDispatcher("password-reset-settings.jsp")
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

    @Data
    public static final class Dto {

        private static final int MAX_PROP_LENGTH = 4000;
        private static final List<ChronoUnit> SUPPORTED_UNITS = asList(
            ChronoUnit.MINUTES,
            ChronoUnit.HOURS,
            ChronoUnit.DAYS
        );
        private final boolean notSupported;
        private final boolean enabled;
        private final String server;
        private final String serverError;
        private final String senderName;
        private final String senderNameError;
        private final String senderAddress;
        private final String senderAddressError;
        private final String subject;
        private final String subjectError;
        private final String body;
        private final String bodyError;
        private final String expiryCount;
        private final String expiryPeriod;
        private final String expiryError;
        private final String minLength;
        private final String minLengthError;
        private final String maxLength;
        private final String maxLengthError;
        private final boolean valid;
        private final List<ResetRequest> resetRequests;

        private Dto() {
            this.notSupported = userProvider.isReadOnly();
            this.enabled = PasswordResetPlugin.ENABLED.getValue();
            this.server = PasswordResetPlugin.SERVER.getValue();
            this.serverError = "";
            this.senderName = PasswordResetPlugin.SENDER_NAME.getValue();
            this.senderNameError = "";
            this.senderAddress = PasswordResetPlugin.SENDER_ADDRESS.getValue();
            this.senderAddressError = "";
            this.subject = PasswordResetPlugin.SUBJECT.getValue();
            this.subjectError = "";
            this.body = PasswordResetPlugin.BODY.getValue();
            this.bodyError = "";
            final long expiry = PasswordResetPlugin.EXPIRY.getValue().toMillis();
            if (expiry % JiveConstants.DAY == 0) {
                expiryCount = String.valueOf(expiry / JiveConstants.DAY);
                expiryPeriod = ChronoUnit.DAYS.name();
            } else if (expiry % JiveConstants.HOUR == 0) {
                expiryCount = String.valueOf(expiry / JiveConstants.HOUR);
                expiryPeriod = ChronoUnit.HOURS.name();
            } else {
                expiryCount = String.valueOf(expiry / JiveConstants.MINUTE);
                expiryPeriod = ChronoUnit.MINUTES.name();
            }
            this.expiryError = "";
            this.minLength = String.valueOf(PasswordResetPlugin.MIN_LENGTH.getValue());
            this.minLengthError = "";
            this.maxLength = String.valueOf(PasswordResetPlugin.MAX_LENGTH.getValue());
            this.maxLengthError = "";
            this.valid = allValid();
            resetRequests = resetTokenManager.getResetRequests();
        }

        private boolean allValid() {
            return serverError.isEmpty() && senderNameError.isEmpty()
                && senderAddressError.isEmpty() && subjectError.isEmpty() && bodyError.isEmpty()
                && expiryError.isEmpty() && minLengthError.isEmpty() && maxLengthError.isEmpty();
        }

        private Dto(final HttpServletRequest request) {
            this.notSupported = userProvider.isReadOnly();
            this.enabled = ParamUtils.getBooleanParameter(request, "enabled");
            this.server = ParamUtils.getStringParameter(request, "server", "");
            this.serverError = validateServer();
            this.senderName = ParamUtils.getStringParameter(request, "sender-name", "");
            this.senderNameError = validateSenderName();
            this.senderAddress = ParamUtils.getStringParameter(request, "sender-address", "");
            this.senderAddressError = validateSenderAddress();
            this.subject = ParamUtils.getStringParameter(request, "subject", "");
            this.subjectError = validateSubject();
            this.body = ParamUtils.getStringParameter(request, "body", "");
            this.bodyError = validateBody();
            this.expiryCount = ParamUtils.getStringParameter(request, "expiryCount", "");
            this.expiryPeriod = ParamUtils.getStringParameter(request, "expiryPeriod", "");
            this.expiryError = validateExpiry();
            this.minLength = ParamUtils.getStringParameter(request, "minLength", "");
            this.minLengthError = validateMinLength();
            this.maxLength = ParamUtils.getStringParameter(request, "maxLength", "");
            this.maxLengthError = validateMaxLength();
            this.valid = allValid();
            resetRequests = resetTokenManager.getResetRequests();
        }

        private String validateMaxLength() {
            final int max = StringUtils.parseInteger(this.maxLength).orElse(-1);
            if (max < 0) {
                return localize("passwordreset.settings.max-length-integer");
            }
            if (max > 0
                && max < StringUtils.parseInteger(this.minLength).orElse(Integer.MAX_VALUE)) {
                return localize("passwordreset.settings.max-length-too-short");
            }
            return "";
        }

        private String validateMinLength() {
            if (StringUtils.parseInteger(minLength).orElse(-1) < 1) {
                return localize("passwordreset.settings.min-length-integer");
            }
            return "";
        }

        private String validateExpiry() {
            if (StringUtils.parseInteger(expiryCount).orElse(0) < 1) {
                return localize("passwordreset.settings.expiry-count-integer");
            }
            try {
                final ChronoUnit chronoUnit = ChronoUnit.valueOf(expiryPeriod);
                if (!SUPPORTED_UNITS.contains(chronoUnit)) {
                    return localize("passwordreset.settings.expiry-period-invalid");
                }
            } catch (final IllegalArgumentException ignored) {
                return localize("passwordreset.settings.expiry-period-invalid");
            }
            return "";
        }

        private String validateBody() {
            if (body.isEmpty()) {
                return localize("passwordreset.settings.no-body");
            }
            if (body.length() > MAX_PROP_LENGTH) {
                return localize("passwordreset.settings.body-too-long");
            }
            return "";
        }

        private String validateSubject() {
            if (subject.isEmpty()) {
                return localize("passwordreset.settings.no-subject");
            }
            if (subject.length() > MAX_PROP_LENGTH) {
                return localize("passwordreset.settings.subject-too-long");
            }
            return "";
        }

        private String validateSenderAddress() {
            if (senderAddress.isEmpty()) {
                return localize("passwordreset.settings.no-sender-address");
            }
            if (senderAddress.length() > MAX_PROP_LENGTH) {
                return localize("passwordreset.settings.sender-address-too-long");
            }
            if (!EmailAddressValidator.isValidStrict(senderAddress)) {
                return localize("passwordreset.settings.sender-address-invalid");
            }
            return "";
        }

        private String validateSenderName() {
            if (senderName.isEmpty()) {
                return localize("passwordreset.settings.no-sender-name");
            }
            if (senderName.length() > MAX_PROP_LENGTH) {
                return localize("passwordreset.settings.sender-name-too-long");
            }
            return "";
        }

        private String validateServer() {
            if (server.isEmpty()) {
                return localize("passwordreset.settings.no-server");
            }
            if (server.length() > MAX_PROP_LENGTH) {
                return localize("passwordreset.settings.server-too-long");
            }
            if (!server.startsWith("http://") && !server.startsWith("https://")) {
                return localize("passwordreset.settings.server-no-http");
            }
            return "";
        }

        private Duration getExpiry() {
            return Duration.of(Long.parseLong(expiryCount), ChronoUnit.valueOf(expiryPeriod));
        }
    }
}
