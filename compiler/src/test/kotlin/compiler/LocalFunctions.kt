package compiler

class LocalFunctions : IntProgramTest() {
    override val sourceCode = """
        int fact(int n);
        
        int main() {
            return fact(5) + 4;
        }
        
        int fact(int n) {
            if (n > 0) return n * fact(n-1);
            return 1;
        }
    """.trimIndent()

    override val expectedReturn = 124
}