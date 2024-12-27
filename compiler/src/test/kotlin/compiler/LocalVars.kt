package compiler

class LocalVars : IntProgramTest() {
    override val sourceCode = """
        int main() {
            int a = 2+1;
            int b;
            b = a << 1;
            int c = a = b;
            return c + (a - b);
        }
    """.trimIndent()

    override val expectedReturn = 6
}