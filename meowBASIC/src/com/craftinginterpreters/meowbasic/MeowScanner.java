package com.craftinginterpreters.meowbasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.meowbasic.TokenType.*;

public class MeowScanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	
	private int start = 0;
	private int current = 0;
	private int line = 1;
	
	private static final Map<String, TokenType> keywords;
	
	static {
		keywords = new HashMap<>();
		keywords.put("bool",	BOOLEAN);
		keywords.put("case", 	CASE);
		keywords.put("char",	CHARACTER);
		keywords.put("default", DEFAULT);
		keywords.put("do", 		DO);
		keywords.put("double",	DOUBLE);
		keywords.put("each", 	EACH);
		keywords.put("else", 	ELSE);
		keywords.put("end", 	END);
		keywords.put("false", 	FALSE);
		keywords.put("for", 	FOR);
		keywords.put("fun", 	FUN);
		keywords.put("if",		IF);
		keywords.put("in", 		IN);
		keywords.put("int",		INTEGER);
		keywords.put("long",	LONG_INTEGER);
		keywords.put("loop", 	LOOP);
		keywords.put("next", 	NEXT);
		keywords.put("null", 	NULL);
		keywords.put("print", 	PRINT);
		keywords.put("rem", 	REM);
		keywords.put("return", 	RETURN);
		keywords.put("select", 	SELECT);
		keywords.put("short",	SHORT_INTEGER);
		keywords.put("single",	SINGLE_FLOAT);
		keywords.put("string",	STRING);
		keywords.put("sub", 	SUB);
		keywords.put("to", 		TO);
		keywords.put("true", 	TRUE);
		keywords.put("until", 	UNTIL);
		keywords.put("var", 	VAR);
		keywords.put("while", 	WHILE);
	}
	
	
	MeowScanner(String source) {
		this.source = source;
	}
	
	List<Token> scanTokens() {
		while (!isAtEnd()) {
			// we are at the beginning of the next lexeme
			start = current;
			scanToken();
		}
		
		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}
	
	private void scanToken() {
		char c = advance();
		
		switch(c) {
			case '(':
				addToken(LEFT_PAREN);
				break;
			case ')':
				addToken(RIGHT_PAREN);
				break;
			case ',':
				addToken(COMMA);
				break;
			case '.':
				addToken(DOT);
				break;
			case ':':
				addToken(COLON);
				break;
			case '*':
				addToken(STAR);
				break;
			case '\\':
				addToken(BACK_SLASH);
				break;
			case '%':
				addToken(PERCENT);
				break;
			case '^':
				addToken(CARET);
				break;

			case '&':
				if (peek() == '&') {
					advance();
					addToken(AND_AND);
				}
				break;
			case '|':
				if (peek() == '|') {
					advance();
					addToken(AND_AND);
				}
				break;
				
			case ';':
				while (peek() != '\n' && !isAtEnd()) advance();
				break;
				
			case '/':
				addToken(match('/') ? SLASH_SLASH : SLASH);
				break;
			case '=':
				addToken(match('=') ? EQUAL_EQUAL : EQUAL);
				break;
			case '>':
				addToken(match('=') ? GREATER_EQUAL : EQUAL);
				break;
			case '<':
				addToken(match('=') ? LESS_EQUAL : LESS);
				break;
			case '+':
				addToken(match('+') ? PLUS_PLUS : PLUS);
				break;
			case '-':
				addToken(match('-') ? MINUS_MINUS : MINUS);
				break;
				
			case ' ':
			case '\r':
			case '\t':
				// ignore whitespace
				break;
			
			case '\n':
				line++;
				break;
				
			case '"':
				string();
				break;
			case '\'':
				character();
				break;
				
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)){
					identifier();
				} else {
					MeowBasic.error(line,  "Unexpected Error");
				}
				break;
		}
		
		
	}
	
	private void identifier() {
		while (isAlphaNumeric(peek())) advance();
		
		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		
		if (type == null) type = IDENTIFIER;
		
		addToken(type);
	}
	
	private void number() {
		TokenType type = INTEGER;
		
		while (isDigit(peek())) advance();
		
		// look for a fractional part
		if (peek() == '.' && isDigit(peekNext())) {
			// set type to DOUBLE
			type = DOUBLE;
			// consume the '.'
			advance();
			
			while (isDigit(peek())) advance();
		}
		
		
		// check for type suffix
		switch (peek()) {
			case 'i':
			case 'I':
				advance();
				addToken(INTEGER, Integer.parseInt(source.substring(start, current - 1)));
				break;
			case 'l':
			case 'L':
				advance();
				addToken(LONG_INTEGER, Long.parseLong(source.substring(start, current - 1)));
				break;
			case 'f':
			case 'F':
				advance();
				addToken(SINGLE_FLOAT, Float.parseFloat(source.substring(start, current - 1)));
				break;
			case 'd':
			case 'D':
				advance();
				addToken(DOUBLE, Double.parseDouble(source.substring(start, current - 1)));
				break;
			default:
				if (type == INTEGER) {
					addToken(INTEGER, Integer.parseInt(source.substring(start, current)));
					break;
				} else if (type == DOUBLE) {
					addToken(DOUBLE, Double.parseDouble(source.substring(start, current)));
					break;
				}
		}
	}
	
	private void string() {
		while (peek()  != '"' && !isAtEnd()) {
			if (peek() == '\n') line++;
			advance();
		}
		
		if (isAtEnd()) {
			MeowBasic.error(line,  "Unterminated string.");
		}
		
		advance(); // closing '"'
		
		// trim the surrounding quotes
		String value = source.substring(start + 1, current - 1);
		addToken(STRING_TYPE, value);
	}
	
	private void character() {
		if (peek() != '\'' && !isAtEnd()) {
			advance();
			if (peekNext() == '\'' && !isAtEnd()) {
				advance();
			} else {
				MeowBasic.error(line,  "Unterminated char.");
			}
		} else {
			MeowBasic.error(line,  "Unterminated char.");
		}
		
		// trim the surrounding quotes
		char value = source.charAt(current - 1);
		addToken(CHARACTER, value);
	}
	
	private boolean match(char expected) {
		if (isAtEnd()) return false;
		if (source.charAt(current) != expected) return false;
		
		current++;
		return true;
	}
	
	private char peek() {
		if (isAtEnd()) return '\0';
		return source.charAt(current);
	}
	
	private char peekNext() {
		if (current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
	
	private boolean isAlpha(char c) {
		return	(c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}
	
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
	
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	private boolean isAtEnd() {
		return current >= source.length();
	}
	
	private char advance() {
		return source.charAt(current++);
	}
	
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
	
	
	
}
