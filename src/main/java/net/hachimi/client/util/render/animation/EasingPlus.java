package net.hachimi.client.util.render.animation;

public enum EasingPlus {
   Linear {
      public double ease(double p) {
         return p;
      }
   },
   SineOut {
      public double ease(double p) {
         return Math.sin(p * Math.PI / (double)2.0F);
      }
   },
   SineInOut {
      public double ease(double p) {
         return -(Math.cos(Math.PI * p) - (double)1.0F) / (double)2.0F;
      }
   },
   CubicIn {
      public double ease(double p) {
         return Math.pow(p, (double)3.0F);
      }
   },
   CubicOut {
      public double ease(double p) {
         return (double)1.0F - Math.pow((double)1.0F - p, (double)3.0F);
      }
   },
   CubicInOut {
      public double ease(double p) {
         return p < (double)0.5F ? (double)4.0F * Math.pow(p, (double)3.0F) : (double)1.0F - Math.pow((double)-2.0F * p + (double)2.0F, (double)3.0F) / (double)2.0F;
      }
   },
   QuadIn {
      public double ease(double p) {
         return Math.pow(p, (double)2.0F);
      }
   },
   QuadOut {
      public double ease(double p) {
         return (double)1.0F - ((double)1.0F - p) * ((double)1.0F - p);
      }
   },
   QuadInOut {
      public double ease(double p) {
         return p < (double)0.5F ? (double)8.0F * Math.pow(p, (double)4.0F) : (double)1.0F - Math.pow((double)-2.0F * p + (double)2.0F, (double)4.0F) / (double)2.0F;
      }
   },
   QuartIn {
      public double ease(double p) {
         return Math.pow(p, (double)4.0F);
      }
   },
   QuartOut {
      public double ease(double p) {
         return (double)1.0F - Math.pow((double)1.0F - p, (double)4.0F);
      }
   },
   QuartInOut {
      public double ease(double p) {
         return p < (double)0.5F ? (double)8.0F * Math.pow(p, (double)4.0F) : (double)1.0F - Math.pow((double)-2.0F * p + (double)2.0F, (double)4.0F) / (double)2.0F;
      }
   },
   QuintIn {
      public double ease(double p) {
         return Math.pow(p, (double)5.0F);
      }
   },
   QuintOut {
      public double ease(double p) {
         return (double)1.0F - Math.pow((double)1.0F - p, (double)5.0F);
      }
   },
   QuintInOut {
      public double ease(double p) {
         return p < (double)0.5F ? (double)16.0F * Math.pow(p, (double)5.0F) : (double)1.0F - Math.pow((double)-2.0F * p + (double)2.0F, (double)5.0F) / (double)2.0F;
      }
   },
   CircIn {
      public double ease(double p) {
         return (double)1.0F - Math.sqrt((double)1.0F - Math.pow(p, (double)2.0F));
      }
   },
   CircOut {
      public double ease(double p) {
         return Math.sqrt((double)1.0F - Math.pow(p - (double)1.0F, (double)2.0F));
      }
   },
   CircInOut {
      public double ease(double p) {
         return p < (double)0.5F ? ((double)1.0F - Math.sqrt((double)1.0F - Math.pow((double)2.0F * p, (double)2.0F))) / (double)2.0F : (Math.sqrt((double)1.0F - Math.pow((double)-2.0F * p + (double)2.0F, (double)2.0F)) + (double)1.0F) / (double)2.0F;
      }
   },
   Expo {
      public double ease(double p) {
         return p == (double)0.0F ? (double)0.0F : (p == (double)1.0F ? (double)1.0F : (p < (double)0.5F ? Math.pow((double)2.0F, (double)20.0F * p - (double)10.0F) / (double)2.0F : ((double)2.0F - Math.pow((double)2.0F, (double)-20.0F * p + (double)10.0F)) / (double)2.0F));
      }
   },
   BackOut {
      public double ease(double p) {
         double c1 = 1.70158;
         double c3 = c1 + (double)1.0F;
         return (double)1.0F + c3 * Math.pow(p - (double)1.0F, (double)3.0F) + c1 * Math.pow(p - (double)1.0F, (double)2.0F);
      }
   },
   BackInOut {
      public double ease(double p) {
         return p < (double)0.5F ? Math.pow((double)2.0F * p, (double)2.0F) * (7.189819 * p - 2.5949095) / (double)2.0F : (Math.pow((double)2.0F * p - (double)2.0F, (double)2.0F) * (3.5949095 * (p * (double)2.0F - (double)2.0F) + 2.5949095) + (double)2.0F) / (double)2.0F;
      }
   },
   Bounce {
      public double ease(double p) {
         if (p < 0.36363636363636365) {
            return (double)7.5625F * p * p;
         } else if (p < 0.7272727272727273) {
            p -= 0.5454545454545454;
            return (double)7.5625F * p * p + (double)0.75F;
         } else if (p < 0.9090909090909091) {
            p -= 0.8181818181818182;
            return (double)7.5625F * p * p + (double)0.9375F;
         } else {
            p -= 0.9545454545454546;
            return (double)7.5625F * p * p + (double)0.984375F;
         }
      }
   };

   public abstract double ease(double var1);

   // $FF: synthetic method
   private static EasingPlus[] $values() {
      return new EasingPlus[]{Linear, SineOut, SineInOut, CubicIn, CubicOut, CubicInOut, QuadIn, QuadOut, QuadInOut, QuartIn, QuartOut, QuartInOut, QuintIn, QuintOut, QuintInOut, CircIn, CircOut, CircInOut, Expo, BackOut, BackInOut, Bounce};
   }
}
