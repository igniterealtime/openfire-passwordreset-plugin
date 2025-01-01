<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core_1_1" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="passwordreset_i18n"/>
<%@ taglib uri="admin" prefix="admin" %>
<jsp:useBean scope="request"
             id="form"
             type="org.jivesoftware.openfire.plugin.passwordreset.servlet.client.PasswordResetSendEmailServlet.Form"/>
<!DOCTYPE html>
<html>
<head>
    <title><fmt:message key="passwordreset.send-email.title"/></title>
</head>

<body>

<c:choose>
    <c:when test="${!form.enabled}">
        <fmt:message key="passwordreset.send-email.disabled"/>
    </c:when>
    <c:when test="${form.emailSent}">
        <fmt:message key="passwordreset.send-email.sent"/>
    </c:when>
    <c:otherwise>
        <p><fmt:message key="passwordreset.send-email.description"/></p>

        <admin:FlashMessage/>

        <c:if test='${!form.valid}'>
            <div class="warning">
                <fmt:message key="passwordreset.send-email.not-valid"/>
            </div>
        </c:if>

        <form method="post">
            <input id="user" name="user" type="text"
                   style="width: 20em"
                   value="<c:out value="${form.user}"/>"
            >
            <label for="user">
                <fmt:message key="passwordreset.send-email.user-label"/>
            </label>
            <c:if test='${!empty form.userError}'>
                <div class="warning">
                    <fmt:message key="${form.userError}"/>
                </div>
            </c:if>
            <br/>
            <input type="submit" name="send"
                   value="<fmt:message key="passwordreset.send-email.send"/>">
        </form>
    </c:otherwise>
</c:choose>

</body>
</html>