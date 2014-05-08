$(function() {
	$("#bankData").stupidtable();

	$("#bankData").bind('aftertablesort', function (event, data) {
	    var th = $(this).find("th");
	    th.find(".arrow").remove();
	    var arrow = data.direction === "asc" ? "↑" : "↓";
	    th.eq(data.column).append('<span class="arrow">' + arrow +'</span>');
	});
});