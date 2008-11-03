Ext.namespace('Gemma');

Gemma.ExperimentalFactorGrid = Ext.extend(Gemma.GemmaGridPanel, {

			loadMask : true,

			record : Ext.data.Record.create([{
						name : "id",
						type : "int"
					}, {
						name : "name",
						type : "string"
					}, {
						name : "description",
						type : "string"
					}, {
						name : "category",
						type : "string"
					}, {
						name : "categoryUri",
						type : "string"
					}, {
						name : 'type',
						type : 'string'
					}]),

			categoryStyler : function(value, metadata, record, row, col, ds) {
				return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.get("categoryUri"));
			},

			initComponent : function() {

				this.experimentalDesign = {
					id : this.edId,
					classDelegatingFor : "ExperimentalDesign"
				};

				Ext.apply(this, {
							columns : [{
										header : "Name",
										dataIndex : "name",
										sortable : true
									}, {
										header : "Category",
										dataIndex : "category",
										renderer : this.categoryStyler,
										sortable : true

									}, {
										header : "Description",
										dataIndex : "description",
										sortable : true
									}, {
										header : "Type",
										dataIndex : 'type'
									}]
						});

				this.store = new Ext.data.Store({
							proxy : new Ext.data.DWRProxy(ExperimentalDesignController.getExperimentalFactors),
							reader : new Ext.data.ListRangeReader({
										id : "id"
									}, this.record)
						});

				if (this.editable) {
					this.tbar = new Gemma.ExperimentalFactorToolbar({});
				}

				Gemma.ExperimentalFactorGrid.superclass.initComponent.call(this);

				this.addEvents('experimentalfactorchange', 'experimentalfactorselected');

				this.getSelectionModel().on("selectionchange", function(model) {
							var selected = model.getSelections();
							if (selected.length == 1) {
								this.fireEvent("experimentalfactorselected", selected[0]);
							}

						}.createDelegate(this));

				this.store.load({
							params : [this.experimentalDesign]
						});

			},

			onRender : function(c, l) {

				Gemma.ExperimentalFactorGrid.superclass.onRender.call(this, c, l);

				var NAME_COLUMN = 0;
				var CATEGORY_COLUMN = 1;
				var DESCRIPTION_COLUMN = 2;

				this.autoExpandColumn = DESCRIPTION_COLUMN;

				this.nameField = new Ext.form.TextField({});
				var nameEditor = new Ext.grid.GridEditor(this.nameField);

				this.categoryCombo = new Gemma.MGEDCombo({
							lazyRender : true,
							termKey : "factor"
						});

				this.descriptionField = new Ext.form.TextField({});

				if (this.editable) {

					var categoryEditor = new Ext.grid.GridEditor(this.categoryCombo);
					this.categoryCombo.on("select", function(combo, record, index) {
								categoryEditor.completeEdit();
							});

					var descriptionEditor = new Ext.grid.GridEditor(this.descriptionField);
					this.getColumnModel().setEditor(NAME_COLUMN, nameEditor);
					this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);
					this.getColumnModel().setEditor(DESCRIPTION_COLUMN, descriptionEditor);

					this.getTopToolbar().on("create", function(newFactorValue) {
						var oldmsg = this.loadMask.msg;
						this.loadMask.msg = "Creating new experimental factor";
						this.loadMask.show();

						var callback = function() {
							this.factorCreated(newFactorValue);
							this.loadMask.hide();
							this.loadMask.msg = oldmsg;
						}.createDelegate(this);

						var errorHandler = function(er) {
							this.loadMask.hide();
							this.loadMask.msg = oldmsg;
							Ext.Msg.alert("Error", er);

						}.createDelegate(this);

						ExperimentalDesignController.createExperimentalFactor(this.experimentalDesign, newFactorValue,
								{
									callback : callback,
									errorHandler : errorHandler
								});
					}.createDelegate(this));

					this.getTopToolbar().on("delete", function() {
								var selected = this.getSelectedIds();
								var oldmsg = this.loadMask.msg;
								this.loadMask.msg = "Deleting experimental factor(s)";
								this.loadMask.show();

								var callback = function() {
									this.idsDeleted(selected);
									this.loadMask.hide();
									this.loadMask.msg = oldmsg;
								}.createDelegate(this);

								var errorHandler = function(er) {
									this.loadMask.hide();
									this.loadMask.msg = oldmsg;
									Ext.Msg.alert("Error", er);
								}.createDelegate(this);

								ExperimentalDesignController.deleteExperimentalFactors(this.experimentalDesign,
										selected, {
											callback : callback,
											errorHandler : errorHandler
										});
							}.createDelegate(this));

					this.getTopToolbar().on("save", function() {
								var edited = this.getEditedRecords();
								var oldmsg = this.loadMask.msg;
								this.loadMask.msg = "Saving ...";
								this.loadMask.show();
								var callback = function() {
									this.recordsChanged(edited);
									this.loadMask.hide();
									this.loadMask.msg = oldmsg;
								}.createDelegate(this);

								var errorHandler = function(er) {
									this.loadMask.hide();
									this.loadMask.msg = oldmsg;
									Ext.Msg.alert("Error", er);
								}.createDelegate(this);

								ExperimentalDesignController.updateExperimentalFactors(edited, callback);
							}.createDelegate(this));

					this.getTopToolbar().on("undo", this.revertSelected.createDelegate(this));

					this.on("afteredit", function(e) {
								var col = this.getColumnModel().getColumnId(e.column);
								if (col == CATEGORY_COLUMN) {
									var f = this.categoryCombo.getTerm.bind(this.categoryCombo);
									var term = f();
									e.record.set("category", term.term);
									e.record.set("categoryUri", term.uri);
								}
							}.createDelegate(this));

					this.on("afteredit", function(model) {
								this.saveButton.enable();
								this.revertButton.enable();
							}, this.getTopToolbar());

					this.getSelectionModel().on("selectionchange", function(model) {
								var selected = model.getSelections();
								if (selected.length > 0) {
									this.deleteButton.enable();
								} else {
									this.deleteButton.disable();
								}
								this.revertButton.disable();
								for (var i = 0; i < selected.length; ++i) {
									if (selected[i].dirty) {
										this.revertButton.enable();
										break;
									}
								}
							}, this.getTopToolbar());
				} // if editable.
			},

			factorCreated : function(factor) {
				this.refresh();
				var efs = [factor];
				this.fireEvent('experimentalfactorchange', this, efs);
			},

			recordsChanged : function(records) {
				this.refresh();
				var efs = [];
				for (var i = 0; i < records.length; ++i) {
					efs.push(records[i].data);
				}
				this.fireEvent('experimentalfactorchange', this, efs);
			},

			idsDeleted : function(ids) {
				this.refresh();
				var efs = [];
				for (var i = 0; i < ids.length; ++i) {
					efs.push(this.store.getById(ids[i]).data);
				}
				this.fireEvent('experimentalfactorchange', this, efs);
			}

		});

