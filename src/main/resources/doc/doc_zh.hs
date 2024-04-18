<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "http://java.sun.com/products/javahelp/helpset_2_0.dtd">
<helpset version="2.0">
   <!-- title --> 
   <title>Logisim - 帮助</title>
			
   <!-- maps --> 
   <maps>
     <homeID>top</homeID>
     <mapref location="map_zh.jhm" />
   </maps>
	
   <!-- views --> 
   <view xml:lang="zh" mergetype="javax.help.UniteAppendMerge">
      <name>TOC</name>
      <label>目录</label>
      <type>javax.help.TOCView</type>
      <data>zh/contents.xml</data>
   </view>

  <view>
    <name>Search</name>
    <label>查找</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">search_lookup_en</data>
  </view>

	<view>
		<name>Favorites</name>
		<label>收藏</label>
		<type>javax.help.FavoritesView</type>
	</view> 

   <!-- presentation windows -->

   <!-- This window is the default one for the helpset. 
     *  It is a tri-paned window because displayviews, not
     *  defined, defaults to true and because a toolbar is defined.
     *  The toolbar has a back arrow, a forward arrow, and
     *  a home button that has a user-defined image.
   -->
   <presentation default=true>
       <name>main window</name> 
       <location x="200" y="10" />
       <toolbar>
           <helpaction>javax.help.BackAction</helpaction>
           <helpaction>javax.help.ForwardAction</helpaction>
           <helpaction image="homeicon">javax.help.HomeAction</helpaction>
           <helpaction>javax.help.SeparatorAction</helpaction>
           <helpaction>javax.help.FavoritesAction</helpaction>
       </toolbar>
   </presentation>

   <!-- implementation section -->
   <impl>
      <helpsetregistry> helpbrokerclass="javax.help.DefaultHelpBroker" </helpsetregistry>
      <viewerregistry> viewertype="text/html" 
         viewerclass="com.sun.java.help.impl.CustomKit" </viewerregistry>
      <viewerregistry> viewertype="text/xml" 
         viewerclass="com.sun.java.help.impl.CustomXMLKit" </viewerregistry>
   </impl>
</helpset>
