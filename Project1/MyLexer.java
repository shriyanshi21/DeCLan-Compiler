package edu.depauw.declan;

import java.util.NoSuchElementException;

import edu.depauw.declan.common.Lexer;
import edu.depauw.declan.common.Position;
import edu.depauw.declan.common.Source;
import edu.depauw.declan.common.Token;
import edu.depauw.declan.common.TokenFactory;
import edu.depauw.declan.common.TokenType;

public class MyLexer implements Lexer {
	private Source source;
	private TokenFactory tokenFactory;
	private Token nextToken;

	public MyLexer(Source source, TokenFactory tokenFactory) {
		this.source = source;
		this.tokenFactory = tokenFactory;
		this.nextToken = null;
	}

	public boolean hasNext() {
		if (nextToken == null) {
			scanNext();
		}

		return nextToken != null;
	}

	public Token next() {
		if (nextToken == null) {
			scanNext();
		}

		if (nextToken == null) {
			throw new NoSuchElementException("No more tokens");
		}

		Token result = nextToken;
		nextToken = null;
		return result;
	}

	public void close() {
		source.close();
	}

	private static enum State {
		INIT, IDENT, COLON, SEMI, PERIOD, COMMA, NUM, LPAR, STRING, COMMENT, LT, GT, STAR
		
		// TODO add more states here
	}

	/**
	 * Scan through characters from source, starting with the current one, to find
	 * the next token. If found, store it in nextToken and leave the source on the
	 * next character after the token. If no token found, set nextToken to null.
	 */
	private void scanNext() {
		State state = State.INIT;
		StringBuilder lexeme = new StringBuilder();
		Position position = null;

		while (!source.atEOF()) {
			char c = source.current();

			switch (state) {
			case INIT:
				// Look for the start of a token
				if (Character.isWhitespace(c)) {
					source.advance();
					continue;
					
				} else if (Character.isLetter(c)) {
					state = State.IDENT;
					lexeme.append(c);
					// Record starting position of identifier or keyword token
					position = source.getPosition();
					source.advance();
					continue;
					
				} else if (c == ':') {
					state = state.COLON;
					position = source.getPosition();
					source.advance();
					continue;
					
				} else if (c == '=') {
					position = source.getPosition();
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.EQ, position);
				    return;
				    
				} else if (c == ';') {
				    position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.SEMI, position);
				    return;
				    
				} else if (c == '#') {
					position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.NE, position);
				    return;
				    
				} else if (c == '+') {
                    position = source.getPosition();
                    source.advance();
                    nextToken = tokenFactory.makeToken(TokenType.PLUS, position);
                    return;
                    
				} else if (c == '-') {
					position = source.getPosition();
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.MINUS, position);
					return;
					
				} else if (c == '/') {
					position = source.getPosition();
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.DIVIDE, position);
					return;
					
				} else if (c == '&') {
					position = source.getPosition();
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.AND, position);
					return;
					
				} else if (c == '~'){
					position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.NOT, position);
				    return;
				    
				} else if (c == ',') {
				    position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.COMMA, position);
				    return;	  
				    
				} else if (c == '.') {
				    position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.PERIOD, position);
				    return; 
				    
				} else if (c== '(') {
				    state = state.LPAR;
				    position = source.getPosition();
				    source.advance();
				    continue;
				    
				} else if (c == ')') {
					position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.RPAR, position);
				    return;
				    
				} else if (c == '<') {
					state = State.LT;
					position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.LT, position);
				    continue;
				    
				} else if (c == '>') {
					state = State.GT;
					position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.GT, position);
				    continue;
				    
				} else if (Character.isDigit(c)){
					state = State.NUM;
					lexeme.append(c);
					position = source.getPosition();
					source.advance();	
					continue;
					
				} else if (c == '*') {
					position = source.getPosition();
				    source.advance();
				    nextToken = tokenFactory.makeToken(TokenType.TIMES, position);
				    return;
					
				} else if (c == '"'){
					state = state.STRING;
					position = source.getPosition();
					source.advance();	
					continue;
					
				} else {
					position = source.getPosition();
					System.err.println("Unrecognized character " + c + " at " + position);
					source.advance();
					continue;
				}
				
			case IDENT:
				// Handle next character of an identifier or keyword
				if (Character.isLetterOrDigit(c)) {
					lexeme.append(c);
					source.advance();
					continue;
				} else {
					nextToken = tokenFactory.makeIdToken(lexeme.toString(), position);
					return;
				}
			
			case COLON:
				// Check for : vs :=
				if (c == '=') {
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.ASSIGN, position);
					return;
				} else {
					nextToken = tokenFactory.makeToken(TokenType.COLON, position);
					return;
				}
				
			case LT:
			   // Check for < vs <=
				if (c == '<') {
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.LE,position);
					return;
					} else {
					nextToken = tokenFactory.makeToken(TokenType.LT,position);
					return;
				}
				
			   
			case GT:
				   // Check for > vs >=
				if (c == '=') {
					source.advance();
					nextToken = tokenFactory.makeToken(TokenType.GE, position);
					return;
					} else {
					nextToken = tokenFactory.makeToken(TokenType.GT, position);
					return;
				}
				    
			case NUM: 
				
				if (Character.isDigit(c)){
					lexeme.append(c);
					source.advance();
					continue;
				} else {
					nextToken = tokenFactory.makeNumToken(lexeme.toString(), position);
					return;
				}
				
			case LPAR:
				
				if (c=='*'){
					state = State.COMMENT;
					continue;
				} else {
					nextToken = tokenFactory.makeToken(TokenType.LPAR, position);
					return;
				}
				
				
				
			case STRING: 
			
				if(c == '"'){
				source.advance();
				nextToken = tokenFactory.makeStringToken(lexeme.toString(), position);
				return;	
				} else {
					lexeme.append(c);
					source.advance();
					continue;
				}
				
			case STAR:
				 if (c== ')'){
					 state = State.INIT;
					 source.advance();
					 continue;
				 } else if (c=='*') {
					 state = State.STAR;
					 source.advance();
					 continue;
				 } else {
					 state = State.COMMENT;
					 source.advance();
					 continue;
				 }
			case COMMENT:
				source.advance();
				if (c== '*'){
					state = State.STAR;
					continue;
				}
				continue;
				
			}
		}

		// Clean up at end of source
		switch (state) {
		case INIT:
			// No more tokens found
			nextToken = null;
			return;
			
		case IDENT:
			// Successfully ended an identifier or keyword
			nextToken = tokenFactory.makeIdToken(lexeme.toString(), position);
			return;
			
		case COLON:
			// Final token was :
			nextToken = tokenFactory.makeToken(TokenType.COLON, position);
			return;
			
		case NUM:
			nextToken = tokenFactory.makeToken(TokenType.NUM, position);
			return;
			
		case GT: 
			nextToken = tokenFactory.makeToken(TokenType.GT, position);
			return;
			
		case LT: 
			nextToken = tokenFactory.makeToken(TokenType.LT, position);
			return;
	
		case STRING: 
			nextToken = tokenFactory.makeToken(TokenType.STRING, position);
			return;
		}
	}
}
