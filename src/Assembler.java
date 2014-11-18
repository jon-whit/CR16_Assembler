import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Assembler 
{
    // Private member variables
	private File inputFile;
	
	/**
	 * Main entry point for our Assembler. This drives our CLI.
	 * 
	 * @param args - Possible arguments are outlined below.
	 */
	public static void main(String[] args) 
        {
		
		// Create both optional and required command line arguments
		Option help = new Option("help", "print this message");
		OptionBuilder.withArgName("filepath");
		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withDescription("use given assembly file as input");
		Option inputfile = OptionBuilder.create("inputfile");
		OptionBuilder.withArgName("filepath");
		OptionBuilder.hasArg();
		OptionBuilder.isRequired();
		OptionBuilder.withDescription("path to write the machine language file to");
		Option outputfile = OptionBuilder.create("outputfile");
		
		// Add those options that were created above
		Options options = new Options();
		options.addOption(help);
		options.addOption(inputfile);
		options.addOption(outputfile);
		
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			
			// Create a new CommandLine instance with the given options above,
			// and parse the input arguments to this application
			cmd = parser.parse(options, args);
			if(cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				String header = "Converts the given assembly instructions to their"
						+ " machine language equivalent instructions.";
				String footer = "";
				formatter.printHelp("java Assembler", header, options, footer, true);
				return;
			}
			
			// Create a new Assembler instance to assemble the given
			// input file.
			Assembler a = new Assembler(cmd.getOptionValue("inputfile"));
			a.generateBinary(new File(cmd.getOptionValue("outputfile")));
			
			System.out.println("Assembly complete. Please see the output file in your CWD.");
		} catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
		}
	}

	
	/*
	 * Creates an immutable lookup-table for each of the instructions
	 * in our ISA.
	 */
	private final static HashMap<String, String> op;
        private final static HashMap<String, String> cond;
        private final static HashMap<String, String> func;
        private final static String RTYPE_OP = "0000";
      
	static 
        {
            HashMap<String, String> map = new HashMap<String, String>();
            
            map.put("add",   RTYPE_OP);
            map.put("addi",  "0101");
            map.put("addu",   RTYPE_OP);
            map.put("mul",  RTYPE_OP);
            map.put("sub",   RTYPE_OP);
            map.put("subi",  "1001");
            map.put("cmp",   RTYPE_OP);
            map.put("cmpi",  "1011");
            map.put("and",   RTYPE_OP);
            map.put("andi",  "0001");
            map.put("or",    RTYPE_OP);
            map.put("ori",   "0010");
            map.put("xor",   RTYPE_OP);
            map.put("xori",  "0011");
            map.put("mov",   RTYPE_OP);
            map.put("movi",  "1101");
            map.put("lsh",   "1000");
            map.put("lshi",  "1000");
            map.put("ashu",  "1000");
            map.put("ashui", "1000");
            map.put("lui",   "1111");
            map.put("load",  "0100");
            map.put("stor",  "0100");
            map.put("scond", "0100");
            map.put("bcond", "1100");
            map.put("jcond", "0100");
            map.put("jal",   "0100");
            op = map;
            
            HashMap<String, String> condMap = new HashMap<String, String>();
            condMap.put("eq", "0000");
            condMap.put("ne", "0001");
            condMap.put("ge", "1101");
            condMap.put("cs", "0010");
            condMap.put("cc", "0011");
            condMap.put("hi", "0100");
            condMap.put("ls", "0101");
            condMap.put("lo", "1010");
            condMap.put("hs", "1011");
            condMap.put("gt", "0110");
            condMap.put("le", "0111");
            condMap.put("fs", "1000");
            condMap.put("fc", "1001");
            condMap.put("lt", "1100");
            condMap.put("uc", "1110");
            cond = condMap;
            
            HashMap<String, String> funcMap = new HashMap<String, String>();
            
            funcMap.put("add",   "0101");
            funcMap.put("addu",  "0110");
            funcMap.put("mul",   "1110");
            funcMap.put("sub",   "1001");
            funcMap.put("cmp",   "1011");
            funcMap.put("and",   "0001");
            funcMap.put("or",    "0010");
            funcMap.put("xor",   "0011");
            funcMap.put("mov",   "1101");
            funcMap.put("lsh",   "0100");
            funcMap.put("ashu",  "0110");
            funcMap.put("load",  "0000");
            funcMap.put("stor",  "0100");
            funcMap.put("scond", "1101");
            funcMap.put("jcond", "1100");
            funcMap.put("jal",   "1000");
            func = funcMap;
	}
        
	public Assembler(String fileName) 
        {
            this.inputFile = new File(fileName);
	}
        
   /**
	 * Generates a file with binary content representative of the assembly instructions
	 * provided in the input file.
	 * 
	 * @param file - The output file for which the binary instructions will be written to.
	 */
	public void generateBinary(File file) 
        {
            try
            {
                HashMap<String, Integer> labels = genLabelMap(this.inputFile);
                Scanner s = new Scanner(this.inputFile);
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter w = new BufferedWriter(fw);
                Pattern p = Pattern.compile("^[a-z]+[_*\\d*a-z*]*:");

                int lineNum = 0;
                
                while(s.hasNext()) 
                {
                    String line = s.nextLine().trim().toLowerCase();
                    //System.out.println(line);
                    Matcher m = p.matcher(line);
                    
                    // If the current line isn't a label, then parse the instruction.
                    if(!m.matches()) 
                    {    
                    	StringBuilder instruction = new StringBuilder();
                        
                    	// Split the instruction up based on white space. For example,
                    	// add R1 R2 will be split into three strings [add, R1, R2]
                    	String[] operands = line.split("\\s+");
                        
   
						if (operands[0].startsWith("s")
								&& !operands[0].equals("stor")
								&& !(operands[0].equals("sub") || operands[0]
										.equals("subi"))) {
							instruction.append(op.get("scond"));
							instruction.append(toBinary(operands[1], 4));
							instruction.append(func.get("scond"));
							instruction.append(cond.get(operands[0].substring(1)));
						} else if (operands[0].startsWith("j") && !operands[0].equals("jal")) {
							instruction.append(op.get("jcond"));
							instruction.append(cond.get(operands[0].substring(1)));
							instruction.append(func.get("jcond"));
							instruction.append(toBinary(operands[1], 4));
						} else if (operands[0].startsWith("b")) {
							instruction.append(op.get("bcond"));
							instruction.append(cond.get(operands[0].substring(1)));
							instruction.append(toBinary(labels.get(operands[1]), 8));
						} else if (operands[0].equals("stor")
								|| operands[0].equals("load")
								|| operands[0].equals("jal")) {
							instruction.append(op.get(operands[0]));
							instruction.append(toBinary(operands[1], 4));
							instruction.append(func.get(operands[0]));
							instruction.append(toBinary(operands[2], 4));
						} else {
							instruction.append(op.get(operands[0]));
							instruction.append(toBinary(operands[2], 4));
	
							if (func.containsKey(operands[0])) {
								instruction.append(func.get(operands[0]));
								instruction.append(toBinary(operands[1], 4));
							} else if (operands[0].equals("lshi")
									|| operands[0].equals("ashui")) {
								instruction
										.append(operands[0].equals("lshi") ? "000"
												: "001");
								int imm = Integer.parseInt(operands[1]);
								instruction.append(imm < 0 ? "1" : "0");
								instruction.append(imm < 0 ? toBinary(-imm, 4)
										: toBinary(imm, 4));
							} else {
								instruction.append(toBinary(operands[1], 8));
							}
						}

                        lineNum++;
                        instruction.append('\n');
                        w.write(instruction.toString());
                    }
                }
                
                s.close();
                w.close();
            }
            catch(IOException e)
            {
                System.err.println(e.getMessage());
            }
	}
	
	/**
	 * Generates a HashMap of labels and the line number which that label occurred. The
	 * labels are keys, and the line numbers are the values.
	 * @param f - The input file to parse
	 * @return
	 */
	private HashMap<String, Integer> genLabelMap(File f) 
        {
            HashMap<String, Integer> labels = new HashMap<String, Integer>();
            try
            {
                Scanner s = new Scanner(f);
                String pattern = "^[a-zA-Z]+[_*\\d*a-zA-Z*]*:";
                int lineNum = 0;

                while(s.hasNext()) 
                {
                    String line = s.nextLine().trim();
                    Pattern p = Pattern.compile(pattern);
                    Matcher m = p.matcher(line);

                    if(m.matches()) 
                        labels.put(line.substring(0, line.length() - 1), lineNum);
                    else
                        lineNum++;
                }

                s.close();
            }
            catch(FileNotFoundException e)
            {
                System.err.println(e.getMessage());
            }
            
            return labels;
	}
        
        private static String toBinary(String decimal, int size)
        {
            return toBinary(Integer.parseInt(decimal), size);
        }
        
        private static String toBinary(int decimal, int size)
        {
            String binary = Integer.toBinaryString(decimal);
            
            while(binary.length() < size)
                binary = "0" + binary;
            
            binary = binary.substring(binary.length() - size, binary.length());
            
            return binary;
        }
}

