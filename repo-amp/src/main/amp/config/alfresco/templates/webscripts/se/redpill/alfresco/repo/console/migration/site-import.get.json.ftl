{
"sites": [
<#escape x as jsonUtils.encodeJSONString(x)>
	<#list places as place>
		{
			"title" : "${place.title}",
			"description" : "${place.description}",
			"shortName" : "${place.shortName}",
			"type" : "${place.type}",
			"preset" : "${place.preset}",
			"visibility" : "${place.visibility}",
			"imported" : ${place.imported?string}
		}<#if place_has_next>,</#if>
	</#list>
</#escape>
]
}