import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;


public class AES 
{
	protected static int [][] x_box;
	protected static int [][] inv_x_box;
	protected static int [][] state;
	protected static int [][] key;
	protected static int [][] expanded_key;
	protected static int [][] rcon;
	protected static int [][] e_table;
	protected static int [][] l_table;
	protected static int [][] mix_col_matrix; 
	protected static double num_bytes;
	protected static boolean encrypt;
	protected static int key_size;
	protected static int num_rounds;
	
	public static void main(String[] args) throws IOException 
	{
		fill_xbox();
		fill_inv_xbox();
		fill_rcon();
		fill_e();
		fill_l();

		encrypt = true;
		if(!args[0].toLowerCase().equals("e"))
			encrypt = false;

		boolean ecb = true;
		File key = null;
		File f = null;
		Path path = null;
		Scanner s = null;
		
		if(args.length == 7){
			key_size = Integer.parseInt(args[2]);
			key = new File(args[5]);
			if(!encrypt){
				s = new Scanner(new File(args[6])); 
				f = new File(args[6].toString() + ".dec");
			}
			else
				path = Paths.get(args[6]);
				
			if(!args[4].toLowerCase().equals("ecb"))
				ecb = false;
			
		}else if(args.length == 3){
			key_size = 128;
			key = new File(args[1]);
			if(!encrypt){
				s = new Scanner(new File(args[2])); 
				f = new File(args[2].toString() + ".dec");
			}
			else
				path = Paths.get(args[2]);
		}else{
			System.out.println("Incorrect execution line"); System.exit(-1);
		}
		
		byte[] data = null;
		ByteArrayInputStream is = null;
		Writer w = null;
		
		
		if(encrypt){
			//read the entire file into this byte array
			data = Files.readAllBytes(path);

			//create a stream on the array
			is = new ByteArrayInputStream(data);
		}

		make_key(key);
		expand_key();
		
		Stopwatch sc = new Stopwatch();
		
		if (encrypt)
		{
			PrintWriter pw = null;
			if(args.length == 7)
				pw = new PrintWriter(new File (args[6].toString() + ".enc"));
			else
				pw = new PrintWriter(new File (args[2].toString() + ".enc"));
			sc.start();
			encrypt(pw, is, s);
			sc.stop();
			pw.close();
			System.out.println("Encryption: " + ((num_bytes/1024)/1024)/sc.time() + " MB/sec");
		}
		else
		{
			
			try {
				w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			sc.start();
			decrypt(is, s, w);
			sc.stop();
			System.out.println("Decryption: " + ((num_bytes/1024)/1024)/sc.time() + " MB/sec");
			w.close();
		}
	}
	
	
	static void encrypt(PrintWriter pw, ByteArrayInputStream is, Scanner sc){
		while (is.available() > 0)
		{
			make_a_state(is, sc);
			int ex_key = 0;
			ex_key = addRoundKey(ex_key);
			
			int round = 1;
			while(round++ < num_rounds){
				subBytes();
				shiftRows();
				mixColumns();
				ex_key = addRoundKey(ex_key);
			}
			subBytes();
			shiftRows();
			ex_key = addRoundKey(ex_key);
			if(is.available() > 0)
				pw.println(string_from_state());
			else
				pw.print(string_from_state());
		}
	}


	static void decrypt(ByteArrayInputStream is, Scanner sc, Writer w){
		while (sc.hasNextLine())
		{
			make_a_state(is, sc);
			//int ex_key = 43;
			int ex_key = ((num_rounds + 1) * 4) - 1;
			ex_key = (invAddRoundKey(ex_key));
			invShiftRows();
			invSubBytes();
			
			int round = num_rounds -1;
			while(round-- > 0){
				ex_key = invAddRoundKey(ex_key);
				invMixColumns();
				invShiftRows();
				invSubBytes();
			}
			ex_key = invAddRoundKey(ex_key);
			
			for(int i = 0; i < state.length; i++){
				for(int j = 0; j < state[i].length; j++){
					try {
						w.write(state[j][i]);
					} catch (IOException e) {
						System.out.println("Something bad happened");
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	static String string_from_state(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[i].length; j++){
				String result = Integer.toHexString(state[j][i]);
				if(result.equals("0") && result.length() == 1)
					result = "00";
				else if(result.length() == 1)
					result = "0" + result;
				if(encrypt)		sb.append(result.toUpperCase());
			}
		}		
		return sb.toString();
	}
	
	static void subBytes(){
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[0].length; j++){
				int top = (state[j][i] >> 4);
				int bottom = (state[j][i] & 0x0F);
				state[j][i] = x_box[top][bottom];
			}
		}
	}
	
	private static void invSubBytes() {
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[0].length; j++){
				int top = (state[j][i] >> 4);
				int bottom = (state[j][i] & 0x0F);
				state[j][i] = inv_x_box[top][bottom];
			}
		}
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
	}
	
