"""Checks the intro/death strips: size, NES palette, the 2 sprite palettes, colours per 8x8 area."""
import glob,os
from PIL import Image
D=os.path.dirname(os.path.abspath(__file__))
NES=set("""000000 7C7C7C BCBCBC F8F8F8 FCFCFC 787878 0000FC 0000BC 4428BC 940084 A80020 A81000 881400 503000 007800 006800 005800 004058
0078F8 0058F8 6844FC D800CC E40058 F83800 E45C10 AC7C00 00B800 00A800 00A844 008888 3CBCFC 6888FC 9878F8 F878F8 F85898 F87858 FCA044 F8B800 B8F818 58D854 58F898 00E8D8
A4E4FC B8B8F8 D8B8F8 F8B8F8 F8A4C0 F0D0B0 FCE0A8 F8D878 D8F878 B8F8B8 B8F8D8 00FCFC F8D8F8""".split())
BODY={'000000','503000','940084','D800CC'}      # the body palette of the brief (outline + 3)
FX={'FCFCFC','B8F818','00A800'}                 # effects palette: white, lime, green
ok=True
for f in sorted(glob.glob(D+'/*.png')):
    n=os.path.basename(f)
    if n=='contact-sheet.png': continue
    im=Image.open(f).convert('RGBA'); px=im.load()
    assert im.width==160 and im.height%160==0,n
    cols=set();semi=0;strict=0;cells=0;bodybad=0
    for by in range(0,im.height,8):
        for bx in range(0,160,8):
            s=set()
            for y in range(by,by+8):
                for x in range(bx,bx+8):
                    r,g,b,a=px[x,y]
                    if a==0: continue
                    if a!=255: semi+=1
                    s.add('%02X%02X%02X'%(r,g,b))
            if not s: continue
            cells+=1; cols|=s
            if len(s)>3: strict+=1
            if len((s&BODY)-{'000000'})>3 or len(s&FX)>3: bodybad+=1
    pal=cols<=NES; two=cols<=(BODY|FX)
    feet=any(px[x,im.height-1][3] for x in range(160))
    good=pal and two and semi==0 and bodybad==0
    ok&=good
    print(f"{n:16s} cells={im.height//160} NES={'ok' if pal else 'FAIL'} two-palettes={'ok' if two else 'FAIL'} semi-alpha={semi} "
          f"8x8 over (body 3 + outline, fx 3)={bodybad} | strict >3 colours in 8x8: {strict}/{cells} | bottom row used={feet}")
print('RESULT:', 'ok' if ok else 'FAIL')
