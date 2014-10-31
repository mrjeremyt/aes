import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;


public class AES 
{
	protected static int [][] x_box;
	protected static int [][] state;
	protected static int [][] key;
	protected static int [][] expanded_key;
	protected static int [][] rcon;
	
	public static void main(String[] args) throws IOException 
	{
		fill_xbox();
		fill_rcon();
		Boolean encrypt = true;
		if(!args[0].toLowerCase().equals("e"))
			encrypt = false;
		Scanner file = new Scanner(new File(args[2]));
		make_key(args);
		expand_key();
		
		if (encrypt)
		{
			PrintWriter pw = new PrintWriter(new File (args[2].toString() + ".enc"));
			encrypt(file, pw);
			pw.close();
		}
		else
		{
			PrintWriter pw = new PrintWriter(new File (args[2].toString() + ".dec"));
			decrypt(file, pw);
			pw.close();
		}
		file.close();
	}
	
	
	static void encrypt(Scanner sc, PrintWriter pw){
		while (sc.hasNextLine())
		{
			make_a_state(sc);
			subBytes();
			shiftRows();
		}
	}
	
	static void decrypt(Scanner sc, PrintWriter pw){
		while(sc.hasNextLine()){
			make_a_state(sc);
		}
	}
	
	static void subBytes(){
//		print_array(state);
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[0].length; j++){
				int top = (state[j][i] >> 4);
				int bottom = (state[j][i] & 0x0F);
				state[j][i] = x_box[top][bottom];
			}
		}
//		print_array(state);
	}
	
	static void shiftRows(){
		//shift of 1
		int[] row_1 = state[1];
		int[] row_2 = state[2];
		int[] row_3 = state[3];
		
		//setup the temp vars
		int temp_1;
		int temp_2;
		int temp_3;
		
//		print_array(state);
		
		//first shift
		temp_1 = row_1[0];
		row_1[0] = row_1[1]; row_1[1] = row_1[2]; row_1[2] = row_1[3]; row_1[3] = temp_1;
		
		//second shift
		temp_1 = row_2[0];
		temp_2 = row_2[1];
		row_2[0] = row_2[2]; row_2[1] = row_2[3]; row_2[2] = temp_1; row_2[3] = temp_2;
		
		//third shift
		temp_1 = row_3[0];
		temp_2 = row_3[1];
		temp_3 = row_3[2];
		row_3[0] = row_3[3]; row_3[1] = temp_1; row_3[2] = temp_2; row_3[3] = temp_3;
		
		//put back into state
		state[1] = row_1;
		state[2] = row_2;
		state[3] = row_3;
		
//		print_array(state);
	}
	
	static void mixColumns(){
		
	}
	
	static void addRoundKey()
	{
		
	}
	
	private static void expand_key(){
		expanded_key = new int[4][44];
		
		for(int i = 0; i <key.length; i++){
			for(int j = 0; j < key[i].length; j++){
				expanded_key[j][i] = key[j][i];		
				}		
			}

		
	}
	
	private static void rotword (int offset)
	{
		int temp_0 = expanded_key [offset][0];
		int temp_1 = expanded_key [offset][1];
		int temp_2 = expanded_key [offset][2];
		int temp_3 = expanded_key [offset][3];
		
		expanded_key[offset][0] = temp_1;
		expanded_key[offset][1] = temp_2;
		expanded_key[offset][2] = temp_3;
		expanded_key[offset][3] = temp_0;
	}
	
	private static  void subword (int offset)
	{
		for (int i = 0; i < 4; i++){
			int top = (expanded_key[offset][i] >> 4);
			int bottom = (expanded_key[offset][i] & 0x0F);
			expanded_key[offset][i] = x_box[top][bottom];
		}
	}
	
	private static void make_key(String[] args) throws FileNotFoundException {
		Scanner the_key = new Scanner(new File(args[1]));
		if(!the_key.hasNextLine()){
			System.out.println("No key present");
			System.exit(-1);
		}else{
			String k = the_key.nextLine();
			if(!(k.length() == 32)){
				System.out.println("Incorrect key size");
				System.out.println(k);
				System.exit(-1);
			}else{
				key = new int [4][4];
				int subindex = 0;
				ArrayList<Integer> al = new ArrayList<Integer>(); 
				for (int i = 0; i < 16; i++){
					al.add((Integer.decode("0x" +  k.substring(subindex, subindex+2))));
					subindex+=2;
				}
				int count = 0;
				for(int i = 0; i < key.length; i++){
					for(int j = 0; j < key[i].length; j++){
						key[j][i] = al.get(count++);	}
				}
//				print_array(key);
			}
		}
		the_key.close();
	}
	
	
	static void make_a_state(Scanner sc)
	{
		String test_state = sc.nextLine();
		if (test_state.length() > 32) 
		{
			//Truncate sheet.
			test_state = test_state.substring(0, 32);
		}
		else if (test_state.length() < 32)
		{
			//Paaaaaad 0s at back.
			StringBuilder sb = new StringBuilder(test_state);
			while(sb.length() < 32)
				sb.append('0');
			
			test_state = sb.toString();		
		}
		
		state = new int [4][4];
		int subindex = 0;
		ArrayList<Integer> al = new ArrayList<Integer>(); 
		for (int i = 0; i < 16; i++){
			al.add((Integer.decode("0x" +  test_state.substring(subindex, subindex+2))));
			subindex+=2;
		}
		int count = 0;
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[i].length; j++){
				state[j][i] = al.get(count++);
			}
		}
