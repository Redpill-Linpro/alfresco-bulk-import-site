<@markup id="css" >
  <#-- CSS Dependencies -->
  <@link href="${url.context}/res/components/console/rl-bulk-import-site.css" group="console"/>
</@>

<@markup id="js">
  <#-- JavaScript Dependencies -->
  <@script src="${url.context}/res/components/console/consoletool.js" group="console"/>
  <@script type="text/javascript" src="${url.context}/res/components/console/rl-bulk-import-site.js" group="console" />
</@>

<@markup id="widgets">
  <@createWidgets group="console"/>
</@>

<@markup id="html">
  <@uniqueIdDiv>
  	<#assign el=args.htmlid?html>
    <h1>${msg("rl-bulk-import-site.title")}</h1>
    <div id="${el}-datatable"></div>
     <!-- Search button -->
    <span class="yui-button yui-push-button" id="${el}-submitbutton">
       <span class="first-child"><button>${msg("rl-bulk-import-site.start-import")}</button></span>
    </span>

  </@>
</@>
