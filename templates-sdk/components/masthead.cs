
<?cs # The default search box that goes in the header ?><?cs 
def:default_search_box() ?>

		<?cs set:temproot=toroot?>
		<?cs if:reference?>
		<?cs set:temproot=toroot?>
		<?cs else ?>
		<?cs set:toroot = toroot + "javadoc/"?>
		<?cs /if ?>
		  <div id="search" >
			  <div id="searchForm">
				  <form accept-charset="utf-8" class="gsc-search-box" 
						>
					
							<input id="search_autocomplete" class="gsc-input" type="text" size="33" autocomplete="off"
							  title="search javadoc" name="q"
							  value="search javadoc"
							  onFocus="search_focus_changed(this, true)"
							  onBlur="search_focus_changed(this, false)"
							  onkeydown="return search_changed(event, true, '<?cs var:toroot?>')"
							  onkeyup="return search_changed(event, false, '<?cs var:toroot?>')" />
						  <div id="search_filtered_div" class="no-display">
							  <table id="search_filtered" cellspacing=0>
							  </table>
						  </div>
						  
				  </form>
			  </div><!-- searchForm -->
		  </div><!-- search -->
		  
		  <?cs set:toroot = temproot?>
<?cs 
/def ?>

<?cs # The default API filter selector that goes in the header ?><?cs
def:default_api_filter() ?><?cs
  if:reference.apilevels ?>
  <div id="api-level-toggle">
    <input type="checkbox" id="apiLevelCheckbox" onclick="toggleApiLevelSelector(this)" />
    <label for="apiLevelCheckbox" class="disabled">Filter by API Level: </label>
	</br>
    <select id="apiLevelSelector">
      <!-- option elements added by buildApiLevelSelector() -->
    </select>
  </div>
  <script>
   var SINCE_DATA = [ <?cs 
      each:since = since ?>'<?cs 
        var:since.key ?>'<?cs 
        if:!last(since) ?>, <?cs /if ?><?cs
      /each 
    ?> ];
    
    var SINCE_LABELS = [ <?cs 
      each:since = since ?>'<?cs 
        var:since.name ?>'<?cs 
        if:!last(since) ?>, <?cs /if ?><?cs
      /each 
    ?> ];
    buildApiLevelSelector();
    addLoadEvent(changeApiLevel);
  </script>
<?cs /if ?>
<?cs /def ?>




<?cs 
def:custom_masthead() ?>
<?cs if:doc.type != "overview" ?>
<div style="min-width: 1402px;width:100%;border-bottom:2px solid #ff6600;">
<div style="width:1402px;margin:auto;">
  <div id="header">
  
      <div id="headerRight">
         <?cs 
          call:default_api_filter() ?>
    	 <?cs 	    
          call:default_search_box() ?>
      </div><!-- headerRight -->
      <div id="headerLeft">
		<?cs set:temproot=toroot?>
		<?cs if:reference ?>
		<?cs set:toroot = "../" + toroot?>
		<?cs /if ?>
          <a style="text-decoration:none;" href="<?cs var:toroot?>index.html" tabindex="-1"><img
              src="<?cs var:toroot ?>assets/images/joyn_logo_sdk.png" alt="joyn SDK" />SDK for Android</a>
          <ul class="<?cs if:home ?>home<?cs
                      elif:(doc.type == "source" || doc.type == "apilevel") ?>source<?cs
                      elif:doc.type == "guides" ?>guides<?cs
                      elif:doc.type == "samples" ?>samples<?cs
                      elif:doc.type == "tools" ?>tools<?cs
                      elif:doc.type == "faq" ?>faq<?cs
					  elif:doc.type == "tutorials" ?>tutorials<?cs
                      elif:doc.type == "download" ?>download<?cs /if ?>">
              <li id="home-link"><a href="<?cs var:toroot ?>index.html"><span>Home</span></a></li>
              <li id="source-link"><a href="<?cs var:toroot ?>javadoc/index.html"
                                  onClick="return loadLast('source')"><span>Javadoc</span></a></li>
              <li id="samples-link"><a href="<?cs var:toroot ?>samples/index.html"
                                  onClick="return loadLast('samples')"><span>Samples</span></a></li>
			  <li id="tutorials-link"><a href="<?cs var:toroot ?>tutorials/index.html"
                                  onClick="return loadLast('tutorials')"><span>Tutorials</span></a></li>
              <li id="tools-link"><a href="<?cs var:toroot ?>tools/index.html"
                                  onClick="return loadLast('tools')"><span>Tools</span></a></li>
              <!--<li id="faq-link"><a href="<?cs var:toroot ?>FAQ/index.html"
                                  onClick="return loadLast('faq')"><span>FAQ</span></a></li>-->
              <li id="download-link"><a href="<?cs var:toroot ?>download/index.html"
                                  onClick="return loadLast('download')"><span>Downloads</span></a></li>
			  <li id="faq-link"><a href="<?cs var:toroot ?>support.html"
                                  onClick="return loadLast('faq')"><span>Support</span></a></li>
          </ul> 
		  <?cs set:toroot = temproot?>
      </div>
      
  </div><!-- header -->
  </div>
  </div>
  <?cs 
          call:static_side_nav() ?>
  <?cs /if ?>

