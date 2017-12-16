window.porlet = {};
porlet.extend = function(m) {
	for ( var k in m) {
		porlet[k] = m[k];
	}
};
porlet.extend({
	that : false,

	load : function(panel, cb) {
		panel.find('.porlet').each(function(i, e) {
			e = $(e);
			if (e.attr('data-loaded') != '1') {
				var uri = e.attr('data-uri');
				$.get(uri, function(d) {
					e.html(d);
					e.attr('data-loaded', '1');
					cb && cb(e);
				})
			}
		})
	}
});

$(function() {
	giiwa.css('/css/porlet.css');
})
