CR-16 Assembler
===============
The CR-16 Assembler project contains utilities designed to translate CR-16 assembly instructions into
their equivalent machine language instructions.

Usage
-----
###CLI
The command-line interface (CLI) provides a simple and interactive command-line utility for our assembler. Here are some examples of using the CLI:
```
$ assembler -help // Display usage as well as a list of options and their arguments
```

To generate the machine instructions from a given assembly file, you can use the following command:
```
$ assemble -inputfile 'inputfileName' -outputfile 'outputFilename'
```
This will output a .dat file called 'outputFilename' that contains the machine language instructions in hex format. By default, the output file will be created in the directory which the command was run.

TODO
----
	1. Implement assembly language decoder
	2. Create additional functionality to write out to a .dat file for easy conversion from assembly to machine language instructions.
	  
	