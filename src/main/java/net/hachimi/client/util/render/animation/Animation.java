package net.hachimi.client.util.render.animation;

public class Animation {
   private final Easing easing;
   private float length;
   private long last;
   private boolean state;

   public Animation(float length) {
      this(false, length);
   }

   public Animation(boolean initial, float length) {
      this(initial, length, Easing.LINEAR);
   }

   public Animation(boolean initial, float length, Easing easing) {
      this.last = 0L;
      this.length = length;
      this.state = initial;
      this.easing = easing;
   }

   public void setState(boolean state) {
      this.last = (long)(!state ? (double)System.currentTimeMillis() - ((double)1.0F - this.getFactor()) * (double)this.length : (double)System.currentTimeMillis() - this.getFactor() * (double)this.length);
      this.state = state;
   }

   public boolean getState() {
      return this.state;
   }

   public double getFactor() {
      return this.easing.ease(this.getLinearFactor());
   }

   public double getLinearFactor() {
      return this.state ? this.clamp((double)((float)(System.currentTimeMillis() - this.last) / this.length)) : this.clamp((double)(1.0F - (float)(System.currentTimeMillis() - this.last) / this.length));
   }

   public double getCurrent() {
      return (double)1.0F + (double)1.0F * this.getFactor();
   }

   private double clamp(double in) {
      return in < (double)0.0F ? (double)0.0F : Math.min(in, (double)1.0F);
   }

   public double getLength() {
      return (double)this.length;
   }

   public void setLength(float length) {
      this.length = length;
   }

   public boolean isFinished() {
      return !this.getState() && this.getFactor() == (double)0.0F || this.getState() && this.getFactor() == (double)1.0F;
   }

   public void reset() {
      this.last = System.currentTimeMillis();
   }
}
