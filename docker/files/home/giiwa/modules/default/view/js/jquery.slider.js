/**
 * 
 */
jQuery.extend({
	slider : function(settings) {
		settings = $.extend({
			slides : [],
			dots : [],
			interval : 5000
		}, settings);

		var slides = settings.slides;
		var dots = settings.dots;
		var current = -1;

		if (slides.length == 0)
			return;

		function slide() {
			if (pause) {
				return;
			}

			var p = current + 1;
			if (p >= slides.length) {
				p = 0;
			}
			slides.each(function(i, e) {
				if (i == current) {
					e = $(e);
					dispear(e.find('div'));
				} else if (i == p) {
					e = $(e);
					display(e.find('div'));
				}
			});
			current = p;
			dots.each(function(i, e) {
				if (i == current) {
					$(e).addClass('current');
				} else {
					$(e).removeClass('current');
				}
			});
		}
		function display(es) {
			es.each(function(i, e) {
				var e = $(e);
				var o = e.attr('slide-original');
				if (o) {
					o = eval('({' + o + '})');
					o.left && e.css('left', o.left);
					o.top && e.css('top', o.top);
				}
				var s = e.attr('slide-in');
				if (s) {
					s = eval('({' + s + '})');
					if (s.duration === undefined) {
						s.duration = 1200;
					}
					var pp = {};
					if (s.left !== undefined) {
						pp.left = s.left;
					}
					if (s.top !== undefined) {
						pp.top = s.top;
					}

					if (s.delay) {
						setTimeout(function() {
							e.animate(pp, s.duration, 'easeOutExpo');
						}, s.delay);
					} else {
						e.animate(pp, s.duration, 'easeOutExpo');
					}
				}
			});
		}
		function dispear(es) {
			es.each(function(i, e) {
				var e = $(e);
				var s = e.attr('slide-out');
				if (s) {
					s = eval('({' + s + '})');
					if (s.duration === undefined) {
						s.duration = 1200;
					}
					var pp = {};
					if (s.left !== undefined) {
						pp.left = s.left;
					}
					if (s.top !== undefined) {
						pp.top = s.top;
					}
					if (s.delay) {
						setTimeout(function() {
							e.animate(pp, s.duration, 'easeOutExpo');
						}, s.delay);
					} else {
						e.animate(pp, s.duration, 'easeOutExpo');
					}
				}
			})
		}
		slide();
		setInterval(slide, settings.interval);

		var pause = false;

/*
		dots.each(function(i, e) {
			var e1 = $(e);
			e1.click(function(){
				if (i !== current) {
					// display
					$(dots[current]).removeClass('current');
					dispear($(slides[current]).find('div'));

					current = i;
					$(dots[current]).addClass('current');
					display($(slides[current]).find('div'));

				}
			});
			e1.bind('mouseenter', function() {
				pause = true;
				if (i !== current) {
					// display
					$(dots[current]).removeClass('current');
					dispear($(slides[current]).find('div'));

					current = i;
					$(dots[current]).addClass('current');
					display($(slides[current]).find('div'));

				}
			});
			e1.bind('mouseleave', function() {
				pause = false;
			});
		})
*/			

	}
});