Gemma.ExperimentalFactorAddWindow = Ext.extend(Ext.Window, {

			modal : true,
			closeAction : 'close',
			title : "Fill in new factor details",

			initComponent : function() {

				Ext.apply(this, {
							items : [{
										xtype : 'form',
										bodyStyle : "padding:10px",
										monitorValid : true,
										id : 'factor-create-form',
										items : [new Gemma.MGEDCombo({
															id : 'factor-mged-combo',
															emptyText : "Select a category",
															fieldLabel : "Category",
															allowBlank : false,
															termKey : "factor"
														}), {
													xtype : 'textfield',
													width : 250,
													id : 'factor-description-field',
													allowBlank : false,
													fieldLabel : "Description",
													emptyText : "Type a short distinctive description"
												}, {
													xtype : 'checkbox',
													id : 'factor-type-checkbox',
													fieldLabel : 'Continuous',
													tooltip : "Check if continuous variable"
												}]
									}],
							buttons : [{
										text : "Create",
										id : 'factor-create-button',
										tooltip : "Create the new experimental factor",
										disabled : true,
										handler : function() {
											this.fireEvent("done", this.getExperimentalFactorValueObject());
											this.close();
										},
										scope : this
									}, {
										text : "Cancel",
										handler : function() {
											this.close();
										},
										scope : this
									}]
						});

				Gemma.ExperimentalFactorAddWindow.superclass.initComponent.call(this);

				this.addEvents("done");

				Ext.getCmp('factor-create-form').on('clientvalidation', function(form, valid) {
							if (valid) {
								Ext.getCmp('factor-create-button').enable();
							} else {
								Ext.getCmp('factor-create-button').disable();
							}
						});
			},

			getExperimentalFactorValueObject : function() {
				var category = Ext.getCmp('factor-mged-combo').getTerm();
				var description = Ext.getCmp('factor-description-field').getValue();
				return {
					name : category.term,
					description : description,
					category : category.term,
					categoryUri : category.uri,
					type : Ext.getCmp('factor-type-checkbox').getValue() ? "Continuous" : "Categorical"
				};
			}
		});

Gemma.ExperimentalFactorToolbar = Ext.extend(Ext.Toolbar, {

			onRender : function(c, l) {
				Gemma.ExperimentalFactorToolbar.superclass.onRender.call(this, c, l);

				this.createButton = new Ext.Toolbar.Button({
							text : "Add new",
							tooltip : "Add a new experimental factor",
							disabled : false,
							handler : function() {
								var w = new Gemma.ExperimentalFactorAddWindow();
								w.on('done', function(object) {
											this.fireEvent('create', object);
										}.createDelegate(this))
								w.show();
							},
							scope : this
						});

				this.deleteButton = new Ext.Toolbar.Button({
							text : "Delete",
							tooltip : "Delete the selected experimental factors",
							disabled : true,
							handler : function() {
								Ext.Msg.confirm('Deleting records', 'Are you sure? This cannot be undone',
										function(but) {
											if (but == 'yes') {
												this.deleteButton.disable();
												this.fireEvent("delete");
											}
										}.createDelegate(this));

							},
							scope : this
						});

				this.revertButton = new Ext.Toolbar.Button({
							text : "Undo",
							tooltip : "Undo changes to the selected experimental factors",
							disabled : true,
							handler : function() {
								this.fireEvent("undo");
							},
							scope : this
						});

				this.saveButton = new Ext.Toolbar.Button({
							text : "Save",
							tooltip : "Commit changes",
							disabled : true,
							handler : function() {
								this.saveButton.disable();
								this.revertButton.disable();
								this.fireEvent("save");
							},
							scope : this
						});

				this.addButton(this.createButton);
				this.addSeparator();
				this.addButton(this.deleteButton);
				this.addSeparator();
				this.addButton(this.saveButton);
				this.addSeparator();
				this.addButton(this.revertButton);

			},

			initComponent : function() {
				Gemma.ExperimentalFactorToolbar.superclass.initComponent.call(this);
				this.addEvents("create", "save", "undo", "delete");
			}

		});