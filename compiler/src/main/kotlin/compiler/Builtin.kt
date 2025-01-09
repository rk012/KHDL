package compiler

object MultDiv {
    val header = """
        int __builtin_imul(int a, int b);
        int __builtin_idiv(int x, int y);
    """.trimIndent()

    val src = """
        int __builtin_imul(int a, int b) {
            if (b < 0) return -__builtin_imul(a, -b);
            if (a < b) return __builtin_imul(b, a);
            if (b == 0) return 0;
    
            int res = __builtin_imul(a, b >> 1) << 1;
            if (b & 1) res = res + a;
        
            return res;
        }
        
        int __builtin_idiv(int x, int y) {
            if (y == -32768) return x == y ? 1 : 0;
            
            if (x == -32768) {
                int q = __builtin_idiv(x+1, y);
                if (__builtin_imul(q, y) - x >= y) q = q + (q < 0 ? -1 : 1);
                return q;
            }
            
            if (x < 0) return -__builtin_idiv(-x, y);
            if (y < 0) return -__builtin_idiv(x, -y);
            if (x < y) return 0;
            
            int q = 0;
            if (y < 16384) q = __builtin_idiv(x, y << 1) << 1;
            
            if (x - q * y >= y) q = q + 1;
        
            return q;
        }
        
        int __builtin_imod(int x, int y) {
            return x - __builtin_imul(__builtin_idiv(x, y), y);
        }
    """.trimIndent()

    val obj by lazy { compileObj(src) }
}