	static void invShiftRows(){
		//shift of 1
		int[] row_1 = state[1];
		int[] row_2 = state[2];
		int[] row_3 = state[3];
		
		//setup the temp vars
		int temp_1;
		int temp_2;
		int temp_3;
			
		//first shift
		temp_1 = row_1[3];
		row_1[3] = row_1[2]; row_1[2] = row_1[1]; row_1[1] = row_1[0]; row_1[0] = temp_1; 
		
		//second shift
		temp_1 = row_2[3];
		temp_2 = row_2[2];
		row_2[3] = row_2[1]; row_2[2] = row_2[0]; row_2[0] = temp_2; row_2[1] = temp_1;
		//third shift
		temp_1 = row_3[1];
		temp_2 = row_3[2];
		temp_3 = row_3[3];
		row_3[3] = row_3[0]; row_3[0] = temp_1; row_3[1] = temp_2; row_3[2] = temp_3;
		
		//put back into state
		state[1] = row_1;
		state[2] = row_2;
		state[3] = row_3;
	}
	
	private static void mixColumns(){		
		for(int i = 0; i < 4; i++){
			mixColumn2(i);
		}
	}
	
	private static void invMixColumns(){
		for(int i = 0; i < 4; i++)
			invMixColumn2(i);
	}
	
////////////////////////the mixColumns Tranformation ////////////////////////
	private static int mul (int a, int b) {
		int inda = (a < 0) ? (a + 256) : a;
		int indb = (b < 0) ? (b + 256) : b;
		
		if ( (a != 0) && (b != 0) ) {
			int index = (l_table[inda/16][inda%16] + l_table[indb/16][indb%16]);
			if(index > 0xFF) index -= 0xFF;
			int val = (e_table[index >> 4][index & 0x0F] );
			return val;
		}
		else 
			return 0;
	} 
	
	public static void mixColumn2 (int c) {
		//This is another alternate version of mixColumn, using the 
		//logtables to do the computation.
		
		int a[] = new int[4];
		
		//note that a is just a copy of st[.][c]
		for (int i = 0; i < 4; i++) 
			a[i] = state[i][c];
		
		//This is exactly the same as mixColumns1, if 
		//the mul columns somehow match the b columns there.
		state[0][c] = (mul(2,a[0]) ^ a[2] ^ a[3] ^ mul(3,a[1]));
		state[1][c] = (mul(2,a[1]) ^ a[3] ^ a[0] ^ mul(3,a[2]));
		state[2][c] = (mul(2,a[2]) ^ a[0] ^ a[1] ^ mul(3,a[3]));
		state[3][c] = (mul(2,a[3]) ^ a[1] ^ a[2] ^ mul(3,a[0]));
	} 
	
