"""Palette + per-8x8 color check for the enraged drafts. Run: python3 check.py"""
from PIL import Image
import glob,os
NES=set("""000000 7C7C7C BCBCBC F8F8F8 FCFCFC 787878 0000FC 0000BC 4428BC 940084 A80020 A81000 881400 503000 007800 006800 005800 004058
0078F8 0058F8 6844FC D800CC E40058 F83800 E45C10 AC7C00 00B800 00A800 00A844 008888 3CBCFC 6888FC 9878F8 F878F8 F85898 F87858 FCA044 F8B800 B8F818 58D854 58F898 00E8D8
A4E4FC B8B8F8 D8B8F8 F8B8F8 F8A4C0 F0D0B0 FCE0A8 F8D878 D8F878 B8F8B8 B8F8D8 00FCFC F8D8F8""".split())
BODY={'503000','940084','D800CC'}; FX={'A81000','F83800','FCFCFC'}; BALL={'005800','787878','FCFCFC'}
def blocks(path):
    im=Image.open(path).convert('RGBA'); px=im.load(); cols=set(); worst=0; over3=0; bad2=0; n=0
    for by in range(0,im.height,8):
        for bx in range(0,im.width,8):
            s=set()
            for y in range(by,min(by+8,im.height)):
                for x in range(bx,min(bx+8,im.width)):
                    p=px[x,y]
                    if p[3]: s.add('%02X%02X%02X'%p[:3])
            if not s: continue
            n+=1; cols|=s; worst=max(worst,len(s)); over3+=len(s)>3
            nb=s-{'000000'}
            # two-layer test: block must split into <=2 palettes of <=3 non-black colors
            if len(nb)>6: bad2+=1
    return cols,worst,over3,n,bad2
base='/home/claude/megaman-maverick/assets/sprites/frames/Bosses1/ReactorMonkey/'
here=os.path.dirname(os.path.abspath(__file__))
files=[base+'stand.png',base+'throw.png']+sorted(glob.glob(here+'/*.png'))+sorted(glob.glob(here+'/ball_mad/*.png'))
for f in files:
    if 'contact' in f: continue
    cols,worst,over3,n,bad2=blocks(f)
    off=cols-NES
    print('%-26s colors=%d nonNES=%s max/8x8=%d blocks>3=%d/%d'%(('[current] ' if f.startswith(base) else '')+os.path.relpath(f,here).replace('../','').split('ReactorMonkey/')[-1],len(cols),sorted(off) or 'none',worst,over3,n))
