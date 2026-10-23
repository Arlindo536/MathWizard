package com.arlindodishi.mathwizard.ui.pixel

import com.arlindodishi.mathwizard.engine.School

/**
 * All pixel art in the game. Enemies share a few body templates recoloured per enemy; icons
 * are 8×8. Preview every sprite with `python3 tools/preview_sprites.py sheet.png`.
 */
object Sprites {

    // ---- Body templates (16×16, bosses 24×24) ----

    private val IMP = template(
        """
        ..A..........A..
        ..AA........AA..
        ...AKKKKKKKKA...
        ..K1111111111K..
        .K112222222211K.
        .K122EE22EE221K.
        .K122EP22PE221K.
        .K112222222211K.
        .K111KKKKKK111K.
        ..K11KWKKWK11K..
        ...K11111111K...
        ..K3K111111K3K..
        .K3.K113311K.3K.
        .K..K11KK11K..K.
        ....K33K.K33K...
        ....KKK...KKK...
        """,
    )

    private val SLIME = template(
        """
        ................
        ................
        .......KK.......
        ......K22K......
        .....K2221K.....
        ....K222111K....
        ...K22111111K...
        ..K2211111111K..
        ..K21EE11EE11K..
        .K21EWP11EWP13K.
        .K111111111113K.
        .K11KK1111KK13K.
        .K111KKKKKK133K.
        .K311111111333K.
        ..K3333333333K..
        ...KKKKKKKKKK...
        """,
    )

    private val WISP = template(
        """
        ......K..K......
        .....K2KK2K.....
        ....K22122K.....
        ...K2211112K....
        ...K2111111K....
        ..K211111112K...
        ..K21EE11EE1K...
        ..K1EWP11EWPK...
        ..K111111111K...
        ..K111KKK111K...
        ..K11111111K....
        ...K1111111K....
        ...K11311131K...
        ....K1K31K31K...
        ....K.K.K1K.K...
        .........K......
        """,
    )

    private val BEAST = template(
        """
        ................
        ..KK......KK....
        .K2AK....KA2K...
        .K21KKKKKK12K...
        K2211111111K....
        K21EE11111111K..
        K1EWP111111111K.
        K11111111111113K
        .KWKWK11111111K.
        ..K.K.K1111113K.
        ......K1111133K.
        ......K11KK113K.
        .....K33K..K33K.
        .....K3K...K3K..
        ....KKK...KKK...
        ................
        """,
    )

    private val GOLEM = template(
        """
        ....KKKKKKKK....
        ...K22222222K...
        ...K2EE11EE1K...
        ...K2EP11EP1K...
        ...K11111111K...
        .KKK1KKKKKK1KKK.
        K221K111111K122K
        K21K11122111K13K
        K11K11211111K13K
        K11K11111113K13K
        KKKK11111133KKKK
        ...K11KKKK13K...
        ...K11K..K13K...
        ..K111K..K133K..
        ..K333K..K333K..
        ..KKKKK..KKKKK..
        """,
    )

    private val CRAWLER = template(
        """
        ................
        ................
        .....KKKKKK.....
        ...KK222222KK...
        ..K2221111111K..
        .K221EE11EE111K.
        .K21EWP11EWP11K.
        .K111111111113K.
        KK11111AA11113KK
        K.K1111111133K.K
        K..KK111113KK..K
        ..K..KKKKKK..K..
        .K..K.K..K.K..K.
        K..K..K..K..K..K
        ..K..K....K..K..
        ................
        """,
    )

    private val FLYER = template(
        """
        ................
        K..............K
        KK....K..K....KK
        K2K...KKKK...K2K
        K22K.K1111K.K22K
        K221KK1EE1KK122K
        K2211K1PP1K1122K
        .K221111111122K.
        .K2221111111222K
        ..K221KWWK1122K.
        ..K22K1111K22K..
        ...K2K.K11K.K2K.
        ...KK..K33K..KK.
        .......K33K.....
        ........KK......
        ................
        """,
    )

    private val SERPENT = template(
        """
        ................
        ...KKKK.........
        ..K2222K........
        .K2EE22K........
        .K2WP221K.......
        .K111111K.......
        ..KKKK11K.......
        ..KAK.K11K......
        ......K11K......
        ...KKKK11KKKK...
        ..K2221111222K..
        .K221KKKKKK122K.
        .K11K......K11K.
        .K111KKKKKK113K.
        ..K3311111133K..
        ...KKKKKKKKKK...
        """,
    )

