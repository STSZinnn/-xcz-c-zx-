package com.example.punchsnowball;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import java.util.*;

/** FrostPVP Reborn combat: exactly one F-key active per held weapon, plus a passive.
 *  Works with the original PDC item identity tags and existing trust list.
 *  All visuals are server-side: no installed client mods needed. */
final class FrostRebornCombat implements Listener {
    private final PunchSnowballPlugin plugin;
    private final Map<UUID, Map<String, Long>> readyAt = new HashMap<>();
    private final Map<UUID, Map<UUID, Mark>> frostMarks = new HashMap<>();
    private final Map<UUID, Long> guard = new HashMap<>();
    private record Mark(int stacks, long until) {}
    private static final Set<String> WEAPONS = Set.of("sword", "battleaxe", "spear", "bow", "crossbow",
            "grapple", "riptide", "firework", "shield", "totem", "pickaxe_fortune", "pickaxe_silk", "rod", "horn");
    private static final Map<String, Integer> CD = Map.ofEntries(
            Map.entry("sword",35),Map.entry("battleaxe",32),Map.entry("spear",28),
            Map.entry("bow",42),Map.entry("crossbow",38),Map.entry("grapple",24),
            Map.entry("riptide",26),Map.entry("firework",48),Map.entry("shield",42),
            Map.entry("totem",46),Map.entry("rod",16),Map.entry("horn",60));

