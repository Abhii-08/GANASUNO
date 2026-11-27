'use strict';
function sameAlbum(a,b){const x=a?.album?.trim().toLocaleLowerCase();const y=b?.album?.trim().toLocaleLowerCase();return Boolean(x&&x===y)}
if(typeof module!=='undefined')module.exports={sameAlbum};
