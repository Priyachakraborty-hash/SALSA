Salsa20/10 Stream Cipher 
 Overview

This project is a Java implementation of the Salsa20/10 stream cipher,it demonstrates my understanding of cryptographic algorithms, bitwise operations, and secure system design.

The cipher supports 64-bit (non-standard), 128-bit, and 256-bit keys — following Daniel J. Bernstein’s Salsa20 specification, with modifications for the non-standard key length as described in the assignment brief.

 Features

Multi-key support: 64, 128, and 256-bit keys

⚙️ Core algorithmic components: quarterround, rowround, columnround, and doubleround functions

🧮 Nonce + Counter expansion: 8-byte nonce + 8-byte block counter (little-endian)

🔄 Symmetric encryption/decryption: same function via XOR with keystream

🧱 Fully modular design: easy to extend or adapt for Salsa20/20 and ChaCha variants

🧯 Robust error handling: detects invalid key sizes, nonce length, bad hex input, and >1 KB input limit

💬 Command-line interface: clean hex-encoded I/O for both encryption and decryption

🧠 What I Learned

Implementing Salsa20/10 from scratch helped me strengthen my skills in:

Bitwise operations and low-level data manipulation in Java

Understanding stream cipher design (keystream generation & XOR operation)

Working with endian conversions, integer rotation, and state expansion

Writing self-contained, testable cryptographic code with clear error messages

Using Makefiles and WSL/Linux environments for reproducible builds