    private val DRAGON = template(
        """
        ......A..........A......
        .....AA..........AA.....
        ....AA.KKKKKKKKKK.AA....
        ....AKK2222222222KKA....
        ....K22211111111222K....
        ...K2211111111111122K...
        ...K211EEE1111EEE112K...
        ...K21EWWP1111EWWP12K...
        ...K211EEE1111EEE113K...
        ...K1111111111111113K...
        ....K11KKKKKKKKKK13K....
        ....K11KWKWKKWKWK13K....
        .KK..K11111111111K..KK..
        K22K..K111111113K..K22K.
        K222KKK1112222113KKK222K
        K2222K111122221113K2222K
        .K222K11112222113K3222K.
        ..KK2K111122221133K22KK.
        ....KK11112222113KKK....
        .....K11111111113K......
        .....K1133KKK1133K......
        ....K3333K...K3333K.....
        ....KKKKK.....KKKKK.....
        ........................
        """,
    )

    private val COLOSSUS = template(
        """
        ........KKKKKKKK........
        .......K22222222K.......
        ......K2211111122K......
        ......K21EE11EE112K.....
        ......K21WP11WP113K.....
        ......K21111111113K.....
        ......K11KKKKKK113K.....
        .......K11AAAAA13K......
        ...KKKKKK1111113KKKKK...
        ..K2222AK111113KA2222K..
        .K22111AK111113KA11122K.
        .K21111KK11AA13KK11113K.
        .K2111K1111AA1113K1113K.
        .K111K11111111113K113K..
        ..KKK.K1111111133K.KK...
        ......K1111111133K......
        ......K1111KK1133K......
        ......K111K..K133K......
        .....K1111K..K1333K.....
        .....K3333K..K3333K.....
        .....KKKKKK..KKKKKK.....
        ........................
        ........................
        ........................
        """,
    )

    private val WIZARD = template(
        """
        .......KK.......
        ......K22K......
        .....K2122K..A..
        ....K211112K.A..
        ...K21111112KAK.
        ..KKKKKKKKKKKA..
        ....KWWWWWWK.A..
        ....KWPWWPWK.A..
        ....KWWWWWWK.A..
        ...KAAWWWWAAKA..
        ..K11AAAAAA11KA.
        ..K111AAAA111KA.
        ..K1111111111KA.
        ..K1111111111KA.
        ...KKKKKKKKKK.A.
        ....K3K..K3K..A.
        """,
    )

    // ---- Enemies: template + palette ----

