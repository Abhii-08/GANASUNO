"""Run against an installed GANASUNO V33 on one unlocked emulator/device. No account/network actions."""
import argparse, re, subprocess, time, xml.etree.ElementTree as ET
P = 'com.abhi.ganasuno'
parser=argparse.ArgumentParser();parser.add_argument('--adb',default='adb');parser.add_argument('--serial');args=parser.parse_args()
cmd=[args.adb]+(['-s',args.serial] if args.serial else [])
def adb(*parts): return subprocess.check_output(cmd+list(parts),text=True,encoding='utf-8',errors='replace',timeout=30)
def dump():
    adb('shell','uiautomator','dump','/sdcard/ganasuno-smoke.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/ganasuno-smoke.xml')).iter('node'))
def find(nodes, rid=None, text=None):
    return next((n for n in nodes if (rid is None or n.get('resource-id')==P+':id/'+rid) and (text is None or text.lower() in (n.get('text') or '').lower())),None)
def wait(rid=None,text=None):
    for _ in range(5):
        n=find(dump(),rid,text)
        if n is not None:return n
        time.sleep(.5)
    raise AssertionError(f'Not visible: {rid or text}. Unlock device and dismiss permission/crash prompts before testing.')
def tap(n):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds','')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def nav(rid):tap(wait(rid));time.sleep(.5)
try:
    adb('shell','am','start','-n',P+'/.MainActivity')
    for id in ['navHome','navSearch','navLibrary','btnSettings']:wait(id)
    nav('navLibrary');wait(text='Library');wait(text='Liked Songs');wait(text='Downloaded Music');print('PASS library navigation')
    nav('navSearch');wait('search');wait('btnSearchGo');print('PASS search navigation')
    nav('btnSettings');wait(text='Settings');field=next(n for n in dump() if n.get('class')=='android.widget.EditText');tap(field);adb('shell','input','text','crossfade');time.sleep(.5);adb('shell','input','keyevent','4');tap(wait(text='Crossfade & DJ transitions'));wait(text='Audio transitions');wait(text='Crossfade');print('PASS searchable crossfade action')
    adb('shell','input','keyevent','4');nav('navHome');wait('sectionTitle');print('PASS home navigation')
    print('UI smoke checks passed. Playback/audio/account/device-specific checks are separate.')
finally:
    try:adb('shell','rm','-f','/sdcard/ganasuno-smoke.xml')
    except Exception:pass
