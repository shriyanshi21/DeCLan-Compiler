package edu.depauw.declan.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.depauw.declan.common.ErrorLog;
import edu.depauw.declan.common.Lexer;
import edu.depauw.declan.common.ParseException;
import edu.depauw.declan.common.Parser;
import edu.depauw.declan.common.Position;
import edu.depauw.declan.common.Token;
import edu.depauw.declan.common.TokenType;
import edu.depauw.declan.common.ast.BinaryOperation;
import edu.depauw.declan.common.ast.ConstDecl;
import edu.depauw.declan.common.ast.Identifier;
import edu.depauw.declan.common.ast.NumValue;
import edu.depauw.declan.common.ast.Program;
import edu.depauw.declan.common.ast.Statement;
import edu.depauw.declan.common.ast.UnaryOperation;
import edu.depauw.declan.common.ast.*;



/**
 * A parser for a subset of DeCLan consisting only of integer constant
 * declarations and calls to PrintInt with integer expression arguments. This is
 * starter code for CSC426 Project 2.
 * 
 * @author bhoward
 */
public class MyParser implements Parser {
	private Lexer lexer;
	private ErrorLog errorLog;

	/**
	 * Holds the current Token from the Lexer, or null if at end of file
	 */
	private Token current;

	/**
	 * Holds the Position of the current Token, or the most recent one if at end of
	 * file (or position 0:0 if source file is empty)
	 */
	private Position currentPosition;

	public MyParser(Lexer lexer, ErrorLog errorLog) {
		this.lexer = lexer;
		this.errorLog = errorLog;
		this.current = null;
		this.currentPosition = new Position(0, 0);
		skip();
	}

	@Override
	public void close() {
		lexer.close();
	}

	/**
	 * Check whether the current token will match the given type.
	 * 
	 * @param type
	 * @return true if the TokenType matches the current token
	 */
	boolean willMatch(TokenType type) {
		return current != null && current.getType() == type;
	}

	/**
	 * If the current token has the given type, skip to the next token and return
	 * the matched token. Otherwise, abort and generate an error message.
	 * 
	 * @param type
	 * @return the matched token if successful
	 */
	Token match(TokenType type) {
		if (willMatch(type)) {
			return skip();
		} else if (current == null) {
			errorLog.add("Expected " + type + ", found end of file", currentPosition);
		} else {
			errorLog.add("Expected " + type + ", found " + current.getType(), currentPosition);
		}
		throw new ParseException("Parsing aborted");
	}

	/**
	 * If the current token is null (signifying that there are no more tokens),
	 * succeed. Otherwise, abort and generate an error message.
	 */
	void matchEOF() {
		if (current != null) {
			errorLog.add("Expected end of file, found " + current.getType(), currentPosition);
			throw new ParseException("Parsing aborted");
		}
	}

	/**
	 * Skip to the next token and return the skipped token.
	 * 
	 * @return the skipped token
	 */
	Token skip() {
		Token token = current;
		if (lexer.hasNext()) {
			current = lexer.next();
			currentPosition = current.getPosition();
		} else {
			current = null;
			// keep previous value of currentPosition
		}
		return token;
	}

	// Program -> DeclSequence BEGIN StatementSequence END .
	@Override
	public Program parseProgram() {
		Position start = currentPosition;

		Collection<ConstDecl> constDecls = parseDeclSequence();
		match(TokenType.BEGIN);
		Collection<Statement> statements = parseStatementSequence();
		match(TokenType.END);
		match(TokenType.PERIOD);
		matchEOF();

		return new Program(start, constDecls, statements);
	}

	// DeclSequence -> CONST ConstDeclSequence
	// DeclSequence ->
	//
	// ConstDeclSequence -> ConstDecl ; ConstDeclSequence
	// ConstDeclSequence ->
	private Collection<ConstDecl> parseDeclSequence() {
		List<ConstDecl> constDecls = new ArrayList<>();

		if (willMatch(TokenType.CONST)) {
			skip();

			// FIRST(ConstDecl) = ID
			while (willMatch(TokenType.ID)) {
				ConstDecl constDecl = parseConstDecl();
				constDecls.add(constDecl);

				match(TokenType.SEMI);
			}
		}

		// Return a read-only view of the list of ConstDecl objects
		return Collections.unmodifiableCollection(constDecls);
	}

	// ConstDecl -> ident = number
	private ConstDecl parseConstDecl() {
		Position start = currentPosition;

		Token idTok = match(TokenType.ID);
		Identifier id = new Identifier(idTok.getPosition(), idTok.getLexeme());

		match(TokenType.EQ);

		Token numTok = match(TokenType.NUM);
		NumValue num = new NumValue(numTok.getPosition(), numTok.getLexeme());

		return new ConstDecl(start, id, num);
	}

	// StatementSequence -> Statement StatementSequenceRest
	//
	// StatementSequenceRest -> ; Statement StatementSequenceRest
	// StatementSequenceRest ->

	
	private Collection<Statement> parseStatementSequence() {
		
		List <Statement> statements = new ArrayList<>();
		
		Statement newStatement = parseStatement();
		statements.add(newStatement);
		
		while(willMatch(TokenType.SEMI)){
			match(TokenType.SEMI);
			
			newStatement = parseStatement();
			statements.add(newStatement);
		    }
		
		return Collections.unmodifiableCollection(statements);
		
	}
	
	
	// Handle rest of the grammar
	
