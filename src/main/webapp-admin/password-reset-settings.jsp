<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core_1_1" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="admin" prefix="admin" %>
<jsp:useBean scope="request"
             id="dto"
             type="org.jivesoftware.openfire.plugin.passwordreset.servlet.admin.PasswordResetSettingsServlet.Dto"/>
<!DOCTYPE html>
<html>
<head>
    <title><fmt:message key="passwordreset.settings.title"/></title>
    <meta name="pageID" content="password-reset-conf"/>
</head>

<body>

<admin:FlashMessage/>

<fmt:message key="passwordreset.settings.description"/>

<c:if test='${dto.notSupported}'>
    <div class="warning">
        <fmt:message key="passwordreset.settings.read-only"/>
    </div>
</c:if>

<c:if test='${!dto.valid}'>
    <div class="warning">
        <fmt:message key="passwordreset.settings.not-valid"/>
    </div>
</c:if>

<form method="post">
    <div class="jive-contentBoxHeader"><fmt:message key="passwordreset.settings.form-title"/></div>
    <div class="jive-contentBox">
        <table>
            <tr style="vertical-align:top">
                <td style="width: 1%">
                    <input id="enabledTrue"
                           type="radio"
                           name="enabled"
                           value="true"
                           <c:if test='${dto.enabled}'>checked="checked"</c:if>
                           <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                    >
                </td>
                <td>
                    <label for="enabledTrue">
                        <fmt:message key="passwordreset.settings.enabled-label"/>
                    </label>
                    - <fmt:message key="passwordreset.settings.enabled-description"/>
                </td>
            </tr>
            <tr style="vertical-align:top">
                <td style="width: 1%">
                    <input id="enabledFalse"
                           type="radio"
                           name="enabled"
                           value="false"
                           <c:if test='${!dto.enabled}'>checked="checked"</c:if>
                           <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                    >
                </td>
                <td>
                    <label for="enabledFalse">
                        <fmt:message key="passwordreset.settings.disabled-label"/>
                    </label>
                    - <fmt:message key="passwordreset.settings.disabled-description"/>
                </td>
            </tr>
            <tr>
                <td></td>
                <td>
                    <table>
                        <tr>
                            <td>
                                <label for="server">
                                    <fmt:message key="passwordreset.settings.server-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="server" name="server" type="text"
                                       style="width: 50em"
                                       value="<c:out value="${dto.server}"/>"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                >
                            </td>
                            <td>
                                <fmt:message key="passwordreset.settings.server-description"/>
                            </td>
                        </tr>
                        <c:if test='${!empty dto.serverError}'>
                            <tr>
                                <td colspan="3">
                                    <div class="warning">
                                        <fmt:message key="${dto.serverError}"/>
                                    </div>
                                </td>
                            </tr>
                        </c:if>
                        <tr>
                            <td>
                                <label for="sender-name">
                                    <fmt:message key="passwordreset.settings.sender-name-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="sender-name" name="sender-name" type="text"
                                       style="width: 50em"
                                       value="<c:out value="${dto.senderName}"/>"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                >
                            </td>
                            <td>
                                <fmt:message key="passwordreset.settings.sender-name-description"/>
                            </td>
                        </tr>
                        <c:if test='${!empty dto.senderNameError}'>
                            <tr>
                                <td colspan="3">
                                    <div class="warning">
                                        <c:out value="${dto.senderNameError}"/>
                                    </div>
                                </td>
                            </tr>
                        </c:if>
                        <tr>
                            <td>
                                <label for="sender-address">
                                    <fmt:message key="passwordreset.settings.sender-address-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="sender-address" name="sender-address" type="text"
                                       style="width: 50em"
                                       value="<c:out value="${dto.senderAddress}"/>"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                >
                            </td>
                            <td>
                                <fmt:message
                                        key="passwordreset.settings.sender-address-description"/>
                            </td>
                        </tr>
                        <c:if test='${!empty dto.senderAddressError}'>
                            <tr>
                                <td colspan="3">
                                    <div class="warning">
                                        <c:out value="${dto.senderAddressError}"/>
                                    </div>
                                </td>
                            </tr>
                        </c:if>
                        <tr>
                            <td>
                                <label for="subject">
                                    <fmt:message key="passwordreset.settings.subject-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="subject" name="subject" type="text"
                                       style="width: 50em"
                                       value="<c:out value="${dto.subject}"/>"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                >
                            </td>
                            <td>
                                <fmt:message key="passwordreset.settings.subject-description"/>
                                <c:if test='${!empty dto.subjectError}'>
                                    <div class="warning">
                                        <c:out value="${dto.subjectError}"/>
                                    </div>
                                </c:if>
                            </td>
                        </tr>
                        <tr>
                            <td style="vertical-align: top">
                                <label for="body">
                                    <fmt:message key="passwordreset.settings.body-label"/>
                                </label>
                            </td>
                            <td>
                                <textarea id="body" name="body" wrap="hard"
                                          style="width: 41em; height: 10em"
                                          <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                ><c:out value="${dto.body}"/></textarea>
                            </td>
                            <td style="vertical-align: top">
                                <fmt:message key="passwordreset.settings.body-description"/>
                                <br><code>&#36{url}</code> - <fmt:message
                                    key="passwordreset.settings.body-url"/>
                                <br><code>&#36{userId}</code> - <fmt:message
                                    key="passwordreset.settings.body-user-id"/>
                                <br><code>&#36{userName}</code> - <fmt:message
                                    key="passwordreset.settings.body-user-name"/>
                                <br><code>&#36{userEmail}</code> - <fmt:message
                                    key="passwordreset.settings.body-user-email"/>
                                <c:if test='${!empty dto.bodyError}'>
                                    <div class="warning">
                                        <c:out value="${dto.bodyError}"/>
                                    </div>
                                </c:if>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label for="expiryCount">
                                    <fmt:message key="passwordreset.settings.expiry-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="expiryCount" name="expiryCount" type="number" min="1"
                                       style="width: 5em"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                       value="<c:out value="${dto.expiryCount}"/>"
                                >
                                <select id="expiryPeriod" name="expiryPeriod"
                                        <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                >
                                    <option value=""></option>
                                    <option value="MINUTES"
                                            <c:if test='${dto.expiryPeriod == "MINUTES"}'>selected</c:if>>
                                        <fmt:message
                                                key="passwordreset.settings.expiry-minutes"/></option>
                                    <option value="HOURS"
                                            <c:if test='${dto.expiryPeriod == "HOURS"}'>selected</c:if>>
                                        <fmt:message
                                                key="passwordreset.settings.expiry-hours"/></option>
                                    <option value="DAYS"
                                            <c:if test='${dto.expiryPeriod == "DAYS"}'>selected</c:if>>
                                        <fmt:message
                                                key="passwordreset.settings.expiry-days"/></option>
                                </select>
                            </td>
                            <td>
                                <label for="expiryPeriod">
                                    <fmt:message key="passwordreset.settings.expiry-description"/>
                                </label>
                                <c:if test='${!empty dto.expiryError}'>
                                    <div class="warning">
                                        <c:out value="${dto.expiryError}"/>
                                    </div>
                                </c:if>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label for="minLength">
                                    <fmt:message key="passwordreset.settings.min-length-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="minLength" name="minLength" type="number" min="1"
                                       style="width: 5em"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                       value="<c:out value="${dto.minLength}"/>"
                                >
                            </td>
                            <td>
                                <fmt:message key="passwordreset.settings.min-length-description"/>
                                <c:if test='${!empty dto.minLengthError}'>
                                    <div class="warning">
                                        <c:out value="${dto.minLengthError}"/>
                                    </div>
                                </c:if>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label for="maxLength">
                                    <fmt:message key="passwordreset.settings.max-length-label"/>
                                </label>
                            </td>
                            <td>
                                <input id="maxLength" name="maxLength" type="number" min="0"
                                       style="width: 5em"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                       value="<c:out value="${dto.maxLength}"/>"
                                >
                            </td>
                            <td>
                                <fmt:message key="passwordreset.settings.max-length-description"/>
                                <c:if test='${!empty dto.maxLengthError}'>
                                    <div class="warning">
                                        <c:out value="${dto.maxLengthError}"/>
                                    </div>
                                </c:if>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="3">
                                <input type="submit" name="update"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                       value="<fmt:message key="passwordreset.settings.update"/>">
                                <input type="submit" name="cancel"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                       value="<fmt:message key="passwordreset.settings.cancel"/>">
                                <input type="submit" name="test"
                                       <c:if test='${dto.notSupported}'>disabled="disabled"</c:if>
                                       value="<fmt:message key="passwordreset.settings.test"/>">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </div>
