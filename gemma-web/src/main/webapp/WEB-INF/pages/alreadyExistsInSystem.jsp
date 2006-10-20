<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" isErrorPage="true"%>
<%-- $Id$ --%>

<html>
	<head>
		<title>Data already exists in Gemma</title>

	</head>
	<body>
		<content tag="heading">
		Data already exists in Gemma
		</content>

		<a href="mainMenu.html" onclick="history.back();return false">&#171; Back</a>

		<Gemma:exception exception="${requestScope.exception}" showStackTrace="false" />

		<p>
			It looks like you tried to load data that Gemma thinks are already in the system. Please make sure your data are
			unique, or flag the existing data for replacement.
		</p>

	</body>
</html>