    private val enemies: Map<String, PixelSprite> = mapOf(
        "ember_imp" to sprite(IMP, Palette(outline = 0xFF2A0F1F, base = 0xFFE2533A, light = 0xFFFF8A4C, shadow = 0xFFA8322A, eye = 0xFFFFE066, accent = 0xFFF4D3A1)),
        "cinder_slime" to sprite(SLIME, Palette(outline = 0xFF2A0F1F, base = 0xFFC2410C, light = 0xFFFB923C, shadow = 0xFF7C2D12, eye = 0xFFFFE066, accent = 0xFFFDE68A)),
        "ash_goblin" to sprite(IMP, Palette(outline = 0xFF1C1917, base = 0xFF78716C, light = 0xFFA8A29E, shadow = 0xFF44403C, eye = 0xFFFB923C, accent = 0xFFFDBA74)),
        "flame_wisp" to sprite(WISP, Palette(outline = 0xFF2A0F1F, base = 0xFFF97316, light = 0xFFFDE047, shadow = 0xFFC2410C, eye = 0xFFFFF7CC, accent = 0xFFFDE68A)),
        "magma_golem" to sprite(GOLEM, Palette(outline = 0xFF1C0A0A, base = 0xFF57534E, light = 0xFFFF7A3D, shadow = 0xFF292524, eye = 0xFFFDE047, accent = 0xFFF97316)),
        "blaze_hound" to sprite(BEAST, Palette(outline = 0xFF2A0F1F, base = 0xFFDC2626, light = 0xFFFB923C, shadow = 0xFF7F1D1D, eye = 0xFFFDE047, accent = 0xFFFCD34D)),
        "inferno_drake" to sprite(DRAGON, Palette(outline = 0xFF2A0F1F, base = 0xFFDC2626, light = 0xFFFB923C, shadow = 0xFF7F1D1D, eye = 0xFFFDE047, accent = 0xFFFCD34D)),

        "tide_sprite" to sprite(WISP, Palette(outline = 0xFF0C1A3A, base = 0xFF38BDF8, light = 0xFFBAE6FD, shadow = 0xFF0369A1, eye = 0xFFFFFFFF, accent = 0xFFA5F3FC)),
        "frost_eel" to sprite(SERPENT, Palette(outline = 0xFF0C1A3A, base = 0xFF60A5FA, light = 0xFFDBEAFE, shadow = 0xFF1D4ED8, eye = 0xFFE0F2FE, accent = 0xFFF472B6)),
        "mist_crab" to sprite(CRAWLER, Palette(outline = 0xFF0C1A3A, base = 0xFF22D3EE, light = 0xFFA5F3FC, shadow = 0xFF0E7490, eye = 0xFFFDE047, accent = 0xFFCFFAFE)),
        "abyssal_leviathan" to sprite(DRAGON, Palette(outline = 0xFF0A1230, base = 0xFF2563EB, light = 0xFF60A5FA, shadow = 0xFF1E3A8A, eye = 0xFFA5F3FC, accent = 0xFFBAE6FD)),

        "spark_bat" to sprite(FLYER, Palette(outline = 0xFF1A1405, base = 0xFFA16207, light = 0xFFFACC15, shadow = 0xFF713F12, eye = 0xFFFEF9C3, accent = 0xFFFDE047)),
        "storm_beetle" to sprite(CRAWLER, Palette(outline = 0xFF111827, base = 0xFF64748B, light = 0xFFFACC15, shadow = 0xFF334155, eye = 0xFFFDE047, accent = 0xFFFEF08A)),
        "volt_serpent" to sprite(SERPENT, Palette(outline = 0xFF1A1405, base = 0xFFEAB308, light = 0xFFFEF08A, shadow = 0xFFA16207, eye = 0xFFFFFFFF, accent = 0xFFF472B6)),
        "thunder_titan" to sprite(COLOSSUS, Palette(outline = 0xFF111827, base = 0xFF475569, light = 0xFFFACC15, shadow = 0xFF1E293B, eye = 0xFFFEF08A, accent = 0xFFFDE047)),

        "burrow_mole" to sprite(BEAST, Palette(outline = 0xFF1C1208, base = 0xFF92400E, light = 0xFFD97706, shadow = 0xFF451A03, eye = 0xFFFDE68A, accent = 0xFFFCD34D)),
        "stone_tortoise" to sprite(GOLEM, Palette(outline = 0xFF14140F, base = 0xFF65A30D, light = 0xFFA3E635, shadow = 0xFF3F6212, eye = 0xFFFEF9C3, accent = 0xFFD9F99D)),
        "thorn_beast" to sprite(BEAST, Palette(outline = 0xFF0F1A0A, base = 0xFF4D7C0F, light = 0xFF84CC16, shadow = 0xFF365314, eye = 0xFFFDE047, accent = 0xFFD9F99D)),
        "mountain_giant" to sprite(COLOSSUS, Palette(outline = 0xFF14140F, base = 0xFF78716C, light = 0xFFA3E635, shadow = 0xFF44403C, eye = 0xFFFEF9C3, accent = 0xFFBEF264)),

        "prism_moth" to sprite(FLYER, Palette(outline = 0xFF1E1A0A, base = 0xFFF9A8D4, light = 0xFFFEF3C7, shadow = 0xFFDB2777, eye = 0xFFFFFFFF, accent = 0xFFFDE68A)),
        "halo_wisp" to sprite(WISP, Palette(outline = 0xFF2A2410, base = 0xFFFDE68A, light = 0xFFFFFBEB, shadow = 0xFFD97706, eye = 0xFFFFFFFF, accent = 0xFFFEF3C7)),
        "mirror_knight" to sprite(GOLEM, Palette(outline = 0xFF1F2937, base = 0xFFCBD5E1, light = 0xFFF8FAFC, shadow = 0xFF64748B, eye = 0xFF7DD3FC, accent = 0xFFFDE68A)),
        "solar_sphinx" to sprite(COLOSSUS, Palette(outline = 0xFF2A1A05, base = 0xFFF59E0B, light = 0xFFFDE68A, shadow = 0xFFB45309, eye = 0xFFFFFFFF, accent = 0xFFFEF3C7)),

        "shade_cat" to sprite(BEAST, Palette(outline = 0xFF0B0618, base = 0xFF6D28D9, light = 0xFFA78BFA, shadow = 0xFF3B0764, eye = 0xFFE9D5FF, accent = 0xFFC4B5FD)),
        "echo_wraith" to sprite(WISP, Palette(outline = 0xFF0B0618, base = 0xFF7C3AED, light = 0xFFC4B5FD, shadow = 0xFF3B0764, eye = 0xFFF5F3FF, accent = 0xFFDDD6FE)),
        "void_spider" to sprite(CRAWLER, Palette(outline = 0xFF0B0618, base = 0xFF4338CA, light = 0xFF818CF8, shadow = 0xFF312E81, eye = 0xFFF0ABFC, accent = 0xFFC4B5FD)),
        "night_lich" to sprite(COLOSSUS, Palette(outline = 0xFF05020D, base = 0xFF7E22CE, light = 0xFFC084FC, shadow = 0xFF3B0764, eye = 0xFF86EFAC, accent = 0xFFE9D5FF)),
    )

