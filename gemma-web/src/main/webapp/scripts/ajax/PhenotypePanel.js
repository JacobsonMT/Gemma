Ext.namespace('Gemma');

var phenotypeGrid;
var geneGrid;
var evidenceGrid;

Gemma.PhenotypePanel = Ext.extend(Ext.Panel, {
    initComponent: function() {
    	if ((this.phenotypeStoreProxy && this.geneStoreProxy && this.geneColumnRenderer) ||
    	    (!this.phenotypeStoreProxy && !this.geneStoreProxy && !this.geneColumnRenderer)) {
	    	geneGrid = new Gemma.GeneGrid({
				geneStoreProxy: this.geneStoreProxy ?
							   		this.geneStoreProxy :
							   		new Ext.data.DWRProxy({
								        apiActionToHandlerMap: {
							    	        read: {
							        	        dwrFunction: PhenotypeController.findCandidateGenes,
							            	    getDwrArgsFunction: function(request){
							            	    	return [request.params["phenotypeValueUri"]];
								                }
							    	        }
								        }
							    	})
			});
			geneGrid.getColumnModel().setRenderer(0,
				this.geneColumnRenderer ?
					this.geneColumnRenderer :
					function(value, metadata, record, row, col, ds) {
						return String.format("{1} <a target='_blank' href='/Gemma/gene/showGene.html?id={0}' ext:qtip='Go to {1} Details (in new window)'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a> ",
							record.data.id, record.data.officialSymbol);
					}
			);
			
	    	evidenceGrid = new Gemma.PhenotypeEvidenceGridPanel({
	    		region: 'center'
	    	});
	
			Ext.apply(this, {
		        height: 600,
	    	    width: 760,
				layout: 'border',        
	        	items: [
		        	{
						xtype: 'panel',
					    height: 200,
					    layout: 'border',
					    viewConfig: {
					        forceFit: true
					    },
					    items: [
					        new Gemma.PhenotypeGrid({
					        	phenotypeStoreProxy: this.phenotypeStoreProxy ?
							   		this.phenotypeStoreProxy :
							   		new Ext.data.DWRProxy(PhenotypeController.loadAllPhenotypes)
					        }),
					        geneGrid
					    ],
						region: 'north',
						split: true
		        	},
		            evidenceGrid
		        ]
			});
    	} else {
    		Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorTitle, Gemma.HelpText.WidgetDefaults.PhenotypePanel.setupErrorText);
    	}

		Gemma.PhenotypePanel.superclass.initComponent.call(this);
		
		var isStoreFirstLoad = true;
		phenotypeGrid.getStore().on('load', function() {
			if (isStoreFirstLoad && Ext.get("phenotypeUrlId") != null && Ext.get("phenotypeUrlId").getValue() != "") {
				var currentRecord = phenotypeGrid.getStore().getById(Ext.get("phenotypeUrlId").getValue());

				phenotypeGrid.getSelectionModel().selectRecords( [ currentRecord ], false); // false to not keep existing selections
				
				var a = function() {
					return phenotypeGrid.getView().focusRow(phenotypeGrid.getStore().indexOf(currentRecord));
				};
				a.defer(100);
			}
		});
		geneGrid.getStore().on('load', function() {
			if (isStoreFirstLoad && Ext.get("geneId") != null && Ext.get("geneId").getValue() != "") {
				var currentRecord = geneGrid.getStore().getById(Ext.get("geneId").getValue());
				
				geneGrid.getSelectionModel().selectRecords( [ currentRecord ], false); // false to not keep existing selections
					
				var a = function() {
					return geneGrid.getView().focusRow(geneGrid.getStore().indexOf(currentRecord));
				};
				a.defer(100);
			}
			isStoreFirstLoad = false;
		});	
    }
});

