package io.izzel.aaa.listener;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.imaginary.Quaterniond;
import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.izzel.aaa.Main;
import io.izzel.aaa.util.EquipmentUtil;
import io.izzel.aaa.service.Attributes;
import org.spongepowered.api.data.property.AbstractProperty;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.entity.Equipable;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.scheduler.Task;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.function.Consumer;

@Singleton
public class ArrowListener {

    private static final double DISTANCE = 64;

    private final Provider<Main> provider;

    @Inject
    public ArrowListener(Provider<Main> provider) {
        this.provider = provider;
    }

    @SuppressWarnings("unchecked")
    @Listener
    public <T extends Equipable & Living> void on(SpawnEntityEvent event) {
        event.getEntities().stream().filter(Projectile.class::isInstance).map(Projectile.class::cast)
            .filter(it -> it.getShooter() instanceof Equipable && it.getShooter() instanceof Living)
            .forEach(projectile -> {
                T shooter = (T) projectile.getShooter();
                double tracing = EquipmentUtil.allOf(shooter, Attributes.TRACING);
                if (Math.abs(tracing) > GenericMath.DBL_EPSILON) {
                    Vector3d rot = shooter.getHeadRotation();
                    double pitch = rot.getX();
                    double yaw = rot.getY();
                    double xz = Math.cos(Math.toRadians(pitch));
                    Vector3d vec = new Vector3d(-xz * Math.sin(Math.toRadians(yaw)), -Math.sin(Math.toRadians(pitch)), xz * Math.cos(Math.toRadians(yaw)));
                    Vector3d pos = shooter.getLocation().getPosition();
                    shooter.getNearbyEntities(DISTANCE).stream()
                        .filter(it -> it != shooter)
                        .filter(Living.class::isInstance)
                        .map(Living.class::cast)
                        .min(Comparator.comparingDouble(it -> angle(vec, it.getLocation().getPosition().sub(pos))))
                        .ifPresent(living -> Task.builder().delayTicks(1).intervalTicks(1)
                            .execute(new RedirectProjectileTask(tracing, projectile, living)).submit(this.provider.get()));
                }
                double accelerate = EquipmentUtil.allOf(shooter, Attributes.ACCELERATE);
                if (Math.abs(accelerate) > GenericMath.DBL_EPSILON) {
                    Task.builder().delayTicks(1).intervalTicks(1).execute(new AccelerateProjectileTask(accelerate, projectile));
                }
            });
    }

    private static double angle(Vector3d a, Vector3d b) {
        return Math.acos(a.dot(b) / (a.length() * b.length()));
    }

    private static Vector3d rotate(Vector3d from, Vector3d to, double angle) {
        if (angle(from, to) <= Math.toRadians(1)) {
            return to;
        } else {
            return Quaterniond.fromAngleRadAxis(Math.toRadians(angle), from.cross(to)).rotate(from);
        }
    }

    private static class AccelerateProjectileTask implements Consumer<Task> {

        private final double accelerate;
        private final WeakReference<Projectile> projectileWf;

        public AccelerateProjectileTask(double accelerate, Projectile projectile) {
            this.accelerate = 1D + accelerate / 10D;
            this.projectileWf = new WeakReference<>(projectile);
        }

        @Override
        public void accept(Task task) {
            Projectile projectile = projectileWf.get();
            if (projectile == null || projectile.isOnGround()) {
                task.cancel();
                return;
            }

            projectile.setVelocity(projectile.getVelocity().mul(accelerate));

        }
    }

    private static class RedirectProjectileTask implements Consumer<Task> {

        private final double tracingValue;
        private final WeakReference<Projectile> projectileWf;
        private final WeakReference<Living> targetWf;

        private RedirectProjectileTask(double tracingValue, Projectile projectile, Living target) {
            this.tracingValue = tracingValue / 10D;
            this.projectileWf = new WeakReference<>(projectile);
            this.targetWf = new WeakReference<>(target);
        }

        @Override
        public void accept(Task task) {
            Projectile projectile = projectileWf.get();
            Living living = targetWf.get();
            if (projectile == null || projectile.isOnGround() || living == null) {
                task.cancel();
                return;
            }

            Vector3d velocity = projectile.getVelocity();
            Vector3d direction = living.getProperty(EyeLocationProperty.class)
                .map(AbstractProperty::getValue).orElse(living.getLocation().getPosition())
                .sub(projectile.getLocation().getPosition());

            projectile.setVelocity(rotate(velocity, direction, tracingValue));

        }

    }

}
