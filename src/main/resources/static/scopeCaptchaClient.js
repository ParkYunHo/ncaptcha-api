/**
 * 
 */

var commonInfo = {
	clientKey: null,
	captchaForm: null,
	imageInfo: null
};

function init(){
	commonInfo.captchaForm = document.getElementsByClassName('s-captcha')[0];
	getClientKey();
}

function getClientKey(){
	var url = 'getClientKey';
	var initType = 'ISSUED';
	
	xhr = getXMLHttpRequest();
	xhr.onreadystatechange = function(){
		if(xhr.readyState == 4 && xhr.status == 200){
			var response = JSON.parse(xhr.response);
			if(response.result != 'SUCCESS'){
				alert('Error: ' + response.result);
			}else{
				commonInfo.clientKey = response.clientKey;
				setCSSHTML();
				setBodyHTML();
				getCaptchaImage(initType);
				window.onkeydown = btnKeyboardEvt;
			}
		}
	};
	xhr.open("GET", url, true);
	xhr.send();
}

function setCSSHTML(){
	var fontIcon = '<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.6.3/css/all.css" integrity="sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/" crossorigin="anonymous">';
	var element = document.createElement('style');
	var imageBody = '.imageBody {position:absolute;}';
	var keyBody = '.keyBody {text-align:center;position:absolute;background-color:darkgray;display:none}';
	var keyBtn = '.keyBtn {float:left;background-color:lightgray;border:1px solid #111;cursor:pointer;}';
	var m3 = '.m3 {margin-left:33%;margin-right:33%;}';
	var w3 = '.w3 {width:32.6%;}';
	var marker = '.marker {position:relative;}';
	var refreshBtn = '.refreshBtn {background-color:white;position:absolute;border:1px solid #111;text-align:center;font-size:40px;font-weight:bold;cursor:pointer;display:none}';
	var loading = '.loading {width:302px;height:540px;position:absolute;text-align:center;background-color:lightgray;opacity:0.8;z-index:1;font-size:30px;font-weight:bold;display:table;}';
	var loadingText = '.loadingText {display:table-cell;vertical-align:middle}';
	var iconRender = '.iconRender {text-rendering: optimizelegibility}';
	
	var totalCSS = imageBody + keyBody + keyBtn + w3 + m3 + marker + refreshBtn + loading + loadingText + iconRender; 
	
	element.appendChild(document.createTextNode(totalCSS));
	document.head.innerHTML = fontIcon;
	document.head.appendChild(element);
}

