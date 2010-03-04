<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<head>

	<title><fmt:message key="generalSearch.title" />
	</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/search.js' />

	<script type="text/javascript">
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider( ));
	Ext.QuickTips.init();
	Ext.onReady(Gemma.Search.app.init, Gemma.Search.app);
	</script>
</head>


<h1>
	General search tool for Gemma
</h1>

<div id="messages"></div>
<div style="height: 1em; margin-bottom: " id="validation-messages"></div>
<div style="margin-bottom: 10px" id="general-search-form"></div>
<div style="margin: 5px" id="search-bookmark"></div>
<div style="margin-top: 2px" id="search-results-grid"></div>

