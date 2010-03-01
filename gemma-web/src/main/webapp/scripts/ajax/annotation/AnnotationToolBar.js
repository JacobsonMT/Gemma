Ext.namespace('Gemma');

/**
 * Gemma.AnnotationToolBar constructor... annotationGrid is the grid that contains the annotations. config is a hash
 * with the following options:
 * 
 * @param createHandler :
 *            a function with arguments (characteristic, id, callback ) where characteristic is the new characteristic
 *            to add, id is the 'owner' and callback is the function to be called when the characteristic has been added
 *            if this argument is not present, there will be no create button in the toolbar
 * 
 * @param deleteHandler :
 *            a function with arguments ( ids, callback ) where ids is an array of characteristic ids to remove and
 *            callback is the function to be called when the characteristics have been removed if this argument is not
 *            present, there will be no delete button in the toolbar
 * 
 * @param saveHandler :
 *            a function with arguments ( characteristics, callback ) where characteristics is an array of
 *            characteristics to update and callback is the function to be called when the characteristics have been
 *            updated if this argument is not present, there will be no save button in the toolbar
 * @version $Id$
 */

Gemma.AnnotationToolBar = Ext.extend(Ext.Toolbar, {

	taxonId : null,

	initComponent : function() {

		if (this.annotationGrid.editable && !this.saveHandler) {
			this.saveHandler = CharacteristicBrowserController.updateCharacteristics;
		}

		var charComboOpts = {
			emptyText : 'Enter term',
			width : 140,
			taxonId : this.taxonId
		};

		if (this.charComboWidth) {
			charComboOpts.width = this.charComboWidth;
		}
		var mgedComboOpts = {
			emptyText : "Select a category",
			width : 130
		};
		if (this.mgedComboWidth) {
			mgedComboOpts.width = this.mgedComboWidth;
		}
		if (this.mgedTermKey) {
			mgedComboOpts.termKey = this.mgedTermKey;
		}

		this.charCombo = new Gemma.CharacteristicCombo(charComboOpts);

		this.mgedCombo = new Gemma.MGEDCombo(mgedComboOpts);

		this.mgedCombo.on("select", function(combo, record, index) {
					this.charCombo.setCategory(record.data.term, record.data.uri);
					this.createButton.enable();
				}, this);

		this.descriptionField = new Ext.form.TextField({
					allowBlank : true,
					invalidText : "Enter a description",
					blankText : "Add a simple description",
					emptyText : "Description",
					width : 75
				});

		if (this.createHandler) {
			this.createButton = new Ext.Toolbar.Button({
				text : "create",
				tooltip : "Adds the new annotation",
				disabled : true,
				handler : function() {
					var characteristic = this.charCombo.getCharacteristic();
					if (this.addDescription) {
						characteristic.description = this.descriptionField.getValue();
					}
					this.annotationGrid.loadMask.show();
					this.createHandler(characteristic, this.annotationGrid.refresh.createDelegate(this.annotationGrid));
					this.charCombo.reset();
					this.descriptionField.reset();
				},
				scope : this
			});
		}

		if (this.deleteHandler) {
			this.deleteButton = new Ext.Toolbar.Button({
						text : "delete",
						tooltip : "Removes the selected annotation",
						disabled : true,
						handler : function() {
							this.deleteButton.disable();
							this.annotationGrid.loadMask.show();
							this.deleteHandler(this.annotationGrid.getSelectedIds(), this.annotationGrid.refresh
											.createDelegate(this.annotationGrid));
						},
						scope : this
					});

		}

		if (this.saveHandler) {
			this.saveButton = new Ext.Toolbar.Button({
						text : "save",
						tooltip : "Saves the updated annotations",
						disabled : true,
						handler : function() {
							this.annotationGrid.loadMask.show();
							this.saveHandler(this.annotationGrid.getEditedCharacteristics(),
									this.annotationGrid.refresh.createDelegate(this.annotationGrid));
							this.saveButton.disable();
						},
						scope : this
					});
			this.annotationGrid.on("afteredit", function(model) {
						this.saveButton.enable();
					}.createDelegate(this));
		}

		Gemma.AnnotationToolBar.superclass.initComponent.call(this);

	},

	afterRender : function(l, r) {
		this.add(this.mgedCombo);
		this.addSpacer();
		this.add(this.charCombo);
		this.addSpacer();

		if (this.addDescription) {
			this.add(this.descriptionField);
		}

		if (this.createHandler) {
			this.add(this.createButton);
		}
		if (this.deleteHandler) {
			this.add(this.deleteButton);
		}
		if (this.saveHandler) {
			this.add(this.saveButton);
		}
		Gemma.AnnotationToolBar.superclass.afterRender.call(this, l, r);
	}

});