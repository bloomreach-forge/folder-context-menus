<#include "../include/imports.ftl">

<@hst.setBundle basename="essentials.homepage"/>
<#assign isPreview=hstRequest.requestContext.preview/>
<#assign isChannelManagerPreviewRequest=hstRequest.requestContext.channelManagerPreviewRequest/>

<div>
  <h1><@fmt.message key="homepage.title" var="title"/>${title?html}</h1>
  <p><@fmt.message key="homepage.text" var="text"/>${text?html}</p>
  <#if !isChannelManagerPreviewRequest>
    <p>
      [This text can be edited
      <a href="http://localhost:8080/cms/?1&path=/content/documents/administration/labels/homepage" target="_blank">here</a>.]
    </p>
  </#if>
</div>
<div>
  <@hst.include ref="container"/>
</div>