function setBodyHTML(){
	var upParm = 'up';
	var leftParm = 'left';
	var pickParm = 'pick';
	var rightParm = 'right';
	var downParm = 'down';
	var refreshText = 'Refresh';
	var htmlText =	'<div class="imageBody">' +
						'<img src="data:image/jpeg;base64, /9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAEsASwDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDz+iiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiuX+3az9s+yeZ+/wD7u1PTPXp0oA6iioLMXAtI/tTAz4+YjHr7VPQAUVA17aoxVrmEMDggyDINT0AFFRyzwwY82WOPPTewGaIp4Z8+VLHJjrsYHFAElFFY+oXVwzl7G/tgixlim5WYkZJxwe1AGxRWbol1Nd2TyTvvYSFQcAcYHpVTU7iaW6hazv4VjA5xOoAOepHcdPXoaAN2imvIkSF5HVFHVmOAKh+32f8Az9wf9/BQBYoqOKeGfPlSxyY67GBxTnkSJC8jqijqzHAFADqKr/b7P/n7g/7+CpIp4Z8+VLHJjrsYHFAElFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABXP/wDM3/5/5510Fcf/AGn/AMTj7f5P/AN3+zjrigDqLq+t7PZ9ok2b87flJzj6fWs7xFdSQ20cKEr5pO4g9h2/HNY+p6m2otH+6Eaxg4Gckk//AKhW5rtjJd2yPEC0kRJ2juD1x78CgCrF4bVrYGSZ1nIB4AwvqMd+/OaPD880dxNYy5wgJAJ+6QcEfr/nNQJ4guYYPJeBTMny7mJ7eo7nr3q3oVjPE8t3cB1dxtAbqeckn8h+tAGRqIVNVmMr+cC5J2PyPQZI7cfypNPmMWqwGAvGrOqkFskgkZB4FWpZJNJ1qad4A6uzFScgEHng4684/Oqs+pSXGpR3bqP3bAqgPQA5xn+tAG14iupIbaOFCV80ncQew7fjmqtx4fWHT2lExM6KWb+6QOoHepr+KTWNKhu4oyJFLHywc5GcHHHJ4FUptenmsTblFDsNryeo78dqANLw3/yDpP8Arqf5CsvWNNh0/wAnymkbfuzvIPTHt71Y0O/8i3uYvL3bEafO7GcADFU9T1P+0vK/c+X5ef4s5zj29qAOqurZLu2eCQsFbGSvXg5rl9I0xNReQySMqx4yFHJznv26VrQ675tnc3H2bHk7fl39cnHpWf4euvKvGt9mfO/iz0wCaAHWUH2LxKLdHYqMjPTIK5wf0/Kn6q01/rCWCkqikcZ4zjJb8B/L3ql/af8AxOPt/k/8A3f7OOuK0NZga0vU1GKVQ5IJRmweMDj1HqP8gAh1XRY7O18+B5GCnDhyOh79u/8AOtDw/bJFYCdS26b7wPQYJAxWTqOtPfwCFYvKXOW+fO70Hb/OK3NE/wCQPB/wL/0I0AaFFZun6umoXEkSxMm0blJOcjOOfTqPWjUNXTT7iOJomfcNzEHGBnHHr0PpQBpUVBeXK2dpJOylgg6Duc4FR6dfLqFsZghQhipUnPP1/GgC3RRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABVHUtMTUUQNIyMmdpAyOcZyPwq9RQBgp4ZQODJdMy9wqYP55NbkaLFGsaDCqAoHoBTqKAGJDHGzMkaKznLFVALH3oeGORlZ40ZkOVLKCVPtT6KAEZVdSrAFSMEEcEUiRpEgSNFRR0VRgCnUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAf/Z">' +
					'</div>' +
					'<div class="refreshBtn" onclick=refreshBtnClickEvt()>' + refreshText + '</div>' + 
					'<div class="keyBody">' +
						'<div class="keyBtn w3 m3" onclick=btnClickEvt("' + upParm + '")><i class="fas fa-angle-double-up fa-3x iconRender"></i></div>' +
						'<div class="keyBtn w3" onclick=btnClickEvt("' + leftParm + '")><i class="fas fa-angle-double-left fa-3x iconRender"></i></div>' +
						'<div class="keyBtn w3" onclick=btnClickEvt("' + pickParm + '")><i class="fas fa-check fa-3x iconRender"></i></div>' +
						'<div class="keyBtn w3" onclick=btnClickEvt("' + rightParm + '")><i class="fas fa-angle-double-right fa-3x iconRender"></i></div>' +
						'<div class="keyBtn w3 m3" onclick=btnClickEvt("' + downParm + '")><i class="fas fa-angle-double-down fa-3x iconRender"></i></div>' +
					'</div>' +
					'<div class="marker"></div>';
	commonInfo.captchaForm.innerHTML = htmlText;
}

function getXMLHttpRequest(){
	if(window.ActiveXObject){
		try{
			return new ActiveXObject("Msxml2.XMLHTTP");
		}catch(e1){
			try{
				return new ActiveXObject("Microsoft.XMLHTTP");
			}catch(e2){
				return null;
			}
		}
	}else if(window.XMLHttpRequest){
		return new XMLHttpRequest();
	}else{
		return null;
	}
}
function refreshBtnClickEvt(){
	var refreshType = "REFRESH";
	getCaptchaImage(refreshType);
}

function getCaptchaImage(type){
	loadingFormPop();
	var url = 'getCaptchaImage';
	var param = {
		clientKey: commonInfo.clientKey,
		actionType: type
	}
	var jsonParam = JSON.stringify(param);
	
	xhr = getXMLHttpRequest();
	xhr.onreadystatechange = function(){
		if(xhr.readyState == 4 && xhr.status == 200){
			var response = JSON.parse(xhr.response);
			if(response.result != 'SUCCESS'){
				alert(response.result);
			}else{
				loadingFormDel();
				commonInfo.imageInfo = response.imageInfo;
				setCaptchaImage();
				setMarkerPosition();
			}
		}
	};
	xhr.open("POST", url, true);
	xhr.setRequestHeader('Content-type', 'application/json;charset=UTF-8');
	xhr.send(jsonParam);	
}

function setCaptchaImage(){
	var srcText = 'data:image/jpeg;base64, ' + commonInfo.imageInfo.b64;
	var refreshBtnHeight = 60;
	var keyBodyTop = 0;
	
	document.getElementsByClassName('s-captcha')[0].children[0].children[0].setAttribute('src', srcText);
	
	document.getElementsByClassName('refreshBtn')[0].style.height = refreshBtnHeight + 'px';
	document.getElementsByClassName('refreshBtn')[0].style.width = (commonInfo.imageInfo.width-2) + 'px';
	document.getElementsByClassName('refreshBtn')[0].style.top = (commonInfo.imageInfo.width + commonInfo.imageInfo.descriptHeight + 7) + 'px';
	document.getElementsByClassName('refreshBtn')[0].style.display = 'block';
	
	keyBodyTop = parseInt(document.getElementsByClassName('refreshBtn')[0].style.top) + refreshBtnHeight;
	document.getElementsByClassName('keyBody')[0].style.width = commonInfo.imageInfo.width + 'px';
	document.getElementsByClassName('keyBody')[0].style.top = keyBodyTop + 'px';
	document.getElementsByClassName('keyBody')[0].style.display = 'block';
} 

