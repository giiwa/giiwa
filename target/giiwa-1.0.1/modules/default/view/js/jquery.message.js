jQuery.extend({
	message : function(message, onclick) {
		$('#error.leanmodal').hide();
		var m = $('#message.leanmodal');
		if (m.length == 0) {
			m = $("<div id='message' class='leanmodal'><div class='leanmodal-header'></div><div class='leanmodal-content'></div></div>");
			$('body').append(m);
		}
		m.find('.leanmodal-content').html(message);
		var overlay = $('#lean_overlay');
		if (overlay.length == 0) {
			overlay = $("<div id='lean_overlay'></div>");
			$("body").append(overlay);
		}
		overlay.css({
					'display' : 'block',
					opacity : 0.5
				});
		m.css({
					'display' : 'block'
				});
		overlay.click(function() {
					overlay.hide();
					$('.leanmodal').hide();
					
					onclick && onclick();
				});
		m.click(function() {
					overlay.click();
				});
	},
	
	hint : function(message, delay) {
		if(!delay) {
			delay = 2000;
		}
		$('#error.leanmodal').hide();
		$('#message.leanmodal').hide();
		var m = $('#hint.leanmodal');
		if (m.length == 0) {
			m = $("<div id='hint' class='leanmodal'><div class='leanmodal-content'></div></div>");
			$('body').append(m);
		}
		m.find('.leanmodal-content').html(message);
		m.css({
					'display' : 'block'
				});
		setTimeout(function(){
			m.fadeOut();
		}, delay);
	},
	
	warn : function(message, delay) {
		if(!delay) {
			delay = 2000;
		}
		var m = $('#warn.leanmodal');
		if (m.length == 0) {
			m = $("<div id='warn' class='leanmodal'><div class='leanmodal-content'></div></div>");
			$('body').append(m);
		}
		m.find('.leanmodal-content').html(message);
		m.css({
					'display' : 'block'
				});
		setTimeout(function(){
			m.fadeOut();
		}, delay);
	},	
	error : function(message, onclick) {
		$('#message.leanmodal').hide();
		var m = $('#error.leanmodal');
		if (m.length == 0) {
			m = $("<div id='error' class='leanmodal'><div class='leanmodal-header'></div><div class='leanmodal-content'></div></div>");
			$('body').append(m);
		}
		m.find('.leanmodal-content').html(message);
		var overlay = $('#lean_overlay');
		if (overlay.length == 0) {
			overlay = $("<div id='lean_overlay'></div>");
			$("body").append(overlay);
		}
		overlay.css({
					'display' : 'block',
					opacity : 0.5
				});
		m.css({
					'display' : 'block'
				});
		overlay.click(function() {
					overlay.hide();
					$('.leanmodal').hide();

					onclick && onclick();
				});
		m.click(function() {
					overlay.click();
				});
	},
	confirm : function(title, message) {
	}
});
