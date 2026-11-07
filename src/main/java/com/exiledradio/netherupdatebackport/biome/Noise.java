package com.exiledradio.netherupdatebackport.biome;

import java.util.Random;

/**
 * 1.16.5's noise, ported line for line so a seed gives the same values: {@code ImprovedNoiseGenerator},
 * {@code OctavesNoiseGenerator} and {@code MaxMinNoiseMixer}. 1.12.2's own Perlin classes differ (other
 * gradients, other octave scaling), so the multi-noise biome source and the 1.16 surface builders need these.
 *
 * <p>{@code java.util.Random} stands in for 1.16's {@code SharedSeedRandom}: that class only counts calls, and
 * its {@code consumeCount(n)} is n single steps of the same generator, which {@link #skip} reproduces.
 */
public final class Noise {

    private Noise() {
    }

    /** One {@code ImprovedNoiseGenerator} sample for a fresh random of this seed (for the self-test's cross-check). */
    public static double improved(long seed, double x, double y, double z) {
        return new Improved(new Random(seed)).noise(x, y, z, 0.0D, 0.0D);
    }

    /** 1.16's {@code SimplexNoiseGenerator.GRADIENT}. */
    private static final int[][] GRADIENT = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1},
            {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}, {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}};

    /** {@code SharedSeedRandom.consumeCount}: each step is one {@code next(bits)} of the generator. */
    static void skip(Random rand, int steps) {
        for (int i = 0; i < steps; i++) {
            rand.nextInt();
        }
    }

    static int floor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    static long lfloor(double v) {
        long i = (long) v;
        return v < i ? i - 1L : i;
    }

    static double smoothstep(double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    static double lerp2(double tx, double ty, double a, double b, double c, double d) {
        return lerp(ty, lerp(tx, a, b), lerp(tx, c, d));
    }

    static double lerp3(double tx, double ty, double tz, double a, double b, double c, double d, double e, double f, double g, double h) {
        return lerp(tz, lerp2(tx, ty, a, b, c, d), lerp2(tx, ty, e, f, g, h));
    }

    /** 1.16's {@code ImprovedNoiseGenerator}. */
    static final class Improved {
        private final byte[] p = new byte[256];
        final double xo;
        final double yo;
        final double zo;

        Improved(Random rand) {
            this.xo = rand.nextDouble() * 256.0D;
            this.yo = rand.nextDouble() * 256.0D;
            this.zo = rand.nextDouble() * 256.0D;
            for (int i = 0; i < 256; ++i) {
                this.p[i] = (byte) i;
            }
            for (int k = 0; k < 256; ++k) {
                int j = rand.nextInt(256 - k);
                byte b = this.p[k];
                this.p[k] = this.p[k + j];
                this.p[k + j] = b;
            }
        }

        double noise(double x, double y, double z, double yScale, double yMax) {
            double d0 = x + this.xo;
            double d1 = y + this.yo;
            double d2 = z + this.zo;
            int i = floor(d0);
            int j = floor(d1);
            int k = floor(d2);
            double d3 = d0 - i;
            double d4 = d1 - j;
            double d5 = d2 - k;
            double d6 = smoothstep(d3);
            double d7 = smoothstep(d4);
            double d8 = smoothstep(d5);
            double d9;
            if (yScale != 0.0D) {
                double d10 = Math.min(yMax, d4);
                d9 = floor(d10 / yScale) * yScale;
            } else {
                d9 = 0.0D;
            }
            return sampleAndLerp(i, j, k, d3, d4 - d9, d5, d6, d7, d8);
        }

        private static double gradDot(int hash, double x, double y, double z) {
            int[] g = GRADIENT[hash & 15];
            return g[0] * x + g[1] * y + g[2] * z;
        }

        private int p(int i) {
            return this.p[i & 255] & 255;
        }

        private double sampleAndLerp(int x, int y, int z, double fx, double fy, double fz, double sx, double sy, double sz) {
            int i = p(x) + y;
            int j = p(i) + z;
            int k = p(i + 1) + z;
            int l = p(x + 1) + y;
            int i1 = p(l) + z;
            int j1 = p(l + 1) + z;
            double d0 = gradDot(p(j), fx, fy, fz);
            double d1 = gradDot(p(i1), fx - 1.0D, fy, fz);
            double d2 = gradDot(p(k), fx, fy - 1.0D, fz);
            double d3 = gradDot(p(j1), fx - 1.0D, fy - 1.0D, fz);
            double d4 = gradDot(p(j + 1), fx, fy, fz - 1.0D);
            double d5 = gradDot(p(i1 + 1), fx - 1.0D, fy, fz - 1.0D);
            double d6 = gradDot(p(k + 1), fx, fy - 1.0D, fz - 1.0D);
            double d7 = gradDot(p(j1 + 1), fx - 1.0D, fy - 1.0D, fz - 1.0D);
            return lerp3(sx, sy, sz, d0, d1, d2, d3, d4, d5, d6, d7);
        }
    }

    /** 1.16's {@code OctavesNoiseGenerator}. */
    static final class Octaves {
        private final Improved[] levels;
        private final double[] amplitudes;
        private final double lowestFreqValueFactor;
        private final double lowestFreqInputFactor;

        /** From a set of octaves, as {@code new OctavesNoiseGenerator(random, IntStream / List)}. */
        static Octaves ofOctaves(Random rand, int... octaves) {
            int first = octaves[0];
            int last = octaves[0];
            for (int o : octaves) {
                first = Math.min(first, o);
                last = Math.max(last, o);
            }
            int i = -first;
            double[] amps = new double[i + last + 1];
            for (int o : octaves) {
                amps[o + i] = 1.0D;
            }
            return new Octaves(rand, -i, amps);
        }

        /** As {@code OctavesNoiseGenerator.create(random, firstOctave, amplitudes)}. */
        Octaves(Random rand, int firstOctave, double[] amplitudes) {
            this.amplitudes = amplitudes;
            Improved first = new Improved(rand);
            int j = amplitudes.length;
            int k = -firstOctave;
            this.levels = new Improved[j];
            if (k >= 0 && k < j && amplitudes[k] != 0.0D) {
                this.levels[k] = first;
            }
            for (int i1 = k - 1; i1 >= 0; --i1) {
                if (i1 < j && amplitudes[i1] != 0.0D) {
                    this.levels[i1] = new Improved(rand);
                } else {
                    skip(rand, 262);
                }
            }
            if (k < j - 1) {
                long seed = (long) (first.noise(0.0D, 0.0D, 0.0D, 0.0D, 0.0D) * (double) 9.223372E18F);
                Random second = new Random(seed);
                for (int l = k + 1; l < j; ++l) {
                    if (l >= 0 && amplitudes[l] != 0.0D) {
                        this.levels[l] = new Improved(second);
                    } else {
                        skip(second, 262);
                    }
                }
            }
            this.lowestFreqInputFactor = Math.pow(2.0D, -k);
            this.lowestFreqValueFactor = Math.pow(2.0D, j - 1) / (Math.pow(2.0D, j) - 1.0D);
        }

        double getValue(double x, double y, double z) {
            return getValue(x, y, z, 0.0D, 0.0D);
        }

        double getValue(double x, double y, double z, double yScale, double yMax) {
            double total = 0.0D;
            double in = this.lowestFreqInputFactor;
            double out = this.lowestFreqValueFactor;
            for (int i = 0; i < this.levels.length; ++i) {
                Improved n = this.levels[i];
                if (n != null) {
                    total += this.amplitudes[i] * n.noise(wrap(x * in), wrap(y * in), wrap(z * in), yScale * in, yMax * in) * out;
                }
                in *= 2.0D;
                out /= 2.0D;
            }
            return total;
        }

        /** {@code getSurfaceNoiseValue(x, y, yScale, yMax)}: the surface depth noise. */
        double surface(double x, double y, double yScale, double yMax) {
            return getValue(x, y, 0.0D, yScale, yMax);
        }

        static double wrap(double v) {
            return v - (double) lfloor(v / 3.3554432E7D + 0.5D) * 3.3554432E7D;
        }
    }

    /** 1.16's {@code MaxMinNoiseMixer}: two octave noises summed, scaled to a standard deviation near 1/6. */
    static final class MaxMin {
        private final double valueFactor;
        private final Octaves first;
        private final Octaves second;

        MaxMin(Random rand, int firstOctave, double[] amplitudes) {
            this.first = new Octaves(rand, firstOctave, amplitudes);
            this.second = new Octaves(rand, firstOctave, amplitudes);
            int lo = Integer.MAX_VALUE;
            int hi = Integer.MIN_VALUE;
            for (int i = 0; i < amplitudes.length; i++) {
                if (amplitudes[i] != 0.0D) {
                    lo = Math.min(lo, i);
                    hi = Math.max(hi, i);
                }
            }
            this.valueFactor = 0.16666666666666666D / (0.1D * (1.0D + 1.0D / (double) (hi - lo + 1)));
        }

        double getValue(double x, double y, double z) {
            double sx = x * 1.0181268882175227D;
            double sy = y * 1.0181268882175227D;
            double sz = z * 1.0181268882175227D;
            return (this.first.getValue(x, y, z) + this.second.getValue(sx, sy, sz)) * this.valueFactor;
        }
    }
}
