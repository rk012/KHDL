package compiler

class Scopes : IntProgramTest() {
    override val sourceCode = """
        int main() {
            int a = 6;
            {
                int b = 9;
                a = a + b - 3;
            }
            int b = 0;
            return a + b;
        }
    """.trimIndent()

    override val expectedReturn = 12
}