//		print_array(state);
		
	}
	
	static void print_array(int[][] a){
		for(int[] i: a){
			for(int j: i){
				System.out.print(j + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	static void fill_xbox ()
	{
		x_box = new int [][]
		{
			   {0x63, 0x7C, 0x77, 0x7B, 0xF2, 0x6B, 0x6F, 0xC5, 0x30, 0x01, 0x67, 0x2B, 0xFE, 0xD7, 0xAB, 0x76},
			   {0xCA, 0x82, 0xC9, 0x7D, 0xFA, 0x59, 0x47, 0xF0, 0xAD, 0xD4, 0xA2, 0xAF, 0x9C, 0xA4, 0x72, 0xC0},
			   {0xB7, 0xFD, 0x93, 0x26, 0x36, 0x3F, 0xF7, 0xCC, 0x34, 0xA5, 0xE5, 0xF1, 0x71, 0xD8, 0x31, 0x15},
			   {0x04, 0xC7, 0x23, 0xC3, 0x18, 0x96, 0x05, 0x9A, 0x07, 0x12, 0x80, 0xE2, 0xEB, 0x27, 0xB2, 0x75},
			   {0x09, 0x83, 0x2C, 0x1A, 0x1B, 0x6E, 0x5A, 0xA0, 0x52, 0x3B, 0xD6, 0xB3, 0x29, 0xE3, 0x2F, 0x84},
			   {0x53, 0xD1, 0x00, 0xED, 0x20, 0xFC, 0xB1, 0x5B, 0x6A, 0xCB, 0xBE, 0x39, 0x4A, 0x4C, 0x58, 0xCF},
			   {0xD0, 0xEF, 0xAA, 0xFB, 0x43, 0x4D, 0x33, 0x85, 0x45, 0xF9, 0x02, 0x7F, 0x50, 0x3C, 0x9F, 0xA8},
			   {0x51, 0xA3, 0x40, 0x8F, 0x92, 0x9D, 0x38, 0xF5, 0xBC, 0xB6, 0xDA, 0x21, 0x10, 0xFF, 0xF3, 0xD2},
			   {0xCD, 0x0C, 0x13, 0xEC, 0x5F, 0x97, 0x44, 0x17, 0xC4, 0xA7, 0x7E, 0x3D, 0x64, 0x5D, 0x19, 0x73},
			   {0x60, 0x81, 0x4F, 0xDC, 0x22, 0x2A, 0x90, 0x88, 0x46, 0xEE, 0xB8, 0x14, 0xDE, 0x5E, 0x0B, 0xDB},
			   {0xE0, 0x32, 0x3A, 0x0A, 0x49, 0x06, 0x24, 0x5C, 0xC2, 0xD3, 0xAC, 0x62, 0x91, 0x95, 0xE4, 0x79},
			   {0xE7, 0xC8, 0x37, 0x6D, 0x8D, 0xD5, 0x4E, 0xA9, 0x6C, 0x56, 0xF4, 0xEA, 0x65, 0x7A, 0xAE, 0x08},
			   {0xBA, 0x78, 0x25, 0x2E, 0x1C, 0xA6, 0xB4, 0xC6, 0xE8, 0xDD, 0x74, 0x1F, 0x4B, 0xBD, 0x8B, 0x8A},
			   {0x70, 0x3E, 0xB5, 0x66, 0x48, 0x03, 0xF6, 0x0E, 0x61, 0x35, 0x57, 0xB9, 0x86, 0xC1, 0x1D, 0x9E},
			   {0xE1, 0xF8, 0x98, 0x11, 0x69, 0xD9, 0x8E, 0x94, 0x9B, 0x1E, 0x87, 0xE9, 0xCE, 0x55, 0x28, 0xDF},
			   {0x8C, 0xA1, 0x89, 0x0D, 0xBF, 0xE6, 0x42, 0x68, 0x41, 0x99, 0x2D, 0x0F, 0xB0, 0x54, 0xBB, 0x16}
			};
	}
	
	static void fill_rcon ()
	{
		rcon = new int [][]
		{
			   {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36},
			   {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
			   {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
			   {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}
			};
	}
	
}
