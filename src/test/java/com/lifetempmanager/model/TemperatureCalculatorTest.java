package com.lifetempmanager.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemperatureCalculatorTest {

    private static final double DEFAULT_TEMP = 20.0;
    private static final double BASE_MIN = -80.0;
    private static final double BASE_MAX = 100.0;
    private static final double STAGE1_OFFSET = 40.0;
    private static final double STAGE2_OFFSET = 20.0;
    private static final double WARNING_OFFSET = 20.0;
    private static final int STAGE1_IMMUNITY = 2;
    private static final int STAGE2_DMG_IMMUNITY = 4;

    private TemperatureEvaluation eval(double temp, ArmorResistance res) {
        return TemperatureCalculator.evaluate(
                temp, DEFAULT_TEMP, BASE_MIN, BASE_MAX, res,
                STAGE1_OFFSET, STAGE2_OFFSET, WARNING_OFFSET,
                STAGE1_IMMUNITY, STAGE2_DMG_IMMUNITY
        );
    }

    @Test
    void testDefaultNoArmorThresholds() {
        // 平常時 20℃
        TemperatureEvaluation normal = eval(20.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.NORMAL, normal.severity());
        assertFalse(normal.applySlownessBlindness());
        assertFalse(normal.applyContinuousDamage());
        assertFalse(normal.applyHeatVisual());
        assertFalse(normal.applyColdVisual());
        assertFalse(normal.inWarningZone());
        assertFalse(normal.triggerLethal());

        // 高温 Stage1 (60℃ = 100 - 40)
        TemperatureEvaluation hot1 = eval(60.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.HOT_STAGE1, hot1.severity());
        assertTrue(hot1.applySlownessBlindness());
        assertFalse(hot1.applyContinuousDamage());
        assertFalse(hot1.applyHeatVisual());
        assertFalse(hot1.inWarningZone());

        // 高温 Stage2 + 警告 (80℃ = 100 - 20)
        TemperatureEvaluation hot2 = eval(80.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.HOT_STAGE2, hot2.severity());
        assertTrue(hot2.applySlownessBlindness());
        assertTrue(hot2.applyContinuousDamage());
        assertTrue(hot2.applyHeatVisual());
        assertTrue(hot2.inWarningZone());
        assertTrue(hot2.hotWarning());
        assertFalse(hot2.triggerLethal());

        // 高温 即死 (100℃)
        TemperatureEvaluation hotLethal = eval(100.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.HOT_LETHAL, hotLethal.severity());
        assertTrue(hotLethal.triggerLethal());

        // 低温 Stage1 (-40℃ = -80 + 40)
        TemperatureEvaluation cold1 = eval(-40.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.COLD_STAGE1, cold1.severity());
        assertTrue(cold1.applySlownessBlindness());
        assertFalse(cold1.applyContinuousDamage());
        assertFalse(cold1.applyColdVisual());

        // 低温 Stage2 + 警告 (-60℃ = -80 + 20)
        TemperatureEvaluation cold2 = eval(-60.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.COLD_STAGE2, cold2.severity());
        assertTrue(cold2.applySlownessBlindness());
        assertTrue(cold2.applyContinuousDamage());
        assertTrue(cold2.applyColdVisual());
        assertTrue(cold2.inWarningZone());
        assertFalse(cold2.hotWarning());

        // 低温 即死 (-80℃)
        TemperatureEvaluation coldLethal = eval(-80.0, ArmorResistance.NONE);
        assertEquals(TemperatureSeverity.COLD_LETHAL, coldLethal.severity());
        assertTrue(coldLethal.triggerLethal());
    }

    @Test
    void testTwoPieceHeatArmorNegatesStage1AndExtendsLimit() {
        // 耐熱2部位: 上限 200℃, 下限 -80℃ (独立)
        ArmorResistance twoHeat = new ArmorResistance(2, 100.0, 0, 0.0);

        // 160℃ (200 - 40): 鈍足・盲目は2部位で無効化
        TemperatureEvaluation hot1 = eval(160.0, twoHeat);
        assertEquals(200.0, hot1.maxLimit());
        assertEquals(TemperatureSeverity.HOT_STAGE1, hot1.severity());
        assertFalse(hot1.applySlownessBlindness());
        assertFalse(hot1.applyContinuousDamage());

        // 180℃ (200 - 20): 継続ダメージ・炎上・警告は発動、鈍足・盲目は無効
        TemperatureEvaluation hot2 = eval(180.0, twoHeat);
        assertEquals(TemperatureSeverity.HOT_STAGE2, hot2.severity());
        assertFalse(hot2.applySlownessBlindness());
        assertTrue(hot2.applyContinuousDamage());
        assertTrue(hot2.applyHeatVisual());
        assertTrue(hot2.inWarningZone());

        // 低温側は独立しているため -60℃ で通常通りデメリット発動
        TemperatureEvaluation cold2 = eval(-60.0, twoHeat);
        assertEquals(-80.0, cold2.minLimit());
        assertTrue(cold2.applySlownessBlindness());
        assertTrue(cold2.applyContinuousDamage());
        assertTrue(cold2.applyColdVisual());
    }

    @Test
    void testFullSetArmorNegatesDamageButKeepsVisualAndLethal() {
        // 耐熱フルセット4部位 (+200℃ -> 上限 300℃)
        ArmorResistance fullHeat = new ArmorResistance(4, 200.0, 0, 0.0);
        TemperatureEvaluation hot2 = eval(280.0, fullHeat);
        assertEquals(300.0, hot2.maxLimit());
        assertEquals(TemperatureSeverity.HOT_STAGE2, hot2.severity());
        assertFalse(hot2.applySlownessBlindness());
        assertFalse(hot2.applyContinuousDamage()); // 4部位で継続ダメージ無効
        assertTrue(hot2.applyHeatVisual());        // 炎上視界エフェクトは維持
        assertTrue(hot2.inWarningZone());          // 警告は維持
        assertFalse(hot2.triggerLethal());

        TemperatureEvaluation hotLethal = eval(300.0, fullHeat);
        assertTrue(hotLethal.triggerLethal());     // 即死は無効化されない

        // 耐冷フルセット4部位 (-200℃ -> 下限 -280℃)
        ArmorResistance fullCold = new ArmorResistance(0, 0.0, 4, 200.0);
        TemperatureEvaluation cold2 = eval(-260.0, fullCold);
        assertEquals(-280.0, cold2.minLimit());
        assertEquals(TemperatureSeverity.COLD_STAGE2, cold2.severity());
        assertFalse(cold2.applySlownessBlindness());
        assertFalse(cold2.applyContinuousDamage()); // 4部位で継続ダメージ無効
        assertTrue(cold2.applyColdVisual());        // 粉雪視界エフェクトは維持
        assertTrue(cold2.inWarningZone());

        TemperatureEvaluation coldLethal = eval(-280.0, fullCold);
        assertTrue(coldLethal.triggerLethal());
    }

    @Test
    void testMixedArmorIndependentCalculation() {
        // 耐熱2部位 + 耐冷2部位 -> 上限 200℃, 下限 -180℃
        ArmorResistance mixed = new ArmorResistance(2, 100.0, 2, 100.0);

        TemperatureEvaluation hot = eval(180.0, mixed);
        assertEquals(200.0, hot.maxLimit());
        assertEquals(-180.0, hot.minLimit());
        assertFalse(hot.applySlownessBlindness());
        assertTrue(hot.applyContinuousDamage());

        TemperatureEvaluation cold = eval(-160.0, mixed);
        assertFalse(cold.applySlownessBlindness());
        assertTrue(cold.applyContinuousDamage());
        assertTrue(cold.applyColdVisual());
    }

    @Test
    void testNaturalRecoveryStep() {
        // 高温から20℃へ戻る (1秒あたり20℃、20℃で停止)
        assertEquals(30.0, TemperatureCalculator.stepRecovery(50.0, 20.0, 20.0));
        assertEquals(20.0, TemperatureCalculator.stepRecovery(30.0, 20.0, 20.0));
        assertEquals(20.0, TemperatureCalculator.stepRecovery(20.0, 20.0, 20.0));

        // 低温から20℃へ戻る (1秒あたり20℃、20℃で停止)
        assertEquals(5.0, TemperatureCalculator.stepRecovery(-15.0, 20.0, 20.0));
        assertEquals(20.0, TemperatureCalculator.stepRecovery(5.0, 20.0, 20.0));
    }

    @Test
    void testBossBarProgressClamping() {
        assertEquals(0.0f, TemperatureCalculator.computeBossBarProgress(-80.0, -80.0, 100.0));
        assertEquals(1.0f, TemperatureCalculator.computeBossBarProgress(100.0, -80.0, 100.0));
        assertEquals(0.5f, TemperatureCalculator.computeBossBarProgress(10.0, -80.0, 100.0), 0.001f);
        assertEquals(0.0f, TemperatureCalculator.computeBossBarProgress(-500.0, -80.0, 100.0));
        assertEquals(1.0f, TemperatureCalculator.computeBossBarProgress(500.0, -80.0, 100.0));
    }
}