Gemma.PhenotypeGrid = Ext.extend(Ext.grid.GridPanel, {
    initComponent: function() {
    	phenotypeGrid = this;
    	var checkboxSelectionModel = new Gemma.PhenotypeCheckboxSelectionModel();
    	var phenotypeAssociationFormWindow;
		Ext.apply(this, {
			title: "Phenotypes",
		    autoScroll: true,
		    stripeRows: true,
			width: 350,
height: 300,			
			region: "west",
			split: true,
			store: new Gemma.PhenotypeStore({
				proxy: this.phenotypeStoreProxy
			}),
			loadMask: true,
		    viewConfig: {
		        forceFit: true
		    },
			myPageSize:500,
			
			// grid columns
			columns:[
				checkboxSelectionModel,
				{
					header: "Phenotype",
					dataIndex: 'value',
					width: 285,
					renderer: tooltipRenderer,
					sortable: true
				},{
					header: "Gene Count",
					dataIndex: 'occurence',
					align: "right",
					width: 135,
					renderer: Ext.util.Format.numberRenderer('0,0'),
					sortable: true
			    }
			],
		    sm: checkboxSelectionModel,
		    listeners: {
		    	headerclick: function(gridPanel, columnIndex, event) {
		    		if (columnIndex == 0) {
		    			phenotypeGrid.getStore().sort('isChecked');
		    		}
		    	}
		    },
		    showPhenotypeAssociationFormWindow: function() {
		    	if (phenotypeAssociationFormWindow == null) {
						phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window({
							listeners: {
								hide: function(thisWindow) {
									if (phenotypeAssociationFormWindow.phenotypeAssociationCreated) {
										var phenotypeGridStore = phenotypeGrid.getStore();  
										phenotypeGridStore.reload(phenotypeGridStore.lastOptions);
									}
								}
							}
						});
		    	}
				phenotypeAssociationFormWindow.show();
		    },
		    promptLoginForOpeningCreatePhenotypeAssociationWindow: function() {
				Gemma.AjaxLogin.showLoginWindowFn();
		
				Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){	
					Ext.getBody().unmask();
					this.showPhenotypeAssociationFormWindow();
				}, this);
		    },
			tbar: [
				new Gemma.PhenotypePanelSearchField({
					getSelectionModel: function() { return phenotypeGrid.getSelectionModel(); },
					getStore: function() { return phenotypeGrid.getStore(); },
					filterFields: [ 'value' ],
					emptyText: 'Search Phenotypes'
				})
// NEW create phenotype button
//				{
//					handler: function() {
//						Ext.Ajax.request({
//				         	url : '/Gemma/ajaxLoginCheck.html',
//				            method: 'GET',                  
//				            success: function ( response, options ) {			
//									
//				                    var dataMsg = Ext.util.JSON.decode(response.responseText); 
//				                    
//				                    if (dataMsg.success){
//										this.showPhenotypeAssociationFormWindow();
//									}
//				                    else{
//										this.promptLoginForOpeningCreatePhenotypeAssociationWindow();                      	
//				                    }
//				            },
//				            failure: function ( response, options ) {   
//								this.promptLoginForOpeningCreatePhenotypeAssociationWindow();                      	
//				            },
//				            scope: this,
//				            disableCaching: true
//				       });
//					},
//					scope: this,
//					icon: "/Gemma/images/icons/add.png",
//					tooltip: "Create phenotype association"
//				}
			]
		});
		Gemma.PhenotypeGrid.superclass.initComponent.call(this);

		this.getStore().setDefaultSort('value', 'asc');
    }
});

Gemma.PhenotypeStore = Ext.extend(Ext.data.Store, {
	constructor: function(config) {
		Gemma.PhenotypeStore.superclass.constructor.call(this, config);
	},
	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
		idProperty: "urlId",
		fields: [
			'urlId',
			'value',
			'valueUri',
			{ name: 'occurence', type: "long" },
			{ name: 'isChecked', sortDir: 'DESC' }
		]
	}),
	autoLoad: true
});

