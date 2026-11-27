'use strict';
// Browser mode remains unchanged. Native window controls exist only in the optional local shell.
function installWindowControls(){
  if(!window.pywebview?.api||document.getElementById('nativeQuit'))return;
  const mini=$('mini');
  mini.addEventListener('click',async()=>{
    try{const enabled=document.body.classList.contains('mini');if(!await window.pywebview.api.mini(enabled))throw Error('Window unavailable');mini.textContent=enabled?'Full window':'Mini view';}catch(e){status('Window resize unavailable; layout still works');}
  });
  const quit=document.createElement('button');quit.id='nativeQuit';quit.textContent='Quit';quit.onclick=async()=>{if(!confirm('Quit GANASUNO? Save any playlists you want to keep first.'))return;try{await window.pywebview.api.quit();}catch(e){status('Use the window close button to exit');}};
  document.querySelector('header').append(quit);
}
window.addEventListener('pywebviewready',installWindowControls);
installWindowControls();
