"""Round-5 check: cell size, NES palette, allowed colours, alpha, feet row, 8x8 colour counts. Run: python3 check.py"""
import glob, os, numpy as np
from PIL import Image
HERE=os.path.dirname(os.path.abspath(__file__))
NES="""000000 7C7C7C BCBCBC F8F8F8 FCFCFC 787878 0000FC 0000BC 4428BC 940084 A80020 A81000 881400 503000 007800 006800 005800 004058
0078F8 0058F8 6844FC D800CC E40058 F83800 E45C10 AC7C00 00B800 00A800 00A844 008888 3CBCFC 6888FC 9878F8 F878F8 F85898 F87858 FCA044 F8B800 B8F818 58D854 58F898 00E8D8
A4E4FC B8B8F8 D8B8F8 F8B8F8 F8A4C0 F0D0B0 FCE0A8 F8D878 D8F878 B8F8B8 B8F8D8 00FCFC F8D8F8""".split()
NES={tuple(int(c[i:i+2],16) for i in (0,2,4)) for c in NES}
BODY={(0,0,0),(0x50,0x30,0),(0x94,0,0x84),(0xD8,0,0xCC)}
FX={(0xFC,0xFC,0xFC),(0xF8,0x38,0),(0xA8,0x10,0),(0xB8,0xF8,0x18),(0,0xA8,0),(0,0x68,0)}   # 006800: dim mohawk shade (dying, collapse, dead)
BALL={(0,0x58,0),(0x78,0x78,0x78)}          # the ball that `throw` already holds
ok=True
for p in sorted(glob.glob(os.path.join(HERE,'*.png'))):
    n=os.path.basename(p)
    if n in ('contact-sheet.png','before-after.png','audit-side-by-side.png','chest_beat_sheet.png','heave-track.png') or n.startswith('steam'): continue
    a=np.array(Image.open(p).convert('RGBA')); h,w=a.shape[:2]
    size=w==160 and h%160==0
    semi=int(((a[...,3]>0)&(a[...,3]<255)).sum())
    cols={tuple(c[:3]) for c in a[a[...,3]>0]}
    nes=cols<=NES; allowed=cols<=(BODY|FX|BALL)
    over=tot=0; feet=[]
    for cy in range(0,h,160):
        cell=a[cy:cy+160]; feet.append(bool((cell[159,:,3]>0).any()))
        for y in range(0,160,8):
            for x in range(0,160,8):
                b=cell[y:y+8,x:x+8].reshape(-1,4); b=b[b[:,3]>0]
                if len(b):
                    tot+=1; over+=len({tuple(c[:3]) for c in b})>3
    good=size and nes and allowed and semi==0 and all(feet)
    ok&=good
    print('%-26s cells=%d size=%s NES=%s colours=%s semi-alpha=%d feet-on-bottom-row=%s | strict >3 colours per 8x8: %d/%d'%(n,h//160,'ok' if size else 'BAD','ok' if nes else 'BAD','ok' if allowed else 'BAD %s'%sorted(cols-(BODY|FX|BALL)),semi,'ok' if all(feet) else feet,over,tot))
print('RESULT:','ok' if ok else 'FAIL')
