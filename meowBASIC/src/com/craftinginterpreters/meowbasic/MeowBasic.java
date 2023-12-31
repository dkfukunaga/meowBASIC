package com.craftinginterpreters.meowbasic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MeowBasic {
	
	public static boolean hadError = false;

	public static void main(String[] args) {
		if (args.length > 1) {
			System.out.println("Usage: meow [script]");
			System.exit(64);
		} else if (args.length == 1) {
			try {
				runFile(args[0]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				runPrompt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		
		// indicate an error in the exit code
		if (hadError) System.exit(65);
	}
	
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for(;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) break;
			if (line.equalsIgnoreCase(":quit") || line.equalsIgnoreCase(":q")) System.exit(0);
			run(line);
			hadError = false;
		}
	}
	
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		Expr expression = parser.parse();
		
		// Stop if there was a syntax error
		if (hadError) return;
		
		System.out.println(new AstPrinter().print(expression));
		
		// For now, just print the tokens.
//		for (Token token : tokens) {
//			System.out.println(token);
//		}
	}
	
	static void error(int line, String message) {
		report(line, "", message);
	}
	
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}
	
	static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}

}
