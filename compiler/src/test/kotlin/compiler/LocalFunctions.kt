package compiler

class LocalFunctions : IntProgramTest() {
    override val sourceCode = """
        int foo(int baz);
        
        int main() {
            return foo(2) + 4;
        }
        
        int foo(int bar) {
            return bar+bar;
        }
    """.trimIndent()

    override val expectedReturn = 8
}