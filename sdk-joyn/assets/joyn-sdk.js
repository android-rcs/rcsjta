function showStep(show, hideId, stepId){
	show.style.display = 'none';
	var hide = document.getElementById(hideId);
	var step = document.getElementById(stepId);
	hide.style.display = 'block';
	step.style.display = 'block';
}

function hideStep(hide, showId, stepId){
	hide.style.display = 'none';
	var show = document.getElementById(showId);
	var step = document.getElementById(stepId);
	show.style.display = 'block';
	step.style.display = 'none';
}