function setMarkerPosition(){
	var cellBorderSize = 10;
	document.getElementsByClassName('marker')[0].style.left = commonInfo.imageInfo.startAxis.xAxis + 'px';
	document.getElementsByClassName('marker')[0].style.top = (commonInfo.imageInfo.startAxis.yAxis + commonInfo.imageInfo.descriptHeight) + 'px';
	
	document.getElementsByClassName('marker')[0].style.width = (commonInfo.imageInfo.cellSize-cellBorderSize) + 'px';
	document.getElementsByClassName('marker')[0].style.height = (commonInfo.imageInfo.cellSize-cellBorderSize) + 'px';
	document.getElementsByClassName('marker')[0].style.border = (cellBorderSize/2) + 'px solid red';
}

function btnKeyboardEvt(event){
	var keyCode = event.keyCode;
	switch(keyCode){
	case 38:
		btnClickEvt('up');
		break;
	case 40:
		btnClickEvt('down');
		break;
	case 37:
		btnClickEvt('left');
		break;
	case 39:
		btnClickEvt('right');
		break;
	case 13:
		btnClickEvt('pick');
	}
}

function btnClickEvt(direction){
	var markerStyle = document.getElementsByClassName('marker')[0].style;
	var position = 0;
	
	switch(direction){
		case 'right':
			var position = parseInt(markerStyle.left);
			if(position != commonInfo.imageInfo.width - commonInfo.imageInfo.cellSize) markerStyle.left = (position + commonInfo.imageInfo.cellSize) + 'px';
			break;
		case 'left':
			var position = parseInt(markerStyle.left);
			if(position != 0) markerStyle.left = (position - commonInfo.imageInfo.cellSize) + 'px';
			break;
		case 'up':
			var position = parseInt(markerStyle.top);
			if(position != commonInfo.imageInfo.descriptHeight) markerStyle.top = (position - commonInfo.imageInfo.cellSize) + 'px';
			break;
		case 'down':
			var position = parseInt(markerStyle.top);
			if(position != (commonInfo.imageInfo.height + commonInfo.imageInfo.descriptHeight) - commonInfo.imageInfo.cellSize) markerStyle.top = (position + commonInfo.imageInfo.cellSize) + 'px';
			break;
		case 'pick':
			var Xaxis = Math.floor(document.getElementsByClassName('marker')[0].offsetLeft/10) * 10;
			var Yaxis = Math.floor(document.getElementsByClassName('marker')[0].offsetTop/10) * 10;
			checkValidation(Xaxis, Yaxis);
			break;
	}
}

function checkValidation(selectedXAxis, selectedYAxis){
	loadingFormPop();
	var url = 'checkValidation';
	var param = {
		clientKey: commonInfo.clientKey,
		userInputAxis:{
			xAxis: selectedXAxis,
			yAxis: selectedYAxis
		}
	}
	var jsonParam = JSON.stringify(param);
	
	xhr = getXMLHttpRequest();
	xhr.onreadystatechange = function(){
		if(xhr.readyState == 4 && xhr.status == 200){
			var response = JSON.parse(xhr.response);
			if(response.result == 'SUCCESS'){
				window.onkeydown = null;
				commonInfo.captchaForm.innerHTML = "";
				alert(response.result);
			}else if(response.result == 'FAIL'){
				alert('Error: ' + response.result);
				loadingFormDel();
				commonInfo.imageInfo = response.imageInfo;
				setCaptchaImage();
				setMarkerPosition();
			}else{
				alert(response.result);
			}
		}
	};
	xhr.open("POST", url, true);
	xhr.setRequestHeader('Content-type', 'application/json;charset=UTF-8');
	xhr.send(jsonParam);	
}

function loadingFormPop(){
	var loadingFormHTML = '<div class="loading"><p class="loadingText">Loading...</p></div>';
	commonInfo.captchaForm.innerHTML = loadingFormHTML + commonInfo.captchaForm.innerHTML; 
}

function loadingFormDel(){
	var loadingNode = commonInfo.captchaForm.childNodes[0];
	commonInfo.captchaForm.removeChild(loadingNode);
}