    val enemyIds: Set<String> get() = enemies.keys

    fun enemy(id: String): PixelSprite = enemies[id] ?: enemies.getValue("ember_imp")

    val wizard: PixelSprite = sprite(
        WIZARD,
        Palette(outline = 0xFF140C2A, base = 0xFF5B21B6, light = 0xFF8B5CF6, shadow = 0xFF2E1065, eye = 0xFFFDE047, accent = 0xFFFDE68A, white = 0xFFF1F5F9),
    )

    // ---- 8×8 icons ----

    private val FLAME = template(
        """
        ...K....
        ..K2K...
        ..K21K..
        .K2112K.
        .K1EE1K.
        K11EE11K
        .K1111K.
        ..KKKK..
        """,
    )
    private val FEATHER = template(
        """
        ......KK
        .....K2K
        ....K21K
        ...K21K.
        ..K21K..
        .K21K...
        .KKK....
        K.......
        """,
    )
    private val HEART = template(
        """
        .KK..KK.
        K22KK21K
        K211111K
        K111111K
        .K1111K.
        ..K11K..
        ...KK...
        ........
        """,
    )
    private val POTION = template(
        """
        ...KK...
        ...KK...
        ..K22K..
        .K2111K.
        K211111K
        K1E1111K
        K111113K
        .KKKKKK.
        """,
    )
    private val SHIELD = template(
        """
        KKKKKKKK
        K222211K
        K211111K
        K21AA11K
        K11AA13K
        .K1111K.
        ..K13K..
        ...KK...
        """,
    )
    private val BOLT = template(
        """
        ....KKK.
        ...K22K.
        ..K22K..
        .K2222K.
        .KKK22K.
        ..K22K..
        .K22K...
        .KKK....
        """,
    )
    private val CROWN = template(
        """
        ........
        K..KK..K
        K2K22K2K
        K222222K
        K2E22E2K
        K111111K
        KKKKKKKK
        ........
        """,
    )
    private val ORB = template(
        """
        ..KKKK..
        .K2211K.
        K221111K
        K211E11K
        K11EE11K
        K111113K
        .K1133K.
        ..KKKK..
        """,
    )
    private val DROP = template(
        """
        ...KK...
        ..K21K..
        ..K21K..
        .K2111K.
        .K2111K.
        K211111K
        K111113K
        .KKKKKK.
        """,
    )
    private val EYE = template(
        """
        ........
        ..KKKK..
        .K2222K.
        K22EE22K
        K2EPPE2K
        .K2EE2K.
        ..KKKK..
        ........
        """,
    )
    private val WING = template(
        """
        K.......
        KK....K.
        K2K..K2K
        K22KK22K
        .K2222K.
        ..K22K..
        ...KK...
        ...K....
        """,
    )
    private val SWORD = template(
        """
        ......KK
        .....K2K
        ....K2K.
        .K.K2K..
        ..K2K...
        .KAK....
        KAK.K...
        KK......
        """,
    )
    private val ROCK = template(
        """
        ........
        ..KKKK..
        .K2211K.
        K221111K
        K211113K
        K111133K
        .KKKKKK.
        ........
        """,
    )
    private val SUN = template(
        """
        ...A....
        .A.2.A..
        ..222...
        A22E22A.
        ..222...
        .A.2.A..
        ...A....
        ........
        """,
    )
    private val MOON = template(
        """
        ..KKKK..
        .K22KK..
        K22K....
        K21K....
        K21K....
        K211KKK.
        .K1111K.
        ..KKKK..
        """,
    )
    private val CRYSTAL = template(
        """
        ..KKKK..
        .K2222K.
        K222211K
        K211111K
        .K1113K.
        ..K13K..
        ...KK...
        ........
        """,
    )
    private val BULB = template(
        """
        ..KKKK..
        .K2EE2K.
        .K2EE1K.
        .K2111K.
        ..K11K..
        ..KAAK..
        ..KAAK..
        ...KK...
        """,
    )
    private val LOCK = template(
        """
        ..KKKK..
        .K....K.
        .K....K.
        KKKKKKKK
        K222211K
        K21EE11K
        K111113K
        KKKKKKKK
        """,
    )
    private val HOURGLASS = template(
        """
        KKKKKKKK
        .K2222K.
        ..K22K..
        ...KK...
        ..K1EK..
        .K1EE1K.
        KKKKKKKK
        ........
        """,
    )
    private val BOOK = template(
        """
        ........
        .KKKKKK.
        K22KK22K
        K21KK12K
        K21KK12K
        K11KK11K
        KKKKKKKK
        ...KK...
        """,
    )

