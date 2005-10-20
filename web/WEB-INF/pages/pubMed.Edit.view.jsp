<%@ include file="/common/taglibs.jsp"%>

<spring:bind path="bibliographicReference.*">
    <c:if test="${not empty status.errorMessages}">
        <div class="error"><c:forEach var="error"
            items="${status.errorMessages}">
            <img src="<c:url value="/images/iconWarning.gif"/>"
                alt="<fmt:message key="icon.warning"/>" class="icon" />
            <c:out value="${error}" escapeXml="false" />
            <br />
        </c:forEach></div>
    </c:if>
</spring:bind>
<html>
<head></head>
<body>
<form method="post" action="<c:url value="/flowController.htm"/>"
    id="bibliographicReferenceForm" onsubmit="return onFormSubmit(this)">
<input type="hidden" name="_flowExecutionId"
    value="<c:out value="${flowExecutionId}"/>"> <input type="hidden"
    name="_eventId" value="submit">

<table class="detail" width="75%">
    <c:set var="pageButtons">
        <tr>
            <td></td>
            <td class="buttonBar"><input type="submit" class="button"
                name="save"
                onclick="bCancel=false;this.form._eventId.value='submit'"
                value="<fmt:message key="button.save"/>" /> <c:if
                test="${param.from == 'list'}">
                <input type="submit" class="button" name="delete"
                    onclick="bCancel=false;this.form._eventId.value='delete'"
                    value="<fmt:message key="button.delete"/>" />
            </c:if> <input type="submit" class="button" name="cancel"
                onclick="bCancel=true;this.form._eventId.value='cancel'"
                value="<fmt:message key="button.cancel"/>" /></td>
        </tr>
    </c:set>

    <tr>
        <th><Gemma:label key="pubMed.authors" /></th>
        <td><c:out value="${bibliographicReference.authorList}" /> <%--<c:out value="${arrayDesign.designProvider.name}"/>--%>
        </td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.year" /></th>
        <td><c:out value="${bibliographicReference.publicationDate}" />
        </td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.volume" /></th>
        <td><input
            type="text" name="volume"
            value="<c:out value="${bibliographicReference.volume}"/>"
            id="volume" /></td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.pages" /></th>
        <td><c:out value="${bibliographicReference.pages}" /></td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.title" /></th>
        <td><spring:bind path="bibliographicReference.title">
            <c:choose>
                <c:when test="${empty pubMed.title}">
                    <textarea name="title" id="title" rows=8 cols=60><c:out
                        value="${status.value}" /></textarea>
                    <span class="fieldError"><c:out
                        value="${status.errorMessage}" /></span>
                </c:when>
                <c:otherwise>
                    <c:out value="${pubMed.title}" />
                    <input type="hidden" name="title"
                        value="<c:out value="${status.value}"/>"
                        id="title" />
                </c:otherwise>
            </c:choose>
        </spring:bind></td>
    </tr>

    <tr>
        <th><Gemma:label key="pubMed.abstract" /></th>
        <td><spring:bind path="bibliographicReference.abstractText">
            <textarea name="abstractText" id="abstractText" rows=12
                cols=60><c:out value="${status.value}" /></textarea>
            <span class="fieldError"><c:out
                value="${status.errorMessage}" /></span>
        </spring:bind></td>
    </tr>


    <%-- Print out buttons - defined at top of form --%>
    <%-- This is so you can put them at the top and the bottom if you like --%>
    <c:out value="${pageButtons}" escapeXml="false" />

</table>
</form>

<script type="text/javascript">
<!--
highlightFormElements();

var focusControl = document.forms["bibliographicReferenceForm"].elements["<c:out value="${focus}"/>"];

function onFormSubmit(theForm) {
}
// -->
</script>

<html:javascript formName="bibliographicReferenceForm"
    staticJavascript="false" />
</body>
</html>

