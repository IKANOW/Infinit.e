/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.api.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.LinkedList;

public class SimpleBooleanParser {

	public static class SimpleBooleanParserMTree {
		public char op = '\0';
		public int nTerm = 0;
		public boolean bStartedWithPara = false;
		public boolean bNegated = false;
		public LinkedList<SimpleBooleanParserMTree> terms = null;
		public SimpleBooleanParserMTree(boolean bPara, boolean bNeg) 
		{ terms = new LinkedList<SimpleBooleanParserMTree>(); bStartedWithPara = bPara; bNegated = bNeg; };
		public SimpleBooleanParserMTree(int nTerm_) { nTerm = nTerm_; };
		
	}
	public static SimpleBooleanParserMTree parseExpression(String sLogic) {
		SimpleBooleanParserMTree start = new SimpleBooleanParserMTree(false, false);
		SimpleBooleanParserMTree curr = start;
		
		Reader in = new StringReader(sLogic);
		StreamTokenizer tok = new StreamTokenizer(in);
		tok.lowerCaseMode(true);
		tok.parseNumbers();
		
		LinkedList<SimpleBooleanParserMTree> treeNodeStack = new LinkedList<SimpleBooleanParserMTree>();
		boolean bNegate = false;
		
		try {
			for (int nType = tok.nextToken(); nType != StreamTokenizer.TT_EOF; nType = tok.nextToken()) {
				switch (nType) {
				
				case (int)'(':
					// Create a new node
					treeNodeStack.push(curr);
					curr.terms.push((curr = new SimpleBooleanParserMTree(true, bNegate)));
					bNegate = false;
					break;
				
				case (int)')':
					if (bNegate) {
						return null; // (error)
					}
					//if no operator set to AND, pop from stack
					boolean bExit = false;
					for (; !bExit && (null != curr);) {
						if ('\0' == curr.op) {
							curr.op = '&';
						}
						bExit = curr.bStartedWithPara; 
						// (else haven't reached the node with the ( yet)
						if (!treeNodeStack.isEmpty()) {
							curr = treeNodeStack.pop();
						}
						else {
							curr = null;
						}
					}
					if ((null != curr) && ('\0' == curr.op)) {
						curr.op = '&';
					}
					break;
				
				case StreamTokenizer.TT_NUMBER:
					if (bNegate) {
						curr.terms.push(new SimpleBooleanParserMTree(-1*(int)tok.nval));
						bNegate = false;
					}
					else {
						curr.terms.push(new SimpleBooleanParserMTree((int)tok.nval));						
					}					
					break;
					//TESTED: bNegate for numbers in parser5
					
				case StreamTokenizer.TT_WORD: // "and" or "or"
					if (bNegate) {
						return null; // (error)
					}
					// If no operator specified, specify operator
					if (tok.sval.equals("not")) {
						bNegate = true;
					}//TESTED parser5
					else if ('\0' == curr.op) {
						if (tok.sval.equals("and")) {
							curr.op = '&';
						}
						else if (tok.sval.equals("or")) {
							curr.op = '|';
						}
						else {
							return null;
						}
					}//TESTED parser1
					else if (('&' == curr.op) && tok.sval.equals("or")) {
						// Operator has changed to one of lower precedence
						// Basically the same as if the current expression had brackets...
						// So pop the current expression down and replace it with an OR
						SimpleBooleanParserMTree parent = treeNodeStack.peek();
						if (null != parent) {
							SimpleBooleanParserMTree child = parent.terms.pop(); // (pulls curr, now==child, off parent)
							parent.terms.push((curr = new SimpleBooleanParserMTree(false, false)));
							curr.terms.push(child);
						}//TESTED parser2
						else { // curr is start
							start = new SimpleBooleanParserMTree(false, false);
							start.terms.push(curr);
							curr = start;
						}//TESTED parser7
						curr.op = '|';
					}
					else if (('|' == curr.op) && tok.sval.equals("and")) {
						// Operator has changed to one of higher precedence
						// Basically the same as if the "future" expression had brackets
						int nOldTerm = curr.terms.pop().nTerm;
						treeNodeStack.push(curr);
						curr.terms.push((curr = new SimpleBooleanParserMTree(false, false)));
						curr.op = '&';
						curr.terms.push(new SimpleBooleanParserMTree(nOldTerm));
					}//TESTED parser2
					// else nothing to do
					break;
				}
			}
		}
		catch (IOException e) {
			return null;
		}
		return start;
	}//TOTEST
	
	public static String traverse(SimpleBooleanParserMTree tree, boolean bNewLines) {
		int nNodeCount = 1;
		StringBuffer sb = new StringBuffer();
		
		if (null == tree.terms) {
			sb.append("(").append(tree.nTerm).append(" )");
			return sb.toString();
		}
		
		LinkedList<SimpleBooleanParserMTree> stack = new LinkedList<SimpleBooleanParserMTree>();
		stack.push(tree);
		for (;!stack.isEmpty();) { // Loop over tree nodes
			SimpleBooleanParserMTree node = stack.pop();
			sb.append('$').append(node.nTerm).append(": ");
			if (node.bNegated) {
				sb.append('-');
			}
			sb.append(node.op).append(' ');
			sb.append("(");
			for (SimpleBooleanParserMTree child: node.terms) {
				if (null == child.terms) {
					sb.append(child.nTerm).append(' ');
				}
				else {
					child.nTerm = nNodeCount++;
					sb.append('$').append(child.nTerm).append(' '); 
					stack.push(child);
				}
			}
			sb.append(")");
			if (bNewLines) {
				sb.append("\n");
			}
			else {
				sb.append(" ");
			}
		}
		return sb.toString();
	}//TOTEST
}