    private val SPEAKER = template(
        """
        ...KK...
        ..K2K...
        KK21K.A.
        K221K..A
        K211K..A
        KK11K.A.
        ..K1K...
        ...KK...
        """,
    )
    private val SPEAKER_OFF = template(
        """
        ...KK...
        ..K2K...
        KK21KE.E
        K221K.E.
        K211KE.E
        KK11K...
        ..K1K...
        ...KK...
        """,
    )
    private val NOTE = template(
        """
        ...KKKKK
        ...K222K
        ...K...K
        ...K...K
        .KKK.KKK
        K22KK22K
        K11KK11K
        .KK..KK.
        """,
    )
    private val NOTE_OFF = template(
        """
        E..KKKKK
        .E.K222K
        ..EK...K
        ...E...K
        .KKKE.KK
        K22KKE2K
        K11KK1EK
        .KK..KKE
        """,
    )

    private fun icon(rows: List<String>, base: Long, light: Long, shadow: Long, eye: Long = 0xFFFFF6E0, accent: Long = 0xFFFDE68A) =
        sprite(rows, Palette(outline = 0xFF1A0D10, base = base, light = light, shadow = shadow, eye = eye, accent = accent))

    private val fire = icon(FLAME, 0xFFF97316, 0xFFFDE047, 0xFFC2410C, eye = 0xFFFFF7CC)
    private val water = icon(DROP, 0xFF38BDF8, 0xFFBAE6FD, 0xFF0369A1)
    private val lightning = icon(BOLT, 0xFFEAB308, 0xFFFEF08A, 0xFFA16207)
    private val earth = icon(ROCK, 0xFF65A30D, 0xFFA3E635, 0xFF3F6212)
    private val light = icon(SUN, 0xFFF59E0B, 0xFFFDE68A, 0xFFB45309, eye = 0xFFFFFFFF, accent = 0xFFFDE68A)
    private val shadow = icon(MOON, 0xFF7C3AED, 0xFFC4B5FD, 0xFF3B0764)

    val heart = icon(HEART, 0xFF2DD4BF, 0xFF99F6E4, 0xFF0F766E)
    val crystal = icon(CRYSTAL, 0xFF38BDF8, 0xFFE0F2FE, 0xFF0369A1)
    val hint = icon(BULB, 0xFFFACC15, 0xFFFEF9C3, 0xFFA16207, eye = 0xFFFFFFFF, accent = 0xFF94A3B8)
    val soundOn = icon(SPEAKER, 0xFFE2E8F0, 0xFFFFFFFF, 0xFF94A3B8, accent = 0xFF5EEAD4)
    val soundOff = icon(SPEAKER_OFF, 0xFF94A3B8, 0xFFCBD5E1, 0xFF64748B, eye = 0xFFFF4D6D)
    val musicOn = icon(NOTE, 0xFFE2E8F0, 0xFFFFFFFF, 0xFF94A3B8)
    val musicOff = icon(NOTE_OFF, 0xFF94A3B8, 0xFFCBD5E1, 0xFF64748B, eye = 0xFFFF4D6D)
    val lock = icon(LOCK, 0xFF94A3B8, 0xFFE2E8F0, 0xFF475569, eye = 0xFF1A0D10)