<?cs 
/def ?>

<?cs 
def:static_side_nav() ?>
	<?cs if:doc.type == "samples" ?>
	<div style="margin: 0;position: relative;width: 100%;">
		<div class="background-sdk" style="padding: 0;">
			<div class="content-block nav-block">
				<div id="guide-nav" class="nav">
					<p <?cs if:page.title == "Samples" ?>class="selected"<?cs /if ?> style="margin-top: 15px;"><a  href="<?cs var:toroot ?>samples/index.html"><b>Samples</b></a></p>
					<ul>
						<li>
							<div <?cs if:page.title == "How to connect to a joyn service" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>samples/connectService.html" >> How to connect to a joyn service</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "How to initialise a chat" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>samples/initChat.html" >> How to initialise a chat</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "Get online contacts" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>samples/listContacts.html">> How to get online joyn contacts</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "How to get joyn contacts supporting a given service" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>samples/serviceSupported.html">> How to get joyn contacts supporting a given service</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "Detect if joyn service is started" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>samples/detectServices.html">> How to detect if the joyn service is started</a>
							</div>
						</li>
					</ul>
				</div>
			</div>
		</div>
	</div>
	<?cs elif:doc.type == "tutorials" ?>
	<div style="margin: 0;position: relative;width: 100%;">
		<div class="background-sdk" style="padding: 0;">
			<div class="content-block nav-block" >
				<div id="tutorial-nav" class="nav">
					<p <?cs if:page.title == "Tutorials" ?>class="selected"<?cs /if ?> style="margin-top: 15px;"><a  href="<?cs var:toroot ?>tutorials/index.html"><b>Tutorials</b></a></p>
					<ul>
						<li>
							<div <?cs if:page.title == "Text to speech application" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>tutorials/ttsApp.html">> Text to speech application</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "New multimedia Capability" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>tutorials/multiCapability.html">> New multimedia Capability</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "Popup application" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>tutorials/popUpApp.html">> Popup application</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "New multimedia application" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>tutorials/multiApp.html">> New multimedia application</a>
							</div>
						</li>
						
					</ul>
				</div>
			</div>
		</div>
	</div>
	<?cs elif:doc.type == "tools" ?>
	<div style="margin: 0;position: relative;width: 100%;">
		<div class="background-sdk" style="padding: 0;">
			<div class="content-block nav-block" >
				<div id="tools-nav" class="nav">
					<p <?cs if:page.title == "Tools" ?>class="selected"<?cs /if ?> style="margin-top: 15px;"><a  href="<?cs var:toroot ?>tools/index.html"><b>Tools</b></a></p>
					<ul>
						<li>
							<div <?cs if:page.title == "Install joyn standalone service" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>tools/installJoynServices.html">> Standalone joyn service</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "Get to know joyn services via the RI application" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>tools/getToKnow.html">> RI</a>
							</div>
						</li>						
					</ul>
				</div>
			</div>
		</div>
	</div><?cs elif:doc.type == "apilevel" ?>
	<div style="margin: 0;position: relative;width: 100%;">
		<div class="background-sdk" style="padding: 0;">
			<div class="content-block nav-block" >
				<div id="tools-nav" class="nav">
					<p  style="margin-top: 15px;"><a  href=""><b>API Levels</b></a></p>
					<ul>
						<li>
							<div <?cs if:page.title == "API version Albatros" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>releases/Albatros.html">> Albatros</a>
							</div>
						</li>
						<li>
							<div <?cs if:page.title == "API version Blackbird" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>releases/Blackbird.html">> Blackbird</a>
							</div>
						</li>						
						<li>
							<div <?cs if:page.title == "API version Crane" ?>class="selected"<?cs /if ?>>
								<a href="<?cs var:toroot ?>releases/Crane.html">> Crane</a>
							</div>
						</li>						
					</ul>
				</div>
			</div>
		</div>
	</div>
	<?cs /if ?>
<?cs 
/def ?>