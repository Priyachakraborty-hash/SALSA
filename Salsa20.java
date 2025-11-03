import java.util.Locale;

public class Salsa20 
{
   
    private static int rotl32(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private static int leBytesToU32(byte[] b, int off) {
        return (b[off] & 0xff)
             | ((b[off+1] & 0xff) << 8)
             | ((b[off+2] & 0xff) << 16)
             | ((b[off+3] & 0xff) << 24);
    }

    private static void u32ToLeBytes(int x, byte[] out, int off) {
        out[off]   = (byte)( x        & 0xff);
        out[off+1] = (byte)((x >>> 8) & 0xff);
        out[off+2] = (byte)((x >>>16) & 0xff);
        out[off+3] = (byte)((x >>>24) & 0xff);
    }

    private static int[] quarterround(int y0, int y1, int y2, int y3) {
        int z1 = y1 ^ rotl32((y0 + y3), 7);
        int z2 = y2 ^ rotl32((z1 + y0), 9);
        int z3 = y3 ^ rotl32((z2 + z1), 13);
        int z0 = y0 ^ rotl32((z3 + z2), 18);
        return new int[]{ z0, z1, z2, z3 };
    }

    private static int[] rowround(int[] y) {
        int[] q0 = quarterround(y[0],  y[1],  y[2],  y[3]);
        int[] q1 = quarterround(y[5],  y[6],  y[7],  y[4]);
        int[] q2 = quarterround(y[10], y[11], y[8],  y[9]);
        int[] q3 = quarterround(y[15], y[12], y[13], y[14]);

        return new int[]{
            q0[0], q0[1], q0[2], q0[3],
            q1[3], q1[0], q1[1], q1[2],
            q2[2], q2[3], q2[0], q2[1],
            q3[1], q3[2], q3[3], q3[0]
        };
    }

    private static int[] columnround(int[] x) {
        int[] q0 = quarterround(x[0],  x[4],  x[8],  x[12]);
        int[] q1 = quarterround(x[5],  x[9],  x[13], x[1]);
        int[] q2 = quarterround(x[10], x[14], x[2],  x[6]);
        int[] q3 = quarterround(x[15], x[3],  x[7],  x[11]);

        return new int[]{
            q0[0], q1[3], q2[2], q3[1],
            q0[1], q1[0], q2[3], q3[2],
            q0[2], q1[1], q2[0], q3[3],
            q0[3], q1[2], q2[1], q3[0]
        };
    }

    private static int[] doubleround(int[] x) {
        return rowround(columnround(x));
    }

    private static byte[] salsa20Core(int[] input16, int rounds) {
        if ((rounds & 1) != 0) throw new IllegalArgumentException("Rounds must be even");
        int[] x = input16.clone();
        for (int i = 0; i < rounds / 2; i++) {
            x = doubleround(x);
        }
        byte[] out = new byte[64];
        for (int i = 0; i < 16; i++) {
            int w = x[i] + input16[i];
            u32ToLeBytes(w, out, 4*i);
        }
        return out;
    }

    
    private static final byte[] C32 = "expand 32-byte k".getBytes();
    private static final byte[] C16 = "expand 16-byte k".getBytes();
    private static final byte[] C08 = "expand 08-byte k".getBytes();

    private static int[] constWords(byte[] label16) {
        if (label16.length != 16) throw new IllegalArgumentException("Const label must be 16 bytes");
        return new int[] {
            leBytesToU32(label16, 0),
            leBytesToU32(label16, 4),
            leBytesToU32(label16, 8),
            leBytesToU32(label16, 12)
        };
    }

    private static int[] expandState(byte[] key, byte[] nonce8, long counter, int keybits) {
        if (nonce8.length != 8) throw new IllegalArgumentException("Nonce must be 8 bytes");
        byte[] ctr = new byte[8];
        // little-endian 64-bit counter
        for (int i = 0; i < 8; i++) {
            ctr[i] = (byte)((counter >>> (8*i)) & 0xff);
        }
        byte[] n16 = new byte[16];
        System.arraycopy(nonce8, 0, n16, 0, 8);
        System.arraycopy(ctr,    0, n16, 8, 8);

        int[] c;
        byte[] k0, k1;

        if (keybits == 256) {
            if (key.length != 32) throw new IllegalArgumentException("256-bit key should be 32 bytes");
            c = constWords(C32);
            k0 = new byte[16]; k1 = new byte[16];
            System.arraycopy(key, 0,  k0, 0, 16);
            System.arraycopy(key, 16, k1, 0, 16);
        } else if (keybits == 128) {
            if (key.length != 16) throw new IllegalArgumentException("128-bit key should be 16 bytes");
            c = constWords(C16);
            k0 = key.clone();
            k1 = key.clone(); 
        } else if (keybits == 64) {
            if (key.length != 8) throw new IllegalArgumentException("64-bit key should be 8 bytes");
            c = constWords(C08);
            k0 = new byte[16]; k1 = new byte[16];
            for (int i = 0; i < 2; i++) {
                System.arraycopy(key, 0, k0, i*8, 8);
                System.arraycopy(key, 0, k1, i*8, 8);
            }
        } else {
            throw new IllegalArgumentException("keybits should be 64, 128, or 256");
        }

        return new int[] {
            c[0],
            leBytesToU32(k0,0),  leBytesToU32(k0,4),  leBytesToU32(k0,8),  leBytesToU32(k0,12),
            c[1],
            leBytesToU32(n16,0), leBytesToU32(n16,4), leBytesToU32(n16,8), leBytesToU32(n16,12),
            c[2],
            leBytesToU32(k1,0),  leBytesToU32(k1,4),  leBytesToU32(k1,8),  leBytesToU32(k1,12),
            c[3]
        };
    }

    private static byte[] keystreamBlock(byte[] key, byte[] nonce8, long counter, int keybits, int rounds) {
        int[] state = expandState(key, nonce8, counter, keybits);
        return salsa20Core(state, rounds);
    }

    private static byte[] salsa20Xor(int keybits, String keyHex, String nonceHex, String dataHex, int rounds) {
        byte[] key = fromHex(keyHex);
        byte[] nonce = fromHex(nonceHex);
        byte[] data = fromHex(dataHex);
        if (data.length > 1024) throw new IllegalArgumentException("Input limited to 1 KB");
        if (nonce.length != 8) throw new IllegalArgumentException("Nonce must be 8 bytes (16 hex chars)");

        byte[] out = new byte[data.length];
        for (int off = 0; off < data.length; off += 64) {
            long blockIdx = off / 64;
            byte[] ks = keystreamBlock(key, nonce, blockIdx, keybits, rounds);
            int chunk = Math.min(64, data.length - off);
            for (int j = 0; j < chunk; j++) {
                out[off + j] = (byte)(data[off + j] ^ ks[j]);
            }
        }
        return out;
    }

    private static byte[] fromHex(String s) {
        String z = s.trim().toLowerCase(Locale.ROOT);
        if ((z.length() & 1) != 0) throw new IllegalArgumentException("Hex string must have even length");
        int n = z.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            int hi = hexNib(z.charAt(2*i));
            int lo = hexNib(z.charAt(2*i+1));
            out[i] = (byte)((hi << 4) | lo);
        }
        return out;
    }

