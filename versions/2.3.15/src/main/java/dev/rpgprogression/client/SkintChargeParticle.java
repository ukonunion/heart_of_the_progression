package dev.rpgprogression.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** The supplied gold star drifts upwards; potion-emitter colour arguments are not velocities. */
public final class SkintChargeParticle extends TextureSheetParticle {
    private SkintChargeParticle(ClientLevel level,double x,double y,double z,SpriteSet sprites){
        super(level,x,y,z);
        pickSprite(sprites);
        xd=(random.nextDouble()-.5)*.018;yd=.018+random.nextDouble()*.012;zd=(random.nextDouble()-.5)*.018;
        quadSize=.065F+random.nextFloat()*.025F;lifetime=24+random.nextInt(16);
        gravity=0;hasPhysics=false;
    }
    @Override public void tick(){super.tick();alpha=Math.min(1F,(lifetime-age)/8F);}
    @Override public int getLightColor(float partialTick){return 0xF000F0;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){this.sprites=sprites;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double xd,double yd,double zd){
            return new SkintChargeParticle(level,x,y,z,sprites);
        }
    }
}
