import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class UserMain
{
	public static void main(String[] args)
	{
		try
		{
			FileReader fr = new FileReader("/Volumes/Yo/proj1/unix/input01.txt");
			PrintWriter pw = new PrintWriter("/Volumes/Yo/61932813.txt");
			BufferedReader br = new BufferedReader(fr);
			String check = "";
			FileSystem fs;
			fs = new FileSystem();
			int isin = 0;
			while ((check = br.readLine()) != null)
			{
				check.replaceAll("\\s","");
				if (check.equals(""))
				{
					pw.println();
					continue;
				}
				if (check.substring(0, 2).equals("cr") && isin == 1)
				{
					if (fs.create(check.substring(3)) == -1)
					{
						pw.println("error");
					}
					else
					{
						pw.println(check.substring(3) + " created");
					}
				}
				else if (check.substring(0, 2).equals("de") && isin == 1)
				{
					if (fs.destroy(check.substring(3)) == -1)
					{
						pw.println("error");
					}
					else
					{
						pw.println(check.substring(3) + " destroyed");
					}
				}
				else if (check.substring(0, 2).equals("op") && isin == 1)
				{
					int op = fs.open(check.substring(3));
					if (op == -1)
					{
						pw.println("error");
					}
					else
					{
						pw.println(check.substring(3) + " opened " + op);
					}
				}
				else if (check.substring(0, 2).equals("cl") && isin == 1)
				{
					int cl = fs.close(Integer.parseInt(check.substring(3)));
					if (cl == -1)
					{
						pw.println("error");
					}
					else
					{
						pw.println(check.substring(3) + " closed");
					}
				}
				else if (check.substring(0, 2).equals("rd") && isin == 1)
				{
					byte[] mem = new byte[192];
					fs.read(Integer.parseInt(check.substring(3,4)), mem, Integer.parseInt(check.substring(5)));
					char[] print = new String(mem).toCharArray();
					for (int i = 0; i < Integer.parseInt(check.substring(5)); i++)
					{
						pw.print(print[i]);
					}
					pw.print("\n");
				}
				else if (check.substring(0, 2).equals("wr") && isin == 1)
				{
					byte[] mem = new byte[192];
					for (int i = 0; i < 192; i++)
					{
						mem[i] = check.getBytes()[5];
					}
					if (fs.write(Integer.parseInt(check.substring(3, 4)), mem, Integer.parseInt(check.substring(7))) == -1)
					{
						pw.println("error");
					}
					else
					{
						pw.println(check.substring(7) + " bytes written");
					}
				}
				else if (check.substring(0, 2).equals("sk") && isin == 1)
				{
					if (fs.lseek(Integer.parseInt(check.substring(3,4)), Integer.parseInt(check.substring(5))) == -1)
					{
						pw.println("error");
					}
					else
					{
						pw.println("position is " + check.substring(5));
					}
				}
				else if (check.substring(0, 2).equals("dr") && isin == 1)
				{
					byte[] temp = new byte[96];
					temp = fs.directory();
					for (int i = 0; i < 96; i++)
					{
						if (i % 4 == 0 && i != 0 && temp[i] != 0)
						{
							pw.print(" ");
						}
						pw.print(new String(temp).toCharArray()[i]);
					}
					pw.println();
				}
				else if (check.substring(0, 2).equals("in"))
				{
					fs = new FileSystem();
					if (check.length() == 2)
					{
						fs.init();
						pw.println("disk initialized");
						isin = 1;
					}
					else
					{
						if (fs.init(check.substring(3)) == -1)
						{
							fs.init();
							pw.println("disk initialized");
							isin = 1;
						}
						else
						{
							pw.println("disk restored");
							isin = 1;
						}
					}
				}
				else if (check.substring(0, 2).equals("sv") && isin == 1)
				{
					fs.save(check.substring(3));
					pw.println("disk saved");
				}
				else
				{
					pw.println();
				}
			}
			br.close();
			fr.close();
			pw.close();
		}
		catch (FileNotFoundException e)
		{
			return;
		}
		catch (IOException e)
		{
			return;
		}
	}
}