    fun school(school: School): PixelSprite = when (school) {
        School.ARITHMETIC -> fire
        School.FRACTIONS -> water
        School.ALGEBRA -> lightning
        School.POWERS -> earth
        School.GEOMETRY -> light
        School.SEQUENCES -> shadow
    }

    private val relics: Map<String, PixelSprite> = mapOf(
        "ember_focus" to fire,
        "quicksilver_quill" to icon(FEATHER, 0xFFCBD5E1, 0xFFF8FAFC, 0xFF64748B),
        "heartstone" to icon(HEART, 0xFFF43F5E, 0xFFFDA4AF, 0xFF9F1239),
        "healing_draught" to icon(POTION, 0xFF22C55E, 0xFFBBF7D0, 0xFF15803D, eye = 0xFFF0FDF4),
        "iron_ward" to icon(SHIELD, 0xFF64748B, 0xFFCBD5E1, 0xFF334155, accent = 0xFFFACC15),
        "swift_sigil" to lightning,
        "combo_crown" to icon(CROWN, 0xFFEAB308, 0xFFFDE68A, 0xFFA16207, eye = 0xFFF43F5E),
        "mana_shield" to icon(ORB, 0xFF6366F1, 0xFFC7D2FE, 0xFF3730A3, eye = 0xFFE0E7FF),
        "vampiric_rune" to icon(DROP, 0xFFDC2626, 0xFFFCA5A5, 0xFF7F1D1D),
        "oracle_eye" to icon(EYE, 0xFFE0F2FE, 0xFFFFFFFF, 0xFF94A3B8, eye = 0xFF38BDF8),
        "phoenix_feather" to icon(WING, 0xFFF97316, 0xFFFDE047, 0xFFC2410C),
        "giant_slayer" to icon(SWORD, 0xFF94A3B8, 0xFFF1F5F9, 0xFF475569, accent = 0xFFB45309),
    )

    val relicIds: Set<String> get() = relics.keys

    fun relic(id: String): PixelSprite = relics.getValue(id)

    private val upgrades: Map<String, PixelSprite> = mapOf(
        "vitality" to icon(HEART, 0xFFF43F5E, 0xFFFDA4AF, 0xFF9F1239),
        "second_wind" to icon(POTION, 0xFF22C55E, 0xFFBBF7D0, 0xFF15803D, eye = 0xFFF0FDF4),
        "focus" to icon(HOURGLASS, 0xFFFDE68A, 0xFFFFFBEB, 0xFFD97706, eye = 0xFFF59E0B),
        "hint" to hint,
        "power" to fire,
        "relic_insight" to icon(EYE, 0xFFE0F2FE, 0xFFFFFFFF, 0xFF94A3B8, eye = 0xFFA855F7),
        "starting_relic" to icon(CROWN, 0xFFEAB308, 0xFFFDE68A, 0xFFA16207, eye = 0xFF38BDF8),
    )

    /** Icon for an upgrade; school unlocks use the school's icon. */
    fun upgrade(id: String): PixelSprite =
        upgrades[id] ?: School.entries.firstOrNull { "school_${it.name.lowercase()}" == id }?.let(::school) ?: crystal

    val branchSchools = icon(BOOK, 0xFF7C3AED, 0xFFDDD6FE, 0xFF4C1D95)
    val branchVitality = icon(HEART, 0xFFF43F5E, 0xFFFDA4AF, 0xFF9F1239)
    val branchFocus = icon(HOURGLASS, 0xFFFDE68A, 0xFFFFFBEB, 0xFFD97706, eye = 0xFFF59E0B)
    val branchArcana = icon(ORB, 0xFF6366F1, 0xFFC7D2FE, 0xFF3730A3, eye = 0xFFE0E7FF)
}
