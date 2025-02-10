package com.ykn.fmod.server.base.schedule;

public class ScheduledTask {

    private int delay;
    private int duration;

    public ScheduledTask(int delay, int duration) {
        this.delay = delay;
        this.duration = duration;
    }

    public void tick() {
        if (!isFinished() && shouldCancel()) {
            cancel();
        } else if (delay > 0) {
            delay--;
            if (delay == 0) {
                onTrigger();
            }
        } else if (duration > 0) {
            onTick();
            duration--;
            if (duration == 0) {
                onFinish();
            }
        }
    }

    public boolean isFinished() {
        return delay <= 0 && duration <= 0;
    }

    public void cancel() {
        delay = 0;
        duration = 0;
    }

    public void onTick() {
        // Override by the subclass
    }

    public void onTrigger() {
        // Override by the subclass
    }

    public void onFinish() {
        // Override by the subclass
    }

    public boolean shouldCancel() {
        // Override by the subclass
        return false;
    }

    public void reschedule(int delay, int duration) {
        this.delay = delay;
        this.duration = duration;
    }

    public int getDelay() {
        return delay;
    }

    public int getDuration() {
        return duration;
    }
 }
