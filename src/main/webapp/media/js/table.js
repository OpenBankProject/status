$(function() {
	$("#bankData").tablesorter({
		sortList: [[4,1],[2,0]],
		headers: {
	        0: {
	            sorter: "text"
	        },
	        1: {
	            sorter: "digit"
	        },
	        2: {
	            sorter: "text"
	        },
	        3: {
	            sorter: "text"
	        },
	        4: {
	            sorter: "text"
	        } 
	    }
	});
});