import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileSystem
{

	static class OFT
	{
		byte[] buf;
		int index;
		int pos;
		int length;
		
		OFT(byte[] newBuf, int newI, int newPos, int newLength)
		{
			buf = newBuf;
			index = newI;
			pos = newPos;
			length = newLength;
		}
	}
	
	private IOSystem io;
	private OFT[] oft;
	private int[] MASK;
	private int[] MASK2;

	FileSystem()
	{
	}
	
	//Creates the masks used for the bitmap
	private void createMasks()
	{
		MASK = new int[32];
		MASK2 = new int[32];
		int x = 0x80000000;
		for (int i = 0; i < 32; ++i)
		{
			MASK[i] = x;
			MASK2[i] = ~x;
			x = x >>> 1;
		}
	}
	
	//Packs the int array given to the bitmap to update it.
	private void packBMToDisk(int[] toPack)
	{
		PackableMemory PM = new PackableMemory(64);
		PM.pack(toPack[0], 0);
		PM.pack(toPack[1], 4);
		io.write_block(0, PM.mem);
	}
	
	//Searches the bitmap to find an empty block in the disk and returns that block number.
	//If there are no available blocks, returns -1.
	private int updateBM()
	{
		PackableMemory PM = new PackableMemory(64);
		io.read_block(0, PM.mem);
		for (int i = 0; i < 5; i += 4)
		{
			int t = PM.unpack(i);
			for (int j = 0; j < 32; j++)
			{
				if ((int)(t & MASK[j]) == 0)
				{
					t = (int)(t | MASK[j]);
					int[] temp = new int[2];
					if (i == 0)
					{
						int y = PM.unpack(4);
						temp[0] = t;
						temp[1] = y;
						packBMToDisk(temp);
						return j;
					}
					else
					{
						int y = PM.unpack(0);
						temp[0] = y;
						temp[1] = t;
						packBMToDisk(temp);
						return j + 32;
					}
				}
			}
		}
		return -1;
	}
	
	//Searches the directory. Returns -1 if that file is not in the directory, or 
	//returns the index of the file descriptor
	private int searchDir(String fileName)
	{
		lseek(0, 0);
		PackableMemory PM = new PackableMemory(192);
		byte[] check = new byte[192];
		read(0, check, oft[0].length);
		for (int i = 0; i < oft[0].length; i += 8)
		{
			byte[] t = new byte[4];
			for (int j = 0; j < 4; j++)
			{
				t[j] = check[j+i];
			}
			String temp = fileName;
			for (int k = 0; k < 4 - fileName.length(); k++)
			{
				temp = temp + '/';
			}
			if (temp.equals(new String(t)))
			{
				PM.mem = check;
				return PM.unpack(i + 4);
			}
		}
		return -1;
	}
	
	//Sets the file descriptor with the given number at the given position.
	private void setDesc(int num, int pos)
	{
		byte[] check = new byte[64];
		io.read_block((pos/64) + 1, check);
		PackableMemory PM = new PackableMemory(64);
		PM.pack(num, 0);
		check[pos%64] = PM.mem[0];
		check[(pos%64)+1] = PM.mem[1];
		check[(pos%64)+2] = PM.mem[2];
		check[(pos%64)+3] = PM.mem[3];
		io.write_block((pos/64) + 1, check);
	}
	
	//Finds a free file descriptor and returns its position.
	private int findDesc()
	{
		PackableMemory PM = new PackableMemory(64);
		byte[] check = new byte[64];
		for (int i = 1; i < 7; i++)
		{
			io.read_block(i, check);
			for (int j = 0; j < 64; j += 16)
			{
				PM.mem = check;
				if (PM.unpack(j) == -1)
				{
					PM.pack(-2, 0);
					check[j] = PM.mem[0];
					check[j+1] = PM.mem[1];
					check[j+2] = PM.mem[2];
					check[j+3] = PM.mem[3];
					io.write_block(i, check);
					return j * i;
				}
			}
		}
		return -1;
	}
	
	//Switches to the proper block, and creates a new block if needed.
	private int switchBlock(int toBlock, int index, int currBlock)
	{
		io.write_block(getLoc(oft[index].index, currBlock), oft[index].buf);
		if (getLoc(oft[index].index, toBlock) <= -1)
		{
			int test = updateBM();
			if (test == -1)
			{
				return -1;
			}
			setDesc(test, oft[index].index + 4*toBlock);
		}
		oft[index].buf = new byte[64];
		io.read_block(getLoc(oft[index].index, toBlock), oft[index].buf);
		return 0;
	}
	
	//Gets the location of a block from a file descriptor index.
	private int getLoc(int fdi, int dmn)
	{
		PackableMemory PM = new PackableMemory(64);
		io.read_block((fdi/64) + 1, PM.mem);
		return PM.unpack((fdi%64) + 4*dmn);
	}

	int create(String fileName)
	{
		if (oft[0] != null && oft[0].length == 192)
		{
			return -1;
		}
		if (fileName.length() > 4 || fileName.length() == 0)
		{
			return -1;
		}
		if (searchDir(fileName) != -1)
		{
			return -1;
		}
		PackableMemory PM = new PackableMemory(64);
		write(0, fileName.getBytes(), fileName.length());
		if (fileName.length() < 4)
		{
			byte[] temp = new byte[4];
			temp[0] = '/';
			temp[1] = '/';
			temp[2] = '/';
			temp[3] = '/';
			write(0, temp, 4 - fileName.length());
		}
		int full = findDesc();
		if (full == -1)
		{
			return -1;
		}
		PM.pack(full, 0);
		write(0, PM.mem, 4);
		return 0;
	}
	
	int destroy(String fileName)
	{
		int p = searchDir(fileName);
		if (p == -1)
		{
			return -1;
		}
		PackableMemory PM = new PackableMemory(64);
		io.read_block(0, PM.mem);
		for (int i = 1; i < 4; ++i)
		{
			int x = getLoc(p, i);
			if (x > 0)
			{
				int[] temp1 = new int[2];
				if (x < 32)
				{
					int temp = PM.unpack(0);
					temp = temp & MASK2[x];
					temp1[0] = temp;
					temp1[1] = PM.unpack(4);
					packBMToDisk(temp1);
				}
				else
				{
					int temp = PM.unpack(4);
					temp = temp & MASK2[x];
					temp1[0] = PM.unpack(0);
					temp1[0] = temp;
					packBMToDisk(temp1);
				}
			}
		}
		setDesc(-1, p);
		setDesc(-1, p+4);
		setDesc(-1, p+8);
		setDesc(-1, p+12);
		lseek(0, 0);
		PM = new PackableMemory(192);
		byte[] check = new byte[192];
		read(0, check, oft[0].length);
		for (int i = 0; i < oft[0].length; i += 8)
		{
			byte[] t = new byte[4];
			for (int j = 0; j < 4; j++)
			{
				t[j] = check[j+i];
			}
			String temp = fileName;
			for (int k = 0; k < 4 - fileName.length(); k++)
			{
				temp = temp + '/';
			}
			if (temp.equals(new String(t)))
			{
				lseek(0, i);
				write(0, new String(oft[0].buf).substring(i+8).getBytes(), oft[0].length - oft[0].pos - 8);
				oft[0].length -= 8;
			}
		}
		return 0;
	}
	
	int close(int index)
	{
		if (oft[index].index == -1)
		{
			return -1;
		}
		io.write_block(getLoc(oft[index].index, oft[index].pos/64 + 1), oft[index].buf);
		setDesc(oft[index].length, oft[index].index);
		oft[index].index = -1;
		return 0;
	}
	
	byte[] directory()
	{
		lseek(0, 0);
		byte[] check = new byte[192];
		byte[] ret = new byte[192];
		read(0, check, oft[0].length);
		for (int i = 0; i < oft[0].length; i += 8)
		{
			for (int j = 0; j < 4; j++)
			{
				if (check[j+i] != '/')
				{
					ret[j+i] = check[j+i];
				}
			}
		}
		return ret;
	}
	
	int open(String fileName)
	{
		int p = searchDir(fileName);
		if (p == -1)
		{
			return -1;
		}
		int i;
		int c = -1;
		for (i = 1; i < 4; i++)
		{
			if (oft[i].index == -1)
			{
				c = 0;
				break;
			}
		}
		if (c == -1)
		{
			return -1;
		}
		if (getLoc(p, 0) == -2)
		{
			setDesc(0, p);
			int test = updateBM();
			if (test == -1)
			{
				return -1;
			}
			setDesc(test, p + 4);
			oft[i].buf = new byte[64];
		}
		else
		{
			oft[i].buf = new byte[64];
			io.read_block(getLoc(p, 1), oft[i].buf);
		}
		for (int y = 1; y < 4; y++)
		{
			if (oft[y].index == p)
			{
				return -1;
			}
		}
		oft[i].length = getLoc(p, 0);
		oft[i].index = p;
		oft[i].pos = 0;
		return i;
	}

	byte[] read(int index, byte[] mem_area, int count)
	{
		for (int i = 0; i < count && (i + oft[index].pos) < oft[index].length; i++)
		{
			if ((i + oft[index].pos) == 64)
			{
				switchBlock(2, index, 1);
			}
			if ((i + oft[index].pos) == 128)
			{
				switchBlock(3, index, 2);
			}
			mem_area[i] = oft[index].buf[(i+oft[index].pos) % 64];
		}
		oft[index].pos += count;
		return mem_area;
	}

	int write(int index, byte[] mem_area, int count)
	{
		if ((count + oft[index].pos) >= 192)
		{
			return -1;
		}
		for (int i = 0; i < count; i++)
		{
			if ((i + oft[index].pos) == 64)
			{
				if (switchBlock(2, index, 1) == -1)
				{
					return -1;
				}
			}
			if ((i + oft[index].pos) == 128)
			{
				if (switchBlock(3, index, 2) == -1)
				{
					return -1;
				}
			}
			oft[index].buf[(i+oft[index].pos) % 64] = mem_area[i];
		}
		if (oft[index].pos == oft[index].length)
		{
			oft[index].length += count;
		}
		if (oft[index].pos + count > oft[index].length)
		{
			oft[index].length += oft[index].pos - count;
		}
		oft[index].pos += count;
		return count;
	}

	int lseek(int index, int newPos)
	{
		if (newPos < 64 && newPos < oft[index].length + 1)
		{
			if (oft[index].pos > 64 && oft[index].pos < 128)
			{
				switchBlock(1, index, 2);
				oft[index].pos = newPos;
			}
			else if (oft[index].pos > 128)
			{
				switchBlock(1, index, 3);
				oft[index].pos = newPos;
			}
			else
			{
				if (oft[index].pos == 128)
				{
					switchBlock(1, index, 2);
				}
				oft[index].pos = newPos;
			}
		}
		else if (newPos >= 64 && newPos < 128 && newPos < oft[index].length + 1)
		{
			if (oft[index].pos < 64 || oft[index].pos >= 128)
			{
				switchBlock(2, index, (oft[index].pos/64) + 1);
				oft[index].pos = newPos;
			}
			else
			{
				oft[index].pos = newPos;
			}
		}
		else if (newPos >= 128 && newPos < 192 && newPos < oft[index].length + 1)
		{
			if (oft[index].pos < 128)
			{
				switchBlock(3, index, (oft[index].pos/64) + 1);
				oft[index].pos = newPos;
			}
			else
			{
				oft[index].pos = newPos;
			}
		}
		else
		{
			return -1;
		}
		return 0;
	}
	
	void init()
	{
		io = new IOSystem();
		createMasks();
		oft = new OFT[4];
		oft[0] = new OFT(new byte[64], 0, 0, 0);
		int p = updateBM();
		setDesc(0, 0);
		setDesc(p, 4);
		oft[1] = new OFT(new byte[64], -1, 0, 0);
		oft[2] = new OFT(new byte[64], -1, 0, 0);
		oft[3] = new OFT(new byte[64], -1, 0, 0);
	}
	
	int init(String fileName)
	{
		io = new IOSystem();
		createMasks();
		oft = new OFT[4];
		try
		{
			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);
			String l;
			int k = 0;
			while((l = br.readLine()) != null) 
			{
				String [] temp = l.split(" ");
				byte[] check = new byte[64];
				int i = 0;
				for (String string : temp)
				{
					float x = Float.parseFloat(new String(string));
					check[i] = (byte)x;
					++i;
				}
				io.write_block(k, check);
				++k;
			}
			br.close();
			fr.close();
		}
		catch (FileNotFoundException e)
		{
			return -1;
		}
		catch (IOException e)
		{
			return -1;
		}
		byte[] newb = new byte[64];
		io.read_block(getLoc(0, 1), newb);
		oft[0] = new OFT(newb, 0, 0, getLoc(0,0));
		oft[1] = new OFT(new byte[64], -1, 0, 0);
		oft[2] = new OFT(new byte[64], -1, 0, 0);
		oft[3] = new OFT(new byte[64], -1, 0, 0);
		return 0;
	}
	
	void save(String fileName)
	{
		close(0);
		close(1);
		close(2);
		close(3);
		File file = new File(fileName);
		PrintWriter pw;
		try
		{
			pw = new PrintWriter(file);
			for (int i = 0; i < 64; i++)
			{
				byte[] temp = new byte[64];
				io.read_block(i, temp);
				for (int j = 0; j < 64; j++)
				{
					pw.print(temp[j] + " ");
				}
				pw.println();
			}
		}
		catch (FileNotFoundException e)
		{
			return;
		}
		pw.close();
	}

}
