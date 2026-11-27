(()=>{const button=document.getElementById('fullscreen'), sidebar=document.getElementById('sidebar'), range=document.getElementById('sidebarScroll');
button.addEventListener('click',async()=>{try{if(document.fullscreenElement)await document.exitFullscreen();else await document.documentElement.requestFullscreen();}catch(e){document.getElementById('status').textContent='Fullscreen unavailable in this browser';}});
document.addEventListener('fullscreenchange',()=>{button.textContent=document.fullscreenElement?'Exit fullscreen':'Fullscreen';});
function refresh(){const max=Math.max(0,sidebar.scrollHeight-sidebar.clientHeight);range.disabled=max===0;range.value=max?Math.round(1000*sidebar.scrollTop/max):0;}
range.addEventListener('input',()=>{sidebar.scrollTop=Number(range.value)/1000*Math.max(0,sidebar.scrollHeight-sidebar.clientHeight);});
sidebar.addEventListener('scroll',refresh,{passive:true});window.addEventListener('resize',refresh);new MutationObserver(refresh).observe(sidebar,{childList:true,subtree:true});if(window.ResizeObserver)new ResizeObserver(refresh).observe(sidebar);refresh();})();
