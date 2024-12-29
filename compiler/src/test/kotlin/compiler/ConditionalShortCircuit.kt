package compiler

class ConditionalShortCircuit : IntProgramTest() {
    override val sourceCode = """
        int main() {
            int a;
            int c = 9;
            if (c < 11) a = 2;
            else a = 3;
            
            a = (a == 2) ? 2+a : 3+a;
            
            if (c == 9 || a=0) a = a + 1;
            
            if (1) return a;
            
            if (0) {
                a = a + 1;
            } else {
                a = -a;
            }
            
            return 1;
        }
    """.trimIndent()

    override val expectedReturn = 5
}