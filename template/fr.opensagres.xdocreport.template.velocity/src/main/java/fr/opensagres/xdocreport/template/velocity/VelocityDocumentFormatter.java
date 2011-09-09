/**
 * Copyright (C) 2011 Angelo Zerr <angelo.zerr@gmail.com> and Pascal Leclercq <pascal.leclercq@gmail.com>
 *
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package fr.opensagres.xdocreport.template.velocity;

import java.util.Stack;

import fr.opensagres.xdocreport.core.utils.StringUtils;
import fr.opensagres.xdocreport.template.formatter.AbstractDocumentFormatter;
import fr.opensagres.xdocreport.template.formatter.LoopDirective;

/**
 * Velocity document formatter used to format fields list with Velocity syntax.
 * 
 */
public class VelocityDocumentFormatter extends AbstractDocumentFormatter {

	protected static final String ITEM_TOKEN = "$item_";
	protected static final String ITEM_TOKEN_OPEN_BRACKET = "${item_";

	private static final String START_FOREACH_DIRECTIVE = "#foreach(";
	private static final String IN_DIRECTIVE = " in ";
	private static final String END_FOREACH_DIRECTIVE = "#end";
	private static final String DOLLAR_TOTKEN = "$";
	private static final String OPEN_BRACKET_TOTKEN = "{";

	private final static int START_WITH_DOLLAR = 1;
	private final static int START_WITH_DOLLAR_AND_BRACKET = 2;
	private final static int NO_VELOCITY_FIELD = 3;

	private static final String START_IMAGE_DIRECTIVE = "${"
			+ IMAGE_REGISTRY_KEY + ".registerImage(";
	private static final String END_IMAGE_DIRECTIVE = ")}";

	private static final String START_IF = "#if( ";
	private static final String END_IF = "#end";

	private static final String VELOCITY_COUNT = "$velocityCount";

	public String formatAsFieldItemList(String content, String fieldName,
			boolean forceAsField) {
		int type = getModelFieldType(content, fieldName);
		switch (type) {
		case START_WITH_DOLLAR:
			return StringUtils.replaceAll(content, DOLLAR_TOTKEN + fieldName,
					getItemToken() + fieldName);
		case START_WITH_DOLLAR_AND_BRACKET:
			return StringUtils.replaceAll(content, DOLLAR_TOTKEN
					+ OPEN_BRACKET_TOTKEN + fieldName,
					getItemTokenOpenBracket() + fieldName);
		default:
			if (forceAsField) {
				return getItemToken() + content;
			}
			break;
		}
		return content;
	}

	public String getStartLoopDirective(String itemNameList, String listName) {
		StringBuilder result = new StringBuilder(START_FOREACH_DIRECTIVE);
		if (!itemNameList.startsWith(DOLLAR_TOTKEN)) {
			result.append(DOLLAR_TOTKEN);
		}
		result.append(itemNameList);
		result.append(IN_DIRECTIVE);
		if (!listName.startsWith(DOLLAR_TOTKEN)) {
			result.append(DOLLAR_TOTKEN);
		}
		result.append(listName);
		result.append(')');
		return result.toString();
	}

	public String getEndLoopDirective(String itemNameList) {
		return END_FOREACH_DIRECTIVE;
	}

	@Override
	protected boolean isModelField(String content, String fieldName) {
		return getModelFieldType(content, fieldName) != NO_VELOCITY_FIELD;
	}

	private int getModelFieldType(String content, String fieldName) {
		if (StringUtils.isEmpty(content)) {
			return NO_VELOCITY_FIELD;
		}
		int dollarIndex = content.indexOf(DOLLAR_TOTKEN);
		if (dollarIndex == -1) {
			// Not velocity field
			return NO_VELOCITY_FIELD;
		}
		int fieldNameIndex = content.indexOf(fieldName);
		if (fieldNameIndex == -1) {
			return NO_VELOCITY_FIELD;
		}
		if (fieldNameIndex == dollarIndex + 1) {
			// ex : $name
			return START_WITH_DOLLAR;
		}
		if (fieldNameIndex == dollarIndex + 2) {
			if (content.charAt(fieldNameIndex - 1) == '{') {
				// ex : ${name}
				return START_WITH_DOLLAR_AND_BRACKET;
			}
		}
		// Not velocity field
		return NO_VELOCITY_FIELD;
	}

	@Override
	protected String getItemToken() {
		return ITEM_TOKEN;
	}

	protected String getItemTokenOpenBracket() {
		return ITEM_TOKEN_OPEN_BRACKET;
	}

	public String getImageDirective(String fieldName) {
		StringBuilder directive = new StringBuilder(START_IMAGE_DIRECTIVE);
		if (!fieldName.startsWith("$")) {
			directive.append("$");
		}
		directive.append(fieldName);
		directive.append(END_IMAGE_DIRECTIVE);
		return directive.toString();
	}

	public String formatAsSimpleField(boolean encloseInDirective,
			String... fields) {
		StringBuilder field = new StringBuilder();
		if (encloseInDirective) {
			field.append(DOLLAR_TOTKEN);
		}
		for (int i = 0; i < fields.length; i++) {
			if (i == 0) {
				field.append(fields[i]);
			} else {
				field.append('.');
				String f = fields[i];
				field.append(f.substring(0, 1).toUpperCase());
				field.append(f.substring(1, f.length()));
			}
		}
		return field.toString();
	}

