<#include "../include/imports.ftl">

<#-- @ftlvariable name="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu" -->
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#if menu??>
<div class="has-edit-button">
  <#if menu.siteMenuItems??>
    <ul class="nav nav-pills">
      <#list menu.siteMenuItems as item>
        <#if  item.selected || item.expanded>
          <li class="active"><a href="<@hst.link link=item.hstLink/>">${item.name?html}</a></li>
        <#else>
          <li><a href="<@hst.link link=item.hstLink/>">${item.name?html}</a></li>
        </#if>
      </#list>
    </ul>
  </#if>
  <@hst.cmseditmenu menu=menu/>
</div>
<#else>
  <#if editMode>
    <h5>[Menu Component]</h5>
    <sub>Click to edit Menu</sub>
  </#if>
</#if>