Ext.namespace('Gemma');

/**
 * Gemma.PagingToolbar is an extension of Ext.PagingToolbar that compensates
 * for a bug in how the current active page is calculated.
 * 
 * <pre>
 * Is this bug fixed??
 * </pre>
 * 
 * An alternative to using this class would be to patch the Ext code thusly:
 * 
 * --- gemma-web/src/main/webapp/scripts/ext/ext-all-debug.js +++
 * gemma-web/src/main/webapp/scripts/ext/ext-all-debug.js (FIXED) @@ -15764,7
 * +15764,7 @@
 * 
 * onLoad : function(ds, r, o){ - this.cursor = o.params ? o.params.start : 0; +
 * this.cursor = o.params ? ( o.params.start !=== undefined ? 0.params.start : 0 ) :
 * 0; var d = this.getPageData(), ap = d.activePage, ps = d.pages;
 * 
 */
Gemma.PagingToolbar = function(config) {

	Gemma.PagingToolbar.superclass.constructor.call(this, config);

};

Ext.extend(Gemma.PagingToolbar, Ext.PagingToolbar, {

	onLoad : function(ds, r, o) {
		/*
		 * temporarily set options.params.start to 0 if it's undefined as it
		 * won't be caught by the test in Ext.PagingToolbar.onLoad...
		 */
		var definedStartParameter = false;
		if (o.params && o.params[this.paramNames.start] === undefined) {
			o.params[this.paramNames.start] = 0;
			definedStartParameter = true;
		}

		Gemma.PagingToolbar.superclass.onLoad.call(this, ds, r, o);

		/*
		 * if we defined options.parm.start above, undefine it so we don't
		 * change behaviour elsewhere...
		 */
		if (definedStartParameter) {
			delete o.params[this.paramNames.start];
		}
	},

	bind : function(store) {
		Gemma.PagingToolbar.superclass.bind.call(this, store);
		store = Ext.StoreMgr.lookup(store);
		store.on("add", this.onAdd, this);
		this.store = store;
	},

	// private
	onAdd : function(store, r, o) {
		if (!this.rendered) {
			this.dsLoaded = [store, r, o];
			return;
		}
		this.cursor = (o && o.params) ? o.params[this.paramNames.start] : 0;
		var d = this.getPageData(), ap = d.activePage, ps = d.pages;

		this.afterTextEl.el.innerHTML = String.format(this.afterPageText,
				d.pages);
		this.field.dom.value = ap;
		this.first.setDisabled(ap == 1);
		this.prev.setDisabled(ap == 1);
		this.next.setDisabled(ap == ps);
		this.last.setDisabled(ap == ps);
		this.loading.enable();
		this.updateInfo();
	}

});