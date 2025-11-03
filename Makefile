# Compiler and runtime
JAVAC=javac
JAVA=java
MAIN=Salsa20

# Default: compile the program
all:
	$(JAVAC) $(MAIN).java

# Run with 128-bit 
run128: all
	$(JAVA) $(MAIN) 128 "deadbeefdeadbeefdeadbeefdeadbeef" "1234567890abcdef" "546869736973706c61696e74657874"

# Run with 64-bit 
run64: all
	$(JAVA) $(MAIN) 64 "feedfacefacefeed" "0011223344556677" "546869736973706c61696e74657874"

# Run with 256-bit 
run256: all
	$(JAVA) $(MAIN) 256 "cafebabebaadf11ddeadbeeffeedfaceecafdeeffeebdaedd11fdaabebabefac" "5f3a91d27b8c6e45" "546869736973706c61696e74657874"


clean:
	rm -f *.class
