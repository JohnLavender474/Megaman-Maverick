<?xml version="1.0" encoding="UTF-8"?>
<data version="1.0">
    <struct type="Settings">
        <key>fileFormatVersion</key>
        <int>6</int>
        <key>texturePackerVersion</key>
        <string>7.4.0</string>
        <key>autoSDSettings</key>
        <array>
            <struct type="AutoSDSettings">
                <key>scale</key>
                <double>1</double>
                <key>extension</key>
                <string></string>
                <key>spriteFilter</key>
                <string></string>
                <key>acceptFractionalValues</key>
                <false/>
                <key>maxTextureSize</key>
                <QSize>
                    <key>width</key>
                    <int>-1</int>
                    <key>height</key>
                    <int>-1</int>
                </QSize>
            </struct>
        </array>
        <key>allowRotation</key>
        <true/>
        <key>shapeDebug</key>
        <false/>
        <key>dpi</key>
        <uint>72</uint>
        <key>dataFormat</key>
        <string>libgdx</string>
        <key>textureFileName</key>
        <filename></filename>
        <key>flipPVR</key>
        <false/>
        <key>pvrQualityLevel</key>
        <uint>3</uint>
        <key>astcQualityLevel</key>
        <uint>2</uint>
        <key>basisUniversalQualityLevel</key>
        <uint>2</uint>
        <key>etc1QualityLevel</key>
        <uint>70</uint>
        <key>etc2QualityLevel</key>
        <uint>70</uint>
        <key>dxtCompressionMode</key>
        <enum type="SettingsBase::DxtCompressionMode">DXT_PERCEPTUAL</enum>
        <key>ditherType</key>
        <enum type="SettingsBase::DitherType">NearestNeighbour</enum>
        <key>backgroundColor</key>
        <uint>0</uint>
        <key>libGdx</key>
        <struct type="LibGDX">
            <key>filtering</key>
            <struct type="LibGDXFiltering">
                <key>x</key>
                <enum type="LibGDXFiltering::Filtering">Linear</enum>
                <key>y</key>
                <enum type="LibGDXFiltering::Filtering">Linear</enum>
            </struct>
        </struct>
        <key>shapePadding</key>
        <uint>0</uint>
        <key>jpgQuality</key>
        <uint>80</uint>
        <key>pngOptimizationLevel</key>
        <uint>0</uint>
        <key>webpQualityLevel</key>
        <uint>101</uint>
        <key>textureSubPath</key>
        <string></string>
        <key>textureFormat</key>
        <enum type="SettingsBase::TextureFormat">png</enum>
        <key>borderPadding</key>
        <uint>0</uint>
        <key>maxTextureSize</key>
        <QSize>
            <key>width</key>
            <int>2048</int>
            <key>height</key>
            <int>2048</int>
        </QSize>
        <key>fixedTextureSize</key>
        <QSize>
            <key>width</key>
            <int>-1</int>
            <key>height</key>
            <int>-1</int>
        </QSize>
        <key>algorithmSettings</key>
        <struct type="AlgorithmSettings">
            <key>algorithm</key>
            <enum type="AlgorithmSettings::AlgorithmId">Basic</enum>
            <key>freeSizeMode</key>
            <enum type="AlgorithmSettings::AlgorithmFreeSizeMode">Best</enum>
            <key>sizeConstraints</key>
            <enum type="AlgorithmSettings::SizeConstraints">AnySize</enum>
            <key>forceSquared</key>
            <false/>
            <key>maxRects</key>
            <struct type="AlgorithmMaxRectsSettings">
                <key>heuristic</key>
                <enum type="AlgorithmMaxRectsSettings::Heuristic">Best</enum>
            </struct>
            <key>basic</key>
            <struct type="AlgorithmBasicSettings">
                <key>sortBy</key>
                <enum type="AlgorithmBasicSettings::SortBy">Best</enum>
                <key>order</key>
                <enum type="AlgorithmBasicSettings::Order">Ascending</enum>
            </struct>
            <key>polygon</key>
            <struct type="AlgorithmPolygonSettings">
                <key>alignToGrid</key>
                <uint>1</uint>
            </struct>
        </struct>
        <key>dataFileNames</key>
        <map type="GFileNameMap">
            <key>data</key>
            <struct type="DataFile">
                <key>name</key>
                <filename>../sprite_sheets/Bosses.txt</filename>
            </struct>
        </map>
        <key>multiPackMode</key>
        <enum type="SettingsBase::MultiPackMode">MultiPackOff</enum>
        <key>forceIdenticalLayout</key>
        <false/>
        <key>outputFormat</key>
        <enum type="SettingsBase::OutputFormat">RGBA8888</enum>
        <key>alphaHandling</key>
        <enum type="SettingsBase::AlphaHandling">ClearTransparentPixels</enum>
        <key>contentProtection</key>
        <struct type="ContentProtection">
            <key>key</key>
            <string></string>
        </struct>
        <key>autoAliasEnabled</key>
        <false/>
        <key>trimSpriteNames</key>
        <false/>
        <key>prependSmartFolderName</key>
        <false/>
        <key>autodetectAnimations</key>
        <true/>
        <key>globalSpriteSettings</key>
        <struct type="SpriteSettings">
            <key>scale</key>
            <double>1</double>
            <key>scaleMode</key>
            <enum type="ScaleMode">Smooth</enum>
            <key>extrude</key>
            <uint>0</uint>
            <key>trimThreshold</key>
            <uint>1</uint>
            <key>trimMargin</key>
            <uint>1</uint>
            <key>trimMode</key>
            <enum type="SpriteSettings::TrimMode">None</enum>
            <key>tracerTolerance</key>
            <int>200</int>
            <key>heuristicMask</key>
            <false/>
            <key>defaultPivotPoint</key>
            <point_f>0.5,0.5</point_f>
            <key>writePivotPoints</key>
            <false/>
        </struct>
        <key>individualSpriteSettings</key>
        <map type="IndividualSpriteSettingsMap">
            <key type="filename">../frames/Bosses/Bospider/Climb.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>80,16,160,32</rect>
                <key>scale9Paddings</key>
                <rect>80,16,160,32</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/Bospider/OpenEye.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>64,16,128,32</rect>
                <key>scale9Paddings</key>
                <rect>64,16,128,32</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/Bospider/Still.png</key>
            <key type="filename">../frames/Bosses/ReactorMonkeyMiniBoss/Nuclear_Monkey_Ball.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>16,16,32,32</rect>
                <key>scale9Paddings</key>
                <rect>16,16,32,32</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GlacierMan/duck.png</key>
            <key type="filename">../frames/Bosses/GlacierMan/duck_shoot.png</key>
            <key type="filename">../frames/Bosses/GlacierMan/stand.png</key>
            <key type="filename">../frames/Bosses/GlacierMan/stand_shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>16,12,32,24</rect>
                <key>scale9Paddings</key>
                <rect>16,12,32,24</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GlacierMan/ice_blast_attack.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>16,24,32,48</rect>
                <key>scale9Paddings</key>
                <rect>16,24,32,48</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GlacierMan/jet.png</key>
            <key type="filename">../frames/Bosses/GlacierMan/jet_shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>32,24,64,48</rect>
                <key>scale9Paddings</key>
                <rect>32,24,64,48</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GlacierMan/jet_spin.png</key>
            <key type="filename">../frames/Bosses/GlacierMan/spin.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>32,48,64,96</rect>
                <key>scale9Paddings</key>
                <rect>32,48,64,96</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GlacierMan/slide.png</key>
            <key type="filename">../frames/Bosses/GlacierMan/slide_shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>16,36,32,72</rect>
                <key>scale9Paddings</key>
                <rect>16,36,32,72</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GlacierMan/stop.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>48,36,96,72</rect>
                <key>scale9Paddings</key>
                <rect>48,36,96,72</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GutsTank/Fist.png</key>
            <key type="filename">../frames/Bosses/PreciousMan/StandShoot.png</key>
            <key type="filename">../frames/Bosses/ReactMan/Jump.png</key>
            <key type="filename">../frames/Bosses/ReactMan/Stand.png</key>
            <key type="filename">../frames/Bosses/ReactMan/Throw.png</key>
            <key type="filename">../frames/Bosses/RodentMan/StandStill.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>12,12,24,24</rect>
                <key>scale9Paddings</key>
                <rect>12,12,24,24</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GutsTank/FistLaunched.png</key>
            <key type="filename">../frames/Bosses/PreciousMan/Jump.png</key>
            <key type="filename">../frames/Bosses/PreciousMan/Stand.png</key>
            <key type="filename">../frames/Bosses/RodentMan/Slash.png</key>
            <key type="filename">../frames/Bosses/RodentMan/WallSlide.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,12,48,24</rect>
                <key>scale9Paddings</key>
                <rect>24,12,48,24</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/GutsTank/Laughing.png</key>
            <key type="filename">../frames/Bosses/GutsTank/MouthClosed.png</key>
            <key type="filename">../frames/Bosses/GutsTank/MouthOpen.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>96,48,192,96</rect>
                <key>scale9Paddings</key>
                <rect>96,48,192,96</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/MechaDragonMiniBoss/Defeated.png</key>
            <key type="filename">../frames/Bosses/MechaDragonMiniBoss/Fly.png</key>
            <key type="filename">../frames/Bosses/MechaDragonMiniBoss/Shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>156,94,313,187</rect>
                <key>scale9Paddings</key>
                <rect>156,94,313,187</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/MoonHeadMiniBoss/Angry.png</key>
            <key type="filename">../frames/Bosses/MoonHeadMiniBoss/Dark.png</key>
            <key type="filename">../frames/Bosses/MoonHeadMiniBoss/Defeated.png</key>
            <key type="filename">../frames/Bosses/MoonHeadMiniBoss/Shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>8,7,16,13</rect>
                <key>scale9Paddings</key>
                <rect>8,7,16,13</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/MoonHeadMiniBoss/Awaken.png</key>
            <key type="filename">../frames/Bosses/MoonHeadMiniBoss/Crumble.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,7,48,13</rect>
                <key>scale9Paddings</key>
                <rect>24,7,48,13</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/MoonMan_v2/jump.png</key>
            <key type="filename">../frames/Bosses/MoonMan_v2/jump_shoot.png</key>
            <key type="filename">../frames/Bosses/MoonMan_v2/stand_shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,23,48,45</rect>
                <key>scale9Paddings</key>
                <rect>24,23,48,45</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/MoonMan_v2/stand.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,45,48,90</rect>
                <key>scale9Paddings</key>
                <rect>24,45,48,90</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/MoonMan_v2/throw.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>48,45,96,90</rect>
                <key>scale9Paddings</key>
                <rect>48,45,96,90</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/PreciousMan/JumpFreeze.png</key>
            <key type="filename">../frames/Bosses/PreciousMan/Run.png</key>
            <key type="filename">../frames/Bosses/RodentMan/Jump.png</key>
            <key type="filename">../frames/Bosses/RodentMan/Run.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>48,12,96,24</rect>
                <key>scale9Paddings</key>
                <rect>48,12,96,24</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/PreciousMan/StandFreeze.png</key>
            <key type="filename">../frames/Bosses/RodentMan/Shoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>36,12,72,24</rect>
                <key>scale9Paddings</key>
                <rect>36,12,72,24</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/ReactMan/Dance.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>16,20,32,40</rect>
                <key>scale9Paddings</key>
                <rect>16,20,32,40</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/ReactMan/Die.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>8,10,16,20</rect>
                <key>scale9Paddings</key>
                <rect>8,10,16,20</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/ReactMan/Run.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,24,48,48</rect>
                <key>scale9Paddings</key>
                <rect>24,24,48,48</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/ReactorMonkeyMiniBoss/Jump.png</key>
            <key type="filename">../frames/Bosses/ReactorMonkeyMiniBoss/Stand.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>38,40,76,80</rect>
                <key>scale9Paddings</key>
                <rect>38,40,76,80</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/ReactorMonkeyMiniBoss/Throw.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>152,40,304,80</rect>
                <key>scale9Paddings</key>
                <rect>152,40,304,80</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/RodentMan/Stand.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>72,12,144,24</rect>
                <key>scale9Paddings</key>
                <rect>72,12,144,24</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/Body.png</key>
            <key type="filename">../frames/Bosses/SigmaRat/BodyTittyShoot.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>37,37,74,74</rect>
                <key>scale9Paddings</key>
                <rect>37,37,74,74</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/BodyDamaged.png</key>
            <key type="filename">../frames/Bosses/SigmaRat/BodyTittyShootDamaged.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>74,37,148,74</rect>
                <key>scale9Paddings</key>
                <rect>74,37,148,74</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/ClawClosed.png</key>
            <key type="filename">../frames/Bosses/SigmaRat/ClawOpen.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>6,6,12,12</rect>
                <key>scale9Paddings</key>
                <rect>6,6,12,12</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/ClawFlash.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>12,6,24,12</rect>
                <key>scale9Paddings</key>
                <rect>12,6,24,12</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/ElectricBall.png</key>
            <key type="filename">../frames/Bosses/SigmaRat/ElectricPulse.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>16,8,32,16</rect>
                <key>scale9Paddings</key>
                <rect>16,8,32,16</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/ElectricBallDissipate.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>8,8,16,16</rect>
                <key>scale9Paddings</key>
                <rect>8,8,16,16</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/ElectricBallForm.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,8,48,16</rect>
                <key>scale9Paddings</key>
                <rect>24,8,48,16</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SigmaRat/ElectricDissipate.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>12,4,24,8</rect>
                <key>scale9Paddings</key>
                <rect>12,4,24,8</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SphinxMiniBoss/defeated.png</key>
            <key type="filename">../frames/Bosses/SphinxMiniBoss/open.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>63,63,125,125</rect>
                <key>scale9Paddings</key>
                <rect>63,63,125,125</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/SphinxMiniBoss/laugh.png</key>
            <key type="filename">../frames/Bosses/SphinxMiniBoss/wait.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>63,125,125,250</rect>
                <key>scale9Paddings</key>
                <rect>63,125,125,250</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/TimberWoman/damaged.png</key>
            <key type="filename">../frames/Bosses/TimberWoman/on_fire.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>48,24,96,48</rect>
                <key>scale9Paddings</key>
                <rect>48,24,96,48</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/TimberWoman/jump.png</key>
            <key type="filename">../frames/Bosses/TimberWoman/jump_swing.png</key>
            <key type="filename">../frames/Bosses/TimberWoman/just_land.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>48,48,96,96</rect>
                <key>scale9Paddings</key>
                <rect>48,48,96,96</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/TimberWoman/stand.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>24,48,48,96</rect>
                <key>scale9Paddings</key>
                <rect>24,48,48,96</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
            <key type="filename">../frames/Bosses/TimberWoman/stand_swing.png</key>
            <struct type="IndividualSpriteSettings">
                <key>pivotPoint</key>
                <point_f>0.5,0.5</point_f>
                <key>spriteScale</key>
                <double>1</double>
                <key>scale9Enabled</key>
                <false/>
                <key>scale9Borders</key>
                <rect>168,24,336,48</rect>
                <key>scale9Paddings</key>
                <rect>168,24,336,48</rect>
                <key>scale9FromFile</key>
                <false/>
            </struct>
        </map>
        <key>fileLists</key>
        <map type="SpriteSheetMap">
            <key>default</key>
            <struct type="SpriteSheet">
                <key>files</key>
                <array>
                    <filename>../frames/Bosses</filename>
                </array>
            </struct>
        </map>
        <key>ignoreFileList</key>
        <array/>
        <key>replaceList</key>
        <array/>
        <key>ignoredWarnings</key>
        <array/>
        <key>commonDivisorX</key>
        <uint>1</uint>
        <key>commonDivisorY</key>
        <uint>1</uint>
        <key>packNormalMaps</key>
        <false/>
        <key>autodetectNormalMaps</key>
        <true/>
        <key>normalMapFilter</key>
        <string></string>
        <key>normalMapSuffix</key>
        <string></string>
        <key>normalMapSheetFileName</key>
        <filename></filename>
        <key>exporterProperties</key>
        <map type="ExporterProperties"/>
    </struct>
</data>