Gemma.PhenotypeCheckboxSelectionModel = Ext.extend(Ext.grid.CheckboxSelectionModel, {
	dataIndex: 'isChecked',
	singleSelect: false,
	header: '', // remove the "select all" checkbox on the header 
    listeners: {
		rowdeselect: function(selectionModel, rowIndex, record) {
			record.set('isChecked', false);
		},
		rowselect: function(selectionModel, rowIndex, record) {
			record.set('isChecked', true);
		},
        selectionchange: function(selectionModel) {
			if (selectionModel.hasSelection()) {
				var phenotypeSelections = selectionModel.getSelections();
				
				var storeBaseParams = [];
				
				var selectedPhenotypePrefix = 'Genes associated with';
				var selectedPhenotypeHeader = selectedPhenotypePrefix + ' "';
				var selectedPhenotypeTooltip = '&nbsp;&nbsp;&nbsp;';
				
			    for (var i = 0; i < phenotypeSelections.length; i++) {
			    	var currPhenotypeValue = phenotypeSelections[i].get('value');

			        storeBaseParams.push(phenotypeSelections[i].get('valueUri'));
			        
					selectedPhenotypeHeader += currPhenotypeValue;
					selectedPhenotypeTooltip += currPhenotypeValue;
					
					if (i < phenotypeSelections.length - 1) {
						selectedPhenotypeHeader += '" + "';
						selectedPhenotypeTooltip += '<br />&nbsp;&nbsp;&nbsp;';
					} else {
						selectedPhenotypeHeader += '"';
					}	
				}
				var geneStore = geneGrid.getStore();
				geneStore.baseParams = geneStore.baseParams || {};
			    geneStore.baseParams['phenotypeValueUri'] = storeBaseParams;
evidenceGrid.removeAll(false);
			    geneStore.reload({
			    	params: {
						start: 0,
						limit: geneGrid.myPageSize							
					}
			    });
				geneGrid.getSelectionModel().clearSelections(false);				    
				    
				geneGrid.setTitle("<div style='height: 15px; overflow: hidden;' " +  // Make the header one line only.
					"ext:qtitle='" + selectedPhenotypePrefix + "' " +
					"ext:qtip='" + selectedPhenotypeTooltip + "'>" + selectedPhenotypeHeader + "</div>");
geneGrid.titleText = selectedPhenotypeHeader;
			} else {
				geneGrid.setTitle("Genes");						
				geneGrid.getStore().removeAll(false);
			}
        }
    }
});


Gemma.GeneGrid = Ext.extend(Ext.grid.GridPanel, {
    initComponent: function() {
    	var downloadButton = new Ext.Button({
			text: '<b>Download</b>',
			disabled: true,
			icon: '/Gemma/images/download.gif',
			handler: function() {
				var columnConfig = geneGrid.getColumnModel().config;
				var downloadData = [];
			    var downloadDataRow = [];
				
			    for (var i = 0; i < columnConfig.length; i++) {
			        downloadDataRow.push(columnConfig[i].header);
			    }
			    downloadData.push(downloadDataRow);
			    
				geneGrid.getStore().each(function(record) {
				    downloadDataRow = [];
				    for (var i = 0; i < columnConfig.length; i++) {
				        downloadDataRow.push(record.get(columnConfig[i].dataIndex));
				    }
				    downloadData.push(downloadDataRow);
				});
		
				var downloadDataHeader = geneGrid.titleText;
				if (searchField.getValue() !== '') {
					downloadDataHeader += ' AND matching pattern "' + searchField.getValue() + '"';
				}
		  		var textWindow = new Gemma.DownloadWindow({
		  			windowTitleSuffix: 'Genes associated with selected Phenotype(s)',
		  			downloadDataHeader: downloadDataHeader, 
		  			downloadData: downloadData,
		  			modal: true
		  		});
		  		textWindow.convertToText ();
		  		textWindow.show();
			}
    	});
    	var searchField = new Gemma.PhenotypePanelSearchField({
			getSelectionModel: function() { return geneGrid.getSelectionModel(); },
			getStore: function() { return geneGrid.getStore(); },
			filterFields: [ 'officialSymbol', 'officialName' ],
			emptyText: 'Search Genes'
		});

		Ext.apply(this, {
			title: "Genes",
			titleText: this.title,	
		    autoScroll: true,
		    stripeRows: true,
			region: "center",
			store: new Gemma.GeneStore({
				proxy: this.geneStoreProxy
			}),
			loadMask: true,
		    viewConfig: {
		        forceFit: true
		    },
			myPageSize: 50,
			columns:[{
				header: "Symbol",
				dataIndex: 'officialSymbol',
				width: 65,
				sortable: true
			},{
				header: "Name",
				dataIndex: 'officialName',
				width: 215,
				renderer: tooltipRenderer,
				sortable: true
			},{
				header: "Species",
				dataIndex: 'taxonCommonName',
				width: 100,
				sortable: true
			}],
			selModel: new Ext.grid.RowSelectionModel({
				singleSelect: true,
				listeners: {
					selectionchange: function(selModel) {
						if (selModel.hasSelection()) {
							var geneGridSelection = geneGrid.getSelectionModel().getSelected();
		
							evidenceGrid.loadData(geneGridSelection.json.evidence);
							evidenceGrid.setTitle("Evidence for " + geneGridSelection.get('officialSymbol'));					
						} else {
							evidenceGrid.removeAll(false);
							evidenceGrid.setTitle(evidenceGridDefaultTitle);					
						}                	
					}
				}
			}),
			tbar: [
				searchField,
				downloadButton
			],
			onStoreRecordChange: function() {
				downloadButton.setDisabled(this.getStore().getCount() <= 0);
			}
		});
		Gemma.GeneGrid.superclass.initComponent.call(this);
		
		this.getStore().setDefaultSort('officialSymbol', 'asc');
		
		// TODO: should disable download button if users filter too!
		this.getStore().addListener({
			load: this.onStoreRecordChange,
			clear: this.onStoreRecordChange,
            scope: this
		});
    }
});

