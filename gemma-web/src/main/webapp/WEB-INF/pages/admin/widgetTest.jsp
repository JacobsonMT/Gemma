<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Widget tests</title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />


	<script>
	Ext.namespace('Gemma');
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	Ext.onReady( function() {
		Ext.QuickTips.init();
 	
		var k = new Gemma.DatasetGroupCombo( {
			renderTo : 'eesetcombo'
		});

		k = new Gemma.DatasetGroupComboPanel( {
			renderTo : 'eesetpanel'
		});

		k = new Gemma.DatasetSearchField( {
			renderTo : 'datasetsearchfield',
			taxon : {
				id : 1
			},
			initQuery : "liver"
		});

		k.on('aftersearch', function(f, results) {
			Ext.DomHelper.overwrite('dsresults', results.length + " found.");
		});

		k = new Gemma.GeneCombo( {
			renderTo : 'genecombo'
		});
		k = new Gemma.TaxonCombo( {
			renderTo : 'taxoncombo'
		});
		k = new Gemma.GeneGrid( {
			renderTo : 'genepicker',
			height : 200
		});

		k = new Gemma.DatasetGroupGridPanel( {
			renderTo : 'datasetGroupGrid',
			width : 750,
			tbar : new Gemma.DatasetGroupEditToolbar()
		});

		new Gemma.ExpressionExperimentGrid( {
			renderTo : 'eegrid',
			width : 400,
			height : 200,
			eeids : [ 1, 2, 3, 4, 5, 6 ],
			rowExpander : true
		});

		new Gemma.ArrayDesignCombo( {
			renderTo : 'adCombo'
		});

		new Gemma.AuditTrailGrid( {
			renderTo : 'atGrid',
			auditable : {
				classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl",
				id : 1
			},
			height : 200,
			width : 520
		});

		new Gemma.MGEDCombo( {
			renderTo : 'mgedcombo'
		});

		new Gemma.FactorValueCombo( {
			efId : 1,
			renderTo : 'factorValueCombo'
		});

		new Gemma.CharacteristicCombo( {
			renderTo : 'charCombo',
			listeners : {
				afterrender : function(d) {
					d.focus();
					d.setValue("urinary");
					d.getStore().load( {
						params : [ 'urinary' ]			,
				 		callback : function(rec, op, success) {
				 			d.focus();
				 			d.expand();
				 			d.select(1);
					 	}
					})

				}, 
			}
		});

		var v = new Gemma.ProgressWidget({renderTo : 'progressWidget', width : 400});
		v.allMessages = "Here are some messages for you, widget test.";

	});
</script>

	<style type="text/css">
.widget {
	padding: 10px;
	margin: 5px;
	background-color: #DDDDDD;
}
</style>
</head>

<h1>
	ComboBoxes
</h1>
<h2>
	DatasetGroup combo
</h2>
<div class="widget" id="eesetcombo"></div>

<h2>
	DatasetGroupComboPanel
</h2>
<div class="widget" id="eesetpanel"></div>
<h2>
	TaxonCombo
</h2>
<div class="widget" id="taxoncombo"></div>


<h2>
	FactorValueCombo
</h2>
<div class="widget" id="factorValueCombo"></div>


<h2>
	Dataset search field
</h2>
<div class="widget" id="datasetsearchfield"></div>
<div id="dsresults"></div>



<h2>
	CharacteristicCombo
</h2>
<div class="widget" id="charCombo"></div>

<h2>
	Gene combo
</h2>
<div class="widget" id="genecombo"></div>



<h2>
	MGEDCombo
</h2>
<div class="widget" id="mgedcombo"></div>
<h2>
	ArrayDesignCombo
</h2>
<div class="widget" id="adCombo"></div>

<h1>
	Panels
</h1>

<h2>
	GeneChooserPanel
</h2>
<div class="widget" id="genepicker"></div>



<h2>
	DatasetGroupGridPanel with DatasetGroupEditToolbar
</h2>
<div class="widget" id="datasetGroupGrid"></div>
<h2>
	ExpressionExperiment Grid
</h2>
<div class="widget" id="eegrid"></div>



<h2>
	AuditTrailGrid
</h2>
<div class="widget" id="atGrid"></div>


<h2>
	ProgressWidget
</h2>
<div class="widget" id="progressWidget"></div>

<h2>
	Widget
</h2>
<div class="widget" id=""></div>

