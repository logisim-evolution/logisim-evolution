class TestUnit:

    outputs = ['IsImmediate', 'IsRegister', 'IsTableB', 'IsJump', 'IsBranch', 'IsMem', 'Immediate', 'Offset', 'JumpTarget', 'ShiftAmount',
                 'ALUOpCode', 'ImmediateSelect', 'ImmSignExt', 'CompSign', 'Ra', 'Rb', 'Rd', 'PCSelect', 'BranchSelect', 'MemLoad', 'MemStore', 
                 'MemWord', 'MemSignExt', 'FuncField', 'SaControl', 'ASelect', 'ExecOut']
    output_lens = ['1', '1', '1', '1', '1', '1', '16', '16', '26', '5', '4', '1', '1', '1', '5', '5', '5', '2', '3', '1', '1', '1', '1', '1', '2', '2', '2']

    def __init__(self, inst, non_zero_outs):
        self.inst = inst
        self.non_zero_outs = non_zero_outs

    def __str__(self):
        res = str(self.inst) + " "
        for out in TestUnit.outputs:
            if out in self.non_zero_outs:
                res += str(self.non_zero_outs[out]) + " "
            else:
                res += '0' + " "
        return res



tests = [
    # ADDIU
    TestUnit("00100101010010010000010100110010", {
        "IsImmediate" : "1",
        "Immediate" : "0000010100110010",
        "ALUOpCode" : "0010",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "01010",
        "Rd" : "01001",
    }),
    # ANDI
    TestUnit("00110010010011000000000100100011", {
        "IsImmediate" : "1",
        "Immediate" : "0000000100100011",
        "ALUOpCode" : "1000",
        "ImmediateSelect" : "1",
        "Ra" : "10010",
        "Rd" : "01100",
    }),
    # ORI
    TestUnit("00110101101101100000010001101010", {
        "IsImmediate" : "1",
        "Immediate" : "0000010001101010",
        "ALUOpCode" : "1010",
        "ImmediateSelect" : "1",
        "Ra" : "01101",
        "Rd" : "10110"
    }),
    # XORI
    TestUnit("00111001101101100000010001101010", {
        "IsImmediate" : "1",
        "Immediate" : "0000010001101010",
        "ALUOpCode" : "1100",
        "ImmediateSelect" : "1",
        "Ra" : "01101",
        "Rd" : "10110"
    }),
    # SLTI
    TestUnit("00101001101101100000010001101010", {
        "IsImmediate" : "1",
        "Immediate" : "0000010001101010",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "CompSign" : "1",
        "Ra" : "01101",
        "Rd" : "10110",
        "ExecOut" : "10"
    }),
    # SLTIU
    TestUnit("00101101101101100000010001101010", {
        "IsImmediate" : "1",
        "Immediate" : "0000010001101010",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "01101",
        "Rd" : "10110",
        "ExecOut" : "10"
    }),



    # ADDU
    TestUnit("00000001101010011011000000100001", {
        "IsRegister" : "1",
        "ALUOpCode" : "0010",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1"
    }),
    # SUBU
    TestUnit("00000001101010011011000000100011", {
        "IsRegister" : "1",
        "ALUOpCode" : "0111",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1"
    }),
    # AND
    TestUnit("00000001101010011011000000100100", {
        "IsRegister" : "1",
        "ALUOpCode" : "1000",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1"
    }),
    # OR
    TestUnit("00000001101010011011000000100101", {
        "IsRegister" : "1",
        "ALUOpCode" : "1010",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1"
    }),
    # XOR
    TestUnit("00000001101010011011000000100110", {
        "IsRegister" : "1",
        "ALUOpCode" : "1100",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1"
    }),
    # NOR
    TestUnit("00000001101010011011000000100111", {
        "IsRegister" : "1",
        "ALUOpCode" : "1110",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1"
    }),
    # SLT
    TestUnit("00000001101010011011000000101010", {
        "IsRegister" : "1",
        "CompSign" : "1",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1",
        "ExecOut" : "10"
    }),
    # SLTU
    TestUnit("00000001101010011011000000101011", {
        "IsRegister" : "1",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1",
        "ExecOut" : "10"
    }),



    # MOVN
    TestUnit("00000001101010011011000000001011", {
        "IsRegister" : "1",
        "ALUOpCode" : "1011",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1",
        "ASelect" : "01",
        "ExecOut" : "01"
    }),
    # MOVZ
    TestUnit("00000001101010011011000000001010", {
        "IsRegister" : "1",
        "ALUOpCode" : "1001",
        "Ra" : "01101",
        "Rb" : "01001",
        "Rd" : "10110",
        "FuncField" : "1",
        "ASelect" : "01",
        "ExecOut" : "01"
    }),


    # SLL
    TestUnit("00000000000011011011000101000000", {
        "IsRegister" : "1",
        "ALUOpCode" : "0001",
        "ShiftAmount" : "00101",
        "Rb" : "01101",
        "Rd" : "10110",
        "SaControl" : "10",
        "FuncField" : "1",
    }),
    # SRL
    TestUnit("00000000000011011011000101000010", {
        "IsRegister" : "1",
        "ALUOpCode" : "0100",
        "ShiftAmount" : "00101",
        "Rb" : "01101",
        "Rd" : "10110",
        "SaControl" : "10",
        "FuncField" : "1",
    }),
    # SRA
    TestUnit("00000000000011011011000101000011", {
        "IsRegister" : "1",
        "ALUOpCode" : "0101",
        "ShiftAmount" : "00101",
        "Rb" : "01101",
        "Rd" : "10110",
        "SaControl" : "10",
        "FuncField" : "1",
    }),
    # SLLV
    TestUnit("00000010111011011011000000000100", {
        "IsRegister" : "1",
        "ALUOpCode" : "0001",
        "Ra" : "10111",
        "Rb" : "01101",
        "Rd" : "10110",
        "SaControl" : "11",
        "FuncField" : "1",
    }),
    # SRLV
    TestUnit("00000010111011011011000000000110", {
        "IsRegister" : "1",
        "ALUOpCode" : "0100",
        "Ra" : "10111",
        "Rb" : "01101",
        "Rd" : "10110",
        "SaControl" : "11",
        "FuncField" : "1",
    }),
    # SRAV
    TestUnit("00000010111011011011000000000111", {
        "IsRegister" : "1",
        "ALUOpCode" : "0101",
        "Ra" : "10111",
        "Rb" : "01101",
        "Rd" : "10110",
        "SaControl" : "11",
        "FuncField" : "1",
    }),


    # LUI
    TestUnit("00111100000011010000000100100011", {
        "IsImmediate" : "1",
        "ImmediateSelect" : "1",
        "Immediate" : "0000000100100011",
        "Rd" : "01101",
        "SaControl" : "01",
    }),



    # Table B instructions
    # J
    TestUnit("00001000000000000000000100100011", {
        "IsJump" : "1",
        "IsTableB" : "1",
        "JumpTarget" : "00000000000000000100100011",
        "PCSelect" : "10",
    }),
    # JR
    TestUnit("00000010011000000000000000001000", {
        "IsJump" : "1",
        "IsTableB" : "1",
        "Ra" : "10011",
        "PCSelect" : "01",
        "FuncField" : "1",
    }),
    # JAL
    TestUnit("00001100000000000000000100100011", {
        "IsJump" : "1",
        "IsTableB" : "1",
        "JumpTarget" : "00000000000000000100100011",
        "Rd" : "11111",
        "PCSelect" : "10",
        "ExecOut" : "11"
    }),
    # JALR
    TestUnit("00000001011000001001100000001001", {
        "IsJump" : "1",
        "IsTableB" : "1",
        "Ra" : "01011",
        "Rd" : "10011",
        "PCSelect" : "01",
        "FuncField" : "1",
        "ExecOut" : "11"
    }),

    # BEQ
    TestUnit("00010001001100110000000100100011", {
        "IsBranch" : "1",
        "IsTableB" : "1",
        "Offset" : "0000000100100011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "ALUOpCode" : "0011",
        "Ra" : "01001",
        "Rb" : "10011",
        "BranchSelect" : "101",
        "CompSign" : "01",
        "PCSelect" : "11",
        "ASelect" : "10",
    }),
    # BNE
    TestUnit("00010101001100110000000100100011", {
        "IsBranch" : "1",
        "IsTableB" : "1",
        "Offset" : "0000000100100011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "ALUOpCode" : "0011",
        "Ra" : "01001",
        "Rb" : "10011",
        "BranchSelect" : "110",
        "CompSign" : "01",
        "PCSelect" : "11",
        "ASelect" : "10",
    }),
    # BLEZ
    TestUnit("00011001001000000000000100100011", {
        "IsBranch" : "1",
        "IsTableB" : "1",
        "Offset" : "0000000100100011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "ALUOpCode" : "0011",
        "Ra" : "01001",
        "BranchSelect" : "100",
        "CompSign" : "01",
        "PCSelect" : "11",
        "ASelect" : "10",
    }),
    # BGTZ
    TestUnit("00011101001000000000000100100011", {
        "IsBranch" : "1",
        "IsTableB" : "1",
        "Offset" : "0000000100100011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "ALUOpCode" : "0011",
        "Ra" : "01001",
        "BranchSelect" : "001",
        "CompSign" : "01",
        "PCSelect" : "11",
        "ASelect" : "10",
    }),
    # BLTZ
    TestUnit("00000101001000000000000100100011", {
        "IsBranch" : "1",
        "IsTableB" : "1",
        "Offset" : "0000000100100011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "ALUOpCode" : "0011",
        "Ra" : "01001",
        "BranchSelect" : "010",
        "CompSign" : "01",
        "PCSelect" : "11",
        "ASelect" : "10",
    }),
    # BGEZ
    TestUnit("00000101001000010000000100100011", {
        "IsBranch" : "1",
        "IsTableB" : "1",
        "Offset" : "0000000100100011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "ALUOpCode" : "0011",
        "Ra" : "01001",
        "BranchSelect" : "011",
        "CompSign" : "01",
        "PCSelect" : "11",
        "ASelect" : "10",
    }),


    # LW
    TestUnit("10001110011010110000000100100011", {
        "IsTableB" : "1",
        "IsMem" : "1",
        "Offset" : "0000000100100011",
        "ALUOpCode" : "0011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "10011",
        "Rb" : "01011",
        "Rd" : "01011",
        "MemLoad" : "1",
        "MemWord" : "1",
    }),
    # LB
    TestUnit("10000010011010110000000100100011", {
        "IsTableB" : "1",
        "IsMem" : "1",
        "Offset" : "0000000100100011",
        "MemSignExt" : "1",
        "ALUOpCode" : "0011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "10011",
        "Rb" : "01011",
        "Rd" : "01011",
        "MemLoad" : "1",
    }),
    # LBU
    TestUnit("10010010011010110000000100100011", {
        "IsTableB" : "1",
        "IsMem" : "1",
        "Offset" : "0000000100100011",
        "ALUOpCode" : "0011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "10011",
        "Rb" : "01011",
        "Rd" : "01011",
        "MemLoad" : "1",
    }),
    # SW
    TestUnit("10101110011010110000000100100011", {
        "IsTableB" : "1",
        "IsMem" : "1",
        "Offset" : "0000000100100011",
        "ALUOpCode" : "0011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "10011",
        "Rb" : "01011",
        "Rd" : "01011",
        "MemWord" : "1",
        "MemStore" : "1"
    }),
    # SB
    TestUnit("10100010011010110000000100100011", {
        "IsTableB" : "1",
        "IsMem" : "1",
        "Offset" : "0000000100100011",
        "ALUOpCode" : "0011",
        "ImmediateSelect" : "1",
        "ImmSignExt" : "1",
        "Ra" : "10011",
        "Rb" : "01011",
        "Rd" : "01011",
        "MemStore" : "1"
    }),
]

_ = ['IsImmediate', 'IsRegister', 'IsTableB', 'IsJump', 'IsBranch', 'IsMem', 'Immediate', 
            'Offset', 'JumpTarget', 'ALUOpCode', 'ImmediateSelect', 'ImmSignExt', 'CompSign', 
            'Ra', 'Rb', 'Rd', 'PCSelect', 'BranchSelect', 'MemLoad', 'MemStore', 'MemWord', 
            'MemSignExt', 'FuncField', 'SaControl', 'ASelect', 'ExecOut']

print "Instruction[32] ",
for out, l in zip(TestUnit.outputs, TestUnit.output_lens):
    print out + "[" + str(l) + "] ",
print ""
for test in tests:
    print test