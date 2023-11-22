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
				} else {
					MeowBasic.error(line,  "Unexpected Error");
				}
				break;
		}
		
		
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
				addToken(LONG, Long.parseLong(source.substring(start, current - 1)));
				break;
			case 'f':
			case 'F':
				advance();
				addToken(SINGLE, Float.parseFloat(source.substring(start, current - 1)));
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
		addToken(STRING, value);
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