	private Statement parseStatement(){
		Position start = currentPosition;
		if(willMatch(TokenType.ID)){
			return parseProcedureCall();
		}
		
		return new EmptyStatement(start);
	    }
	
	private Statement parseProcedureCall(){
		Position start = currentPosition;
		Token idTok = match(TokenType.ID);
		
		Identifier id = new Identifier(idTok.getPosition(), idTok.getLexeme());
		match(TokenType.LPAR);
		Expression express = parseExpression();
		match(TokenType.RPAR);
		return new ProcedureCall(start, id, express);
		
	}
	

	
	private Expression parseExpression(){
		Expression express = null;
		Position start = currentPosition;
		
		if(willMatch(TokenType.PLUS)){
			match(TokenType.PLUS); //assigning something to the expression
			Expression newTerm = parseTerm(); //grab term 
			express = new UnaryOperation(start, UnaryOperation.OpType.PLUS, newTerm); //Left hand expression 
			
		}
		
		else if(willMatch(TokenType.MINUS)){ //assigning something to expression 
			match(TokenType.MINUS);
			Expression newTerm = parseTerm();
			express = new UnaryOperation(start, UnaryOperation.OpType.MINUS, newTerm);
			
		}
		else {
			express = parseTerm(); // express is a result of parse-Term
			
		}
		while (willMatch(TokenType.PLUS) || willMatch(TokenType.MINUS)) {
		BinaryOperation.OpType addOp = parseAddOp(); // No separate method case
		Expression right = parseTerm();
		express = new BinaryOperation(start, express, addOp, right);
		
		}
		return express;
		
	}
	
	private BinaryOperation.OpType parseAddOp() {
		if(willMatch(TokenType.PLUS)) {
			skip();
			return BinaryOperation.OpType.PLUS;
		} else {
			match(TokenType.MINUS);
			return BinaryOperation.OpType.MINUS;
		}
	} 
	
	private List<Expression> parseExpressionList(){
	    List<Expression> expList = new ArrayList<Expression>();
	    Expression exp = parseExpression();
	    expList.add(exp);
	    while(willMatch(TokenType.COMMA)){
	      skip();
	      exp = parseExpression();
	      expList.add(exp);
	    }
	    return Collections.unmodifiableList(expList);
	  }
	  //ActualParameters -> ( ExpList )
	  //ActualParameters -> ( )
	  private List<Expression> parseActualParameters(){
	    match(TokenType.LPAR);
	    List<Expression> elist = new ArrayList<>();
	    if(!willMatch(TokenType.RPAR)){
	      elist = parseExpressionList();
	    }
	    match(TokenType.RPAR);
	    return Collections.unmodifiableList(elist);
	  }
	
	
	private Expression parseTerm() {
		Expression left = parseFactor();
		Position start = currentPosition;
		while (willMatch(TokenType.DIV) || willMatch(TokenType.MOD) || willMatch(TokenType.TIMES) || willMatch(TokenType.DIVIDE)) {
			BinaryOperation.OpType op = parseMultOp();
			Expression right = parseFactor();
			left = new BinaryOperation(start, left, op, right);
		}
		return left;
	}
	
	private BinaryOperation.OpType parseMultOp() {
	    if(willMatch(TokenType.TIMES)){
	      skip();
	      return BinaryOperation.OpType.TIMES;
	    } else if(willMatch(TokenType.DIV)) {
	      skip();
	      return BinaryOperation.OpType.DIV;
	    } else {
	      match(TokenType.MOD);
	      return BinaryOperation.OpType.MOD;
	    }
	  }
	
	private Expression parseFactor(){
	    if(willMatch(TokenType.NUM)){
	      return parseNumValue(); 
	      } else if (willMatch(TokenType.ID)){
	    	 Token id = skip();
	         Position start = currentPosition;
	         if(willMatch(TokenType.LPAR)){
	    	 List<Expression> expList = parseActualParameters();
	    	 return new ProcedureCall(start, id, express);
	          } else {
	    	return parseIdentifier(id);
	          }
	      } else {
	      match(TokenType.LPAR);
	      Expression expr = parseExpression();
	      match(TokenType.RPAR);
	      return expr;
	    }
	  }
	
	//number -> NUM
	  private NumValue parseNumValue() {
	    Token num = match(TokenType.NUM);
	    Position start = currentPosition;
	    return new NumValue(start, num.getLexeme());
	  }
	  
	//ident -> IDENT 
	  private Identifier parseIdentifier() {
	    Token id = match(TokenType.ID);
	    Position start = currentPosition;
	    return new Identifier(start, id.getLexeme());
	  }
	  //ident -> IDENT 
	  private Identifier parseIdentifier(Token id) {
	    Position start = currentPosition;
	    return new Identifier(start, id.getLexeme());
	  }
	  
}