    private static int hexNib(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        throw new IllegalArgumentException("Invalid hex char: " + c);
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) {
            int v = value & 0xff;
            char[] HEX = "0123456789abcdef".toCharArray();
            sb.append(HEX[v >>> 4]).append(HEX[v & 0x0f]);
        }
        return sb.toString();
    }

    
    private static boolean verbose() {
        return System.getenv("SALSA20_VERBOSE") != null;
    }

    private static void vlog(String msg) {
        if (verbose()) System.err.println("[salsa20] " + msg);
    }

    private static void require(boolean cond, String errMsg) {
        if (!cond) {
            System.err.println("Error: " + errMsg);
            System.exit(1);
        }
    }

    private static void validateKeybits(int keybits) {
        require(keybits == 64 || keybits == 128 || keybits == 256,
            "Invalid keybits: " + keybits + " (allowed: 64, 128, 256)");
    }

    private static void validateHexLength(String what, String hex, int expectedBytesOrMinus1) {
       
        require((hex.length() & 1) == 0, what + " hex must have even length");
        
        if (expectedBytesOrMinus1 > -1) {
            int expectedHexLen = expectedBytesOrMinus1 * 2;
            require(hex.length() == expectedHexLen,
                what + " must be exactly " + expectedBytesOrMinus1 + " bytes (" + expectedHexLen + " hex chars), got " + hex.length());
        }
    }

    private static void validateInputSize(String dataHex) {
        int bytes = dataHex.length() / 2;
        require(bytes <= 1024, "input limited to 1024 bytes; got " + bytes);
    }

   
    public static void main(String[] args) {
        long t0 = System.nanoTime();
        try {
            if (args.length != 4) {
                System.err.println("Usage: java Salsa20 <keybits:64|128|256> <key_hex> <nonce_hex> <input_hex>");
                System.exit(1);
            }

           
            int keybits;
            try {
                keybits = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.err.println("Error: keybits must be an integer (64, 128, or 256). Got: \"" + args[0] + "\"");
                System.exit(1);
                return; 
            }

            String keyHex   = args[1].trim().toLowerCase(Locale.ROOT);
            String nonceHex = args[2].trim().toLowerCase(Locale.ROOT);
            String dataHex  = args[3].trim().toLowerCase(Locale.ROOT);

          
            validateKeybits(keybits);
            
            int keyBytes = (keybits == 64 ? 8 : (keybits == 128 ? 16 : 32));
            validateHexLength("key", keyHex, keyBytes);
            validateHexLength("nonce", nonceHex, 8); // 8 bytes = 16 hex chars
            validateHexLength("input", dataHex, -1); // any even length ok
            validateInputSize(dataHex); // <= 1KB

            vlog("keybits=" + keybits + ", keyBytes=" + keyBytes);
            vlog("nonceBytes=8, inputBytes=" + (dataHex.length()/2));

           
            byte[] result = salsa20Xor(keybits, keyHex, nonceHex, dataHex, 10); // Salsa20/10

           
            System.out.println(toHex(result));

            long t1 = System.nanoTime();
            vlog("ok in " + ((t1 - t0) / 1_000_000.0) + " ms");

        } catch (IllegalArgumentException iae) {
            
            System.err.println("Error: " + iae.getMessage());
            if (verbose()) iae.printStackTrace(System.err);
            System.exit(1);
        } catch (Exception e) {
           
            System.err.println("Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (verbose()) e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
