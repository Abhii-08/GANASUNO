'use strict';
// Local audio only; no files are uploaded. A shared graph keeps both crossfade decks audible.
let fxContext=null, fxNodes=[], waveformTrack=null, peaks=null, waveGeneration=0;
function initEffects(){
  if(fxContext)return fxContext.resume();
  const AC=window.AudioContext||window.webkitAudioContext;if(!AC)return Promise.reject(Error('Web Audio unsupported'));
  fxContext=new AC();
  fxNodes=audio.map(a=>{
    const source=fxContext.createMediaElementSource(a),pan=fxContext.createStereoPanner(),low=fxContext.createBiquadFilter(),high=fxContext.createBiquadFilter(),dry=fxContext.createGain(),delay=fxContext.createDelay(1),feedback=fxContext.createGain(),wet=fxContext.createGain();
    low.type="lowpass";low.frequency.value=fxContext.sampleRate/2;high.type="highpass";high.frequency.value=10;source.connect(low).connect(high).connect(pan);pan.connect(dry).connect(fxContext.destination);pan.connect(delay);delay.connect(feedback).connect(delay);delay.connect(wet).connect(fxContext.destination);wet.gain.value=0;feedback.gain.value=0;
    return {pan,low,high,dry,delay,feedback,wet};
  });return fxContext.resume();
}
function applyDelay(){if(!fxContext)return;const p={off:[0,0,0],slap:[.095,.12,.15],quarter:[.35,.28,.22],dub:[.48,.53,.30]}[$('delay').value];for(const n of fxNodes){n.delay.delayTime.setTargetAtTime(p[0],fxContext.currentTime,.05);n.feedback.gain.setTargetAtTime(p[1],fxContext.currentTime,.05);n.wet.gain.setTargetAtTime(p[2],fxContext.currentTime,.05);n.dry.gain.setTargetAtTime(1-p[2],fxContext.currentTime,.05)}}
function activateEffects(){initEffects().then(applyDelay).catch(()=>status('Web Audio effects unavailable in this browser'))}
$('panOn').addEventListener('change',activateEffects);$('delay').addEventListener('change',activateEffects);$('period').addEventListener('input',()=>{$('periodText').value=$('period').value+' s'});
setInterval(()=>{if(!fxContext)return;const period=Number($('period').value),direction=Number($('direction').value),p=$('panOn').checked?-.85*Math.sin(fxContext.currentTime*2*Math.PI/period)*direction:0;for(const n of fxNodes)n.pan.pan.setTargetAtTime(p,fxContext.currentTime,.04)},40);
const wave=$('wave'),ctx=wave.getContext('2d');
async function getPeaks(track){
 const token=++waveGeneration;peaks=null;if(!track?.file||track.file.size>60*1024*1024)return;
 const AC=window.AudioContext||window.webkitAudioContext;if(!AC)return;
 const decoder=new AC();try{const b=await decoder.decodeAudioData(await track.file.arrayBuffer()),data=b.getChannelData(0),out=[];for(let i=0;i<180;i++){let max=0,start=Math.floor(i*data.length/180),end=Math.floor((i+1)*data.length/180);for(let j=start;j<end;j+=Math.max(1,Math.floor((end-start)/1500)))max=Math.max(max,Math.abs(data[j]));out.push(max)}if(token===waveGeneration)peaks=out;}catch{}finally{await decoder.close().catch(()=>{})}
}
function drawWave(){const track=tracks[current];if(track!==waveformTrack){waveformTrack=track;getPeaks(track)}const w=wave.width,h=wave.height;ctx.clearRect(0,0,w,h);const a=active(),ratio=Number.isFinite(a.duration)&&a.duration>0?a.currentTime/a.duration:0;if(!peaks){ctx.fillStyle='#a4aabd';ctx.font='18px system-ui';ctx.fillText(track?'Waveform unavailable or decoding…':'Choose a local audio file',12,h/2);return}peaks.forEach((p,i)=>{ctx.fillStyle=i/peaks.length<=ratio?'#a5aaff':'#4a5065';let bh=Math.max(3,p*(h-8));ctx.fillRect(i*w/peaks.length,(h-bh)/2,w/peaks.length-1,bh)})}
wave.addEventListener('click',e=>{const a=active();if(!Number.isFinite(a.duration)||a.duration<=0)return;stopFade();const rect=wave.getBoundingClientRect();a.currentTime=Math.max(0,Math.min(1,(e.clientX-rect.left)/rect.width))*a.duration});setInterval(drawWave,200);

// Optional DJ transition: outgoing low-pass and incoming high-pass, alongside two-deck fade.
function resetSweep(){if(!fxContext)return;for(const n of fxNodes){n.low.frequency.setValueAtTime(fxContext.sampleRate/2,fxContext.currentTime);n.high.frequency.setValueAtTime(10,fxContext.currentTime)}}
function sweepTransition(t,oldIndex){if(!fxContext||!$('djSweep').checked)return;const out=fxNodes[oldIndex],inc=fxNodes[1-oldIndex],now=fxContext.currentTime;out.low.frequency.setTargetAtTime(20000*Math.pow(.035,t),now,.025);inc.high.frequency.setTargetAtTime(1800*Math.pow(10/1800,t),now,.025)}
$('djSweep').addEventListener('change',()=>{if($('djSweep').checked)activateEffects();else resetSweep()});