    FrostRebornCombat(PunchSnowballPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        new BukkitRunnable() {
            @Override public void run() { tickPassives(); }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private String held(Player player) {
        String id = FrostTooltips.identify(player.getInventory().getItemInMainHand());
        return id != null && WEAPONS.contains(id) ? id : null;
    }
    private boolean valid(Player owner, LivingEntity victim) {
        return victim.isValid() && !victim.isDead() && plugin.rebornTarget(owner.getUniqueId(), victim);
    }
    private long now() { return System.currentTimeMillis(); }
    private boolean claim(Player player, String id) {
        int seconds = plugin.getConfig().getInt("reborn."+id+".cooldown-seconds", CD.getOrDefault(id,0));
        if (seconds <= 0) return true;
        long end = readyAt.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(id, 0L);
        if (end > now()) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(ChatColor.AQUA + "❄ " + ChatColor.RED
                    + String.format(Locale.US,"%.1fs",(end-now())/1000.0) + " until ready"));
            return false;
        }
        readyAt.computeIfAbsent(player.getUniqueId(), x -> new HashMap<>()).put(id, now()+seconds*1000L);
        plugin.rebornRecordReady(player,id,seconds*20);
        return true;
    }
    private void damage(Player caster, LivingEntity victim, double amount) {
        if (!valid(caster,victim)) return;
        // Vanilla-attributed damage retains armor, protection, death and totem interactions.
        victim.damage(amount,caster);
    }
    private List<LivingEntity> targets(Player caster, Location origin, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity entity : origin.getWorld().getNearbyEntities(origin,radius,radius,radius)) {
            if (entity instanceof LivingEntity living && valid(caster,living)
                    && living.getLocation().distanceSquared(origin) <= radius*radius) out.add(living);
        }
        return out;
    }
    private LivingEntity aim(Player user, double distance) {
        RayTraceResult result = user.getWorld().rayTraceEntities(user.getEyeLocation(),
                user.getEyeLocation().getDirection(),distance,.6,
                e -> e instanceof LivingEntity living && valid(user,living));
        return result != null && result.getHitEntity() instanceof LivingEntity target ? target : null;
    }
    private void sphere(Location center, Particle particle, int count, double radius) {
        World world = center.getWorld();
        for (int i=0;i<count;i++) {
            double phi = i*2.39996;
            double y = 1 - 2*(i+.5)/count;
            double r = Math.sqrt(Math.max(0,1-y*y));
            world.spawnParticle(particle, center.clone().add(r*Math.cos(phi)*radius,y*radius,r*Math.sin(phi)*radius),1,0,0,0,0);
        }
    }
    private void burst(Location loc, int count) {
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE,loc,count,.65,.6,.65,.05);
        loc.getWorld().playSound(loc,Sound.BLOCK_GLASS_BREAK,.85f,1.1f);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void activate(PlayerSwapHandItemsEvent event) {
        Player player=event.getPlayer();
        String id=held(player);
        if (id==null) return;
        event.setCancelled(true);
        if (plugin.rebornFrozen(player) || player.isDead() || player.getGameMode()==GameMode.SPECTATOR) return;
        if (id.startsWith("pickaxe_")) { plugin.rebornTogglePickaxe(player); return; }
        if (!claim(player,id)) return;
        player.swingMainHand();
        switch (id) {
            case "sword" -> absoluteZero(player);
            case "battleaxe" -> glacierBreaker(player);
            case "spear" -> frostSpires(player);
            case "bow" -> arrowfall(player);
            case "crossbow" -> sonicFrost(player);
            case "grapple" -> frozenTether(player);
            case "riptide" -> thunderfall(player);
            case "firework" -> skyHunters(player);
            case "shield" -> crystalGuard(player);
            case "totem" -> winterBlessing(player);
            case "rod" -> anchorPull(player);
            case "horn" -> callOfWinter(player);
        }
    }
    private void absoluteZero(Player player) {
        Location eye=player.getEyeLocation();
        Vector forward=eye.getDirection().setY(0);
        if (forward.lengthSquared()<.01) forward=new Vector(0,0,1);
        forward.normalize();
        final Vector dir=forward.clone();
        player.getWorld().playSound(eye,Sound.BLOCK_BEACON_POWER_SELECT,1.1f,.7f);
        sphere(player.getLocation().add(0,1,0),Particle.SNOWFLAKE,28,1.6);
        Bukkit.getScheduler().runTaskLater(plugin,()-> {
            if (!player.isOnline() || player.isDead() || !heldIs(player,"sword")) return;
            Location start=player.getEyeLocation().add(0,-.65,0);
            Set<UUID> hit=new HashSet<>();
            new BukkitRunnable(){int step=0; @Override public void run(){
                if (step>=12 || !player.isOnline()){cancel();return;}
                Location middle=start.clone().add(dir.clone().multiply(step+1));
                Vector side=new Vector(-dir.getZ(),0,dir.getX());
                for(double width=-2.0;width<=2.0;width+=.35) {
                    double rise=Math.sqrt(Math.max(0,1-(width*width/4)))*1.2;
                    middle.getWorld().spawnParticle(Particle.SNOWFLAKE,middle.clone().add(side.clone().multiply(width)).add(0,rise,0),1,0,0,0,0);
                }
                for (LivingEntity e:targets(player,middle,2.4)) {
                    if (!hit.add(e.getUniqueId()) || Math.abs(e.getLocation().getY()-middle.getY())>2.8) continue;
                    Map<UUID,Mark> marks=frostMarks.getOrDefault(player.getUniqueId(),Map.of());
                    Mark mark=marks.get(e.getUniqueId());
                    if (mark!=null) marks.remove(e.getUniqueId());
                    int stacks=mark!=null && mark.until()>now()?mark.stacks():0;
                    damage(player,e,4.0+(stacks>=3?3.0:0));
                    burst(e.getLocation().add(0,1,0),26+(stacks>=3?22:0));
                    e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,50,0));
                }
                if (step==0) middle.getWorld().playSound(middle,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.6f,.65f);
                step++;
            }}.runTaskTimer(plugin,0L,2L);
        },12L);
    }
    private boolean heldIs(Player player,String id){return id.equals(held(player));}
    private void glacierBreaker(Player player){
        Location origin=player.getEyeLocation(); Vector dir=origin.getDirection();
        LivingEntity target=aim(player,13);
        Location center=target==null?origin.add(dir.multiply(8)):target.getLocation();
        sphere(center.clone().add(0,1,0),Particle.SNOWFLAKE,34,3.0);
        player.getWorld().playSound(center,Sound.BLOCK_ANVIL_LAND,1f,.6f);
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            burst(center.clone().add(0,1,0),80);
            for(LivingEntity e:targets(player,center,3.5)){
                damage(player,e,5.0);
                e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,55,2));
                Vector kb=e.getLocation().toVector().subtract(center.toVector());
                if(kb.lengthSquared()>.1) e.setVelocity(kb.normalize().multiply(.7).setY(.3));
            }
        },14L);
    }
    private void frostSpires(Player player){
        Location start=player.getLocation(); Vector flat=start.getDirection().setY(0);
        if(flat.lengthSquared()<.01)flat=new Vector(0,0,1);flat.normalize();final Vector dir=flat;
        Set<UUID> hits=new HashSet<>();
        new BukkitRunnable(){int n=0;@Override public void run(){
            if(n>=9 || !player.isOnline()){cancel();return;}
            Location p=start.clone().add(dir.clone().multiply(n*1.55+2));
            Block floor=p.getBlock();int check=0;
            while(floor.isPassable() && check++<5)floor=floor.getRelative(0,-1,0);
            p=floor.getLocation().add(.5,1,.5);
            p.getWorld().spawnParticle(Particle.SNOWFLAKE,p.clone().add(0,1.1,0),35,.6,.9,.6,.07);
            p.getWorld().playSound(p,Sound.BLOCK_GLASS_BREAK,.55f,1.1f);
            for(LivingEntity e:targets(player,p,1.8))if(hits.add(e.getUniqueId())){
                damage(player,e,3.5); e.setVelocity(e.getVelocity().setY(.57));
            }n++;
        }}.runTaskTimer(plugin,0,3L);
    }
    private void arrowfall(Player player){
        Block target=player.getTargetBlockExact(40);
        Location center=target!=null?target.getLocation().add(.5,1,.5):player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(22));
        sphere(center.clone().add(0,5,0),Particle.SNOWFLAKE,48,5);
        new BukkitRunnable(){int tick=0;@Override public void run(){
            if(tick++>=8 || !player.isOnline()){cancel();return;}
            for(int i=0;i<7;i++){
                double x=(Math.random()-.5)*9,z=(Math.random()-.5)*9;
                Location p=center.clone().add(x,Math.max(1,7-tick),z);
                p.getWorld().spawnParticle(Particle.SNOWFLAKE,p,10,.2,1.2,.2,.1);
            }
            for(LivingEntity e:targets(player,center,5)){
                damage(player,e,.85);
                e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,35,0));
            }
        }}.runTaskTimer(plugin,10L,10L);
    }
    private void sonicFrost(Player player){
        Location origin=player.getEyeLocation();Vector dir=origin.getDirection();
        Set<UUID> hit=new HashSet<>();
        for(double d=1;d<=19;d+=.7){
            Location point=origin.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.SNOWFLAKE,point,4,.35,.35,.35,.012);
            for(LivingEntity e:targets(player,point,1.2))if(hit.add(e.getUniqueId())){
                damage(player,e,6);
                e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,50,1));
            }
            if(!point.getBlock().isPassable())break;
        }
        player.getWorld().playSound(origin,Sound.ENTITY_WARDEN_SONIC_BOOM,1.1f,1.3f);
    }
    private void frozenTether(Player player){
        LivingEntity target=aim(player,21);
        if(target==null){player.sendMessage(ChatColor.GRAY+"[Frost] Tether missed.");return;}
        target.getWorld().playSound(target.getLocation(),Sound.ENTITY_FISHING_BOBBER_RETRIEVE,1.1f,.8f);
        new BukkitRunnable(){int t=0;@Override public void run(){
            if(t++>=16 || !valid(player,target) || !player.isOnline() || !player.getWorld().equals(target.getWorld())){cancel();return;}
            Vector delta=player.getLocation().toVector().subtract(target.getLocation().toVector());
            if(delta.lengthSquared()<5){cancel();return;}
            target.setVelocity(delta.normalize().multiply(Math.min(1.2,delta.length()*.2)).setY(.21));
            target.getWorld().spawnParticle(Particle.SNOWFLAKE,target.getLocation().add(0,1,0),5,.35,.35,.35,.015);
        }}.runTaskTimer(plugin,0L,1L);
    }
    private void thunderfall(Player player){
        LivingEntity target=aim(player,17);
        if(target==null)return;
        Location spot=target.getLocation();
        sphere(spot.clone().add(0,1,0),Particle.SNOWFLAKE,30,2.2);
        for(int i=0;i<3;i++)Bukkit.getScheduler().runTaskLater(plugin,()->{
            if(!valid(player,target))return;
            target.getWorld().strikeLightningEffect(target.getLocation());
            damage(player,target,2.0);
        },12L+i*9L);
    }
    private void skyHunters(Player player){
        List<LivingEntity> nearby=targets(player,player.getLocation(),31);
        nearby.sort(Comparator.comparingDouble(e->e.getLocation().distanceSquared(player.getLocation())));
        if(nearby.isEmpty()) {burst(player.getLocation().add(0,2,0),25);return;}
        for(int i=0;i<5;i++){
            final LivingEntity target=nearby.get(i%nearby.size());
            Bukkit.getScheduler().runTaskLater(plugin,()->{
                if(!valid(player,target))return;
                Location at=target.getLocation().clone().add(0,1,0);
                at.getWorld().spawnParticle(Particle.FIREWORK,at,35,.7,1,.7,.08);
                at.getWorld().spawnParticle(Particle.SNOWFLAKE,at,30,.6,.6,.6,.08);
                at.getWorld().playSound(at,Sound.ENTITY_FIREWORK_ROCKET_BLAST,.9f,1.35f);
                damage(player,target,1.5);
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,80,0));
            },10L+i*8L);
        }
    }
    private void crystalGuard(Player player){
        guard.put(player.getUniqueId(),now()+8000);
        sphere(player.getLocation().add(0,1,0),Particle.SNOWFLAKE,55,1.6);
        player.playSound(player.getLocation(),Sound.ITEM_SHIELD_BLOCK,1.1f,.8f);
        player.sendMessage(ChatColor.AQUA+"[Frost] Crystal Guard active for 8 seconds.");
    }
    private void winterBlessing(Player player){
        new BukkitRunnable(){int ticks=0;@Override public void run(){
            if(ticks++>=10 || !player.isOnline()){cancel();return;}
            Location at=player.getLocation().add(0,1,0);
            sphere(at,Particle.SNOWFLAKE,26,5);
            for(Player ally:player.getWorld().getPlayers()){
                if(ally.equals(player) || plugin.rebornIsTrusted(player,ally)){
                    if(ally.getLocation().distanceSquared(at)<=100)ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,35,0));
                }
            }
            for(LivingEntity e:targets(player,at,9))e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,35,0));
        }}.runTaskTimer(plugin,0,20L);
    }
    private void anchorPull(Player player){
        Block target=player.getTargetBlockExact(35);
        if(target==null){player.sendMessage(ChatColor.RED+"[Frost] Aim at a solid block.");return;}
        Location destination=target.getLocation().add(.5,1.5,.5);
        if(!destination.getBlock().isPassable())destination.add(0,1,0);
        if(!destination.getBlock().isPassable())return;
        Vector delta=destination.toVector().subtract(player.getLocation().toVector());
        if(delta.lengthSquared()<1)return;
        player.setVelocity(delta.normalize().multiply(Math.min(2.2,.65+delta.length()*.09)).setY(Math.min(.95,.2+delta.getY()*.15)));
        player.playSound(player.getLocation(),Sound.ENTITY_FISHING_BOBBER_RETRIEVE,1f,.8f);
        burst(destination,14);
    }
    private void callOfWinter(Player player){
        Location origin=player.getLocation();
        for(int i=0;i<3;i++){
            double angle=i*Math.PI*2/3;
            Location loc=origin.clone().add(Math.cos(angle)*2,0,Math.sin(angle)*2);
            loc=loc.getWorld().getHighestBlockAt(loc).getLocation().add(.5,1,.5);
            Stray stray=loc.getWorld().spawn(loc,Stray.class);
            stray.setRemoveWhenFarAway(true);
            stray.getPersistentDataContainer().set(NamespacedKey.fromString("punchsnowball:frozen_horn_owner"),
                    org.bukkit.persistence.PersistentDataType.STRING,player.getUniqueId().toString());
            stray.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            LivingEntity closest=null;double dist=Double.MAX_VALUE;
            for(LivingEntity t:targets(player,origin,18)){double d=t.getLocation().distanceSquared(loc);if(d<dist){dist=d;closest=t;}}
            if(closest!=null)stray.setTarget(closest);
            Stray spawned=stray;
            Bukkit.getScheduler().runTaskLater(plugin,()->{if(spawned.isValid())spawned.remove();},200L);
        }
        sphere(origin.add(0,1,0),Particle.SNOWFLAKE,60,3);
        player.playSound(origin,Sound.EVENT_RAID_HORN,1f,.8f);
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void passiveDamage(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player defender && now()<guard.getOrDefault(defender.getUniqueId(),0L)){
            event.setDamage(event.getDamage()*.6);
            defender.getWorld().spawnParticle(Particle.SNOWFLAKE,defender.getLocation().add(0,1,0),9,.5,.5,.5,.02);
        }
        Player attacker=null;
        if(event.getDamager() instanceof Player p)attacker=p;
        else if(event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p)attacker=p;
        if(attacker==null || !(event.getEntity() instanceof LivingEntity victim) || !valid(attacker,victim))return;
        String id=held(attacker);
        if("sword".equals(id) && event.getDamager() instanceof Player){
            Map<UUID,Mark> marks=frostMarks.computeIfAbsent(attacker.getUniqueId(),k->new HashMap<>());
            Mark previous=marks.get(victim.getUniqueId());
            int stacks=previous==null || previous.until()<now()?1:Math.min(3,previous.stacks()+1);
            marks.put(victim.getUniqueId(),new Mark(stacks,now()+5000));
            victim.getWorld().spawnParticle(Particle.SNOWFLAKE,victim.getLocation().add(0,1,0),stacks*5,.3,.5,.3,.01);
            if(stacks>=2)victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,25,0));
        } else if("battleaxe".equals(id) && event.getDamager() instanceof Player){
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,30,0));
        } else if("spear".equals(id) && event.getDamager() instanceof Player){
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,35,0));
        } else if("bow".equals(id) && event.getDamager() instanceof Arrow){
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,50,0));
        }
    }
    private void tickPassives(){
        long now=now();guard.entrySet().removeIf(e->e.getValue()<now);
        for(Player player:Bukkit.getOnlinePlayers()){
            String id=held(player);
            if("riptide".equals(id) && player.isInWater())player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE,40,0,true,false));
            if("totem".equals(id))player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,35,0,true,false));
            if("horn".equals(id))player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,35,0,true,false));
        }
        frostMarks.values().forEach(map->map.entrySet().removeIf(e->e.getValue().until()<now));
    }
    void clearCooldowns(Player player) { readyAt.remove(player.getUniqueId()); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        readyAt.remove(e.getPlayer().getUniqueId()); guard.remove(e.getPlayer().getUniqueId());
        frostMarks.remove(e.getPlayer().getUniqueId());
    }
}
