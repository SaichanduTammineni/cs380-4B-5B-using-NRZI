import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;

//translating code using 4B/5B with NRZI

// Run the client second, it will connect to a running server
// based on the host and port specified
public final class PhysLayerClient {

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("codebank.xyz", 38002)) {
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            
            System.out.println("Connected to Server");
            //array holds numbers that establishes the baseline
            int[] preamble = new int[64];
            for(int i = 0; i<64;i++){
              preamble[i] = is.read();
            }
            //avg holds baseline
            double avg = 0.0;
            for(int i=0;i<64;i++){
              avg+=preamble[i];
            }
            avg = avg/64;
            System.out.printf("Baseline establisthed from preamble: %.2f\n",avg);
            
            /*
			@prevNum = keeps track of the number held before to compare it with current
			@data = holds 1 or 0, 5B encoded
            */
            int prevNum = 0;
            int[] data = new int[320];
            for(int i = 0;i<320;i++){
            //variables were made before quite understanding the problem, being given 5B code 		
              int fourB = is.read();
              if((fourB>avg && prevNum>avg) ||(fourB<avg && prevNum<avg)){
                //gets 0 if prev and current numbers stay above or below avg/doesnt change
                data[i] =0;
                //current number then set to prev and loop keeps going
                prevNum=fourB; 
              }else{
                data[i]=1;
                prevNum=fourB;
              }
            }
            /*
            @half = keeps track if finding top or bottom part of byte, used in if statement below switch case
            @fiveB = holds 4B translated code that is gets concatenated with another 4bits and put into a byte
            @holder = temporarliy holds the top part of the pair of the byte
            @trans = all translated 4B code
            @counter = index for trans because the for loop numbering doesn't increment by 1 
         	@b = concatenates 5 numbers(bc it was sent over in 5B encoding) from data array
            */
            boolean half = true;
            int fiveB = 0;
            int holder = 0;
            byte[] trans = new byte[32];
            int counter = 0;
            for(int i= 0;i<320;i+=5){
              String b = "";
              b+=data[i];
              b+=data[i+1];
              b+=data[i+2];
              b+=data[i+3];
              b+=data[i+4];
              switch(b){
                case "11110": fiveB = 0;
                  break;
                case "01001": fiveB = 1;
                  break;
                case "10100": fiveB = 2;
                  break;
                case "10101": fiveB = 3;
                  break;
                case "01010": fiveB = 4;
                  break;
                case "01011": fiveB = 5;
                  break;
                case "01110": fiveB = 6;
                  break;
                case "01111": fiveB = 7;
                  break;
                case "10010": fiveB = 8;
                  break;
                case "10011": fiveB = 9;
                  break;
                case "10110": fiveB = 10;
                  break;
                case "10111": fiveB = 11;
                  break;
                case "11010": fiveB = 12;
                  break;
                case "11011": fiveB = 13;
                  break;
                case "11100": fiveB = 14;
                  break;
                case "11101": fiveB = 15;
                  break;
              }

              if(half){
                holder = fiveB;
                //moving over bits so theres room for the next 4 bits to come thru(and & just in case to clear things)
                holder = (holder<<4) & 0xF0; 
                half = false;
              }else{
                int holder2 = fiveB;
                holder2 = holder2 & 0x0F;
                int concat = (holder|holder2);
                trans[counter] = (byte)(concat);
                counter++;
                half = true;
              }
            }
            
            System.out.print("Received 32 bytes: ");
            //self note, %X prints hex in upper case and %x prints hex in lower case
            for(int i = 0;i<32;i++){
              int high = (trans[i] & 0xF0) >> 4;
              int low = trans[i] & 0xF;
              System.out.format("%X", high);
              System.out.format("%X", low);
            }
           
            os.write(trans);

            if(is.read() == 1){
          		System.out.println("\nResponse good.");
        	}else{
          		System.out.println("\nResponse bad.");
        	}

        	System.out.println("Disconnected from server");
            socket.close();
            System.exit(0);
        }
    }
}