Gemma.GeneStore = Ext.extend(Ext.data.Store, {
	constructor: function(config) {
		Gemma.GeneStore.superclass.constructor.call(this, config);
	},
	reader: new Ext.data.JsonReader({
		root: 'records', // required.
		successProperty: 'success', // same as default.
		messageProperty: 'message', // optional
		totalProperty: 'totalRecords', // default is 'total'; optional unless paging.
		idProperty: 'id', // same as default
		fields: [ 'id', 'officialSymbol', 'officialName', 'taxonCommonName', 'evidence' ]
    })
});

Gemma.PhenotypePanelSearchField = Ext.extend(Ext.form.TwinTriggerField, {
    initComponent: function() {
    	if (this.getSelectionModel && this.getStore && this.filterFields && this.emptyText) {
	        Gemma.PhenotypePanelSearchField.superclass.initComponent.call(this);
	        this.on('specialkey', function(f, e) {
	            if (e.getKey() == e.ENTER) {
	                this.onTrigger2Click();
	            }
	        }, this);
    	} else {
    		Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanelSearchField.setupErrorTitle, 
							Gemma.HelpText.WidgetDefaults.PhenotypePanelSearchField.setupErrorText);
    	}
    },
	
	enableKeyEvents: true,
    validationEvent: false,
    validateOnBlur: false,
    trigger1Class: 'x-form-clear-trigger',
    trigger2Class: 'x-form-search-trigger',
    hideTrigger1: true,
    width: 220,
    hasSearch: false,
	listeners: {
		keyup: function(field, e) {
            this.onTrigger2Click();
		}
	},
    onTrigger1Click : function() {
        if (this.hasSearch) {
        	this.getStore().clearFilter(false);
            this.el.dom.value = '';
            this.triggers[0].hide();
            this.hasSearch = false;
        }
    },
    onTrigger2Click: function() {
        var typedString = this.getRawValue().toLowerCase();
        if (typedString.length < 1) {
            this.onTrigger1Click();
            return;
        }

		this.getStore().filterBy(
			function(record) {
				for (var i = 0; i < this.filterFields.length; i++) {
					if (record.get(this.filterFields[i]).toLowerCase().indexOf(typedString) >= 0 || this.getSelectionModel().isSelected(record)) {
						return true;
					}
				}
			    return false;
		    },
		    this	
		);

        this.hasSearch = true;
        this.triggers[0].show();
    }
});

function tooltipRenderer(value, metadata) {
    metadata.attr = 'ext:qtip="' + value + '"';
    return value;
}
