<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core_1_1" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="passwordreset_i18n"/>
<%@ taglib uri="admin" prefix="admin" %>
<jsp:useBean scope="request"
             id="form"
             type="org.jivesoftware.openfire.plugin.passwordreset.servlet.client.PasswordResetChangePasswordServlet.Form"/>
<!DOCTYPE html>
<html>
<head>
    <title><fmt:message key="passwordreset.change-password.title"/></title>
</head>

<body>

<h1><fmt:message key="passwordreset.change-password.header"/></h1>

<p><fmt:message key="passwordreset.change-password.description"/></p>

<admin:FlashMessage/>

<c:if test='${!form.valid}'>
    <div class="warning">
        <fmt:message key="passwordreset.change-password.not-valid"/>
    </div>
</c:if>

<form method="post">
    <!--
    This looks a bit hinky, but allows Firefox to identify the username whose
    password is being saved. Normally an input field with type="hidden" would be used.
    -->
    <%--suppress HtmlFormInputWithoutLabel --%>
    <input type="text" name="userId" autocomplete="username" style="display:none"
           value="<c:out value="${form.userId}"/>">
    <input type="hidden" name="token" value="<c:out value="${form.token}"/>">
    <div class="jive-contentBox">
        <table>
            <tr>
                <td>
                    <label for="newPassword">
                        <fmt:message key="passwordreset.change-password.password-label"/>
                    </label>
                </td>
                <td>
                    <input id="newPassword" name="newPassword" type="password"
                           autocomplete="new-password"
                           style="width: 20em;"
                           value="<c:out value="${form.newPassword}"/>"
                    >
                </td>
                <td>
                    <c:if test="${!empty form.newPasswordError}">
                        <div class="warning">
                            <c:out value="${form.newPasswordError}"/>
                        </div>
                    </c:if>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="newPasswordConfirmation">
                        <fmt:message
                                key="passwordreset.change-password.password-confirmation-label"/>
                    </label>
                </td>
                <td>
                    <input id="newPasswordConfirmation" name="newPasswordConfirmation"
                           type="password" autocomplete="new-password"
                           style="width: 20em;"
                           value="<c:out value="${form.newPasswordConfirmation}"/>"
                    >
                </td>
                <td>
                    <c:if test="${!empty form.newPasswordConfirmationError}">
                        <div class="warning">
                            <c:out value="${form.newPasswordConfirmationError}"/>
                        </div>
                    </c:if>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    <input type="submit" name="update"
                           value="<fmt:message key="passwordreset.change-password.change-password"/>"/>
                    <input type="submit" name="cancel"
                           value="<fmt:message key="passwordreset.change-password.cancel-change"/>"/>
                </td>
            </tr>
        </table>
    </div>
</form>

</body>
</html>