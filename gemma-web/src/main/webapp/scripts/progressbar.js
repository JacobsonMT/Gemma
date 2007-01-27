function refreshProgress() {
	//HttpProgressMonitor.getProgressStatus(updateProgress);
	HttpProgressMonitor.getProgressStatus(updateProgress,DWRUtil.getValue("taskId"));
}
var determinate;

function updateProgress(data) {
 
 	if (determinate == 1)
 		updateDeterminateProgress(data);
 	else 
 		updateIndeterminateProgress(data);

//As it turns out the forwardingURL will never be null as its always set by default by the progressManager.
//Still good to check though, if the size of the forwardingURL is 1 charcter then i know its just a blank character
//I should implement a setting to turn forwarding on and off in the datapack, or in the progressjob checking for a size == 1 is just bad.
 		
 	if (data.done && data.forwardingURL != null && data.forwardingURL.size != 1) {
			redirect( data.forwardingURL );
	} else {
		window.setTimeout("refreshProgress()", 800);
	}
	
	return true;
}

function updateDeterminateProgress(data){
	document.getElementById("progressBarText").innerHTML = data.description + " " + data.percent + "%";
	document.getElementById("progressBarBoxContent").style.width = parseInt(data.percent * 3.5) + "px";
}

var previousMessage = "";
function updateIndeterminateProgress(data){
 		
   if (previousMessage != data.description) {
		previousMessage = data.description
		
		document.getElementById("progressTextArea").innerHTML += data.description + "<br />";	
   	document.getElementById("progressTextArea").scrollTop = document.getElementById("progressTextArea").scrollHeight;
	}
	

}

function redirect(url) {
   window.location = url;
}

function startProgress() {
	document.getElementById("progressBar").style.display = "block";
	
   

   if (determinate == 0){
		//progressMotion();
		document.getElementById("progressTextArea").innerHTML = "Monitoring Progress...";
	}	else
		document.getElementById("progressBarText").value = "Monitoring Progress...";
	
	
	window.setTimeout("refreshProgress()", 400);
	return true;
}
function createIndeterminateProgressBar() {
	determinate = 0;
	var barId = createIndeterminateBarDetails(500,20,'white',1,'black','#FF9933',85,7,3,"");
	
}

function createDeterminateProgressBar(){
	determinate = 1;
	var barHtml = '<div id="progressBar" style="display: none;"> <div id="theMeter">  <div id="progressBarText"></div>   <div id="progressBarBox">  <div id="progressBarBoxContent"></div>  </div>  </div>  </div>';
	document.write(barHtml);
	
}


// xp_progressbar
// Copyright 2004 Brian Gosselin of ScriptAsylum.com
//

var w3c=(document.getElementById)?true:false;
var ie=(document.all)?true:false;
var N=-1;

function createIndeterminateBarDetails(w,h,bgc,brdW,brdC,blkC,speed,blocks,count,action){
if(ie||w3c){
var t='<div id="_xpbar'+(++N)+'" style="visibility:visible; position:relative; overflow:hidden; width:'+w+'px; height:'+h+'px; background-color:'+bgc+'; border-color:'+brdC+'; border-width:'+brdW+'px; border-style:solid; font-size:1px;">';
t+='<span id="blocks'+N+'" style="left:-'+(h*2+1)+'px; position:absolute; font-size:1px">';
for(i=0;i<blocks;i++){
t+='<span style="background-color:'+blkC+'; left:-'+((h*i)+i)+'px; font-size:1px; position:absolute; width:'+h+'px; height:'+h+'px; '
t+=(ie)?'filter:alpha(opacity='+(100-i*(100/blocks))+')':'-Moz-opacity:'+((100-i*(100/blocks))/100);
t+='"></span>';
}
t+='</span></div>';
var ipbHeader = '<div id="progressBar"> <div id="theMeter">	<div id="progressBarText"> <div class="clob" id="progressTextArea"> </div>	</div>';
var ipbFooter = '</div>	</div>	<form> <input type="hidden" name="taskId\" />		</form> ';
document.write(ipbHeader + t + ipbFooter);
var bA=(ie)?document.all['blocks'+N]:document.getElementById('blocks'+N);
bA.bar=(ie)?document.all['_xpbar'+N]:document.getElementById('_xpbar'+N);
bA.blocks=blocks;
bA.N=N;
bA.w=w;
bA.h=h;
bA.speed=speed;
bA.ctr=0;
bA.count=count;
bA.action=action;
bA.togglePause=togglePause;
bA.showBar=function(){
this.bar.style.visibility="visible";
}
bA.hideBar=function(){
this.bar.style.visibility="hidden";
}
bA.tid=setInterval('startBar('+N+')',speed);
return bA;
}}

function startBar(bn){
var t=(ie)?document.all['blocks'+bn]:document.getElementById('blocks'+bn);
if(parseInt(t.style.left)+t.h+1-(t.blocks*t.h+t.blocks)>t.w){
t.style.left=-(t.h*2+1)+'px';
t.ctr++;
if(t.ctr>=t.count){
eval(t.action);
t.ctr=0;
}}else t.style.left=(parseInt(t.style.left)+t.h+1)+'px';
}

function togglePause(){
if(this.tid==0){
this.tid=setInterval('startBar('+this.N+')',this.speed);
}else{
clearInterval(this.tid);
this.tid=0;
}}

function togglePause(){
if(this.tid==0){
this.tid=setInterval('startBar('+this.N+')',this.speed);
}else{
clearInterval(this.tid);
this.tid=0;
}}

