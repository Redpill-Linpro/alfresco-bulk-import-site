if (typeof RPL == "undefined" || !RPL) {
  var RPL = {};
}

(function()
{
   /**
  * YUI Library aliases
  */
   var Dom = YAHOO.util.Dom,
    Event = YAHOO.util.Event,
    Element = YAHOO.util.Element,
    KeyListener = YAHOO.util.KeyListener;
   
   /**
  * Alfresco Slingshot aliases
  */
   var $html = Alfresco.util.encodeHTML,
    parseURL = Alfresco.util.parseURL
   
   /**
  * BulkImportSite constructor.
  *
  * @param {String} htmlId The HTML id of the parent element
  * @return {RPL.BulkImportSite} The new BulkImportSite instance
  * @constructor
  */
   RPL.BulkImportSite = function(htmlId)
   {
    this.name = "RPL.BulkImportSite";
    RPL.BulkImportSite.superclass.constructor.call(this, htmlId);
    
    /* Register this component */
    Alfresco.util.ComponentManager.register(this);
    
    /* Load YUI Components */
    Alfresco.util.YUILoaderHelper.require(["button", "container", "datasource", "datatable", "paginator", "json", "history"], this.onComponentsLoaded, this);
   
    /* Define panel handlers */
    var parent = this;
    
    this.selectedPlaces = [];
    this.progress = 0;
    this.failed = 0;

    this.widgets = {};
   
    /* BulkImportSitePanelHandler Panel Handler */
    BulkImportSitePanelHandler = function BulkImportSitePanelHandler()
    {
      BulkImportSitePanelHandler.superclass.constructor.call(this, "bulk-import-site");
    };
    
    YAHOO.extend(BulkImportSitePanelHandler, Alfresco.ConsolePanelHandler, {});
    new BulkImportSitePanelHandler();
    
    return this;

   };

   YAHOO.extend(RPL.BulkImportSite, Alfresco.ConsoleTool,
   {
      /**
       * Places List
       * @property placesList
       * @type {Array}
       */
      placesList: null,

      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function BulkImportSite_onReady()
      {
        // Call super-class onReady() method
        RPL.BulkImportSite.superclass.onReady.call(this);
       
        this.widgets.dataSource = new YAHOO.util.DataSource(Alfresco.constants.PROXY_URI + "redpill/bulkimporttool/sites",
        {
          responseType: YAHOO.util.DataSource.TYPE_JSON,
          responseSchema:
          {
            resultsList: "sites",
            metaFields:
            {
              recordOffset: "startIndex",
              totalRecords: "totalResults",
              searchElapsedTime: "searchElapsedTime"
            }
          }
        });
        this._setupDataTable();

        Alfresco.util.createYUIButton(this,"submitbutton", this.onSubmitClick);

      },
      _setupDataTable: function BulkImportSite_setupDataTable()
      {
        /**
         * Generic HTML-safe custom datacell formatter
         */
        var renderCellSafeHTML = function renderCellSafeHTML(elCell, oRecord, oColumn, oData)
        {
           elCell.innerHTML = $html(oData);
        };

        var renderCheckbox = function renderCellSafeHTML(elCell, oRecord, oColumn, oData)
        {
           if (oData===true) {
            elCell.innerHTML = '<input type="checkbox" CHECKED="CHECKED" DISABLED="DISABLED">';
           } else {
            elCell.innerHTML = '<input type="checkbox" DISABLED="DISABLED">';
           }
        };

        // DataTable column defintions
        var columnDefinitions =
        [
           { key: "id", label: '<input type="checkbox" id="'+this.id+'-select-all" class="protocol-select-all" />', sortable: false, formatter: "checkbox" },
           { key: "imported", label: this.msg("label.imported"), sortable: true, formatter: renderCheckbox },
           { key: "title", label: this.msg("label.title"), sortable: true, formatter: renderCellSafeHTML },
           { key: "description", label: this.msg("label.description"), sortable: true, formatter: renderCellSafeHTML },
           { key: "shortName", label: this.msg("label.shortname"), sortable: true, formatter: renderCellSafeHTML },
           { key: "type", label: this.msg("label.type"), sortable: true, formatter: renderCellSafeHTML },
           { key: "preset", label: this.msg("label.preset"), sortable: true, formatter: renderCellSafeHTML }
        ];

        // DataTable definition
        this.widgets.dataTable = new YAHOO.widget.DataTable(this.id + "-datatable", columnDefinitions, this.widgets.dataSource,
        {
           initialLoad: true,
           renderLoopSize: 32,
           sortedBy:
           {
              key: "shortName",
              dir: "asc"
           },
           MSG_EMPTY: this.msg("message.empty")
        });
        var me = this;

        //Add remove selected row to global selection list
        this.widgets.dataTable.subscribe("checkboxClickEvent", function(oArgs){ 
          var elCheckbox = oArgs.target; 
          var oRecord = this.getRecord(elCheckbox); 
          if (oRecord.getData().imported!==true) {
            oRecord.setData("check",elCheckbox.checked); 
            var shortName = oRecord.getData().shortName;
            if (elCheckbox.checked) {
              me.selectedPlaces.push(shortName);
            } else {
              for (var i=0;i<me.selectedPlaces.length;i++) {
                var obj = me.selectedPlaces[i];
                if (obj===shortName) {
                  me.selectedPlaces.splice(i, 1);
                  break;
                }
              }
            }
          }
        });

        var selectAllCheckbox = Dom.get(this.id+'-select-all');
        selectAllCheckbox.onchange = this.onSelectAllChange;
      },

      onSelectAllChange: function BulkImportSite_onSelectAllChange(a, b, c) {
        //alert("onSelectAllChange");

        var checkBoxes = this.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.getElementsByClassName("yui-dt-checkbox");
        for (var i=0;i<checkBoxes.length;i++) {
          var checkBox = checkBoxes[i];
          if ((checkBox.checked===true && this.checked===false) || 
             (checkBox.checked===false && this.checked===true)) {
            checkBox.click();
          }
        }
        //this.parentElement.parentElement.parentElement.parentElement.parentElement.parentElement.getElementsByClassName("yui-dt-checkbox")[0].checked=false
      },

      onSubmitClick: function BulkImportSite_onSubmitClick()  {
        this.progress = 0;
        this.failed = 0;
        this.showPopup();
        this.doImport();
      },

      showPopup: function BulkImportSite_showPopup() {
        var thetext = this.msg("label.progress")+': <span id="'+this.id+'-processed">0</span>/'+this.selectedPlaces.length+'<br />';
        thetext += this.msg("label.failed")+': <span id="'+this.id+'-failed">0</span>';
        Alfresco.util.PopupManager.displayPrompt({
          title : this.msg("label.importstatus"),
          text : thetext,
          modal : true,
          noEscape : true
        });
      },

      doImport: function BulkImportSite_doImport() {
        var me = this;
        if (this.selectedPlaces.length > 0) {
          var shortName = this.selectedPlaces[0];
          this.selectedPlaces.splice(0, 1);
          Alfresco.util.Ajax.request(
            {
              url: Alfresco.constants.PROXY_URI + "redpill/bulkimporttool/sites/" + shortName,
              method: Alfresco.util.Ajax.POST,
              successCallback:
              {
                fn: this.onImportSuccess,
                scope: me
              },
              failureCallback:
              {
                fn: this.onImportFailure,
                scope: me
              }
          });
        }
      },

      onImportSuccess: function BulkImportSite_onImportSuccess(p_obj)
      {
        this.progress++;
        var processedDomItem = Dom.get(this.id + "-processed");
        if (processedDomItem===null) {
          return;
          //this.showPopup();
        }
        this.updateUiStatistics();
        this.doImport();
      },

      onImportFailure: function BulkImportSite_onImportSuccess(p_obj)
      {
        this.progress++;
        this.failed++;
        var processedDomItem = Dom.get(this.id + "-processed");
        if (processedDomItem===null) {
          return;
          //this.showPopup();
        }
        this.updateUiStatistics();
        this.doImport();
      },

      updateUiStatistics: function BulkImportSite_onImportSuccess() {
        Dom.get(this.id + "-processed").innerHTML = this.progress;
        Dom.get(this.id + "-failed").innerHTML = this.failed;
      }

   });
})();