public class CPU {
	// RAM
	private char[] ram = new char[0xff];
	// Register file
	// Shouldn't that be short?
	private char[] register = new char[16];
	// Program counter
	private byte pc = 0;

	// Public methods
	public char getRam(char address) {
		return ram[address];
	}

	public void setRam(int address, int value) {
		ram[address] = (char) value;
	}

	public char getRegister(byte a) {
		return register[a];
	}

	public byte getPC() {
		return pc;
	}

	public void reset() {
		pc = 0;
	}

	private static enum Operation {
		ADD, SUB, MUL, DIV, AND, OR, XNOR, MOV, JZE, JZN, JZG, JZL, LOAD, STOR;
	}

	// Symbolic instruction set: symbol[0x0] is ADD, symbol[0x1] is SUB, etc.
	private static Operation[] symbol = { Operation.ADD, Operation.SUB, Operation.MUL, Operation.DIV, Operation.AND,
			Operation.OR, Operation.XNOR, Operation.MOV, Operation.JZE, Operation.JZN, Operation.JZG, Operation.JZL,
			Operation.LOAD, Operation.LOAD, Operation.STOR, Operation.STOR };

	// Instruction decoder
	private static class Instruction {
		public byte opcode;
		public byte[] operand = new byte[3];
		public byte imm;

		Instruction(char bits) {
			opcode = (byte) ((bits & 0xF000) >> 12); // Bits 12-15 are opcode
			operand[0] = (byte) ((bits & 0x0F00) >> 8); // Bits 8-11 are the
														// first operand
			operand[1] = (byte) ((bits & 0x00F0) >> 4); // Bits 4-7 are the
														// second operand
			operand[2] = (byte) (bits & 0x000F); // Bits 0-3 are the third
													// operand
			imm = (byte) (bits & 0x00FF); // Bits 0-7 are the immediate
		}
	}

	// Arithmetic/Logic Unit
	private char ALU(byte opcode, char a, char b, byte imm) {
		switch (symbol[opcode]) {
		case ADD:
			return (char) (a + b); // ADD Rd, Ra, Rb Rd = Ra + Rb
		case SUB:
			return (char) (a - b); // SUB Rd, Ra, Rb Rd = Ra - Rb
		case MUL:
			return (char) (a * b); // MUL Rd, Ra, Rb Rd = Ra * Rb
		case DIV:
			return (char) (a / b); // DIV Rd, Ra, Rb Rd = Ra / Rb
		case AND:
			return (char) (a & b); // AND Rd, Ra, Rb Rd = Ra & Rb
		case OR:
			return (char) (a | b); // OR Rd, Ra, Rb Rd = Ra | Rb
		case XNOR:
			return (char) ~(a ^ b); // XNOR Rd, Ra, Rb Rd = !(Ra ^ Rb)
		case MOV:
			return (char) imm; // MOV Rd, mmm Rd = mmm
		default:
			return 0;
		}
	}

	// Program Control
	private byte PC(byte opcode, byte a, byte imm) {
		switch (symbol[opcode]) {
		case JZE:
			if (register[a] == 0)
				return imm; // JZE Ra, mmm if (Ra == 0) goto mmm
		case JZN:
			if (register[a] != 0)
				return imm; // JZN Ra, mmm if (Ra != 0) goto mmm
		case JZG:
			if (register[a] > 0)
				return imm; // JZG Ra, mmm if (Ra > 0) goto mmm
		case JZL:
			if (register[a] < 0)
				return imm; // JZL Ra, mmm if (Ra < 0) goto mmm
		default:
			return (byte) (pc + 1);
		}
	}

	// Bus control
	private void bus(byte opcode, byte a, byte imm) {
		switch (opcode) {
		case 0xC:
			register[a] = ram[(imm & 0xff)];
			break; // LOAD Ra, mmm Ra = RAM[mmm]
		case 0xD:
			register[a] = ram[register[a]];
			break; // LOAD Ra Ra = RAM[Ra]
		case 0xE:
			ram[(imm & 0xff)] = register[a];
			break; // STOR Ra, mmm RAM[mmm] = Ra
		case 0xF:
			ram[register[a]] = register[a];
			break; // STOR Ra RAM[Ra] = Ra
		}
	}

	// Fetch the next instruction for the CPU to decode and execute from RAM
	private char fetch() {
		return ram[pc];
	}

	// Decode the instruction into the opcode and operands
	private static Instruction decode(char instruction) {
		return new Instruction(instruction);
	}

	// Perform the operation on the operands
	private void execute(Instruction instruction) {
		if (instruction.opcode <= 0x7) // ALU instructions
			register[instruction.operand[0]] = ALU(instruction.opcode, register[instruction.operand[1]],
					register[instruction.operand[2]], instruction.imm);
		else if (instruction.opcode >= 0xC) // Load/store instructions
			bus(instruction.opcode, instruction.operand[0], instruction.imm);
		pc = PC(instruction.opcode, instruction.operand[0], instruction.imm);
	}

	// The fetch/decode/execute execute cycle
	public void run() {
		while (ram[pc] != 0)
			execute(decode(fetch()));
	}

	public static void main(String[] args) {
		CPU cpu = new CPU();
		cpu.setRam(0, 0x7142); // MOV R1 0x42
		cpu.setRam(1, 0xe180); // STOR R1 RAM[0x80]
		cpu.run();
		System.out.println(Integer.toHexString(cpu.getRam((char) 0x80)));
	}
}