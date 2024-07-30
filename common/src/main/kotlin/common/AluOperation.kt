package common

enum class AluOperation(val opcode: Int, val computation: (a: Int, b: Int) -> Int) {
    ZERO        (0b101000, { _, _ ->    0                  }),
    ONE         (0b111111, { _, _ ->    1                  }),
    NEG_ONE     (0b111010, { _, _ ->    -1                 }),
    A           (0b001100, { a, _ ->    a                  }),
    B           (0b110000, { _, b ->    b                  }),
    NOT_A       (0b001101, { a, _ ->    a.inv()            }),
    NOT_B       (0b110001, { _, b ->    b.inv()            }),
    NEG_A       (0b001111, { a, _ ->    -a                 }),
    NEG_B       (0b110011, { _, b ->    -b                 }),
    A_PLUS_1    (0b011111, { a, _ ->    a+1                }),
    B_PLUS_1    (0b110111, { _, b ->    b+1                }),
    A_MINUS_1   (0b001110, { a, _ ->    a-1                }),
    B_MINUS_1   (0b110010, { _, b ->    b-1                }),
    A_PLUS_B    (0b000010, { a, b ->    a+b                }),
    A_MINUS_B   (0b010011, { a, b ->    a-b                }),
    B_MINUS_A   (0b000111, { a, b ->    b-a                }),
    AND         (0b000000, { a, b ->    a and b            }),
    OR          (0b010101, { a, b ->    a or b             }),
    NAND        (0b000001, { a, b ->    (a and b).inv()    }),
    NOR         (0b010100, { a, b ->    (a or b).inv()     }),

    XOR         (0b101010, { a, b ->    a xor b            }),
    A_SHR       (0b001010, { a, _ ->    a shr 1            }),
    B_SHR       (0b100010, { _, b ->    b shr 1            });

    val mask = opcode
}