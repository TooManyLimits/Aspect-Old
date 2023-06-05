package io.github.moonlightmaya.model.animation;

import io.github.moonlightmaya.Aspect;
import io.github.moonlightmaya.manage.data.BaseStructures;
import petpet.external.PetPetWhitelist;
import petpet.lang.run.PetPetException;
import petpet.types.PetPetList;
import petpet.types.immutable.PetPetListView;

@PetPetWhitelist
public class Animation {

    //Properties
    public final Aspect aspect;
    public final String name;
    public final float length;

    //Fields that change
    private float time;
    private float speed = 1f;
    private float weight = 1f;
    private LoopMode loopMode;
    private boolean override; //Currently unused

    private boolean isPlaying; //Whether time is currently advancing

    public PetPetList<Animator> animators = new PetPetList<>();

    public Animation(BaseStructures.AnimationStructure baseStructure, Aspect aspect) {
        this.aspect = aspect;
        this.name = baseStructure.name();
        this.length = baseStructure.length();

        this.time = 0;
        this.loopMode = baseStructure.loopMode();
        this.override = baseStructure.override();
    }

    public void tick() {
        if (isPlaying) {
            time += 0.05f * speed;
            if (time > length) switch (loopMode) {
                case ONCE -> {
                    time = length;
                    stop();
                }
                case HOLD -> {
                    time = length;
                    pause();
                }
                case LOOP -> time = (time % length + length) % length;
            }
        }
    }

    public void render(float delta) {
        if (isPlaying) {
            float d = delta * 0.05f * speed;
            for (Animator animator : animators) {
                animator.updateTime(time + d);
            }
        }
    }

    // CONTROL FLOW

    /**
     * Pause the animation. May be resume()d later.
     */
    @PetPetWhitelist
    public void pause() {
        isPlaying = false;
    }

    /**
     * Resumes the animation after it's been pause()d.
     */
    @PetPetWhitelist
    public void resume() {
        isPlaying = true;
    }

    /**
     * Starts the animation from the beginning!
     */
    @PetPetWhitelist
    public void start() {
        time = 0;
        isPlaying = true;
    }

    /**
     * Stops the animation entirely, deactivating the animators.
     * It'll need to be start()ed again to start again.
     */
    @PetPetWhitelist
    public void stop() {
        isPlaying = false;
        //Deactivate all animators and such
        for (Animator animator : animators) {
            animator.deactivate();
        }
    }

    //SETTING AND GETTING
    @PetPetWhitelist
    public double time_0() {
        return time;
    }
    @PetPetWhitelist
    public Animation time_1(double d) {
        this.time = (float) d;
        return this;
    }
    @PetPetWhitelist
    public double speed_0() {
        return speed;
    }
    @PetPetWhitelist
    public Animation speed_1(double d) {
        this.speed = (float) d;
        return this;
    }
    @PetPetWhitelist
    public double weight_0() {
        return weight;
    }
    @PetPetWhitelist
    public Animation weight_1(double d) {
        this.weight = (float) d;
        return this;
    }
    @PetPetWhitelist
    public String loopMode_0() {
        return this.loopMode.toString();
    }
    @PetPetWhitelist
    public Animation loopMode_1(String mode) {
        try {
            this.loopMode = LoopMode.valueOf(mode);
        } catch (Exception e) {
             throw new PetPetException("Unrecognized loop mode \"" + mode + "\"");
        }
        return this;
    }
    @PetPetWhitelist
    public boolean playing_0() {
        return this.isPlaying;
    }
    @PetPetWhitelist
    public Animation playing_1(boolean b) {
        this.isPlaying = b;
        return this;
    }
    @PetPetWhitelist
    public String name_0() {
        return this.name;
    }
    @PetPetWhitelist
    public double length_0() {
        return this.length;
    }
    @PetPetWhitelist
    public PetPetListView<Animator> animators() {
        return this.animators.view();
    }

    public enum LoopMode {
        ONCE,
        HOLD,
        LOOP
    }

    public String toString() {
        return "Animation(" + animators.size() + " animators)";
    }

}
