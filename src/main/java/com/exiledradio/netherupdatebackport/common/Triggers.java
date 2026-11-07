package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One advancement criterion for everything 1.12.2 has no criterion for.
 *
 * <p>1.16's advancements test things 1.12.2's criteria cannot express (charging a respawn anchor,
 * bartering, riding a strider over lava). Each stream names its moments with a string and calls
 * {@link #fire(EntityPlayer, String)} when one happens; the advancement JSON asks for it:
 *
 * <pre>
 *   "criteria": {"charged": {"trigger": "netherupdatebackport:event",
 *                            "conditions": {"event": "charge_respawn_anchor_max"}}}
 * </pre>
 *
 * <p>Registered through vanilla's own trigger table, which is private in 1.12.2, so it is reached by
 * reflection (never a patch).
 */
public final class Triggers {

    public static final ResourceLocation ID = Content.id("event");

    private static final EventTrigger TRIGGER = new EventTrigger();

    private Triggers() {
    }

    /** Called from pre-init, before any advancement is loaded. */
    public static void init() {
        try {
            Method register = ReflectionHelper.findMethod(CriteriaTriggers.class, "register", "func_192118_a",
                    ICriterionTrigger.class);
            register.invoke(null, TRIGGER);
        } catch (ReflectiveOperationException | RuntimeException e) {
            NetherUpdateBackport.LOGGER.error("Could not register the advancement trigger; this mod's advancements stay locked.", e);
        }
    }

    /** Tells every advancement waiting for this event that it happened, for this player. */
    public static void fire(EntityPlayer player, String event) {
        if (player instanceof EntityPlayerMP && !(player instanceof net.minecraftforge.common.util.FakePlayer)) {
            TRIGGER.fire(((EntityPlayerMP) player).getAdvancements(), event);
        }
    }

    public static final class Instance extends AbstractCriterionInstance {

        final String event;

        public Instance(String event) {
            super(ID);
            this.event = event;
        }
    }

    private static final class EventTrigger implements ICriterionTrigger<Instance> {

        private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners =
                new HashMap<PlayerAdvancements, Set<Listener<Instance>>>();

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public void addListener(PlayerAdvancements advancements, Listener<Instance> listener) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                set = new HashSet<Listener<Instance>>();
                this.listeners.put(advancements, set);
            }
            set.add(listener);
        }

        @Override
        public void removeListener(PlayerAdvancements advancements, Listener<Instance> listener) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) {
                    this.listeners.remove(advancements);
                }
            }
        }

        @Override
        public void removeAllListeners(PlayerAdvancements advancements) {
            this.listeners.remove(advancements);
        }

        @Override
        public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
            return new Instance(JsonUtils.getString(json, "event"));
        }

        void fire(PlayerAdvancements advancements, String event) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                return;
            }
            // Granting a criterion removes its listener, so collect first.
            List<Listener<Instance>> hit = new ArrayList<Listener<Instance>>();
            for (Listener<Instance> listener : set) {
                if (listener.getCriterionInstance().event.equals(event)) {
                    hit.add(listener);
                }
            }
            for (Listener<Instance> listener : hit) {
                listener.grantCriterion(advancements);
            }
        }
    }
}
