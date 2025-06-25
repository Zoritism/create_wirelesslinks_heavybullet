package com.zoritism.wirelesslinks.foundation.animation;

public class LerpedFloat {
    public enum Chaser {
        EXP, LINEAR
    }

    protected float value;
    protected float chaseTarget;
    protected float speed;
    protected Chaser chaser;

    public static LerpedFloat linear() {
        return new LerpedFloat().chase(0, 1, Chaser.LINEAR);
    }

    public static LerpedFloat exp() {
        return new LerpedFloat().chase(0, 1, Chaser.EXP);
    }

    public LerpedFloat startWithValue(float value) {
        this.value = value;
        this.chaseTarget = value;
        return this;
    }

    public LerpedFloat chase(float target, float speed, Chaser chaser) {
        this.chaseTarget = target;
        this.speed = speed;
        this.chaser = chaser;
        return this;
    }

    public void tickChaser() {
        float delta = chaseTarget - value;
        if (Math.abs(delta) < 1e-4f)
            return;

        float partial = switch (chaser) {
            case EXP -> delta * speed;
            case LINEAR -> Math.signum(delta) * speed;
        };

        if (Math.abs(partial) > Math.abs(delta))
            partial = delta;

        value += partial;
    }

    public float getValue(float partialTicks) {
        return value;
    }

    public float getValue() {
        return value;
    }
}
