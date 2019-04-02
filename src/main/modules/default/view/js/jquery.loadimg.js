jQuery.extend( {
	loadimg : function(a, width, show) {
		var e = $(a);
		if (e.attr('g') == '1' || e.attr('l') == '1') {
			return;
		}
		var img = new Image();
		img.onload = function() {
			if (e.attr('g') == '1' || e.attr('l') == '1') {
				return;
			}
			e.attr('g', '1');
			e.attr('l', '1');
			var w = img.width;
			var h = img.height;

			if (width > 0) {
				if (w == h) {
					e.attr('width', width);
					e.attr('height', width);
					e.css('margin', "0");
				} else if (w > h) {
					h = parseInt(h / w * width);
					var d = parseInt((width - h) / 2);
					e.attr("height", h);
					e.css('margin', d + "px 0 0 0");
					e.attr('width', width);
				} else {
					w = parseInt(w / h * width);
					var d = parseInt((width - w) / 2);
					e.attr("width", w);
					e.attr('height', width);
					e.css('margin', "0");// 0 0 " + d + "px");
				}
			}
			e.hide();
			e.attr('src', img.src);
			if (show != 0) {
				e.attr('seq', 1);
				e.fadeIn(1000);
			} else {
				e.attr('seq', 0);
			}
		};
		if (typeof e.attr('g') != 'undefined') {
			img.src = e.attr('g');
		} else if (typeof e.attr('l') != 'undefined') {
			if ($.browser.msie && $.browser.version < 9) {
				img.src = e.attr('l');
			} else {
				$(window).scroll(function() {
					if (e.attr('g') == '1' || e.attr('l') == '1') {
						return;
					}
					var h = $(window).height() + $(window).scrollTop();
					if (e.offset().top < (h + 100)) {
						img.src = e.attr('l');
					}
				});
			}
		}
	},
	loadimg2 : function(a, width, show) {
		var e = $(a);
		if (e.attr('g') == '1' || e.attr('l') == '1') {
			return;
		}
		var img = new Image();
		img.onload = function() {
			if (e.attr('g') == '1' || e.attr('l') == '1') {
				return;
			}
			e.attr('g', '1');
			e.attr('l', '1');
			var w = img.width;
			var h = img.height;

			if (width > 0) {
				h = parseInt(width / w * h);
				e.attr("height", h);
				e.attr('width', width);
				e.css('margin', "0");
			}
			e.hide();
			e.attr('src', img.src);
			if (show != 0) {
				e.attr('seq', 1);
				e.fadeIn(1000);
			} else {
				e.attr('seq', 0);
			}
		};
		if (typeof e.attr('g') != 'undefined') {
			img.src = e.attr('g');
		} else if (typeof e.attr('l') != 'undefined') {
			if ($.browser.msie && $.browser.version < 9) {
				img.src = e.attr('l');
			} else {
				$(window).scroll(function() {
					if (e.attr('g') == '1' || e.attr('l') == '1') {
						return;
					}
					var h = $(window).height() + $(window).scrollTop();
					if (e.offset().top < (h + 100)) {
						img.src = e.attr('l');
					}
				});
			}
		}
	}
});
