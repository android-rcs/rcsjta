
<?cs set:temproot=toroot?>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<link rel="shortcut icon" type="image/x-icon" href="<?cs var:toroot ?>favicon.ico" />
<title><?cs 
  if:page.title ?><?cs 
    var:page.title ?> | <?cs
  /if ?>joyn SDK</title>


</script><?cs 
if:reference ?>
<?cs set:doc.type = "source"?>

<?cs set:toroot = "../" + toroot?>
<script src="<?cs var:toroot ?>assets/android-developer-reference.js" type="text/javascript"></script>

<?cs /if ?>
<link href="<?cs var:toroot ?>assets/android-developer-docs-devguide.css" rel="stylesheet" type="text/css" />
<link href="<?cs var:toroot ?>assets/android-developer-docs.css" rel="stylesheet" type="text/css" />
<link href="<?cs var:toroot ?>assets/joyn-sdk.css" rel="stylesheet" type="text/css" />
<link href="<?cs var:toroot ?>assets/skin-carousel-ri.css" rel="stylesheet" type="text/css" />
<link href="<?cs var:toroot ?>assets/skin-carousel-home.css" rel="stylesheet" type="text/css" />
<script src="<?cs var:toroot ?>assets/jquery-1.10.2.min.js" type="text/javascript"></script>
<script src="<?cs var:toroot ?>assets/search_autocomplete.js" type="text/javascript"></script>
<script src="<?cs var:toroot ?>assets/jquery-resizable.min.js" type="text/javascript"></script>
<script src="<?cs var:toroot ?>assets/android-developer-docs.js" type="text/javascript"></script>
<script src="<?cs var:toroot ?>assets/prettify.js" type="text/javascript"></script>
<script src="<?cs var:toroot ?>assets/joyn-sdk.js" type="text/javascript"></script>
<script src="<?cs var:toroot ?>assets/carousel.js" type="text/javascript"></script>
<script type="text/javascript" src="<?cs var:toroot ?>assets/jquery.jcarousel.min.js"></script>
<script type="text/javascript" src="<?cs var:toroot ?>assets/widget.js"></script>
<script type="text/javascript">
  setToRoot("<?cs var:toroot ?>");
</script>
<noscript>
  <style type="text/css">
    html,body{overflow:auto;}
    #body-content{position:relative; top:0;}
    #doc-content{overflow:visible;border-left:3px solid #666;}
    #side-nav{padding:0;}
    #side-nav .toggle-list ul {display:block;}
    #resize-packages-nav{border-bottom:3px solid #666;}
  </style>
</noscript>
<script type="text/javascript">
	function mycarousel_initCallback(carousel) {
		jQuery('.jcarousel-control a').bind('click', function() {
			if(jQuery(this).attr('id') != ''){
				carousel.scroll(jQuery.jcarousel.intval(jQuery(this).attr('id')));
				return false;
			}
		});
		 // Disable autoscrolling if the user clicks the prev or next button.
		carousel.buttonNext.bind('click', function() {
			carousel.startAuto(0);
		});

		carousel.buttonPrev.bind('click', function() {
			carousel.startAuto(0);
		});

		// Pause autoscrolling if the user moves with the cursor over the clip.
		carousel.clip.hover(function() {
			carousel.stopAuto();
		}, function() {
			carousel.startAuto();
		});
	 };
	 
	 function mycarousel_itemVisibleInCallback(a1,a2,index,a4) {
		$('#' + index).find(">:first-child").css('background-color','#ff6600');
	 };
	 
	 function mycarousel_itemVisibleOutCallback(a1,a2,index,a4) {
		$('#' + index).find(">:first-child").css('background-color',null);
	 };
	jQuery(document).ready(function() {
		jQuery('#mycarousel').jcarousel({
			scroll: 1,
			initCallback: mycarousel_initCallback,
			itemVisibleInCallback: {
			  onBeforeAnimation: mycarousel_itemVisibleInCallback			  
			},
			itemVisibleOutCallback: {
			  onBeforeAnimation: mycarousel_itemVisibleOutCallback			  
			},
			auto: 5,
			wrap: 'last'
			
		});
	});
	
	jQuery(document).ready(function() {
		jQuery('#carousel-home').jcarousel({
			scroll: 3,
			vertical:true
		});
	});

</script>
</head>

<?cs set:toroot = temproot?>