</form>


<c:if test="${dto.enabled}">
    <div class="jive-contentBoxHeader">
        <c:choose>
            <c:when test="${empty dto.resetRequests}">
                <fmt:message key="passwordreset.settings.no-current-requests"/>
            </c:when>
            <c:otherwise>
                <fmt:message key="passwordreset.settings.current-requests">
                    <fmt:param value="${dto.resetRequests.size()}"/>
                </fmt:message>
                <div class="jive-contentBox">
                    <table>
                        <thead>
                        <tr>
                            <th><fmt:message key="passwordreset.settings.request-user"/></th>
                            <th><fmt:message key="passwordreset.settings.request-source"/></th>
                            <th><fmt:message key="passwordreset.settings.request-expires"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:set var="rowClass" value="jive-even"/>
                        <c:forEach var="resetRequest" items="${dto.resetRequests}">
                            <c:choose>
                                <c:when test="${rowClass == 'jive-even'}"><c:set var="rowClass" value="jive-odd"/></c:when>
                                <c:otherwise><c:set var="rowClass" value="jive-even"/></c:otherwise>
                            </c:choose>
                            <tr class="${rowClass}">
                                <td>
                                    <c:url var="url" value="/user-properties.jsp">
                                        <c:param name="username" value="${resetRequest.userId}"/>
                                    </c:url>
                                    <a href="${url}"><c:out value="${resetRequest.userId}"/></a>
                                </td>
                                <td>
                                    <c:out value="${resetRequest.sourceAddress}"/>
                                </td>
                                <td>
                                    <c:out value="${admin:formatDateTime(resetRequest.expires)}"/>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>
</body>

</html>