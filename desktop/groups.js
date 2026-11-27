'use strict';
function groupSaved(rows,kind,query=''){
 const groups=new Map(), norm=x=>String(x||'').trim().toLocaleLowerCase();
 if(kind==='playlist')return rows.filter(row=>norm(row.name)&&norm(row.name).includes(norm(query))).map(row=>({title:row.name,items:(row.files||[]).map((file,i)=>({name:file.name,file,album:row.albums?.[i]||'',artist:row.artists?.[i]||'',kind:row.kinds?.[i]||'music'}))})).sort((a,b)=>a.title.localeCompare(b.title));
 for(const row of rows) for(let i=0;i<(row.files||[]).length;i++){
  const file=row.files[i], album=row.albums?.[i]||'',artist=row.artists?.[i]||'',type=row.kinds?.[i]||'music';
  if(!file)continue;
  const title=kind==='album'?album:kind==='artist'?artist:type==='podcast'?(album||artist||'Podcasts'):'';
  if(!norm(title)||!norm(title).includes(norm(query)))continue;
  const key=norm(title)+(kind==='album'?'\u0000'+norm(artist):'');
  if(!groups.has(key))groups.set(key,{title:kind==='album'&&artist?title+' — '+artist:title,items:[],seen:new Set()});
  const g=groups.get(key),id=JSON.stringify([file.name,file.size,file.lastModified]);
  if(!g.seen.has(id)){g.seen.add(id);g.items.push({name:file.name,file,album,artist,kind:type})}
 }
 return [...groups.values()].sort((a,b)=>a.title.localeCompare(b.title));
}
if(typeof module!=='undefined')module.exports={groupSaved};
