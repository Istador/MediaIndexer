/*
	Quelle: http://mathiasbynens.be/notes/html5-details-jquery
*/
var isDetailsSupported = (function(doc) {
  var el = doc.createElement('details'),
      fake,
      root,
      diff;
  if (!('open' in el)) {
    return false;
  }
  root = doc.body || (function() {
    var de = doc.documentElement;
    fake = true;
    return de.insertBefore(doc.createElement('body'), de.firstElementChild || de.firstChild);
  }());
  el.innerHTML = '<summary>a</summary>b';
  el.style.display = 'block';
  root.appendChild(el);
  diff = el.offsetHeight;
  el.open = true;
  diff = diff != el.offsetHeight;
  root.removeChild(el);
  if (fake) {
    root.parentNode.removeChild(root);
  }
  return diff;
}(document));



$(document).ready(function() {if(localStorage){

	// <details> wird vom Browser unterstützt
	if(isDetailsSupported){
		
		//initial details laden
		$("details").each(function(){
			this.open = (localStorage[this.id] === "true");
		});
		
		//auf-/zuklappen von details -> speichern
		$("details").bind("DOMSubtreeModified", function(){
			localStorage[this.id] = this.open;
		});
		
	}
	// <details> wird NICHT vom Browser unterstützt
	else {
		
		//initial details laden
		$("details").each(function(){
			var id = this.id;
			var open = (localStorage[id] === "true");
			
			//für alle <details> Kind-Elemente ohne <summary>
			$(this).children(":not(summary)").each(function(){
				if(open)
					//aufklappen
					$(this).css("display", "block");
				else
					//zuklappen
					$(this).css("display", "none");
			});
			
			//Gliederungselement hinzufügen
			$(this).children("summary").each(function(){
				if(open)
					$(this).html("<span>&#x25bc;</span> " + $(this).html());
				else
					$(this).html("<span>&#x25b6;</span> " + $(this).html());
			});
			
		});
		
		//klicken auf <summary> -> auf-/zuklappen und speichern
		$("summary").click(function(){
			
			var open = false;
			
			//für alle <details> Kind-Elemente ohne <summary>
			$(this).parent().children(":not(summary)").each(function(){
				//aktueller Status
				open = $(this).css("display") === "none";
				
				if($(this).css("display") === "none")
					//aufklappen
					$(this).css("display", "block");
				else
					//zuklappen
					$(this).css("display", "none");
			});
			
			//Gliederungselement switchen
			if(open){
				$(this).children(":first(span)").html("&#x25bc;");
				
			} else {
				$(this).children(":first(span)").html("&#x25b6;");
			}
			
			//speichern im lokalem Speicher
			localStorage[$(this).parent().attr("id")] = open;
		});
		
	}
	
	
	//initial checkboxen laden
	$(":checkbox").each(function(){
		this.checked = (localStorage[this.name] === "true");
	});
	
	//ändern der checkbox -> speichern
	$(":checkbox").change(function() {
		localStorage[this.name] = this.checked;
	});
	
	//check all
	$("#check").click(function(){
		$(":checkbox").each(function(){
			this.checked = true;
			localStorage[this.name] = true;
		});
	});
	
	//uncheck all
	$("#uncheck").click(function(){
		$(":checkbox").each(function(){
			this.checked = false;
			localStorage[this.name] = false;
		});
	});
	
	//reset speicher
	$("#reset").click(function(){
		localStorage.clear();
		window.location.reload();
	});
	
}});
