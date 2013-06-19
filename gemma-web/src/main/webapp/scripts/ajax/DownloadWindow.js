Ext.namespace('Gemma');

/**
 */
Gemma.DownloadWindow = Ext.extend(Ext.Window, {
      // maybe should make a delim field so it's easily changed
      dataToFormat : null, // should be overridden by instantiation configs
      bodyStyle : 'padding: 7px',
      width : 800,
      height : 400,
      layout : 'fit',

      replaceDelimiterWithSpace : function(str) {
         return str.replace('\t', ' ');
      },

      timeToString : function(timeStamp) {
         // Make minutes double digits.
         var min = (timeStamp.getMinutes() < 10) ? '0' + timeStamp.getMinutes() : timeStamp.getMinutes();
         return timeStamp.getFullYear() + "/" + (timeStamp.getMonth() + 1) + "/" + timeStamp.getDate() + " " + timeStamp.getHours() + ":" + min;
      },

      convertToText : function() {
         var text = '# Generated by Gemma\n' + '# ' + this.timeToString(new Date()) + '\n' + '# \n' + '# ' + String.format(Gemma.CITATION_DIRECTIONS, '\n# ') + '\n' + '# \n'
            + '# This functionality is currently in beta. The file format may change in the near future. \n' + '# Fields are delimited by tabs.\n' + '# \n' + "# "
            + this.downloadDataHeader + "\n";

         for (var i = 0; i < this.downloadData.length; i++) {
            var downloadDataRow = this.downloadData[i];
            text += downloadDataRow.join('\t') + "\n";
         }
         // TODO: should use it in the future.
         // this.replaceDelimiterWithSpace(text);

         this.show();
         this.textAreaPanel.update({
               text : text
            });
      },

      initComponent : function() {
         Ext.apply(this, {
               title : 'Text Display of ' + this.windowTitleSuffix,
               downloadDataHeader : this.downloadDataHeader,
               downloadData : this.downloadData,

               tbar : [{
                     ref : 'selectAllButton',
                     xtype : 'button',
                     text : 'Select All',
                     scope : this,
                     handler : function() {
                        this.textAreaPanel.selectText();
                     }
                  }],
               items : [new Ext.form.TextArea({
                     ref : 'textAreaPanel',
                     readOnly : true,
                     // tabs don't show up in Chrome, but \t doesn't work any better than \t
                     // \n line breaks don't show up in IE9 but are there if the text is pasted into excel
                     // (using <br> instead will work in IE9, but not in FF or excel)
                     tpl : new Ext.XTemplate('<tpl>', '{text}', '</tpl>'),
                     // tplWriteMode: 'append',
                     bodyStyle : 'white-space: nowrap',
                     style : 'white-space: nowrap',
                     wordWrap : false,
                     padding : 7,
                     autoScroll : true
                  })]
            });

         Gemma.DownloadWindow.superclass.initComponent.call(this);
      }

   });
