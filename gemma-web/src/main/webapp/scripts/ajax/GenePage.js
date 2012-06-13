Ext.namespace('Gemma');

Gemma.DIFF_THRESHOLD = 0.01;
Gemma.MAX_DIFF_RESULTS = 75;
/**
 * 
 * Top level container for all sections of gene info
 * 
 * To open the page at a specific tab, include ?tab=[tabName] suffix in the URL. Tab names are each tab's itemId.
 * 
 * @class Gemma.GenePage
 * @extends Ext.TabPanel
 * 
 */


Gemma.GenePage =  Ext.extend(Ext.TabPanel, {

	autoScroll:true,
	defaults: {
		autoScroll: true,
		width: 850
	},
	deferredRender: true,
	listeners: {
		'tabchange': function(tabPanel, newTab){
			newTab.fireEvent('tabChanged');
		},
		'beforetabchange': function(tabPanel, newTab, currTab){
			// if false is returned, tab isn't changed
			if (currTab) {
				return currTab.fireEvent('leavingTab');
			}
			return true;
		}
	},
	initComponent: function(){

		var geneId = this.geneId;
		
		var isAdmin = (Ext.get("hasAdmin"))?(Ext.get("hasAdmin").getValue() === 'true')?true:false:false;
		
		Gemma.GenePage.superclass.initComponent.call(this);
		
		//DETAILS TAB
		var details = new Gemma.GeneDetails({
			title: 'Details',
			itemId: 'details',
			geneId: geneId
		});
		details.on('changeTab', function(tabName){
			this.setActiveTab(tabName);
		},this);
		this.add(details);
				
		this.add(this.initDiffExTab( geneId ));
		
		this.add(this.initCoexTab( geneId ));
						
		this.add(this.initPhenotypeTab( geneId ));
		
		this.add({
			title:'Gene Ontology Terms',
			xtype: 'genegogrid',
			border: true,
			geneid: this.geneId,
			minHeight: 150,
			deferLoadToRender: true,
			itemId: 'goGrid'
		});
		
		//ALLEN BRAIN ATLAS IMAGES
		this.add({
			xtype:'geneallenbrainatlasimages',
			geneId: geneId,
			title:'Expression Images',
			itemId:'expression'
		});
		

		/* see bug 2972
		 * this.add({
			xtype: 'geneproductgrid',
			geneid: geneId,
			title: 'Gene Products',
			deferLoadToRender: true,
			itemId: 'products'
		});*/
		
		var initialTab = 'details';
		this.loadSpecificTab = (document.URL.indexOf("?") > -1 && (document.URL.indexOf("tab=") > -1));
        if ( this.loadSpecificTab ) {
            var param = Ext.urlDecode(document.URL.substr(document.URL.indexOf("?") + 1));
            if (param.tab) {
            	if(this.getComponent( param.tab ) != undefined){
            		initialTab =  param.tab;
            	}
            }
        }
		
		this.on('render', function(){
			this.setActiveTab( initialTab );
		});
	},
	initCoexTab: function( geneId ){
		var coexpressedGeneGrid = new Gemma.CoexpressionGrid({
			title: 'Coexpression',
			colspan: 2,
			lite: true,
			noSmallGemma: true,
			itemId: 'coex'
		});
		coexpressedGeneGrid.on('render', function(){
			coexpressedGeneGrid.doSearch({
				geneIds: [geneId],
				quick: true,
				stringency: 2,
				forceProbeLevelSearch: false
			});
		});
		return coexpressedGeneGrid;
		
	},
	initDiffExTab: function( geneId ){
		var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
			title: 'Differential Expression',
			itemId: 'diffEx'
		});
		diffExGrid.on('render', function(){
			var visColumnIndex = diffExGrid.getColumnModel().getIndexById('visualize');
			diffExGrid.getColumnModel().setHidden(visColumnIndex, false);
			// this should go in grid itself, but it wasn't working properly (or at all)
			if (!this.loadMask && this.getEl()) {
				this.loadMask = new Ext.LoadMask(this.getEl(), {
					msg : Gemma.StatusText.Loading.generic,
					msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
				});
			}
			this.loadMask.show();
			diffExGrid.getStore().load({
				params: [geneId, Gemma.DIFF_THRESHOLD, Gemma.MAX_DIFF_RESULTS]
			});
		});
		return diffExGrid;
	},
	initPhenotypeTab : function( geneId ){
		var phenotypeEvidenceGridPanel = new Gemma.PhenotypeEvidenceGridPanel({
			title: 'Phenotypes',
			itemId: 'phenotypes',
			hasRelevanceColumn: false,
			deferLoadToRender: true,
			currentGene: {
	    		id: this.geneId,
	    		ncbiId: this.geneNcbiId,
	    		officialSymbol: this.geneSymbol,
	    		officialName: this.geneName,
	    		taxonCommonName: this.geneTaxonName,
	    		taxonId: this.geneTaxonId
    		},
			evidencePhenotypeColumnRenderer: function(value, metadata, record, row, col, ds) {
				var phenotypesHtml = '';
				for (var i = 0; i < record.data.phenotypes.length; i++) {
					phenotypesHtml += String.format('<a target="_blank" href="/Gemma/phenotypes.html?phenotypeUrlId={0}&geneId={2}" ext:qtip="Go to Phenotype Page (in new window)">{1}</a><br />',
						record.data.phenotypes[i].urlId, record.data.phenotypes[i].value, geneId);
				}					
				return phenotypesHtml;
			}
		});
		// In PhenotypePanel, when a user logs in, PhenotypeGridPanel will be reloaded first, followed by  
		// PhenotypeGeneGridPanel and then PhenotypeEvidenceGridPanel. So, the following code should not 
		// be done in PhenotypeEvidenceGridPanel. Otherwise, PhenotypeEvidenceGridPanel would be reloaded twice.
		Gemma.Application.currentUser.on("logIn", 
			function(userName, isAdmin) {
				var phenotypeEvidenceGridStore = phenotypeEvidenceGridPanel.getStore();
				phenotypeEvidenceGridStore.reload(phenotypeEvidenceGridStore.lastOptions);
			},
			this);
		
		return phenotypeEvidenceGridPanel;
	}
});
