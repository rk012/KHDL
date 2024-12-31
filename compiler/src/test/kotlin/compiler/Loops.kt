package compiler

class Loops : IntProgramTest() {
    override val sourceCode = """
        int main() {
            int a = 0;
            int b = 1;
            
            for (int i = 0; i < 5; i=i+1) {
                int tmp = a;
                a = b;
                b = tmp + a;
            }
            
            int i = 0;
            
            while (b) {
                i = i + 1;
                b = b - 1;
            }
            
            for (;;a = a - 1) {
                if (a == 5) continue;
                if (a == 0) break;
                b = b + 1;
            }
            
            return i + b;
        }
    """.trimIndent()

    override val expectedReturn = 12
}