	public String getStartIfDirective(String fieldName) {
		StringBuilder directive = new StringBuilder(START_IF);
		if (!fieldName.startsWith(DOLLAR_TOTKEN)) {
			directive.append(DOLLAR_TOTKEN);
		}
		directive.append(fieldName);
		directive.append(')');
		return directive.toString();
	}

	public String getEndIfDirective(String fieldName) {
		return END_IF;
	}

	public String getLoopCountDirective(String fieldName) {
		return VELOCITY_COUNT;
	}

	public boolean containsInterpolation(String content) {
		if (StringUtils.isEmpty(content)) {
			return false;
		}
		int dollarIndex = content.indexOf(DOLLAR_TOTKEN);
		if (dollarIndex == -1) {
			// Not included to FM directive
			return false;
		}
		return true;
	}

	public int extractListDirectiveInfo(String content,
			Stack<LoopDirective> directives, boolean dontRemoveListDirectiveInfo) {
		// content='xxxx#foreach($d in $developers)yyy'
		int startOfEndListDirectiveIndex = content
				.indexOf(END_FOREACH_DIRECTIVE);
		int startOfStartListDirectiveIndex = content
				.indexOf(START_FOREACH_DIRECTIVE);
		if (startOfStartListDirectiveIndex == -1
				&& startOfEndListDirectiveIndex == -1) {
			return 0;
		}

		if (startOfStartListDirectiveIndex == -1
				|| (startOfEndListDirectiveIndex != -1 && startOfStartListDirectiveIndex > startOfEndListDirectiveIndex)) {
			// content contains (at first #end)
			if (!dontRemoveListDirectiveInfo && !directives.isEmpty()) {
				// remove the LoopDirective from the stack
				directives.pop();
			}
			// get content after the #end
			String afterEndList = content.substring(
					END_FOREACH_DIRECTIVE.length()
							+ startOfEndListDirectiveIndex, content.length());
			int nbLoop = -1;
			// parse the content after the #end
			nbLoop += extractListDirectiveInfo(afterEndList, directives);
			return nbLoop;
		}

		// contentWichStartsWithList='#foreach($d in $developers)yyy'
		String contentWhichStartsWithList = content.substring(
				startOfStartListDirectiveIndex, content.length());
		int endOfStartListDirectiveIndex = contentWhichStartsWithList
				.indexOf(')');
		if (endOfStartListDirectiveIndex == -1) {
			// [#list not closed with ')'
			return 0;
		}
		// startLoopDirective='#foreach($d in $developers)'
		String startLoopDirective = contentWhichStartsWithList.substring(0,
				endOfStartListDirectiveIndex + 1);
		// insideLoop='developers as d]'
		String insideLoop = startLoopDirective.substring(
				START_FOREACH_DIRECTIVE.length(), startLoopDirective.length());
		int indexBeforeIn = insideLoop.indexOf(" ");
		if (indexBeforeIn == -1) {
			return 0;
		}

		// afterItem=' in $developers]'
		String afterItem = insideLoop.substring(indexBeforeIn,
				insideLoop.length());
		int indexAfterIn = afterItem.indexOf(IN_DIRECTIVE);
		if (indexAfterIn == -1) {
			return 0;
		}

		// item='$d'
		String item = insideLoop.substring(0, indexBeforeIn).trim();
		// remove $
		// item='d'
		if (item.startsWith(DOLLAR_TOTKEN)) {
			item = item.substring(1, item.length());
		}
		if (StringUtils.isEmpty(item)) {
			return 0;
		}
		// afterIn='$developers)'
		String afterIn = afterItem.substring(IN_DIRECTIVE.length(),
				afterItem.length());
		int endListIndex = afterIn.indexOf(')');
		if (endListIndex == -1) {
			return 0;
		}
		// sequence='$developers'
		String sequence = afterIn.substring(0, endListIndex).trim();
		// remove $
		// item='d'
		if (sequence.startsWith(DOLLAR_TOTKEN)) {
			sequence = sequence.substring(1, sequence.length());
		}
		if (StringUtils.isEmpty(sequence)) {
			return 0;
		}

		int nbLoop = 1;
		directives.push(new LoopDirective(startLoopDirective,
				getEndLoopDirective(null), sequence, item));

		// afterList = 'yyy'
		String afterList = content.substring(startOfStartListDirectiveIndex
				+ startLoopDirective.length(), content.length());
		nbLoop += extractListDirectiveInfo(afterList, directives);
		return nbLoop;
	}

	public String extractModelTokenPrefix(String fieldName) {
		// fieldName = '$developers.Name'
		if (fieldName == null) {
			return null;
		}
		int dollarIndex = fieldName.indexOf(DOLLAR_TOTKEN);
		if (dollarIndex == -1) {
			return null;
		}
		int endIndex = fieldName.indexOf(' ');
		if (endIndex != -1) {
			fieldName = fieldName.substring(0, fieldName.length());
		}
		// fieldNameWithoutDollar='developers.Name'
		String fieldNameWithoutDollar = fieldName.substring(dollarIndex
				+ DOLLAR_TOTKEN.length(), fieldName.length());
		int lastDotIndex = fieldNameWithoutDollar.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return fieldNameWithoutDollar;
		}
		// fieldNameWithoutDollar='developers'
		return fieldNameWithoutDollar.substring(0, lastDotIndex);
	}
}
