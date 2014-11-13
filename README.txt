UTEID: jmt2939; dbd453;
FIRSTNAME: Jeremy; Daniel;
LASTNAME: Thompson; Durbin;
CSACCOUNT: jmt2939; majisto;
EMAIL: jthompson2214@gmail.com; majisto@gmail.com;

[Program 4]
[Description]
This program takes an input file of hex characters (adding zeros or taking away extra characters to equal 32 chars) and encodes all of them and writes this to an output file. 
It does this by reading in 32 hex chars at a time and turning pairs of these into int values. a 4x4 array of int values to be precise. then this block is run through a process 
of methods each of them altering it, subBytes, shiftRows, mixColumns, and addRoundKey. the addRoundKey step multiplies each column of the state matrix by a predetermined value 
from the expanded key. The expanded key is created based on the inputed key value and then using a process takes those columns and expands upon them until it reaches enough values 
for the 4 columns of the state times the number of rounds that are going to be ran (for 128bit key this is 11 rounds and 44 expanded key columns). after getting this value you can 
run it the first time and then start the rounds going in the order that I listed them above for 10 rounds. after this you do all of the steps except for mixColumns (this is the 11th 
round) and then the block is printed out to the encrypted file. This is repeated until the whole input file is encrypted. 

for decode it is literally all of this in reverse, but with a couple changes in regard to the processes of the 4 main functions. 


[Finish]
we finished both encrypt and decrypt. We did end up having to use Young's mixColumns code though (we're not sure why ours wasn't quite working)
we have the program setup so that it will not only create the respective file (encrypted or decrypted) but the bandwidth is also being printed 
to stdout. There was nothing in the instructions specifying if we should or shouldn't print anything so we left that part in as a nice touch. 
Also the way the program is supposed to work is to pad or truncate the malformed or short lines, ours does that but it also decrypts the file 
into the corrected form. we weren't sure what the instructions were talking about with this output, but this is the way ours works. The we did 
the program there is no way to remove the added zeros or add in the truncated hex characters from the original file, so if the test case has 
either of these situations then a diff will not work on the program. (that would be silly to expect though, so no loss).

Test cases 1:
Encrypt: 0.028537209906339654 MB/sec
Decrypt: 0.009723910203141341 MB/sec
Test Case 2:
Encrypt: 0.013607080044248814 MB/sec
Decrypt: 0.008596384647475188 MB/sec 
Test case 3:
Encrypt: 0.08931309994232485 MB/sec
Decrypt: 0.08436755048452278 MB/sec
 

[Test Cases]
[test1]
00000000000000000000000000000000

[key1]
00000000000000000000000000000000

[encrypted test1]
66e94bd4ef8a2c3b884cfa59ca342b2e

[decrypted test1]
00000000000000000000000000000000

[test2]
00000000000000000000000000000001

[key2]
00000000000000000000000000000000

[encrypted test2]
58e2fccefa7e3061367f1d57a4e7455a

[decrypted test2]
00000000000000000000000000000001

[test3]
(these are all files included in the zip)
test3

[key3]
FFEEDDCCBBAA00998877665544332211

[encrypted test3]
test3.enc

[decrypted test3]
test3.enc.dec