	public static void invMixColumn2 (int c) {
		int a[] = new int[4];
		
		//note that a is just a copy of st[.][c]
		for (int i = 0; i < 4; i++) 
			a[i] = state[i][c];
		
		state[0][c] = (mul(0xE,a[0]) ^ mul(0xB,a[1]) ^ mul(0xD, a[2]) ^ mul(0x9,a[3]));
		state[1][c] = (mul(0xE,a[1]) ^ mul(0xB,a[2]) ^ mul(0xD, a[3]) ^ mul(0x9,a[0]));
		state[2][c] = (mul(0xE,a[2]) ^ mul(0xB,a[3]) ^ mul(0xD, a[0]) ^ mul(0x9,a[1]));
		state[3][c] = (mul(0xE,a[3]) ^ mul(0xB,a[0]) ^ mul(0xD, a[1]) ^ mul(0x9,a[2]));
	} 
	
	
	private static int addRoundKey(int round)
	{
		int[] state_col = new int[4];
		int[] ek_col = new int[4];
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[i].length; j++){
				state_col[j] = state[j][i];
				ek_col[j] = expanded_key[j][round];
			}	
			round++;
			state_col = xor(state_col, ek_col);
			for(int j = 0; j < state[i].length; j++){
				state[j][i] = state_col[j];
			}
		}
		return round;
	}
	
	private static int invAddRoundKey(int round)
	{
		int[] state_col = new int[4];
		int[] ek_col = new int[4];
		for(int i = 3; i >= 0; i--){
			for(int j = 0; j < 4; j++){
				state_col[j] = state[j][i];
				ek_col[j] = expanded_key[j][round];
			}	
			round--;
			state_col = xor(state_col, ek_col);
			for(int j = 0; j < 4; j++){
				state[j][i] = state_col[j];
			}
		}
		return round;
	}
	
	private static void expand_key(){
		if(key_size == 128){
			expanded_key = new int[4][44];
			for(int i = 0; i <key.length; i++){
				for(int j = 0; j < key[i].length; j++){
					expanded_key[j][i] = key[j][i];		
				}		
			}

			for(int i = 4; i < 44; i++){
				if((i%4) == 0){
					int[] result = xor(xor(subword(rotword(ek(i-1))), archon(i)), ek(i-4));
					expanded_key[0][i] = result[0]; expanded_key[1][i] = result[1];
					expanded_key[2][i] = result[2]; expanded_key[3][i] = result[3];
				}else{
					int[] result = xor(ek(i-1), ek(i-4));
					expanded_key[0][i] = result[0]; expanded_key[1][i] = result[1];
					expanded_key[2][i] = result[2]; expanded_key[3][i] = result[3];
				}
			}
		}else if(key_size == 192){
			expanded_key = new int[4][(num_rounds + 1) *4];
			for(int i = 0; i <key[0].length; i++){
				for(int j = 0; j < key.length; j++){
					expanded_key[j][i] = key[j][i];		
				}		
			}

			for(int i = (key_size/32); i < ((num_rounds + 1) *4); i++){
				if((i%6) == 0){
					int[] result = xor(xor(subword(rotword(ek(i-1))), archon(i)), ek(i-6));
					expanded_key[0][i] = result[0]; expanded_key[1][i] = result[1];
					expanded_key[2][i] = result[2]; expanded_key[3][i] = result[3];
				}else{
					int[] result = xor(ek(i-1), ek(i-6));
					expanded_key[0][i] = result[0]; expanded_key[1][i] = result[1];
					expanded_key[2][i] = result[2]; expanded_key[3][i] = result[3];
				}
			}
		}else if(key_size == 256){
			expanded_key = new int[4][(num_rounds + 1) *4];
			for(int i = 0; i <key[0].length; i++){
				for(int j = 0; j < key.length; j++){
					expanded_key[j][i] = key[j][i];		
				}		
			}

			for(int i = (key_size/32); i < ((num_rounds + 1) *4); i++){
				if((i%8) == 0){
					int[] result = xor(xor(subword(rotword(ek(i-1))), archon(i)), ek(i-8));
					expanded_key[0][i] = result[0]; expanded_key[1][i] = result[1];
					expanded_key[2][i] = result[2]; expanded_key[3][i] = result[3];
				}else{
					int[] result = xor(ek(i-1), ek(i-8));
					expanded_key[0][i] = result[0]; expanded_key[1][i] = result[1];
					expanded_key[2][i] = result[2]; expanded_key[3][i] = result[3];
				}
			}
		}else{
			System.out.println("bad things"); System.exit(-1);
		}
	}
	
	
	
	private static int[] xor(int[] a, int[] b){
		int[] result = new int[a.length];
		for(int i = 0; i < result.length; i++){
			result[i] = a[i] ^ b[i];
		}
		return result;
	}
	
	private static int[] ek(int offset){
		 return new int [] {expanded_key[0][offset], expanded_key[1][offset], expanded_key[2][offset], expanded_key[3][offset]	};
	}
	
	private static int[] archon(int offset){
		int math = ((offset/4) - 1);
		 return new int [] {rcon[0][math], rcon[1][math], rcon[2][math], rcon[3][math]	};
	}
	
	private static int[] rotword (int[] offset){
		int temp_0 = offset [0];
		int temp_1 = offset [1];
		int temp_2 = offset [2];
		int temp_3 = offset [3];
		
		return new int[] {temp_1, temp_2, temp_3, temp_0}; 
	}
	
	private static int[] subword (int[] offset){
		int[] result = new int[4];
		for (int i = 0; i < 4; i++){
			int top = (offset[i] >> 4);
			int bottom = (offset[i] & 0x0F);
			result[i] = x_box[top][bottom];
		}
		return result;
	}
	
	private static void make_key(File f) throws FileNotFoundException {
		Scanner the_key = new Scanner(f);
		if(!the_key.hasNextLine()){
			System.out.println("No key present");	System.exit(-1);
		}else{
			String k = the_key.nextLine();
			if(key_size == 128){	key = new int [4][4]; num_rounds = 10;	}
			else if(key_size == 192){	key = new int [4][6]; num_rounds = 12; }
			else if(key_size == 256){	key = new int [4][8]; num_rounds = 14; }
			else{ System.out.println("Incorrect key size"); System.exit(-1);}
			int subindex = 0;
			ArrayList<Integer> al = new ArrayList<Integer>();
			for (int i = 0; i < (key_size / 8); i++){
				al.add((Integer.decode("0x" +  k.substring(subindex, subindex+2))));
				subindex+=2;
			}
			int count = 0;
			for(int i = 0; i < key[0].length; i++){
				for(int j = 0; j < key.length; j++){
					key[j][i] = al.get(count++);	}
			}
		}
		the_key.close();
	}
	
	
	static void make_a_state(ByteArrayInputStream is, Scanner sc){	
		state = new int[4][4];
		ArrayList<Integer> al = new ArrayList<Integer>();
				
		if(!encrypt){	
			String test_state = sc.nextLine();
			int subindex = 0;
			for (int i = 0; i < 16; i++){
				al.add((Integer.decode("0x" +  test_state.substring(subindex, subindex+2))));
				subindex+=2;
			}
		}else{
			if(is.available() >= 16){
				for(int i = 0; i < 16; i++){
					al.add(is.read());
				}	
				
			}else{
				while(is.available() > 0)
					al.add(is.read());
				while(al.size() < 16){
					al.add(0);
				}
			}
		}

		Iterator<Integer> it = al.iterator();
		for(int i = 0; i < state.length; i++){
			for(int j = 0; j < state[i].length; j++){
				state[j][i] = it.next();
				num_bytes++;
			}
		}
	}
	
	static void print_array(int[][] a, boolean hex){
		for(int[] i: a){
			for(int j: i){
				if(hex)
					System.out.print(Integer.toHexString(j) + " ");
				else
					System.out.print(j + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	static void print_array_1d (int[] a, boolean hex){
		for (int i: a){
			if (hex)
				System.out.print(Integer.toHexString(i) + " ");
			else
				System.out.print(i + " ");
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
	
	
	static void fill_inv_xbox ()
	{
		inv_x_box = new int [][]
		{
				{0x52, 0x09, 0x6A, 0xD5, 0x30, 0x36, 0xA5, 0x38, 0xBF, 0x40, 0xA3, 0x9E, 0x81, 0xF3, 0xD7, 0xFB,},
				{0x7C, 0xE3, 0x39, 0x82, 0x9B, 0x2F, 0xFF, 0x87, 0x34, 0x8E, 0x43, 0x44, 0xC4, 0xDE, 0xE9, 0xCB,},
				{0x54, 0x7B, 0x94, 0x32, 0xA6, 0xC2, 0x23, 0x3D, 0xEE, 0x4C, 0x95, 0x0B, 0x42, 0xFA, 0xC3, 0x4E,},
				{0x08, 0x2E, 0xA1, 0x66, 0x28, 0xD9, 0x24, 0xB2, 0x76, 0x5B, 0xA2, 0x49, 0x6D, 0x8B, 0xD1, 0x25,},
				{0x72, 0xF8, 0xF6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xD4, 0xA4, 0x5C, 0xCC, 0x5D, 0x65, 0xB6, 0x92,},
				{0x6C, 0x70, 0x48, 0x50, 0xFD, 0xED, 0xB9, 0xDA, 0x5E, 0x15, 0x46, 0x57, 0xA7, 0x8D, 0x9D, 0x84,},
				{0x90, 0xD8, 0xAB, 0x00, 0x8C, 0xBC, 0xD3, 0x0A, 0xF7, 0xE4, 0x58, 0x05, 0xB8, 0xB3, 0x45, 0x06,},
				{0xD0, 0x2C, 0x1E, 0x8F, 0xCA, 0x3F, 0x0F, 0x02, 0xC1, 0xAF, 0xBD, 0x03, 0x01, 0x13, 0x8A, 0x6B,},
				{0x3A, 0x91, 0x11, 0x41, 0x4F, 0x67, 0xDC, 0xEA, 0x97, 0xF2, 0xCF, 0xCE, 0xF0, 0xB4, 0xE6, 0x73,},
				{0x96, 0xAC, 0x74, 0x22, 0xE7, 0xAD, 0x35, 0x85, 0xE2, 0xF9, 0x37, 0xE8, 0x1C, 0x75, 0xDF, 0x6E,},
				{0x47, 0xF1, 0x1A, 0x71, 0x1D, 0x29, 0xC5, 0x89, 0x6F, 0xB7, 0x62, 0x0E, 0xAA, 0x18, 0xBE, 0x1B,},
				{0xFC, 0x56, 0x3E, 0x4B, 0xC6, 0xD2, 0x79, 0x20, 0x9A, 0xDB, 0xC0, 0xFE, 0x78, 0xCD, 0x5A, 0xF4,},
				{0x1F, 0xDD, 0xA8, 0x33, 0x88, 0x07, 0xC7, 0x31, 0xB1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xEC, 0x5F,},
				{0x60, 0x51, 0x7F, 0xA9, 0x19, 0xB5, 0x4A, 0x0D, 0x2D, 0xE5, 0x7A, 0x9F, 0x93, 0xC9, 0x9C, 0xEF,},
				{0xA0, 0xE0, 0x3B, 0x4D, 0xAE, 0x2A, 0xF5, 0xB0, 0xC8, 0xEB, 0xBB, 0x3C, 0x83, 0x53, 0x99, 0x61,},
				{0x17, 0x2B, 0x04, 0x7E, 0xBA, 0x77, 0xD6, 0x26, 0xE1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0C, 0x7D,}
			};
	}
	
	static void fill_rcon ()
	{
		rcon = new int [][]
		{
			   {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36, 0x6c, 0xd8, 0xab, 0x4d, 0x98},
			   {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
			   {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
			   {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}
			};
	}
	
	static void fill_e(){
		e_table = new int [][]
		{
				{0x01, 0x03, 0x05, 0x0F, 0x11, 0x33, 0x55, 0xFF, 0x1A, 0x2E, 0x72, 0x96, 0xA1, 0xF8, 0x13, 0x35,},
				{0x5F, 0xE1, 0x38, 0x48, 0xD8, 0x73, 0x95, 0xA4, 0xF7, 0x02, 0x06, 0x0A, 0x1E, 0x22, 0x66, 0xAA,},
				{0xE5, 0x34, 0x5C, 0xE4, 0x37, 0x59, 0xEB, 0x26, 0x6A, 0xBE, 0xD9, 0x70, 0x90, 0xAB, 0xE6, 0x31,},
				{0x53, 0xF5, 0x04, 0x0C, 0x14, 0x3C, 0x44, 0xCC, 0x4F, 0xD1, 0x68, 0xB8, 0xD3, 0x6E, 0xB2, 0xCD,},
				{0x4C, 0xD4, 0x67, 0xA9, 0xE0, 0x3B, 0x4D, 0xD7, 0x62, 0xA6, 0xF1, 0x08, 0x18, 0x28, 0x78, 0x88,},
				{0x83, 0x9E, 0xB9, 0xD0, 0x6B, 0xBD, 0xDC, 0x7F, 0x81, 0x98, 0xB3, 0xCE, 0x49, 0xDB, 0x76, 0x9A,},
				{0xB5, 0xC4, 0x57, 0xF9, 0x10, 0x30, 0x50, 0xF0, 0x0B, 0x1D, 0x27, 0x69, 0xBB, 0xD6, 0x61, 0xA3,},
				{0xFE, 0x19, 0x2B, 0x7D, 0x87, 0x92, 0xAD, 0xEC, 0x2F, 0x71, 0x93, 0xAE, 0xE9, 0x20, 0x60, 0xA0,},
				{0xFB, 0x16, 0x3A, 0x4E, 0xD2, 0x6D, 0xB7, 0xC2, 0x5D, 0xE7, 0x32, 0x56, 0xFA, 0x15, 0x3F, 0x41,},
				{0xC3, 0x5E, 0xE2, 0x3D, 0x47, 0xC9, 0x40, 0xC0, 0x5B, 0xED, 0x2C, 0x74, 0x9C, 0xBF, 0xDA, 0x75,},
				{0x9F, 0xBA, 0xD5, 0x64, 0xAC, 0xEF, 0x2A, 0x7E, 0x82, 0x9D, 0xBC, 0xDF, 0x7A, 0x8E, 0x89, 0x80,},
				{0x9B, 0xB6, 0xC1, 0x58, 0xE8, 0x23, 0x65, 0xAF, 0xEA, 0x25, 0x6F, 0xB1, 0xC8, 0x43, 0xC5, 0x54,},
				{0xFC, 0x1F, 0x21, 0x63, 0xA5, 0xF4, 0x07, 0x09, 0x1B, 0x2D, 0x77, 0x99, 0xB0, 0xCB, 0x46, 0xCA,},
				{0x45, 0xCF, 0x4A, 0xDE, 0x79, 0x8B, 0x86, 0x91, 0xA8, 0xE3, 0x3E, 0x42, 0xC6, 0x51, 0xF3, 0x0E,},
				{0x12, 0x36, 0x5A, 0xEE, 0x29, 0x7B, 0x8D, 0x8C, 0x8F, 0x8A, 0x85, 0x94, 0xA7, 0xF2, 0x0D, 0x17,},
				{0x39, 0x4B, 0xDD, 0x7C, 0x84, 0x97, 0xA2, 0xFD, 0x1C, 0x24, 0x6C, 0xB4, 0xC7, 0x52, 0xF6, 0x01,}
		};
	}

	static void fill_l(){
		l_table = new int [][]
		{
				{0x00, 0x00, 0x19, 0x01, 0x32, 0x02, 0x1A, 0xC6, 0x4B, 0xC7, 0x1B, 0x68, 0x33, 0xEE, 0xDF, 0x03,},
				{0x64, 0x04, 0xE0, 0x0E, 0x34, 0x8D, 0x81, 0xEF, 0x4C, 0x71, 0x08, 0xC8, 0xF8, 0x69, 0x1C, 0xC1,},
				{0x7D, 0xC2, 0x1D, 0xB5, 0xF9, 0xB9, 0x27, 0x6A, 0x4D, 0xE4, 0xA6, 0x72, 0x9A, 0xC9, 0x09, 0x78,},
				{0x65, 0x2F, 0x8A, 0x05, 0x21, 0x0F, 0xE1, 0x24, 0x12, 0xF0, 0x82, 0x45, 0x35, 0x93, 0xDA, 0x8E,},
				{0x96, 0x8F, 0xDB, 0xBD, 0x36, 0xD0, 0xCE, 0x94, 0x13, 0x5C, 0xD2, 0xF1, 0x40, 0x46, 0x83, 0x38,},
				{0x66, 0xDD, 0xFD, 0x30, 0xBF, 0x06, 0x8B, 0x62, 0xB3, 0x25, 0xE2, 0x98, 0x22, 0x88, 0x91, 0x10,},
				{0x7E, 0x6E, 0x48, 0xC3, 0xA3, 0xB6, 0x1E, 0x42, 0x3A, 0x6B, 0x28, 0x54, 0xFA, 0x85, 0x3D, 0xBA,},
				{0x2B, 0x79, 0x0A, 0x15, 0x9B, 0x9F, 0x5E, 0xCA, 0x4E, 0xD4, 0xAC, 0xE5, 0xF3, 0x73, 0xA7, 0x57,},
				{0xAF, 0x58, 0xA8, 0x50, 0xF4, 0xEA, 0xD6, 0x74, 0x4F, 0xAE, 0xE9, 0xD5, 0xE7, 0xE6, 0xAD, 0xE8,},
				{0x2C, 0xD7, 0x75, 0x7A, 0xEB, 0x16, 0x0B, 0xF5, 0x59, 0xCB, 0x5F, 0xB0, 0x9C, 0xA9, 0x51, 0xA0,},
				{0x7F, 0x0C, 0xF6, 0x6F, 0x17, 0xC4, 0x49, 0xEC, 0xD8, 0x43, 0x1F, 0x2D, 0xA4, 0x76, 0x7B, 0xB7,},
				{0xCC, 0xBB, 0x3E, 0x5A, 0xFB, 0x60, 0xB1, 0x86, 0x3B, 0x52, 0xA1, 0x6C, 0xAA, 0x55, 0x29, 0x9D,},
				{0x97, 0xB2, 0x87, 0x90, 0x61, 0xBE, 0xDC, 0xFC, 0xBC, 0x95, 0xCF, 0xCD, 0x37, 0x3F, 0x5B, 0xD1,},
				{0x53, 0x39, 0x84, 0x3C, 0x41, 0xA2, 0x6D, 0x47, 0x14, 0x2A, 0x9E, 0x5D, 0x56, 0xF2, 0xD3, 0xAB,},
				{0x44, 0x11, 0x92, 0xD9, 0x23, 0x20, 0x2E, 0x89, 0xB4, 0x7C, 0xB8, 0x26, 0x77, 0x99, 0xE3, 0xA5,},
				{0x67, 0x4A, 0xED, 0xDE, 0xC5, 0x31, 0xFE, 0x18, 0x0D, 0x63, 0x8C, 0x80, 0xC0, 0xF7, 0x70, 0x07,}  
		};		
	}
}

class Stopwatch 
{	
	private long startTime;
	private long stopTime;

	public static final double NANOS_PER_SEC = 1000000000.0;

	/**
	 start the stop watch.
	 */
	public void start()
	{	
		System.gc();
		startTime = System.nanoTime();
	}

	/**
	 stop the stop watch.
	 */
	public void stop()
	{	
		stopTime = System.nanoTime();	
	}

	/**
	elapsed time in seconds.
	@return the time recorded on the stopwatch in seconds
	 */
	public double time()
	{	
		return (stopTime - startTime) / NANOS_PER_SEC;	
	}

	public String toString()
	{   
		return "elapsed time: " + time() + " seconds.";
	}

	/**
	elapsed time in nanoseconds.
	@return the time recorded on the stopwatch in nanoseconds
	 */
	public long timeInNanoseconds()
	{	
		return (stopTime - startTime);	
	}
}