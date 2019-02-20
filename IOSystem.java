
public class IOSystem
{

	private final byte[][] ldisk;
	private int[] BM;

	IOSystem()
	{
		ldisk = new byte[64][64];
		makeBM();
		setDescriptor();
	}
	
	private void setDescriptor()
	{
		PackableMemory PM = new PackableMemory(64);
		PM.pack(-1, 0);
		for (int i = 1; i < 7; i++)
		{
			for (int j = 0; j < 64; j += 4)
			{
				if (i == 1 && j == 0)
				{
					ldisk[1][0] = 0;
					continue;
				}
				ldisk[i][j] = PM.mem[0];
				ldisk[i][j+1] = PM.mem[1];
				ldisk[i][j+2] = PM.mem[2];
				ldisk[i][j+3] = PM.mem[3];
			}
		}
	}
	
	private void makeBM()
	{
		PackableMemory PM = new PackableMemory(64);
		BM = new int[2];
		BM[0] = 0xFE000000;
		BM[1] = 0x00000000;
		PM.pack(BM[0], 0);
		PM.pack(BM[1], 4);
		ldisk[0] = PM.mem;
	}

	void read_block(int i, byte[] p)
	{
		for (int j = 0; j < 64; j++)
		{
			if (i == 0 && j == 8)
			{
				break;
			}
			p[j] = ldisk[i][j];
		}
	}


	void write_block(int i, byte[] p)
	{
		for (int j = 0; j < 64; j++)
		{
			ldisk[i][j] = p[j];
		}
	}

}
