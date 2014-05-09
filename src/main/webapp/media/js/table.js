$(function() {
	var table = $("#bankData").stupidtable();
  $(table).find("th").eq(3).click();
	$("#bankData").bind('aftertablesort', function (event, data) {
	    var th = $(this).find("th");
	    th.find(".arrow").remove();
	    var arrow = data.direction === "asc" ?  "↓" : "↑";
	    th.eq(data.column).append('<span class="arrow">' + arrow +'</span>');
	});
});